package com.lframework.xingyun.sc.impl.stock;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.sc.dto.stock.ProductStockPendingCostResolveDto;
import com.lframework.xingyun.sc.entity.ProductStockPendingCost;
import com.lframework.xingyun.sc.entity.ProductStockPendingCostSettle;
import com.lframework.xingyun.sc.entity.RetailOutSheetDetailLot;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetailLot;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.ProductStockPendingCostStatus;
import com.lframework.xingyun.sc.enums.StockCostStatus;
import com.lframework.xingyun.sc.mappers.ProductStockPendingCostMapper;
import com.lframework.xingyun.sc.mappers.ProductStockPendingCostSettleMapper;
import com.lframework.xingyun.sc.service.retail.RetailOutSheetDetailLotService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailLotService;
import com.lframework.xingyun.sc.service.stock.ProductStockPendingCostService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockPendingCostServiceImpl
    extends BaseMpServiceImpl<ProductStockPendingCostMapper, ProductStockPendingCost>
    implements ProductStockPendingCostService {

  @Autowired
  private SaleOutSheetDetailLotService saleOutSheetDetailLotService;

  @Autowired
  private RetailOutSheetDetailLotService retailOutSheetDetailLotService;

  @Autowired
  private ProductStockPendingCostSettleMapper productStockPendingCostSettleMapper;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void create(String scId, String productId, String outBizId, String outBizDetailId,
      ProductStockBizType outBizType, String lotId, BigDecimal pendingNum, LocalDateTime outTime) {

    Assert.notBlank(scId);
    Assert.notBlank(productId);
    Assert.notNull(outBizType);
    Assert.greaterThanZero(pendingNum);

    ProductStockPendingCost record = new ProductStockPendingCost();
    record.setId(IdUtil.getId());
    record.setScId(scId);
    record.setProductId(productId);
    record.setOutBizId(outBizId);
    record.setOutBizDetailId(outBizDetailId);
    record.setOutBizType(outBizType);
    record.setLotId(lotId);
    record.setOutTime(outTime == null ? LocalDateTime.now() : outTime);
    record.setPendingNum(pendingNum);
    record.setSettledNum(BigDecimal.ZERO);
    record.setSettledTaxAmount(BigDecimal.ZERO);
    record.setStatus(ProductStockPendingCostStatus.PENDING);

    getBaseMapper().insert(record);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ProductStockPendingCostResolveDto settle(String scId, String productId, BigDecimal inNum,
      BigDecimal taxAmount, String inBizId, String inBizDetailId, ProductStockBizType inBizType) {

    Assert.notBlank(scId);
    Assert.notBlank(productId);
    Assert.greaterThanZero(inNum);

    ProductStockPendingCostResolveDto result = new ProductStockPendingCostResolveDto();
    result.setSettledNum(BigDecimal.ZERO);
    result.setSettledTaxAmount(BigDecimal.ZERO);
    result.setRemainNum(inNum);
    result.setRemainTaxAmount(taxAmount == null ? BigDecimal.ZERO : NumberUtil.getNumber(taxAmount, 2));

    BigDecimal taxPrice = NumberUtil.equal(inNum, BigDecimal.ZERO) || taxAmount == null
        ? BigDecimal.ZERO
        : NumberUtil.getNumber(NumberUtil.div(taxAmount, inNum), 6);

    Wrapper<ProductStockPendingCost> queryWrapper = Wrappers.lambdaQuery(ProductStockPendingCost.class)
        .eq(ProductStockPendingCost::getScId, scId)
        .eq(ProductStockPendingCost::getProductId, productId)
        .ne(ProductStockPendingCost::getStatus, ProductStockPendingCostStatus.FINISHED)
        .orderByAsc(ProductStockPendingCost::getOutTime)
        .orderByAsc(ProductStockPendingCost::getId);
    List<ProductStockPendingCost> records = this.list(queryWrapper);

    for (ProductStockPendingCost record : records) {
      if (NumberUtil.le(result.getRemainNum(), BigDecimal.ZERO)) {
        break;
      }

      BigDecimal unresolvedNum = NumberUtil.sub(record.getPendingNum(),
          record.getSettledNum() == null ? BigDecimal.ZERO : record.getSettledNum());
      if (NumberUtil.le(unresolvedNum, BigDecimal.ZERO)) {
        continue;
      }

      BigDecimal settleNum = NumberUtil.min(unresolvedNum, result.getRemainNum());
      BigDecimal settleTaxAmount = NumberUtil.getNumber(NumberUtil.mul(taxPrice, settleNum), 2);

      record.setSettledNum(NumberUtil.add(defaultValue(record.getSettledNum()), settleNum));
      record.setSettledTaxAmount(
          NumberUtil.add(defaultValue(record.getSettledTaxAmount()), settleTaxAmount));

      BigDecimal remainPendingNum = NumberUtil.sub(record.getPendingNum(), record.getSettledNum());
      record.setStatus(NumberUtil.le(remainPendingNum, BigDecimal.ZERO)
          ? ProductStockPendingCostStatus.FINISHED
          : ProductStockPendingCostStatus.PARTIAL);
      this.updateById(record);

      ProductStockPendingCostSettle settle = new ProductStockPendingCostSettle();
      settle.setId(IdUtil.getId());
      settle.setPendingId(record.getId());
      settle.setInBizId(inBizId);
      settle.setInBizDetailId(inBizDetailId);
      settle.setInBizType(inBizType);
      settle.setSettleNum(settleNum);
      settle.setSettleTaxAmount(settleTaxAmount);
      productStockPendingCostSettleMapper.insert(settle);

      updateLotCost(record, settleNum, settleTaxAmount);

      result.setSettledNum(NumberUtil.add(result.getSettledNum(), settleNum));
      result.setSettledTaxAmount(NumberUtil.add(result.getSettledTaxAmount(), settleTaxAmount));
      result.setRemainNum(NumberUtil.sub(result.getRemainNum(), settleNum));
      result.setRemainTaxAmount(
          NumberUtil.getNumber(NumberUtil.sub(result.getRemainTaxAmount(), settleTaxAmount), 2));
    }

    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public BigDecimal rollback(String inBizDetailId, ProductStockBizType inBizType) {

    Assert.notBlank(inBizDetailId);
    Assert.notNull(inBizType);

    Wrapper<ProductStockPendingCostSettle> queryWrapper = Wrappers.lambdaQuery(ProductStockPendingCostSettle.class)
        .eq(ProductStockPendingCostSettle::getInBizDetailId, inBizDetailId)
        .eq(ProductStockPendingCostSettle::getInBizType, inBizType);
    List<ProductStockPendingCostSettle> settles = productStockPendingCostSettleMapper.selectList(queryWrapper);
    if (CollectionUtil.isEmpty(settles)) {
      return BigDecimal.ZERO;
    }

    BigDecimal rollbackTaxAmount = BigDecimal.ZERO;
    for (ProductStockPendingCostSettle settle : settles) {
      ProductStockPendingCost pending = this.getById(settle.getPendingId());
      if (pending != null) {
        pending.setSettledNum(NumberUtil.sub(defaultValue(pending.getSettledNum()), settle.getSettleNum()));
        pending.setSettledTaxAmount(
            NumberUtil.sub(defaultValue(pending.getSettledTaxAmount()), settle.getSettleTaxAmount()));

        BigDecimal remainPendingNum = NumberUtil.sub(pending.getPendingNum(), pending.getSettledNum());
        if (NumberUtil.le(pending.getSettledNum(), BigDecimal.ZERO)) {
          pending.setSettledNum(BigDecimal.ZERO);
          pending.setSettledTaxAmount(BigDecimal.ZERO);
          pending.setStatus(ProductStockPendingCostStatus.PENDING);
        } else {
          pending.setStatus(NumberUtil.le(remainPendingNum, BigDecimal.ZERO)
              ? ProductStockPendingCostStatus.FINISHED
              : ProductStockPendingCostStatus.PARTIAL);
        }
        this.updateById(pending);

        rollbackLotCost(pending, settle.getSettleNum(), settle.getSettleTaxAmount());
      }

      rollbackTaxAmount = NumberUtil.add(rollbackTaxAmount, settle.getSettleTaxAmount());
      productStockPendingCostSettleMapper.deleteById(settle.getId());
    }

    return rollbackTaxAmount;
  }

  private void updateLotCost(ProductStockPendingCost record, BigDecimal settleNum,
      BigDecimal settleTaxAmount) {

    if (record.getLotId() == null) {
      return;
    }

    if (record.getOutBizType() == ProductStockBizType.SALE) {
      SaleOutSheetDetailLot lot = saleOutSheetDetailLotService.getById(record.getLotId());
      if (lot == null) {
        return;
      }

      lot.setCostTaxAmount(NumberUtil.add(defaultValue(lot.getCostTaxAmount()), settleTaxAmount));
      lot.setSettledCostNum(NumberUtil.add(defaultValue(lot.getSettledCostNum()), settleNum));
      lot.setCostStatus(NumberUtil.equal(lot.getSettledCostNum(), lot.getOrderNum())
          ? StockCostStatus.FINAL
          : StockCostStatus.PARTIAL);
      saleOutSheetDetailLotService.updateById(lot);
      return;
    }

    if (record.getOutBizType() == ProductStockBizType.RETAIL) {
      RetailOutSheetDetailLot lot = retailOutSheetDetailLotService.getById(record.getLotId());
      if (lot == null) {
        return;
      }

      lot.setCostTaxAmount(NumberUtil.add(defaultValue(lot.getCostTaxAmount()), settleTaxAmount));
      lot.setSettledCostNum(NumberUtil.add(defaultValue(lot.getSettledCostNum()), settleNum));
      lot.setCostStatus(NumberUtil.equal(lot.getSettledCostNum(), lot.getOrderNum())
          ? StockCostStatus.FINAL
          : StockCostStatus.PARTIAL);
      retailOutSheetDetailLotService.updateById(lot);
    }
  }

  private void rollbackLotCost(ProductStockPendingCost record, BigDecimal settleNum,
      BigDecimal settleTaxAmount) {

    if (record.getLotId() == null) {
      return;
    }

    if (record.getOutBizType() == ProductStockBizType.SALE) {
      SaleOutSheetDetailLot lot = saleOutSheetDetailLotService.getById(record.getLotId());
      if (lot == null) {
        return;
      }

      lot.setCostTaxAmount(NumberUtil.sub(defaultValue(lot.getCostTaxAmount()), settleTaxAmount));
      lot.setSettledCostNum(NumberUtil.sub(defaultValue(lot.getSettledCostNum()), settleNum));
      if (NumberUtil.le(defaultValue(lot.getSettledCostNum()), BigDecimal.ZERO)) {
        lot.setCostTaxAmount(null);
        lot.setSettledCostNum(BigDecimal.ZERO);
        lot.setCostStatus(StockCostStatus.PENDING);
      } else {
        lot.setCostStatus(StockCostStatus.PARTIAL);
      }
      saleOutSheetDetailLotService.updateById(lot);
      return;
    }

    if (record.getOutBizType() == ProductStockBizType.RETAIL) {
      RetailOutSheetDetailLot lot = retailOutSheetDetailLotService.getById(record.getLotId());
      if (lot == null) {
        return;
      }

      lot.setCostTaxAmount(NumberUtil.sub(defaultValue(lot.getCostTaxAmount()), settleTaxAmount));
      lot.setSettledCostNum(NumberUtil.sub(defaultValue(lot.getSettledCostNum()), settleNum));
      if (NumberUtil.le(defaultValue(lot.getSettledCostNum()), BigDecimal.ZERO)) {
        lot.setCostTaxAmount(null);
        lot.setSettledCostNum(BigDecimal.ZERO);
        lot.setCostStatus(StockCostStatus.PENDING);
      } else {
        lot.setCostStatus(StockCostStatus.PARTIAL);
      }
      retailOutSheetDetailLotService.updateById(lot);
    }
  }

  private BigDecimal defaultValue(BigDecimal value) {

    return value == null ? BigDecimal.ZERO : value;
  }
}
