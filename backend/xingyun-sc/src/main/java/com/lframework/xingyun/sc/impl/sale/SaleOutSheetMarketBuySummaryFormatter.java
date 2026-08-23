package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.basedata.entity.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * 买菜汇总客户明细格式化器。
 */
final class SaleOutSheetMarketBuySummaryFormatter {

  private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy/M/d");

  private SaleOutSheetMarketBuySummaryFormatter() {
  }

  /**
   * 解析客户展示名称，优先使用昵称，昵称为空时回退到客户名称。
   *
   * @param customer 客户
   * @return 客户展示名称
   */
  static String resolveCustomerName(Customer customer) {
    if (customer == null) {
      return "";
    }

    return StringUtils.isNotBlank(customer.getNickName())
        ? customer.getNickName() : defaultString(customer.getName());
  }

  /**
   * 格式化单个客户的商品数量明细。
   *
   * @param customerName 客户展示名称
   * @param unit 商品单位
   * @param orderNum 数量
   * @param descriptions 备注集合
   * @return 客户明细文本
   */
  static String formatCustomerDetail(String customerName, String unit, BigDecimal orderNum,
      Collection<String> descriptions) {
    unit = formatUnit(unit);

    Set<String> distinctDescriptions = distinctDescriptions(descriptions);
    BigDecimal quantity = orderNum == null ? BigDecimal.ZERO : orderNum;
    if (quantity.compareTo(BigDecimal.ZERO) == 0 && distinctDescriptions.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder("【")
        .append(defaultString(customerName))
        .append("】");
    if (quantity.compareTo(BigDecimal.ZERO) != 0) {
      result.append(formatNumber(quantity));
      if (StringUtils.isNotBlank(unit)) {
        result.append('/').append(unit);
      }
    }
    appendDescriptions(result, distinctDescriptions);
    return result.toString();
  }

  /**
   * 格式化动态客户列的数量和备注，不包含客户名称及商品单位。
   *
   * @param orderNum 数量
   * @param descriptions 备注集合
   * @return 数量和备注文本
   */
  static String formatCustomerQuantity(BigDecimal orderNum, Collection<String> descriptions) {
    Set<String> remarks = distinctDescriptions(descriptions);
    BigDecimal quantity = orderNum == null ? BigDecimal.ZERO : orderNum;
    if (quantity.compareTo(BigDecimal.ZERO) == 0 && remarks.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    if (quantity.compareTo(BigDecimal.ZERO) != 0) {
      result.append(formatNumber(quantity));
    }
    appendDescriptions(result, remarks);
    return result.toString();
  }

  /**
   * 按备注分别格式化同一客户的商品数量，保留数量与备注的对应关系。
   *
   * @param customerName 客户展示名称
   * @param unit 商品单位
   * @param quantityWithoutDescription 无备注数量
   * @param quantityByDescription 各备注对应的汇总数量
   * @return 客户明细文本
   */
  static String formatCustomerDetailByDescription(String customerName, String unit,
      BigDecimal quantityWithoutDescription, Map<String, BigDecimal> quantityByDescription) {
    BigDecimal plainQuantity = quantityWithoutDescription == null
        ? BigDecimal.ZERO : quantityWithoutDescription;
    BigDecimal totalQuantity = plainQuantity;
    List<String> descriptionDetails = new ArrayList<>();

    if (quantityByDescription != null) {
      for (Map.Entry<String, BigDecimal> entry : quantityByDescription.entrySet()) {
        if (StringUtils.isBlank(entry.getKey())) {
          continue;
        }

        BigDecimal quantity = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
        totalQuantity = totalQuantity.add(quantity);
        StringBuilder detail = new StringBuilder();
        if (quantity.compareTo(BigDecimal.ZERO) != 0
            && !startsWithQuantity(entry.getKey())) {
          detail.append(formatNumber(quantity)).append('/');
        }
        detail.append(entry.getKey());
        descriptionDetails.add(detail.toString());
      }
    }
    if (plainQuantity.compareTo(BigDecimal.ZERO) != 0 && !descriptionDetails.isEmpty()) {
      descriptionDetails.add(0, formatNumber(plainQuantity));
    }

    if (totalQuantity.compareTo(BigDecimal.ZERO) == 0 && descriptionDetails.isEmpty()) {
      return "";
    }
    StringBuilder result = new StringBuilder("【")
        .append(defaultString(customerName))
        .append("】");
    if (totalQuantity.compareTo(BigDecimal.ZERO) != 0) {
      result.append(formatNumber(totalQuantity));
      String formattedUnit = formatUnit(unit);
      if (StringUtils.isNotBlank(formattedUnit)) {
        result.append(formattedUnit);
      }
    }
    if (!descriptionDetails.isEmpty()) {
      result.append('（').append(formatDescriptionDetails(plainQuantity, totalQuantity,
          descriptionDetails)).append('）');
    }
    return result.toString();
  }

  /**
   * 格式化备注数量明细；单条备注与总数量一致时省略重复的数量前缀。
   *
   * @param plainQuantity 无备注数量
   * @param totalQuantity 总数量
   * @param descriptionDetails 备注数量明细
   * @return 格式化后的备注数量明细
   */
  private static String formatDescriptionDetails(BigDecimal plainQuantity, BigDecimal totalQuantity,
      List<String> descriptionDetails) {
    if (plainQuantity.compareTo(BigDecimal.ZERO) == 0 && descriptionDetails.size() == 1) {
      String quantityPrefix = formatNumber(totalQuantity) + "/";
      String detail = descriptionDetails.get(0);
      return StringUtils.startsWith(detail, quantityPrefix)
          ? detail.substring(quantityPrefix.length()) : detail;
    }
    return String.join("；", descriptionDetails);
  }

  /**
   * 按输入顺序合并多个客户的商品数量明细。
   *
   * @param details 客户明细
   * @return 合并后的单列文本
   */
  static String mergeCustomerDetails(List<CustomerDetail> details) {
    if (details == null || details.isEmpty()) {
      return "";
    }

    List<String> texts = details.stream()
        .map(detail -> formatCustomerDetail(detail.getCustomerName(), detail.getUnit(),
            detail.getOrderNum(), detail.getDescriptions()))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
    return String.join("+", texts);
  }

  public static String formatUnit(String unit) {
    if (StringUtils.isBlank(unit)) {
      return "";
    }

    return unit.replaceAll("千克", "kg").replaceAll("公斤", "kg");

  }

  /**
   * 格式化总重量，将数量和单位拼接为“数量单位”。
   *
   * @param total 数量
   * @param unit 单位
   * @return 总重量文本
   */
  static String formatTotalWithUnit(BigDecimal total, String unit) {
    if (total == null) {
      return "";
    }

    return formatNumber(total) + formatUnit(unit);
  }

  /**
   * 格式化订单日期，月份和日期不补前导零。
   *
   * @param orderDate 订单日期
   * @return 订单日期文本
   */
  static String formatOrderDate(LocalDate orderDate) {
    return orderDate == null ? "" : orderDate.format(ORDER_DATE_FORMATTER);
  }

  /**
   * 对备注去空、去重并保持首次出现顺序。
   *
   * @param descriptions 原始备注
   * @return 去重后的备注集合
   */
  private static Set<String> distinctDescriptions(Collection<String> descriptions) {
    if (descriptions == null || descriptions.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> result = new LinkedHashSet<>();
    for (String description : descriptions) {
      if (StringUtils.isNotBlank(description)) {
        result.add(description);
      }
    }
    return result;
  }

  /**
   * 将备注追加到客户明细文本。
   *
   * @param result 客户明细文本
   * @param descriptions 去重后的备注
   */
  private static void appendDescriptions(StringBuilder result, Set<String> descriptions) {
    if (descriptions.isEmpty()) {
      return;
    }

    result.append('（').append(String.join("；", descriptions)).append('）');
  }

  /**
   * 判断备注是否已使用数字开头描述数量，避免重复追加该条明细数量。
   *
   * @param description 商品备注
   * @return 数字开头时返回 true
   */
  private static boolean startsWithQuantity(String description) {
    String value = StringUtils.trimToEmpty(description);
    return StringUtils.isNotEmpty(value) && Character.isDigit(value.charAt(0));
  }

  /**
   * 格式化数量并去掉尾部无意义的零。
   *
   * @param number 数量
   * @return 数量文本
   */
  private static String formatNumber(BigDecimal number) {
    return number.stripTrailingZeros().toPlainString();
  }

  /**
   * 将空字符串值转换为默认值。
   *
   * @param value 原始值
   * @return 非空字符串或空字符串
   */
  private static String defaultString(String value) {
    return value == null ? "" : value;
  }

  /**
   * 客户在单个商品下的数量明细。
   */
  static final class CustomerDetail {

    private final String customerName;
    private final String unit;
    private final BigDecimal orderNum;
    private final List<String> descriptions;

    /**
     * 创建客户数量明细。
     *
     * @param customerName 客户展示名称
     * @param unit 商品单位
     * @param orderNum 数量
     * @param descriptions 备注集合
     */
    CustomerDetail(String customerName, String unit, BigDecimal orderNum,
        Collection<String> descriptions) {
      this.customerName = customerName;
      this.unit = unit;
      this.orderNum = orderNum;
      this.descriptions = descriptions == null
          ? new ArrayList<>() : new ArrayList<>(descriptions);
    }

    /**
     * 获取客户展示名称。
     *
     * @return 客户展示名称
     */
    String getCustomerName() {
      return customerName;
    }

    /**
     * 获取商品单位。
     *
     * @return 商品单位
     */
    String getUnit() {
      return unit;
    }

    /**
     * 获取商品数量。
     *
     * @return 商品数量
     */
    BigDecimal getOrderNum() {
      return orderNum;
    }

    /**
     * 获取商品备注。
     *
     * @return 商品备注
     */
    List<String> getDescriptions() {
      return descriptions;
    }
  }
}
