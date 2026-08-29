package com.lframework.xingyun.api.migration;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/** V2.9 报价单迁移脚本契约测试。 */
public class QuoteSheetMigrationContractTest {
  /** 校验报价单表、关键列、索引和可空引用字段均存在。 */
  @Test public void shouldContainQuoteSheetMigrationContract() throws Exception { String sql=new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql")),StandardCharsets.UTF_8); Assert.assertTrue(sql.contains("CREATE TABLE `tbl_quote_sheet`")); Assert.assertTrue(sql.contains("CREATE TABLE `tbl_quote_sheet_detail`")); Assert.assertTrue(sql.contains("`start_date` date NOT NULL")); Assert.assertTrue(sql.contains("`sale_price` decimal(18,2) NOT NULL")); Assert.assertTrue(sql.contains("idx_quote_sheet_tenant_date")); Assert.assertTrue(sql.contains("uk_quote_sheet_detail_sheet_product")); Assert.assertTrue(sql.contains("ALTER TABLE `tbl_sale_out_sheet`")); Assert.assertFalse(sql.contains("ALTER TABLE `tbl_sale_out_sheet_detail`")); Assert.assertTrue(sql.contains("`quote_sheet_id` varchar(32) NULL DEFAULT NULL")); }
}
