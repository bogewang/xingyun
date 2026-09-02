package com.lframework.xingyun.sc.mappers;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * 销售出库单明细金额为零筛选 SQL 测试。
 */
public class SaleOutSheetZeroAmountDetailFilterTest {

  /**
   * 校验查询 SQL 同时支持存在和不存在金额为零明细的筛选。
   *
   * @throws Exception 读取 Mapper 文件失败
   */
  @Test
  public void queryShouldSupportZeroAmountDetailFilter() throws Exception {
    String mapperXml = new String(Files.readAllBytes(Paths.get(
        "src/main/resources/mappers/sale/SaleOutSheetMapper.xml")), StandardCharsets.UTF_8);

    assertTrue(mapperXml.contains("vo.hasZeroAmountDetail != null"));
    assertTrue(mapperXml.contains("IFNULL(sd.tax_amount, 0) = 0"));
    assertTrue(mapperXml.contains("AND NOT EXISTS ("));
  }
}
