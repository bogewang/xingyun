package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售出库分类明细导出工具。
 */
public final class SaleOutSheetCategoryDetailExportHelper {

  private static final String UNREMARKED = "未填写备注二";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  private SaleOutSheetCategoryDetailExportHelper() {
  }

  /**
   * 按客户生成商品备注二汇总工作簿并写入响应。
   *
   * @param details 明细数据
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param response HTTP 响应
   * @throws IOException 写出失败时抛出
   */
  public static void export(List<QuerySaleOutSheetDetailDto> details, LocalDate startDate,
      LocalDate endDate, HttpServletResponse response) throws IOException {
    Map<String, List<QuerySaleOutSheetDetailDto>> customerDetails = groupByCustomer(details);
    Set<String> categories = collectCategories(details);
    prepareResponse(response);

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Styles styles = new Styles(workbook);
      for (Map.Entry<String, List<QuerySaleOutSheetDetailDto>> entry : customerDetails.entrySet()) {
        writeSheet(workbook, styles, entry.getKey(), entry.getValue(), categories, startDate, endDate);
      }
      workbook.write(response.getOutputStream());
      response.flushBuffer();
    }
  }

  /**
   * 按客户名称分组，保证导出顺序稳定。
   *
   * @param details 销售出库明细
   * @return 客户对应的明细
   */
  private static Map<String, List<QuerySaleOutSheetDetailDto>> groupByCustomer(
      List<QuerySaleOutSheetDetailDto> details) {
    Map<String, List<QuerySaleOutSheetDetailDto>> result = new LinkedHashMap<>();
    details.stream().sorted(Comparator.comparing(SaleOutSheetCategoryDetailExportHelper::customerName))
        .forEach(detail -> result.computeIfAbsent(customerName(detail), key -> new ArrayList<>())
            .add(detail));
    return result;
  }

  /**
   * 收集全部商品备注二，保证同一文件中的工作表列结构一致。
   *
   * @param details 销售出库明细
   * @return 有序商品备注二集合
   */
  private static Set<String> collectCategories(List<QuerySaleOutSheetDetailDto> details) {
    List<String> categoryList = new ArrayList<>();
    for (QuerySaleOutSheetDetailDto detail : details) {
      categoryList.add(summaryName(detail));
    }
    categoryList.sort(String::compareTo);
    return new LinkedHashSet<>(categoryList);
  }

  /**
   * 设置文件下载响应头。
   *
   * @param response HTTP 响应
   * @throws IOException 编码失败时抛出
   */
  private static void prepareResponse(HttpServletResponse response) throws IOException {
    String fileName = "销售出库商品分类汇总.xlsx";
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
        .replace("+", "%20");
    response.reset();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("filename", encodedFileName);
    response.setHeader("Content-Disposition",
        "attachment;filename=" + encodedFileName + ";filename*=utf-8''" + encodedFileName);
  }

  /**
   * 写入单个客户工作表。
   */
  private static void writeSheet(XSSFWorkbook workbook, Styles styles, String customerName,
      List<QuerySaleOutSheetDetailDto> details, Set<String> categories, LocalDate startDate,
      LocalDate endDate) {
    int columnCount = categories.size() + 2;
    Sheet sheet = workbook.createSheet(buildSheetName(customerName));
    setColumnWidths(sheet, columnCount);

    Row titleRow = sheet.createRow(0);
    titleRow.setHeightInPoints(24);
    setTextCell(titleRow, 0, customerName + startDate.getMonthValue() + "月份汇总表", styles.title);
    merge(sheet, 0, 0, 0, columnCount - 1);

    Row headerRow = sheet.createRow(1);
    setTextCell(headerRow, 0, "发货时间", styles.header);
    int columnIndex = 1;
    for (String category : categories) {
      setTextCell(headerRow, columnIndex++, category, styles.header);
    }
    setTextCell(headerRow, columnIndex, "合计", styles.header);

    Map<LocalDate, Map<String, BigDecimal>> amountMap = buildAmountMap(details);
    Map<String, BigDecimal> categoryTotals = createCategoryTotals(categories);
    BigDecimal grandTotal = BigDecimal.ZERO;
    int rowIndex = 2;
    LocalDate currentDate = startDate;
    while (!currentDate.isAfter(endDate)) {
      Row row = sheet.createRow(rowIndex++);
      setTextCell(row, 0, DATE_FORMATTER.format(currentDate), styles.date);
      Map<String, BigDecimal> dailyAmounts = amountMap.getOrDefault(currentDate,
          new LinkedHashMap<>());
      BigDecimal dailyTotal = BigDecimal.ZERO;
      columnIndex = 1;
      for (String category : categories) {
        BigDecimal amount = dailyAmounts.get(category);
        setAmountCell(row, columnIndex++, amount, styles.amount);
        if (amount != null) {
          categoryTotals.put(category, categoryTotals.get(category).add(amount));
          dailyTotal = dailyTotal.add(amount);
        }
      }
      setAmountCell(row, columnIndex, dailyTotal.signum() == 0 ? null : dailyTotal, styles.amount);
      grandTotal = grandTotal.add(dailyTotal);
      currentDate = currentDate.plusDays(1);
    }

    Row totalRow = sheet.createRow(rowIndex++);
    setTextCell(totalRow, 0, "合计（元）", styles.totalLabel);
    columnIndex = 1;
    for (String category : categories) {
      setAmountCell(totalRow, columnIndex++, categoryTotals.get(category), styles.totalAmount);
    }
    setAmountCell(totalRow, columnIndex, grandTotal, styles.totalAmount);

    Row capitalRow = sheet.createRow(rowIndex++);
    setTextCell(capitalRow, 0, "大写：", styles.totalLabel);
    setTextCell(capitalRow, 1, toChineseCurrency(grandTotal), styles.capital);
    setMergedCellsStyle(capitalRow, 1, columnCount - 1, styles.capital);
    merge(sheet, rowIndex - 1, rowIndex - 1, 1, columnCount - 1);

    Row footerRow = sheet.createRow(rowIndex);
    int splitColumn = Math.max(0, (columnCount - 1) / 2);
    setTextCell(footerRow, 0, "生活服务中心：", styles.footer);
    merge(sheet, rowIndex, rowIndex, 0, splitColumn);
    setTextCell(footerRow, splitColumn + 1, "司务长：", styles.footer);
    merge(sheet, rowIndex, rowIndex, splitColumn + 1, columnCount - 1);
  }

  /**
   * 生成客户日期和商品备注二维度的金额汇总。
   */
  private static Map<LocalDate, Map<String, BigDecimal>> buildAmountMap(
      List<QuerySaleOutSheetDetailDto> details) {
    Map<LocalDate, Map<String, BigDecimal>> result = new LinkedHashMap<>();
    for (QuerySaleOutSheetDetailDto detail : details) {
      LocalDate orderDate = LocalDate.parse(detail.getOrderDate(), DATE_FORMATTER);
      Map<String, BigDecimal> categoryAmounts = result.computeIfAbsent(orderDate,
          key -> new LinkedHashMap<>());
      String category = summaryName(detail);
      BigDecimal amount = detail.getConfirmAmt() == null ? detail.getTaxAmount() : detail.getConfirmAmt();
      if (amount != null) {
        categoryAmounts.merge(category, amount, BigDecimal::add);
      }
    }
    return result;
  }

  /**
   * 创建分类合计初始值。
   */
  private static Map<String, BigDecimal> createCategoryTotals(Set<String> categories) {
    Map<String, BigDecimal> result = new LinkedHashMap<>();
    for (String category : categories) {
      result.put(category, BigDecimal.ZERO);
    }
    return result;
  }

  /**
   * 设置工作表列宽。
   */
  private static void setColumnWidths(Sheet sheet, int columnCount) {
    sheet.setColumnWidth(0, 16 * 256);
    for (int index = 1; index < columnCount; index++) {
      sheet.setColumnWidth(index, 14 * 256);
    }
  }

  /**
   * 写入文本单元格。
   */
  private static void setTextCell(Row row, int columnIndex, String value, CellStyle style) {
    Cell cell = row.createCell(columnIndex);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  /**
   * 写入金额单元格，空金额保持空白。
   */
  private static void setAmountCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
    Cell cell = row.createCell(columnIndex);
    if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
      cell.setCellValue(value.doubleValue());
    }
    cell.setCellStyle(style);
  }

  /**
   * 合并单元格。
   */
  private static void merge(Sheet sheet, int firstRow, int lastRow, int firstColumn,
      int lastColumn) {
    if (firstColumn < lastColumn) {
      sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
    }
  }

  /**
   * 为合并前的每个单元格设置样式，确保合并区域右侧边框能够写入工作簿。
   *
   * @param row 行
   * @param firstColumn 起始列
   * @param lastColumn 结束列
   * @param style 单元格样式
   */
  private static void setMergedCellsStyle(Row row, int firstColumn, int lastColumn,
      CellStyle style) {
    for (int columnIndex = firstColumn; columnIndex <= lastColumn; columnIndex++) {
      Cell cell = row.getCell(columnIndex);
      if (cell == null) {
        cell = row.createCell(columnIndex);
      }
      cell.setCellStyle(style);
    }
  }

  /**
   * 获取可展示的客户名称。
   */
  private static String customerName(QuerySaleOutSheetDetailDto detail) {
    return StringUtils.defaultIfBlank(detail.getCustomerName(), "未命名客户");
  }

  /**
   * 获取可展示的商品备注二。
   */
  private static String summaryName(QuerySaleOutSheetDetailDto detail) {
    return StringUtils.defaultIfBlank(detail.getProductRemark2(), UNREMARKED);
  }

  /**
   * 生成合法且不重复的工作表名称。
   */
  private static String buildSheetName(String customerName) {
    String result = customerName.replaceAll("[\\\\/?*\\[\\]:]", "_");
    if (result.length() > 31) {
      result = result.substring(0, 31);
    }
    return result;
  }

  /**
   * 将金额转为人民币大写。
   */
  private static String toChineseCurrency(BigDecimal amount) {
    BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount.setScale(2,
        RoundingMode.HALF_UP);
    long fen = normalizedAmount.movePointRight(2).longValue();
    if (fen == 0) {
      return "零元整";
    }
    String[] digits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    String[] units = {"分", "角", "元", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟", "万"};
    StringBuilder result = new StringBuilder();
    boolean zero = false;
    for (int index = 0; fen > 0 && index < units.length; index++) {
      int digit = (int) (fen % 10);
      if (digit == 0) {
        zero = result.length() > 0;
      } else {
        if (zero) {
          result.insert(0, digits[0]);
        }
        result.insert(0, units[index]);
        result.insert(0, digits[digit]);
        zero = false;
      }
      fen /= 10;
    }
    String currency = result.toString().replaceAll("零(万|亿|元)", "$1")
        .replaceAll("零+", "零").replaceAll("亿万", "亿").replaceAll("零元", "元");
    if (!currency.contains("元") && normalizedAmount.compareTo(BigDecimal.ONE) >= 0) {
      currency += "元";
    }
    if (!currency.contains("角") && !currency.contains("分")) {
      return currency + "整";
    }
    return currency.replaceAll("零分$", "");
  }

  /**
   * 导出样式集合。
   */
  private static class Styles {
    private final CellStyle title;
    private final CellStyle header;
    private final CellStyle date;
    private final CellStyle amount;
    private final CellStyle totalLabel;
    private final CellStyle totalAmount;
    private final CellStyle capital;
    private final CellStyle footer;

    /**
     * 初始化样式集合。
     */
    private Styles(XSSFWorkbook workbook) {
      this.title = createStyle(workbook, true, HorizontalAlignment.CENTER, false);
      this.header = createStyle(workbook, false, HorizontalAlignment.CENTER, true);
      this.date = createStyle(workbook, false, HorizontalAlignment.CENTER, true);
      this.amount = createAmountStyle(workbook, false);
      this.totalLabel = createStyle(workbook, false, HorizontalAlignment.CENTER, true);
      this.totalAmount = createAmountStyle(workbook, true);
      this.capital = createStyle(workbook, true, HorizontalAlignment.CENTER, true);
      this.footer = createStyle(workbook, true, HorizontalAlignment.CENTER, false);
    }

    /**
     * 创建普通单元格样式。
     */
    private CellStyle createStyle(XSSFWorkbook workbook, boolean bold, HorizontalAlignment alignment,
        boolean border) {
      CellStyle style = workbook.createCellStyle();
      style.setFont(createFont(workbook, bold));
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      if (border) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
      }
      return style;
    }

    /**
     * 创建金额单元格样式。
     */
    private CellStyle createAmountStyle(XSSFWorkbook workbook, boolean bold) {
      CellStyle style = createStyle(workbook, bold, HorizontalAlignment.RIGHT, true);
      DataFormat dataFormat = workbook.createDataFormat();
      style.setDataFormat(dataFormat.getFormat("0.##"));
      return style;
    }

    /**
     * 创建字体。
     */
    private Font createFont(XSSFWorkbook workbook, boolean bold) {
      Font font = workbook.createFont();
      font.setFontName("宋体");
      font.setFontHeightInPoints((short) 12);
      font.setBold(bold);
      return font;
    }
  }
}
