package com.lframework.xingyun.sc.excel.purchase.receive;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiveSheetDetailExportModel extends BaseBo<ReceiveSheetDetail> implements ExcelModel {

    @ExcelProperty("订单日期")
    private String orderDate;

    @ExcelProperty("商品编号")
    private String productCode;

    @ExcelProperty("名称")
    private String productName;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("简称")
    private String shortName;

    @ExcelProperty("商品分类")
    private String categoryName;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("数量")
    private BigDecimal orderNum;

    @ExcelProperty("单价（元）")
    private BigDecimal taxPrice;

    @ExcelProperty("金额")
    private BigDecimal taxAmount;

    @ExcelProperty("生产日期")
    private String productionDate;

    @ExcelProperty("商品备注")
    private String productRemark;

    @ExcelProperty("备注")
    private String description;

    @ExcelProperty("单据备注")
    private String sheetDescription;

    /**
     * 是否询价商品。
     */
    @ExcelProperty("是否询价商品")
    private String inquiryProduct;

    public ReceiveSheetDetailExportModel() {
    }

    public ReceiveSheetDetailExportModel(ReceiveSheetDetail dto) {

        super(dto);
    }

    @Override
    public <A> BaseBo<ReceiveSheetDetail> convert(ReceiveSheetDetail dto) {

        return this;
    }

    @Override
    protected void afterInit(ReceiveSheetDetail dto) {

        ProductService productService = ApplicationUtil.getBean(ProductService.class);
        Product product = productService.findById(dto.getProductId());

        ProductCategoryService productCategoryService = ApplicationUtil.getBean(ProductCategoryService.class);
        ProductCategory productCategory = productCategoryService.findById(product.getCategoryId());

        ReceiveSheetFullDto sheetFullDto = ApplicationUtil.getBean(ReceiveSheetService.class).getDetail(dto.getSheetId());
        Supplier supplier = ApplicationUtil.getBean(SupplierService.class).findById(sheetFullDto.getSupplierId());

        this.setOrderDate(sheetFullDto.getOrderDate() == null ? null : sheetFullDto.getOrderDate().toString());
        this.setProductCode(product.getCode());
        this.setProductName(product.getName());
        this.setShortName(product.getShortName());
        this.setSpec(product.getSpec());
        this.setUnit(product.getUnit());
        this.setCategoryName(productCategory.getName());
        this.setTaxPrice(dto.getTaxPrice());
        this.setOrderNum(dto.getOrderNum());
        this.setTaxAmount(dto.getTaxAmount());
        this.setProductionDate(dto.getProductionDate());
        this.setProductRemark(product.getRemark());
        this.setDescription(dto.getDescription());
        this.setSheetDescription(sheetFullDto.getDescription());
        if (supplier != null) {
            this.setSupplierName(supplier.getName());
        }
    }

    /**
     * 将询价商品标识转换为导出文本。
     *
     * @param inquiryProduct 询价商品标识
     * @return 导出文本
     */
    public static String formatInquiryProduct(Boolean inquiryProduct) {

        return Boolean.TRUE.equals(inquiryProduct) ? "是" : "否";
    }
}
