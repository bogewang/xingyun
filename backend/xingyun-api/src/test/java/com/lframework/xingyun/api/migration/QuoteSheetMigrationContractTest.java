package com.lframework.xingyun.api.migration;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 报价单迁移脚本契约测试。
 */
public class QuoteSheetMigrationContractTest {
    /**
     * 校验报价单表、关键列、索引和可空引用字段均存在，且不包含租户字段。
     */
    @Test
    public void shouldContainQuoteSheetMigrationContract() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql")), StandardCharsets.UTF_8);
        Assert.assertTrue(sql.contains("CREATE TABLE `tbl_quote_sheet`"));
        Assert.assertTrue(sql.contains("CREATE TABLE `tbl_quote_sheet_detail`"));
        Assert.assertTrue(sql.contains("`start_date` date NOT NULL"));
        Assert.assertTrue(sql.contains("`sale_price` decimal(18,2) NOT NULL"));
        Assert.assertTrue(sql.contains("idx_quote_sheet_date"));
        Assert.assertFalse(sql.contains("tenant_id"));
        Assert.assertTrue(sql.contains("uk_quote_sheet_detail_sheet_product"));
        Assert.assertTrue(sql.contains("ALTER TABLE `tbl_sale_out_sheet`"));
        Assert.assertTrue(sql.contains("ALTER TABLE `tbl_sale_out_sheet_detail`"));
        Assert.assertTrue(sql.contains("`quote_sheet_id` varchar(32) NULL DEFAULT NULL"));
    }

    /**
     * 校验 V3.1 迁移删除已废弃的商品销售价和询价字段。
     */
    @Test
    public void shouldDropDeprecatedProductPriceAndInquiryColumns() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/tenant/V3.1-remove-product-sale-price-and-inquiry-product.sql")), StandardCharsets.UTF_8);
        Assert.assertTrue(sql.contains("DROP COLUMN `sale_price`"));
        Assert.assertTrue(sql.contains("DROP COLUMN `inquiry_product`"));
    }

    /** 校验报价单明细排序号迁移存在。 */
    @Test
    public void shouldContainQuoteSheetDetailOrderMigration() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/tenant/V3.5-quote-sheet-detail-order.sql")), StandardCharsets.UTF_8);
        Assert.assertTrue(sql.contains("ADD COLUMN `order_no` int NOT NULL DEFAULT 0"));
        Assert.assertTrue(sql.contains("idx_quote_sheet_detail_order"));
    }

    /** 校验销售出库计划日期展示参数迁移存在。 */
    @Test
    public void shouldContainSaleOutPlanDateDisplayMigration() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/tenant/V3.6-sale-out-plan-date-display.sql")), StandardCharsets.UTF_8);
        Assert.assertTrue(sql.contains("INSERT IGNORE INTO `sys_parameter`"));
        Assert.assertTrue(sql.contains("'sale_out_show_plan_date', 'true'"));
    }
}
