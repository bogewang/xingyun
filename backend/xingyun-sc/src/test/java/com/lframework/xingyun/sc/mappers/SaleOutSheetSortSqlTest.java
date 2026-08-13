package com.lframework.xingyun.sc.mappers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class SaleOutSheetSortSqlTest {

  /** 验证销售出库列表仅允许按客户名称和验收金额动态排序。 */
  @Test
  void shouldSupportWhitelistedCustomerNameAndConfirmAmountSort() throws IOException {
    String mapperXml = readMapperXml();
    String querySql = getQuerySql(mapperXml);

    assertTrue(mapperXml.contains("LEFT JOIN base_data_customer AS cu ON cu.id = s.customer_id"));
    assertTrue(querySql.contains("vo.sortField == 'customerName'"));
    assertTrue(querySql.contains("cu.name"));
    assertTrue(querySql.contains("vo.sortField == 'confirmAmt'"));
    assertTrue(querySql.contains("s.confirm_amt"));
    assertTrue(querySql.contains("vo.sortOrder == 'asc'"));
    assertTrue(querySql.contains("s.create_time DESC, s.order_date DESC, s.id DESC"));
  }

  /** 读取销售出库 Mapper 配置内容。 */
  private String readMapperXml() throws IOException {
    return new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")), StandardCharsets.UTF_8);
  }

  /** 提取销售出库列表查询 SQL。 */
  private String getQuerySql(String mapperXml) {
    int queryStart = mapperXml.indexOf("<select id=\"query\"");
    int queryEnd = mapperXml.indexOf("</select>", queryStart);
    return mapperXml.substring(queryStart, queryEnd);
  }
}
