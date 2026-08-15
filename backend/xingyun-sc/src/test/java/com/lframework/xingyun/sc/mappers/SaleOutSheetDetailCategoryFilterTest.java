package com.lframework.xingyun.sc.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.time.LocalDate;
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

  /**
   * 验证明细查询可按明细备注过滤。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldFilterDetailByDescription() throws IOException {
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setDescription("加急");

    String mapperXml = new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")),
        StandardCharsets.UTF_8);
    int detailWhereStart = mapperXml.indexOf("<sql id=\"SaleOutSheetDetailWhere_sql\">");
    int detailWhereEnd = mapperXml.indexOf("</sql>", detailWhereStart);
    String detailWhereSql = mapperXml.substring(detailWhereStart, detailWhereEnd);

    assertEquals("加急", vo.getDescription());
    assertTrue(detailWhereSql.contains("vo.description != null and vo.description != ''"));
    assertTrue(detailWhereSql.contains("AND d.description LIKE CONCAT('%', #{vo.description}, '%')"));
  }

  /**
   * 验证明细查询可按计划日期范围过滤。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldFilterDetailByPlanDateRange() throws IOException {
    QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
    vo.setPlanDateStart(LocalDate.of(2026, 8, 1));
    vo.setPlanDateEnd(LocalDate.of(2026, 8, 31));

    String mapperXml = new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")),
        StandardCharsets.UTF_8);
    int detailWhereStart = mapperXml.indexOf("<sql id=\"SaleOutSheetDetailWhere_sql\">");
    int detailWhereEnd = mapperXml.indexOf("</sql>", detailWhereStart);
    String detailWhereSql = mapperXml.substring(detailWhereStart, detailWhereEnd);

    assertEquals(LocalDate.of(2026, 8, 1), vo.getPlanDateStart());
    assertEquals(LocalDate.of(2026, 8, 31), vo.getPlanDateEnd());
    assertTrue(detailWhereSql.contains("AND d.plan_date >= #{vo.planDateStart}"));
    assertTrue(detailWhereSql.contains("AND d.plan_date <= #{vo.planDateEnd}"));
  }

  /**
   * 验证单据查询按同一明细的计划日期范围过滤。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldFilterSheetByPlanDateRange() throws IOException {
    String mapperXml = new String(Files.readAllBytes(
        Paths.get("src/main/resources/mappers/sale/SaleOutSheetMapper.xml")),
        StandardCharsets.UTF_8);
    int queryStart = mapperXml.indexOf("<select id=\"query\"");
    int queryEnd = mapperXml.indexOf("</select>", queryStart);
    String querySql = mapperXml.substring(queryStart, queryEnd);

    assertTrue(querySql.contains("vo.planDateStart != null or vo.planDateEnd != null"));
    assertTrue(querySql.contains("FROM tbl_sale_out_sheet_detail AS sd"));
    assertTrue(querySql.contains("AND sd.plan_date >= #{vo.planDateStart}"));
    assertTrue(querySql.contains("AND sd.plan_date <= #{vo.planDateEnd}"));
  }
}
