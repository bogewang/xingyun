package com.lframework.xingyun.sc.impl.stock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.sc.dto.stock.ProductStockChangeDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.entity.ProductStockLog;
import com.lframework.xingyun.sc.entity.ProductStockPendingCost;
import com.lframework.xingyun.sc.entity.ProductStockPendingCostSettle;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetailLot;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.StockCostStatus;
import com.lframework.xingyun.sc.mappers.ProductStockPendingCostSettleMapper;
import com.lframework.xingyun.sc.service.purchase.PurchaseReturnService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetDetailService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.service.retail.RetailOutSheetService;
import com.lframework.xingyun.sc.service.retail.RetailReturnService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailLotService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.service.stock.ProductStockLogService;
import com.lframework.xingyun.sc.service.stock.ProductStockPendingCostService;
import com.lframework.xingyun.sc.service.stock.ProductStockRebuildService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.service.stock.adjust.StockAdjustSheetService;
import com.lframework.xingyun.sc.service.stock.take.TakeStockSheetService;
import com.lframework.xingyun.sc.service.stock.transfer.ScTransferOrderService;
import com.lframework.xingyun.sc.vo.stock.AddProductStockVo;
import com.lframework.xingyun.sc.vo.stock.SubProductStockVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockRebuildServiceImpl implements ProductStockRebuildService {

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockLogService productStockLogService;

    @Autowired
    private ProductStockPendingCostService productStockPendingCostService;

    @Autowired
    private ProductStockPendingCostSettleMapper productStockPendingCostSettleMapper;

    @Autowired
    private ReceiveSheetService receiveSheetService;

    @Autowired
    private ReceiveSheetDetailService receiveSheetDetailService;

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    @Autowired
    private SaleOutSheetDetailService saleOutSheetDetailService;

    @Autowired
    private SaleOutSheetDetailLotService saleOutSheetDetailLotService;

    @Autowired
    private PurchaseReturnService purchaseReturnService;

    @Autowired
    private SaleReturnService saleReturnService;

    @Autowired
    private RetailOutSheetService retailOutSheetService;

    @Autowired
    private RetailReturnService retailReturnService;

    @Autowired
    private ScTransferOrderService scTransferOrderService;

    @Autowired
    private StockAdjustSheetService stockAdjustSheetService;

    @Autowired
    private TakeStockSheetService takeStockSheetService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rebuildByReceiveAndSaleSheets() {

        validateNoUnsupportedStockBizData();
        clearDerivedStockData();
        replayReceiveAndSaleSheets();
    }

    private void validateNoUnsupportedStockBizData() {

        validateNoOtherStockBiz(purchaseReturnService.count(), "存在采购退货单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(saleReturnService.count(), "存在销售退货单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(retailOutSheetService.count(), "存在零售出库单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(retailReturnService.count(), "存在零售退货单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(scTransferOrderService.count(), "存在仓库调拨单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(stockAdjustSheetService.count(), "存在库存调整单，当前重建方法不支持执行！");
        validateNoOtherStockBiz(takeStockSheetService.count(), "存在盘点单，当前重建方法不支持执行！");
    }

    private void validateNoOtherStockBiz(long count, String message) {

        if (count > 0) {
            throw new DefaultClientException(message);
        }
    }

    private void clearDerivedStockData() {

        productStockService.remove(Wrappers.lambdaQuery(ProductStock.class).isNotNull(ProductStock::getId));
        productStockLogService.remove(Wrappers.lambdaQuery(ProductStockLog.class).isNotNull(ProductStockLog::getId));
        productStockPendingCostService.remove(
                Wrappers.lambdaQuery(ProductStockPendingCost.class).isNotNull(ProductStockPendingCost::getId));
        productStockPendingCostSettleMapper.delete(Wrappers.lambdaQuery(ProductStockPendingCostSettle.class)
                .isNotNull(ProductStockPendingCostSettle::getId));
        saleOutSheetDetailLotService.remove(
                Wrappers.lambdaQuery(SaleOutSheetDetailLot.class).isNotNull(SaleOutSheetDetailLot::getId));
    }

    private void replayReceiveAndSaleSheets() {

        List<StockReplayNode> nodes = new ArrayList<>();

        receiveSheetService.list()
                .forEach(item -> nodes.add(new StockReplayNode(item.getId(), item.getOrderDate(), item.getCreateTime(),
                        StockReplayType.RECEIVE)));
        saleOutSheetService.list()
                .forEach(item -> nodes.add(new StockReplayNode(item.getId(), item.getOrderDate(), item.getCreateTime(),
                        StockReplayType.SALE_OUT)));

        nodes.sort(Comparator
                .comparing(StockReplayNode::getOrderDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparing(StockReplayNode::getCreateTime, Comparator.nullsFirst(LocalDateTime::compareTo))
                .thenComparing(StockReplayNode::getType)
                .thenComparing(StockReplayNode::getSheetId));

        for (StockReplayNode node : nodes) {
            if (node.getType() == StockReplayType.RECEIVE) {
                replayReceiveSheet(node.getSheetId());
            } else {
                replaySaleOutSheet(node.getSheetId());
            }
        }
    }

    private void replayReceiveSheet(String sheetId) {

        ReceiveSheet sheet = receiveSheetService.getById(sheetId);
        if (sheet == null) {
            return;
        }

        List<ReceiveSheetDetail> details = receiveSheetDetailService.getBySheetId(sheetId);
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        for (ReceiveSheetDetail detail : details) {
            AddProductStockVo addProductStockVo = new AddProductStockVo();
            addProductStockVo.setProductId(detail.getProductId());
            addProductStockVo.setScId(sheet.getScId());
            addProductStockVo.setStockNum(detail.getOrderNum());
            addProductStockVo.setTaxPrice(detail.getTaxPrice());
            addProductStockVo.setCreateTime(sheet.getCreateTime());
            addProductStockVo.setBizId(sheet.getId());
            addProductStockVo.setBizDetailId(detail.getId());
            addProductStockVo.setBizCode(sheet.getCode());
            addProductStockVo.setBizType(ProductStockBizType.PURCHASE.getCode());
            addProductStockVo.setLogTaxAmount(detail.getTaxAmount());

            if (Boolean.TRUE.equals(detail.getIsGift())) {
                addProductStockVo.setTaxAmount(null);
                addProductStockVo.setDefaultTaxAmount(BigDecimal.ZERO);
            } else {
                addProductStockVo.setTaxAmount(productStockPendingCostService
                        .settle(sheet, detail, ProductStockBizType.PURCHASE)
                        .getRemainTaxAmount());
                addProductStockVo.setDefaultTaxAmount(detail.getTaxAmount());
            }

            productStockService.addStock(addProductStockVo);
        }
    }

    private void replaySaleOutSheet(String sheetId) {

        SaleOutSheet sheet = saleOutSheetService.getById(sheetId);
        if (sheet == null) {
            return;
        }

        List<SaleOutSheetDetail> details = saleOutSheetDetailService.getBySheetId(sheetId);
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        int orderNo = 1;
        for (SaleOutSheetDetail detail : details) {
            SubProductStockVo subProductStockVo = new SubProductStockVo();
            subProductStockVo.setProductId(detail.getProductId());
            subProductStockVo.setScId(sheet.getScId());
            subProductStockVo.setStockNum(detail.getOrderNum());
            subProductStockVo.setCreateTime(sheet.getCreateTime());
            subProductStockVo.setBizId(sheet.getId());
            subProductStockVo.setBizDetailId(detail.getId());
            subProductStockVo.setBizCode(sheet.getCode());
            subProductStockVo.setBizType(ProductStockBizType.SALE.getCode());

            ProductStockChangeDto stockChange = productStockService.subStock(subProductStockVo);
            orderNo = createSaleOutDetailLots(sheet, detail, stockChange, orderNo);
        }
    }

    private int createSaleOutDetailLots(SaleOutSheet sheet, SaleOutSheetDetail detail,
            ProductStockChangeDto stockChange, int orderNo) {

        if (NumberUtil.gt(defaultValue(stockChange.getCostNum()), BigDecimal.ZERO)) {
            SaleOutSheetDetailLot costedLot = new SaleOutSheetDetailLot();
            costedLot.setId(IdUtil.getId());
            costedLot.setDetailId(detail.getId());
            costedLot.setOrderNum(stockChange.getCostNum());
            costedLot.setReturnNum(BigDecimal.ZERO);
            costedLot.setCostTaxAmount(stockChange.getTaxAmount());
            costedLot.setSettledCostNum(stockChange.getCostNum());
            costedLot.setCostStatus(StockCostStatus.FINAL);
            costedLot.setSettleStatus(detail.getSettleStatus());
            costedLot.setOrderNo(orderNo++);
            saleOutSheetDetailLotService.save(costedLot);
        }

        if (NumberUtil.gt(defaultValue(stockChange.getPendingNum()), BigDecimal.ZERO)) {
            SaleOutSheetDetailLot pendingLot = new SaleOutSheetDetailLot();
            pendingLot.setId(IdUtil.getId());
            pendingLot.setDetailId(detail.getId());
            pendingLot.setOrderNum(stockChange.getPendingNum());
            pendingLot.setReturnNum(BigDecimal.ZERO);
            pendingLot.setCostTaxAmount(null);
            pendingLot.setSettledCostNum(BigDecimal.ZERO);
            pendingLot.setCostStatus(StockCostStatus.PENDING);
            pendingLot.setSettleStatus(detail.getSettleStatus());
            pendingLot.setOrderNo(orderNo++);
            saleOutSheetDetailLotService.save(pendingLot);

            productStockPendingCostService.create(sheet.getScId(), detail.getProductId(), sheet.getId(),
                    detail.getId(), ProductStockBizType.SALE, pendingLot.getId(),
                    stockChange.getPendingNum(), stockChange.getCreateTime());
        }

        return orderNo;
    }

    private BigDecimal defaultValue(BigDecimal num) {

        return num == null ? BigDecimal.ZERO : num;
    }

    private enum StockReplayType {
        RECEIVE, SALE_OUT
    }

    private static class StockReplayNode {

        private final String sheetId;

        private final LocalDate orderDate;

        private final LocalDateTime createTime;

        private final StockReplayType type;

        private StockReplayNode(String sheetId, LocalDate orderDate, LocalDateTime createTime,
                StockReplayType type) {
            this.sheetId = sheetId;
            this.orderDate = orderDate;
            this.createTime = createTime;
            this.type = type;
        }

        private String getSheetId() {
            return sheetId;
        }

        private LocalDate getOrderDate() {
            return orderDate;
        }

        private LocalDateTime getCreateTime() {
            return createTime;
        }

        private StockReplayType getType() {
            return type;
        }
    }
}
