package com.lframework.xingyun.settle.impl;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.settle.bo.sheet.SettleSheetSummaryBo;
import com.lframework.xingyun.settle.service.SettleSheetSummaryService;
import com.lframework.xingyun.settle.vo.sheet.QuerySettleSheetSummaryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商结算汇总服务实现
 */
@Service
public class SettleSheetSummaryServiceImpl implements SettleSheetSummaryService {

    @Autowired
    private ReceiveSheetService receiveSheetService;

    @Autowired
    private SupplierService supplierService;

    @Override
    public List<SettleSheetSummaryBo> query(QuerySettleSheetSummaryVo vo) {

        Map<String, SettleSheetSummaryBo> summaryMap = new LinkedHashMap<>();

        merge(summaryMap, vo, SettleStatus.UN_CHECK_BILL);
        merge(summaryMap, vo, SettleStatus.UN_SETTLE);
        merge(summaryMap, vo, SettleStatus.PART_SETTLE);
        merge(summaryMap, vo, SettleStatus.SETTLED);

        return new ArrayList<>(summaryMap.values());
    }

    /**
     * 直接按采购收货单结算状态汇总，保证结算页统计对象始终是采购收货单本身。
     */
    private void merge(Map<String, SettleSheetSummaryBo> summaryMap, QuerySettleSheetSummaryVo vo,
                       SettleStatus settleStatus) {

        QueryReceiveSheetVo queryVo = new QueryReceiveSheetVo();
        queryVo.setSupplierId(vo.getSupplierId());
        queryVo.setOrderDateStart(
                vo.getOrderStartTime() == null ? null : vo.getOrderStartTime().toLocalDate());
        queryVo.setOrderDateEnd(vo.getOrderEndTime() == null ? null : vo.getOrderEndTime().toLocalDate());
        // queryVo.setStatus(ReceiveSheetStatus.APPROVE_PASS.getCode());
        queryVo.setSettleStatus(settleStatus.getCode());

        List<ReceiveSheet> receiveSheets = receiveSheetService.query(queryVo);
        if (CollectionUtil.isEmpty(receiveSheets)) {
            return;
        }

        for (ReceiveSheet item : receiveSheets) {
            SettleSheetSummaryBo summary = getOrCreate(summaryMap, item.getSupplierId());
            BigDecimal totalAmount = item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount();

            if (settleStatus == SettleStatus.UN_CHECK_BILL) {
                summary.setUnCheckSheetNum(summary.getUnCheckSheetNum() + 1);
                summary.setUnCheckTotalAmount(NumberUtil.add(summary.getUnCheckTotalAmount(), totalAmount));
            } else if (settleStatus == SettleStatus.UN_SETTLE) {
                summary.setUnSettleSheetNum(summary.getUnSettleSheetNum() + 1);
                summary.setUnSettleTotalAmount(NumberUtil.add(summary.getUnSettleTotalAmount(), totalAmount));
            } else if (settleStatus == SettleStatus.PART_SETTLE) {
                summary.setPartSettleSheetNum(summary.getPartSettleSheetNum() + 1);
                summary.setPartSettleTotalAmount(
                        NumberUtil.add(summary.getPartSettleTotalAmount(), totalAmount));
            } else if (settleStatus == SettleStatus.SETTLED) {
                summary.setSettledSheetNum(summary.getSettledSheetNum() + 1);
                summary.setSettledTotalAmount(NumberUtil.add(summary.getSettledTotalAmount(), totalAmount));
            }
        }
    }

    private SettleSheetSummaryBo getOrCreate(Map<String, SettleSheetSummaryBo> summaryMap,
                                             String supplierId) {

        SettleSheetSummaryBo summary = summaryMap.get(supplierId);
        if (summary != null) {
            return summary;
        }

        Supplier supplier = supplierService.findById(supplierId);
        summary = new SettleSheetSummaryBo();
        summary.setSupplierId(supplierId);
        summary.setSupplierCode(supplier.getCode());
        summary.setSupplierName(supplier.getName());
        summaryMap.put(supplierId, summary);
        return summary;
    }
}
