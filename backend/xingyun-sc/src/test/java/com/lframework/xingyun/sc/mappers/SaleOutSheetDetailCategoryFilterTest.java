package com.lframework.xingyun.sc.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * 销售出库明细商品分类筛选测试。
 */
class SaleOutSheetDetailCategoryFilterTest {

  /**
   * 验证明细查询可接收多个商品分类。
   */
  @Test
  void shouldAcceptCategoryIdList() {
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setCategoryIdList(Arrays.asList("category-1", "category-2"));

    assertEquals(Arrays.asList("category-1", "category-2"), vo.getCategoryIdList());
  }

  /**
   * 验证明细查询 SQL 按商品分类过滤。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldFilterDetailByCategoryIdList() throws IOException {
    String mapperXml = new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")),
        StandardCharsets.UTF_8);
    int detailWhereStart = mapperXml.indexOf("<sql id=\"SaleOutSheetDetailWhere_sql\">");
    int detailWhereEnd = mapperXml.indexOf("</sql>", detailWhereStart);
    String detailWhereSql = mapperXml.substring(detailWhereStart, detailWhereEnd);

    assertTrue(detailWhereSql.contains("vo.categoryIdList != null and vo.categoryIdList.size() > 0"));
    assertTrue(detailWhereSql.contains("AND g.category_id IN"));
  }
}
