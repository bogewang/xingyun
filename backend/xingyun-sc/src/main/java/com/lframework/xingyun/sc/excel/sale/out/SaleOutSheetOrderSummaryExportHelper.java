package com.lframework.xingyun.sc.excel.sale.out;

import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** 按采购结算单模板导出选中的销售出库单。 */
public final class SaleOutSheetOrderSummaryExportHelper {

  /** 禁止实例化工具类。 */
  private SaleOutSheetOrderSummaryExportHelper() {
  }

  /** 输出订单汇总文件。 */
  public static void export(List<QuerySaleOutSheetDetailDto> details,
      HttpServletResponse response) throws IOException {
    try (XSSFWorkbook workbook = buildWorkbook(details)) {
      String filename = URLEncoder.encode("订单汇总.xlsx", "UTF-8");
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader("filename", filename);
      response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename);
      workbook.write(response.getOutputStream());
      response.flushBuffer();
    }
  }

  /** 在同一工作表中逐单排列，保留每张单据的明细和签字栏。 */
  public static XSSFWorkbook buildWorkbook(List<QuerySaleOutSheetDetailDto> details) {
    XSSFWorkbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("订单汇总");
    double[] widths = {6, 19, 6, 11, 11, 11, 12, 11, 8};
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, (int) (widths[i] * 256));
    }
    CellStyle body = style(workbook, false, true);
    body.setAlignment(HorizontalAlignment.CENTER);
    CellStyle info = style(workbook, false, false);
    CellStyle summary = workbook.createCellStyle();
    summary.cloneStyleFrom(info);
    summary.setAlignment(HorizontalAlignment.CENTER);
    CellStyle title = style(workbook, true, false);
    title.setAlignment(HorizontalAlignment.CENTER);
    CellStyle number = workbook.createCellStyle();
    number.cloneStyleFrom(body);
    number.setDataFormat(workbook.createDataFormat().getFormat("General"));
    CellStyle amount = workbook.createCellStyle();
    amount.cloneStyleFrom(body);
    amount.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
    Map<String, List<QuerySaleOutSheetDetailDto>> groups = details.stream().collect(
        Collectors.groupingBy(QuerySaleOutSheetDetailDto::getId, LinkedHashMap::new,
            Collectors.toList()));
    int row = 0;
    String[] headers = {"序号", "商品名称", "单位", "计划数量", "送货数量", "下单单价", "下单小计", "验收情况", "备注"};
    for (List<QuerySaleOutSheetDetailDto> group : groups.values()) {
      QuerySaleOutSheetDetailDto first = group.get(0);
      merged(sheet, row++, 0, 8, "生活服务中心采购结算单", title);
      merged(sheet, row++, 0, 8, "日期：" + first.getOrderDate(), info);
      merged(sheet, row++, 0, 8, "客户名称：" + first.getCustomerName(), info);
      Row header = sheet.createRow(row++);
      header.setHeightInPoints(28);
      for (int i = 0; i < headers.length; i++) {
        cell(header, i, headers[i], body);
      }
      BigDecimal total = BigDecimal.ZERO;
      int sequence = 1;
      for (QuerySaleOutSheetDetailDto detail : group) {
        Row line = sheet.createRow(row++);
        line.setHeightInPoints(25);
        cell(line, 0, sequence++, number);
        cell(line, 1, detail.getProductName(), body);
        cell(line, 2, detail.getUnit(), body);
        // 当前出库单的下单数量同时作为计划数量与送货数量，验收情况留给签收人填写。
        cell(line, 3, detail.getOrderNum(), number);
        cell(line, 4, detail.getOrderNum(), number);
        cell(line, 5, detail.getTaxPrice(), amount);
        cell(line, 6, detail.getTaxAmount(), amount);
        cell(line, 7, "", body);
        cell(line, 8, detail.getDescription(), body);
        if (detail.getTaxAmount() != null) {
          total = total.add(detail.getTaxAmount());
        }
      }
      merged(sheet, row, 0, 4, "第1 / 共1页", summary);
      merged(sheet, row++, 5, 8,
          "下单金额合计" + total.setScale(2, RoundingMode.HALF_UP).toPlainString(), summary);
      merged(sheet, row, 0, 2, "生活服务中心签字：", info);
      merged(sheet, row, 3, 5, "值班司务长签字：", info);
      merged(sheet, row++, 6, 8, "风气监督员：", info);
      merged(sheet, row, 0, 2, "炊事班长：", info);
      merged(sheet, row, 3, 5, "给养员签字：", info);
      merged(sheet, row++, 6, 8, "厨房值班员：", info);
      // 单据间空白行横跨整个表格，增加留白便于区分相邻单据。
      sheet.createRow(row).setHeightInPoints(50);
      sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 8));
      row++;
    }
    sheet.getPrintSetup().setLandscape(false);
    sheet.getPrintSetup().setFitWidth((short) 1);
    sheet.getPrintSetup().setFitHeight((short) 0);
    sheet.setFitToPage(true);
    // 缩小左右页边距，让单页宽度适配时充分使用纸张的可打印区域。
    sheet.setMargin(Sheet.LeftMargin, 0.25);
    sheet.setMargin(Sheet.RightMargin, 0.25);
    return workbook;
  }

  /** 创建与模板一致的字体、对齐和边框。 */
  private static CellStyle style(XSSFWorkbook workbook, boolean bold, boolean border) {
    Font font = workbook.createFont();
    font.setFontName("宋体");
    font.setFontHeightInPoints((short) (bold ? 14 : 9));
    font.setBold(bold);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setWrapText(true);
    if (border) {
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
    }
    return style;
  }

  /** 写入合并行并设置整个合并区域的四周边框，避免仅首格有边框。 */
  private static void merged(Sheet sheet, int index, int from, int to, String value,
      CellStyle style) {
    Row row = sheet.getRow(index);
    if (row == null) {
      row = sheet.createRow(index);
      row.setHeightInPoints(25);
    }
    cell(row, from, value, style);
    CellRangeAddress region = new CellRangeAddress(index, index, from, to);
    sheet.addMergedRegion(region);
    RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
    RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
    RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
    RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
  }

  /** 写入真实数值或文本，避免金额和数量变成文本。 */
  private static void cell(Row row, int column, Object value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellStyle(style);
    if (value instanceof Number) {
      cell.setCellValue(((Number) value).doubleValue());
    } else {
      cell.setCellValue(value == null ? "" : value.toString());
    }
  }
}
