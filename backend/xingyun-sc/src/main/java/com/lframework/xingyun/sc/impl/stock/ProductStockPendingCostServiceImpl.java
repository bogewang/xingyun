package com.lframework.xingyun.sc.impl.stock;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.sc.dto.stock.ProductStockPendingCostResolveDto;
import com.lframework.xingyun.sc.entity.*;
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
    public ProductStockPendingCostResolveDto settle(ReceiveSheet receiveSheet,
                                                    ReceiveSheetDetail detail,
                                                    ProductStockBizType inBizType) {

        Assert.notBlank(receiveSheet.getScId());
        Assert.notBlank(detail.getProductId());
        Assert.greaterThanZero(detail.getOrderNum());

        // 记录本次入库明细实际结转了多少数量/金额，以及尚未分摊出去的剩余值。
        ProductStockPendingCostResolveDto result = new ProductStockPendingCostResolveDto();
        result.setSettledNum(BigDecimal.ZERO);
        result.setSettledTaxAmount(BigDecimal.ZERO);
        result.setRemainNum(detail.getOrderNum());
        result.setRemainTaxAmount(detail.getTaxAmount() == null ? BigDecimal.ZERO : NumberUtil.getNumber(detail.getTaxAmount(), 2));

        // 用本次入库的税额反推含税单价，后续按结转数量等比例分摊到每一条待结转记录。
        BigDecimal taxPrice = NumberUtil.equal(detail.getOrderNum(), BigDecimal.ZERO) || detail.getTaxAmount() == null
                ? BigDecimal.ZERO
                : NumberUtil.getNumber(NumberUtil.div(detail.getTaxAmount(), detail.getOrderNum()), 6);

        // 按出库时间正序查找未完成的待结转记录，保证成本按历史出库顺序依次回填。
        List<ProductStockPendingCost> records = getProductStockPendingCosts(receiveSheet, detail);

        for (ProductStockPendingCost record : records) {
            // result.getRemainNum() 代表当前入库明细剩余可结转的数量。
            // 当该值小于等于 0 时，说明本次入库的数量已经全部被前面的待结转记录分摊完毕，
            // 后续记录已无可分配的数量来源，因此提前跳出循环以提升性能并保证逻辑正确。
            if (NumberUtil.le(result.getRemainNum(), BigDecimal.ZERO)) {
                break;
            }

            // 当前记录剩余待结转数量=待结转总数-已结转数量；没有余额的记录直接跳过。
            BigDecimal unresolvedNum = NumberUtil.sub(record.getPendingNum(), record.getSettledNum());
            if (NumberUtil.le(unresolvedNum, BigDecimal.ZERO)) {
                continue;
            }

            // 本次只结转两者中的较小值：当前记录未结转数量 与 当前入库还可分摊的数量。
            BigDecimal settleNum = NumberUtil.min(unresolvedNum, result.getRemainNum());
            BigDecimal settleTaxAmount = NumberUtil.getNumber(NumberUtil.mul(taxPrice, settleNum), 2);

            record.setSettledNum(NumberUtil.add(record.getSettledNum(), settleNum));
            record.setSettledTaxAmount(NumberUtil.add(record.getSettledTaxAmount(), settleTaxAmount));

            // 根据剩余待结转数量回写状态，标记为已完成或部分完成。
            BigDecimal remainPendingNum = NumberUtil.sub(record.getPendingNum(), record.getSettledNum());
            record.setStatus(NumberUtil.le(remainPendingNum, BigDecimal.ZERO)
                    ? ProductStockPendingCostStatus.FINISHED
                    : ProductStockPendingCostStatus.PARTIAL);
            this.updateById(record);

            // 记录本次“哪一条入库明细结转了哪一条待结转记录”，供后续回滚和追溯使用。
            persistPendingCostSettle(receiveSheet, detail, inBizType, record, settleNum, settleTaxAmount);

            // 如果出库明细已经关联了批次，需要同步回写批次层面的成本结转进度。
            updateLotCost(record, settleNum, settleTaxAmount);

            // 持续收敛本次入库明细的已分摊值与剩余值，供后续记录继续结转。
            result.setSettledNum(NumberUtil.add(result.getSettledNum(), settleNum));
            result.setSettledTaxAmount(NumberUtil.add(result.getSettledTaxAmount(), settleTaxAmount));
            result.setRemainNum(NumberUtil.sub(result.getRemainNum(), settleNum));
            result.setRemainTaxAmount(
                    NumberUtil.getNumber(NumberUtil.sub(result.getRemainTaxAmount(), settleTaxAmount), 2));
        }

        return result;
    }

    /**
     * 记录本次“哪一条入库明细结转了哪一条待结转记录”，供后续回滚和追溯使用
     * @param receiveSheet
     * @param detail
     * @param inBizType
     * @param record
     * @param settleNum
     * @param settleTaxAmount
     */
    private void persistPendingCostSettle(ReceiveSheet receiveSheet,
                                          ReceiveSheetDetail detail,
                                          ProductStockBizType inBizType,
                                          ProductStockPendingCost record,
                                          BigDecimal settleNum,
                                          BigDecimal settleTaxAmount) {
        ProductStockPendingCostSettle settle = new ProductStockPendingCostSettle();
        settle.setId(IdUtil.getId());
        settle.setPendingId(record.getId());
        settle.setInBizId(receiveSheet.getId());
        settle.setInBizDetailId(detail.getId());
        settle.setInBizType(inBizType);
        settle.setSettleNum(settleNum);
        settle.setSettleTaxAmount(settleTaxAmount);
        productStockPendingCostSettleMapper.insert(settle);
    }

    /**
     * 获取未完成的待结转记录
     * @param receiveSheet
     * @param detail
     * @return
     */
    private List<ProductStockPendingCost> getProductStockPendingCosts(ReceiveSheet receiveSheet, ReceiveSheetDetail detail) {
        Wrapper<ProductStockPendingCost> queryWrapper = Wrappers.lambdaQuery(ProductStockPendingCost.class)
                .eq(ProductStockPendingCost::getScId, receiveSheet.getScId())
                .eq(ProductStockPendingCost::getProductId, detail.getProductId())
                .ne(ProductStockPendingCost::getStatus, ProductStockPendingCostStatus.FINISHED)
                .orderByAsc(ProductStockPendingCost::getOutTime)
                .orderByAsc(ProductStockPendingCost::getId);
        List<ProductStockPendingCost> records = this.list(queryWrapper);
        return records;
    }

    /**
     * 回滚待处理成本记录
     * <p>
     * 当入库业务被取消或冲销时，回滚已关联的待处理成本记录，恢复库存成本状态至处理前的状态。
     * 主要流程：
     * 1. 根据入库业务明细ID和业务类型查询已结算记录
     * 2. 更新待处理成本记录的已结算数量和金额
     * 3. 根据剩余待处理数量更新状态（PENDING/PARTIAL/FINISHED）
     * 4. 回滚批次成本结转进度
     * 5. 删除结算关联记录
     * </p>
     *
     * @param inBizDetailId 入库业务单据明细ID
     * @param inBizType     入库业务类型
     * @return 回滚的含税成本金额
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BigDecimal rollback(String inBizDetailId, ProductStockBizType inBizType) {

        // 参数校验：入库业务单据明细ID不能为空
        Assert.notBlank(inBizDetailId);
        // 参数校验：入库业务类型不能为空
        Assert.notNull(inBizType);

        List<ProductStockPendingCostSettle> settles = getProductStockPendingCostSettles(inBizDetailId, inBizType);
        // 如果没有已结算记录，直接返回0
        if (CollectionUtil.isEmpty(settles)) {
            return BigDecimal.ZERO;
        }

        // 累计回滚的含税成本金额
        BigDecimal rollbackTaxAmount = BigDecimal.ZERO;
        // 遍历每条结算记录进行回滚处理
        for (ProductStockPendingCostSettle settle : settles) {
            // 获取对应的待处理成本记录
            ProductStockPendingCost pending = this.getById(settle.getPendingId());
            if (pending != null) {
                // 回滚已结算数量：减去本次结算的数量
                pending.setSettledNum(NumberUtil.sub(pending.getSettledNum(), settle.getSettleNum()));
                // 回滚已结算含税金额：减去本次结算的含税金额
                pending.setSettledTaxAmount(
                        NumberUtil.sub(pending.getSettledTaxAmount(), settle.getSettleTaxAmount()));

                // 计算剩余待处理数量
                BigDecimal remainPendingNum = NumberUtil.sub(pending.getPendingNum(), pending.getSettledNum());
                // 如果已结算数量小于等于0，重置为未处理状态
                if (NumberUtil.le(pending.getSettledNum(), BigDecimal.ZERO)) {
                    pending.setSettledNum(BigDecimal.ZERO);
                    pending.setSettledTaxAmount(BigDecimal.ZERO);
                    pending.setStatus(ProductStockPendingCostStatus.PENDING);
                } else {
                    // 根据剩余待处理数量更新状态：全部结算完则为FINISHED，否则为PARTIAL
                    pending.setStatus(NumberUtil.le(remainPendingNum, BigDecimal.ZERO)
                            ? ProductStockPendingCostStatus.FINISHED
                            : ProductStockPendingCostStatus.PARTIAL);
                }
                // 更新待处理成本记录
                this.updateById(pending);

                // 回滚批次成本结转进度
                rollbackLotCost(pending, settle.getSettleNum(), settle.getSettleTaxAmount());
            }

            // 累加本次回滚的含税金额
            rollbackTaxAmount = NumberUtil.add(rollbackTaxAmount, settle.getSettleTaxAmount());
            // 删除结算关联记录
            productStockPendingCostSettleMapper.deleteById(settle.getId());
        }

        // 返回累计回滚的含税成本金额
        return rollbackTaxAmount;
    }

    /**
     * 查询已结算记录列表
     * @param inBizDetailId
     * @param inBizType
     * @return
     */
    private List<ProductStockPendingCostSettle> getProductStockPendingCostSettles(String inBizDetailId,
                                                                                  ProductStockBizType inBizType) {
        // 构建查询条件：根据入库业务明细ID和业务类型查询已结算记录
        Wrapper<ProductStockPendingCostSettle> queryWrapper = Wrappers.lambdaQuery(ProductStockPendingCostSettle.class)
                .eq(ProductStockPendingCostSettle::getInBizDetailId, inBizDetailId)
                .eq(ProductStockPendingCostSettle::getInBizType, inBizType);
        // 查询已结算记录列表
        List<ProductStockPendingCostSettle> settles = productStockPendingCostSettleMapper.selectList(queryWrapper);
        return settles;
    }

    /**
     * 更新批次成本结转进度
     * @param record
     * @param settleNum
     * @param settleTaxAmount
     */
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

            lot.setCostTaxAmount(NumberUtil.add(lot.getCostTaxAmount(), settleTaxAmount));
            lot.setSettledCostNum(NumberUtil.add(lot.getSettledCostNum(), settleNum));
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

            lot.setCostTaxAmount(NumberUtil.add(lot.getCostTaxAmount(), settleTaxAmount));
            lot.setSettledCostNum(NumberUtil.add(lot.getSettledCostNum(), settleNum));
            lot.setCostStatus(NumberUtil.equal(lot.getSettledCostNum(), lot.getOrderNum())
                    ? StockCostStatus.FINAL
                    : StockCostStatus.PARTIAL);
            retailOutSheetDetailLotService.updateById(lot);
        }
    }

    /**
     * 回滚批次成本结转进度
     * @param record
     * @param settleNum
     * @param settleTaxAmount
     */
    private void rollbackLotCost(ProductStockPendingCost record, BigDecimal settleNum,
                                 BigDecimal settleTaxAmount) {

        // 如果批次ID为空，无需处理，直接返回
        if (record.getLotId() == null) {
            return;
        }

        // 处理销售出库批次成本回滚
        if (record.getOutBizType() == ProductStockBizType.SALE) {
            rollbackLotCostSale(record, settleNum, settleTaxAmount);
            return;
        }

        // 处理零售出库批次成本回滚
        if (record.getOutBizType() == ProductStockBizType.RETAIL) {
            rollbackLotCostRetail(record, settleNum, settleTaxAmount);
        }
    }

    /**
     * 回滚销售出库批次成本结转进度
     * @param record
     * @param settleNum
     * @param settleTaxAmount
     */
    private void rollbackLotCostSale(ProductStockPendingCost record, BigDecimal settleNum, BigDecimal settleTaxAmount) {
        // 获取销售出库单明细批次记录
        SaleOutSheetDetailLot lot = saleOutSheetDetailLotService.getById(record.getLotId());
        if (lot == null) {
            return;
        }

        // 回滚含税成本金额：减去本次结算的金额
        lot.setCostTaxAmount(NumberUtil.sub(lot.getCostTaxAmount(), settleTaxAmount));
        // 回滚已结算成本数量：减去本次结算的数量
        lot.setSettledCostNum(NumberUtil.sub(lot.getSettledCostNum(), settleNum));

        // 如果已结算数量小于等于0，重置为待处理状态
        if (NumberUtil.le(lot.getSettledCostNum(), BigDecimal.ZERO)) {
            lot.setCostTaxAmount(null);
            lot.setSettledCostNum(BigDecimal.ZERO);
            lot.setCostStatus(StockCostStatus.PENDING);
        } else {
            // 否则设置为部分结算状态
            lot.setCostStatus(StockCostStatus.PARTIAL);
        }
        // 更新销售出库单明细批次记录
        saleOutSheetDetailLotService.updateById(lot);
    }

    /**
     * 回滚零售出库批次成本结转进度
     * @param record
     * @param settleNum
     * @param settleTaxAmount
     */
    private void rollbackLotCostRetail(ProductStockPendingCost record, BigDecimal settleNum, BigDecimal settleTaxAmount) {
        // 获取零售出库单明细批次记录
        RetailOutSheetDetailLot lot = retailOutSheetDetailLotService.getById(record.getLotId());
        if (lot == null) {
            return;
        }

        // 回滚含税成本金额：减去本次结算的金额
        lot.setCostTaxAmount(NumberUtil.sub(lot.getCostTaxAmount(), settleTaxAmount));
        // 回滚已结算成本数量：减去本次结算的数量
        lot.setSettledCostNum(NumberUtil.sub(lot.getSettledCostNum(), settleNum));

        // 如果已结算数量小于等于0，重置为待处理状态
        if (NumberUtil.le(lot.getSettledCostNum(), BigDecimal.ZERO)) {
            lot.setCostTaxAmount(null);
            lot.setSettledCostNum(BigDecimal.ZERO);
            lot.setCostStatus(StockCostStatus.PENDING);
        } else {
            // 否则设置为部分结算状态
            lot.setCostStatus(StockCostStatus.PARTIAL);
        }
        // 更新零售出库单明细批次记录
        retailOutSheetDetailLotService.updateById(lot);
    }
}