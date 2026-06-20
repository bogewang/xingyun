package com.lframework.xingyun.sc.excel.sale.out;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class SaleOutSheetDetailExportModel extends BaseBo<QuerySaleOutSheetDetailDto> implements ExcelModel {

    @ExcelProperty("订单日期")
    private String orderDate;

    @ExcelProperty("客户名称")
    private String customerName;

    // @ExcelProperty("商品编号")
    @ExcelIgnore
    private String productCode;

    @ExcelProperty("名称")
    private String productName;

    // @ExcelProperty("简称")
    @ExcelIgnore
    private String shortName;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("商品分类")
    private String categoryName;

    @ExcelProperty("出库数量")
    private BigDecimal orderNum;

    @ExcelProperty("进价")
    private BigDecimal costPrice;

    @ExcelProperty("售价")
    private BigDecimal taxPrice;

    @ExcelProperty("成本")
    private BigDecimal costAmount;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("销售额")
    private BigDecimal taxAmount;

    @ExcelProperty("毛利率")
    private String profitRate;

    @ExcelProperty("备注")
    private String description;

    public SaleOutSheetDetailExportModel() {
    }

    public SaleOutSheetDetailExportModel(QuerySaleOutSheetDetailDto dto) {

        super(dto);
    }

    @Override
    public <A> BaseBo<QuerySaleOutSheetDetailDto> convert(QuerySaleOutSheetDetailDto dto) {

        return this;
    }

    @Override
    protected void afterInit(QuerySaleOutSheetDetailDto dto) {

        // 补齐订单日期和客户名称
        this.setOrderDate(dto.getOrderDate());
        this.setCustomerName(dto.getCustomerName());

        this.setProductCode(dto.getProductCode());
        this.setProductName(dto.getProductName());
        // this.setShortName(dto.gets);
        this.setSpec(dto.getSpec());
        this.setUnit(dto.getUnit());
        this.setCategoryName(dto.getCategoryName());
        this.setTaxPrice(defaultValue(dto.getTaxPrice()));
        this.setOrderNum(defaultValue(dto.getOrderNum()));
        this.setTaxAmount(defaultValue(dto.getTaxAmount()));
        this.setCostPrice(defaultValue(dto.getCostPrice()));
        BigDecimal totalProfit = defaultValue(dto.getTotalProfit());
        this.setCostAmount(NumberUtil.sub(this.taxAmount, totalProfit));
        this.setProfitRate(this.buildProfitRate(this.taxAmount, totalProfit));
        this.setDescription(dto.getDescription());

        Supplier supplier = ApplicationUtil.getBean(SupplierService.class).findById(dto.getSupplierId());
        if (supplier != null) {
            this.setSupplierName(supplier.getName());
        }
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildProfitRate(BigDecimal amount, BigDecimal profit) {
        if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) {
            return "0.00%";
        }
        return profit.multiply(new BigDecimal("100")).divide(amount, 2, RoundingMode.HALF_UP) + "%";
    }
}
