package com.lframework.xingyun.sc.excel.sale.out;

import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SaleOutSheetSalesExportHelper {

  private static final int COLUMN_COUNT = 10;
  private static final short DEFAULT_FONT_SIZE = 12;
  private static final String DEFAULT_FONT_NAME = "宋体";
  private static final String CUSTOMER = "云南交投绿美生活服务有限公司";
  private static final String DEFAULT_TITLE = "红河聚亿商贸有限公司销售单";
  private static final String[] TABLE_HEADERS = {
      "序号", "商品名称", "规格", "单位", "数量", "单价", "金额", "备注", "验收数量", "验收金额"
  };

  private SaleOutSheetSalesExportHelper() {
  }

  public static void export(List<SheetData> datas, HttpServletResponse response) throws IOException {

    String fileName = "销售导出_" + LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
        .replace("+", "%20");

    response.reset();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("filename", encodedFileName);
    response.setHeader("Content-Disposition",
        "attachment;filename=" + encodedFileName + ";filename*=utf-8''" + encodedFileName);

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Styles styles = new Styles(workbook);
      for (int i = 0; i < datas.size(); i++) {
        writeSheet(styles, datas.get(i), i + 1);
      }

      workbook.write(response.getOutputStream());
      response.flushBuffer();
    }
  }

  private static void writeSheet(Styles styles, SheetData data, int index) {

    Sheet sheet = styles.workbook.createSheet(buildSheetName(data.getCustomerName(), index));
    setColumnWidths(sheet);

    int rowIndex = 0;

    Row titleRow = sheet.createRow(rowIndex++);
    titleRow.setHeightInPoints(22);
    setCell(titleRow, 0, DEFAULT_TITLE, styles.title);
    merge(sheet, 0, 0, 0, COLUMN_COUNT - 1);

    Row customerRow = sheet.createRow(rowIndex++);
    customerRow.setHeightInPoints(20);
    setCell(customerRow, 0, "客户: " + CUSTOMER, styles.infoLeft);
    merge(sheet, 1, 1, 0, COLUMN_COUNT - 1);

    Row infoRow = sheet.createRow(rowIndex++);
    infoRow.setHeightInPoints(20);
    setCell(infoRow, 0, "收货地址：" + defaultString(data.getCustomerName()), styles.infoLeft);
    merge(sheet, 2, 2, 0, 4);
    setCell(infoRow, 5, "单据日期: " + formatDate(data.getOrderDate()), styles.infoRight);
    merge(sheet, 2, 2, 5, 7);

    Row headerRow = sheet.createRow(rowIndex++);
    headerRow.setHeightInPoints(20);
    for (int i = 0; i < TABLE_HEADERS.length; i++) {
      setCell(headerRow, i, TABLE_HEADERS[i], styles.header);
    }

    List<DetailData> details = data.getDetails() == null ? new ArrayList<>() : data.getDetails();
    for (int i = 0; i < details.size(); i++) {
      DetailData detail = details.get(i);
      Row row = sheet.createRow(rowIndex++);
      row.setHeightInPoints(18);
      setCell(row, 0, String.valueOf(i + 1), styles.bodyCenter);
      setCell(row, 1, defaultString(detail.getProductName()), styles.bodyLeft);
      setCell(row, 2, defaultString(detail.getSpec()), styles.bodyLeft);
      setCell(row, 3, defaultString(detail.getUnit()), styles.bodyCenter);
      setCell(row, 4, formatNumber(detail.getQty()), styles.bodyRight);
      setCell(row, 5, formatAmount(detail.getPrice()), styles.bodyRight);
      setCell(row, 6, formatAmount(detail.getAmount()), styles.bodyRight);
      setCell(row, 7, defaultString(detail.getRemark()), styles.bodyLeft);
      setCell(row, 8, formatNumber(detail.getConfirmQty()), styles.bodyRight);
      setCell(row, 9, formatAmount(detail.getConfirmAmount()), styles.bodyRight);
    }

    Row totalRow = sheet.createRow(rowIndex);
    totalRow.setHeightInPoints(20);
    setCell(totalRow, 0, "合计：", styles.totalLabel);
    CellRangeAddress totalRegion = merge(sheet, rowIndex, rowIndex, 0, 3);
    setCell(totalRow, 4, formatNumber(data.getTotalQty()), styles.totalQty);
    setCell(totalRow, 5, "", styles.totalBlank);
    setCell(totalRow, 6, formatAmount(data.getTotalAmount()), styles.totalAmount);
    setCell(totalRow, 7, "", styles.totalBlank);
    setCell(totalRow, 8, formatNumber(data.getTotalConfirmQty()), styles.totalQty);
    setCell(totalRow, 9, formatAmount(data.getTotalConfirmAmount()), styles.totalAmount);
    RegionUtil.setBorderBottom(BorderStyle.THIN, totalRegion, sheet);
    RegionUtil.setBorderLeft(BorderStyle.THIN, totalRegion, sheet);
    RegionUtil.setBorderRight(BorderStyle.THIN, totalRegion, sheet);

    Row signRow = sheet.createRow(++rowIndex);
    signRow.setHeightInPoints(22);
    setCell(signRow, 0, "验收人：", styles.signLeft);
    merge(sheet, rowIndex, rowIndex, 0, 1);
    setCell(signRow, 2, "", styles.signBlank);
    setCell(signRow, 3, "制单人：", styles.signLeft);
    merge(sheet, rowIndex, rowIndex, 3, 5);
    setCell(signRow, 6, "送货人：", styles.signLeft);
    merge(sheet, rowIndex, rowIndex, 6, 9);
  }

  private static void setColumnWidths(Sheet sheet) {
    int[] widths = { 8, 28, 18, 10, 10, 12, 14, 20, 10, 14 };
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i] * 256);
    }
  }

  private static void setCell(Row row, int colIndex, String value, CellStyle style) {
    Cell cell = row.createCell(colIndex);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private static CellRangeAddress merge(Sheet sheet, int firstRow, int lastRow, int firstCol,
      int lastCol) {
    CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
    sheet.addMergedRegion(region);
    return region;
  }

  private static String buildSheetName(String code, int index) {
    String name = code;
    if (name == null || name.trim().isEmpty()) {
      name = "销售单" + index;
    }

    String sanitized = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
    return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
  }

  private static String defaultString(String value) {
    return defaultString(value, "");
  }

  private static String defaultString(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static String formatDate(LocalDate date) {
    return date == null ? "" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private static String formatNumber(BigDecimal num) {
    if (num == null) {
      return "";
    }

    return num.stripTrailingZeros().toPlainString();
  }

  private static String formatAmount(BigDecimal num) {
    if (num == null) {
      return "";
    }

    return num.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
  }

  @Data
  public static class SheetData {
    private String code;
    private String title;
    private String customerName;
    private String address;
    private LocalDate orderDate;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private BigDecimal totalConfirmQty;
    private BigDecimal totalConfirmAmount;
    private List<DetailData> details = new ArrayList<>();
  }

  @Data
  public static class DetailData {
    private String productName;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
    private String remark;
    private BigDecimal confirmQty;
    private BigDecimal confirmAmount;
  }

  private static class Styles {
    private final XSSFWorkbook workbook;
    private final CellStyle title;
    private final CellStyle infoLeft;
    private final CellStyle infoRight;
    private final CellStyle header;
    private final CellStyle bodyLeft;
    private final CellStyle bodyCenter;
    private final CellStyle bodyRight;
    private final CellStyle totalLabel;
    private final CellStyle totalQty;
    private final CellStyle totalAmount;
    private final CellStyle totalBlank;
    private final CellStyle signLeft;
    private final CellStyle signBlank;

    private Styles(XSSFWorkbook workbook) {
      this.workbook = workbook;
      this.title = createTitleStyle(workbook);
      this.infoLeft = createInfoStyle(workbook, HorizontalAlignment.LEFT);
      this.infoRight = createInfoStyle(workbook, HorizontalAlignment.RIGHT);
      this.header = createHeaderStyle(workbook);
      this.bodyLeft = createBodyStyle(workbook, HorizontalAlignment.LEFT);
      this.bodyCenter = createBodyStyle(workbook, HorizontalAlignment.CENTER);
      this.bodyRight = createBodyStyle(workbook, HorizontalAlignment.RIGHT);
      this.totalLabel = createTotalStyle(workbook, HorizontalAlignment.CENTER);
      this.totalQty = createTotalStyle(workbook, HorizontalAlignment.RIGHT);
      this.totalAmount = createTotalStyle(workbook, HorizontalAlignment.RIGHT);
      this.totalBlank = createTotalStyle(workbook, HorizontalAlignment.LEFT);
      this.signLeft = createSignStyle(workbook, HorizontalAlignment.LEFT);
      this.signBlank = createSignStyle(workbook, HorizontalAlignment.LEFT);
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
      CellStyle style = workbook.createCellStyle();
      style.setFont(createFont(workbook, true));
      style.setAlignment(HorizontalAlignment.CENTER);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      return style;
    }

    private CellStyle createInfoStyle(XSSFWorkbook workbook, HorizontalAlignment alignment) {
      CellStyle style = workbook.createCellStyle();
      style.setFont(createFont(workbook, false));
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
      CellStyle style = createBorderStyle(workbook);
      style.setFont(createFont(workbook, true));
      style.setAlignment(HorizontalAlignment.CENTER);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook, HorizontalAlignment alignment) {
      CellStyle style = createBorderStyle(workbook);
      style.setFont(createFont(workbook, false));
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      return style;
    }

    private CellStyle createTotalStyle(XSSFWorkbook workbook, HorizontalAlignment alignment) {
      CellStyle style = createBorderStyle(workbook);
      style.setFont(createFont(workbook, true));
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      style.setBorderBottom(BorderStyle.THIN);
      return style;
    }

    private CellStyle createSignStyle(XSSFWorkbook workbook, HorizontalAlignment alignment) {
      CellStyle style = workbook.createCellStyle();
      style.setFont(createFont(workbook, true));
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      return style;
    }

    private Font createFont(XSSFWorkbook workbook, boolean bold) {
      Font font = workbook.createFont();
      font.setFontName(DEFAULT_FONT_NAME);
      font.setBold(bold);
      font.setFontHeightInPoints(DEFAULT_FONT_SIZE);
      return font;
    }

    private CellStyle createBorderStyle(XSSFWorkbook workbook) {
      CellStyle style = workbook.createCellStyle();
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
      return style;
    }
  }
}
