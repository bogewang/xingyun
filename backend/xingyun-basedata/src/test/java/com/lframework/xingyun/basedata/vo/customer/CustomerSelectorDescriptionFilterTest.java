package com.lframework.xingyun.basedata.vo.customer;

import com.lframework.xingyun.basedata.bo.customer.CustomerSelectorBo;
import com.lframework.xingyun.basedata.entity.Customer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 客户选择器备注筛选测试。
 */
class CustomerSelectorDescriptionFilterTest {

  /**
   * 验证客户选择器查询对象支持备注条件。
   */
  @Test
  void shouldAcceptDescriptionFilter() {
    QueryCustomerSelectorVo vo = QueryCustomerSelectorVo.builder()
        .description("重点客户")
        .orderByDescription(Boolean.TRUE)
        .build();

    Assertions.assertEquals("重点客户", vo.getDescription());
    Assertions.assertTrue(vo.getOrderByDescription());
  }

  /**
   * 验证客户列表查询对象支持备注条件。
   */
  @Test
  void shouldAcceptCustomerListDescriptionFilter() {
    QueryCustomerVo vo = new QueryCustomerVo();
    vo.setDescription("重点客户");

    Assertions.assertEquals("重点客户", vo.getDescription());
  }

  /**
   * 验证客户选择器返回客户备注。
   */
  @Test
  void shouldReturnCustomerDescription() {
    Customer customer = new Customer();
    customer.setId("customer-1");
    customer.setName("客户一");
    customer.setDescription("重点客户");

    CustomerSelectorBo bo = new CustomerSelectorBo(customer);

    Assertions.assertEquals("customer-1", bo.getValue());
    Assertions.assertEquals("客户一", bo.getLabel());
    Assertions.assertEquals("重点客户", bo.getDescription());
  }

  /**
   * 验证客户选择器使用备注模糊匹配。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldFilterCustomerSelectorByDescription() throws IOException {
    Path mapperPath = Paths.get(
        "src/main/resources/mappers/customer/CustomerMapper.xml");
    String mapperXml = new String(Files.readAllBytes(mapperPath), StandardCharsets.UTF_8);

    Assertions.assertTrue(mapperXml.contains(
        "AND description LIKE CONCAT('%', #{vo.description}, '%')"));
    Assertions.assertEquals(2, countOccurrences(mapperXml,
        "vo.description != null and vo.description != ''"));
  }

  /**
   * 验证客户选择器可按备注和客户编号稳定排序。
   *
   * @throws IOException 读取 Mapper 文件失败
   */
  @Test
  void shouldOrderCustomerSelectorByDescription() throws IOException {
    Path mapperPath = Paths.get(
        "src/main/resources/mappers/customer/CustomerMapper.xml");
    String mapperXml = new String(Files.readAllBytes(mapperPath), StandardCharsets.UTF_8);

    Assertions.assertTrue(mapperXml.contains(
        "vo.orderByDescription != null and vo.orderByDescription"));
    Assertions.assertTrue(mapperXml.contains("ORDER BY IFNULL(description, ''), code"));
  }

  /**
   * 统计指定片段出现次数。
   *
   * @param source 源文本
   * @param fragment 目标片段
   * @return 出现次数
   */
  private int countOccurrences(String source, String fragment) {
    return (source.length() - source.replace(fragment, "").length()) / fragment.length();
  }
}
