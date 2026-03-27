package com.lframework.xingyun.core.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EasyExcelUtils {
    // 默认列宽
    private static final int DEFAULT_COLUMN_WIDTH = 20;
    // 默认表头高度
    private static final short DEFAULT_HEADER_HEIGHT = 30;
    // 默认内容高度 
    private static final short DEFAULT_CONTENT_HEIGHT = 20;

    /**
     * 同步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param filePath excel文件的绝对路径
     */
    public static List<Map<Integer, String>> syncRead(String filePath) {
        return EasyExcelFactory.read(filePath).sheet().doReadSync();
    }

    /**
     * 同步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param inputStream excel文件的输入流
     */
    public static List<Map<Integer, String>> syncRead(InputStream inputStream) {
        return EasyExcelFactory.read(inputStream).sheet().doReadSync();
    }

    /**
     * 同步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param file excel文件
     */
    public static List<Map<Integer, String>> syncRead(File file) {
        return EasyExcelFactory.read(file).sheet().doReadSync();
    }

    /**
     * 同步无模型读（自定义读取sheetX，从第2行开始读）
     *
     * @param filePath excel文件的绝对路径
     * @param sheetNo  sheet页号，从0开始
     */
    public static List<Map<Integer, String>> syncRead(String filePath, Integer sheetNo) {
        return EasyExcelFactory.read(filePath).sheet(sheetNo).doReadSync();
    }

    /**
     * 同步无模型读（自定义读取sheetX，从第2行开始读）
     *
     * @param inputStream excel文件的输入流
     * @param sheetNo     sheet页号，从0开始
     */
    public static List<Map<Integer, String>> syncRead(InputStream inputStream, Integer sheetNo) {
        return EasyExcelFactory.read(inputStream).sheet(sheetNo).doReadSync();
    }

    /**
     * 同步无模型读（自定义读取sheetX，从第2行开始读）
     *
     * @param file    excel文件
     * @param sheetNo sheet页号，从0开始
     */
    public static List<Map<Integer, String>> syncRead(File file, Integer sheetNo) {
        return EasyExcelFactory.read(file).sheet(sheetNo).doReadSync();
    }

    /**
     * 同步无模型读（指定sheet和表头占的行数）
     *
     * @param filePath
     * @param sheetNo    sheet页号，从0开始
     * @param headRowNum 表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static List<Map<Integer, String>> syncRead(String filePath, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(filePath).sheet(sheetNo).headRowNumber(headRowNum).doReadSync();
    }

    /**
     * 同步无模型读（指定sheet和表头占的行数）
     *
     * @param inputStream
     * @param sheetNo     sheet页号，从0开始
     * @param headRowNum  表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static List<Map<Integer, String>> syncRead(InputStream inputStream, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(inputStream).sheet(sheetNo).headRowNumber(headRowNum).doReadSync();
    }

    /**
     * 同步无模型读（指定sheet和表头占的行数）
     *
     * @param file
     * @param sheetNo    sheet页号，从0开始
     * @param headRowNum 表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static List<Map<Integer, String>> syncRead(File file, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(file).sheet(sheetNo).headRowNumber(headRowNum).doReadSync();
    }

    /**
     * 同步按模型读（默认读取sheet0,从第2行开始读）
     *
     * @param filePath
     * @param clazz    模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> List<T> syncReadModel(String filePath, Class clazz) {
        return EasyExcelFactory.read(filePath).sheet().head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（默认读取sheet0,从第2行开始读）
     *
     * @param inputStream
     * @param clazz       模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> List<T> syncReadModel(InputStream inputStream, Class clazz) {
        return EasyExcelFactory.read(inputStream).sheet().head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（默认读取sheet0,从第2行开始读）
     *
     * @param file
     * @param clazz 模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> List<T> syncReadModel(File file, Class clazz) {
        return EasyExcelFactory.read(file).sheet().head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（默认表头占一行，从第2行开始读）
     *
     * @param filePath
     * @param clazz    模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo  sheet页号，从0开始
     */
    public static <T> List<T> syncReadModel(String filePath, Class clazz, Integer sheetNo) {
        return EasyExcelFactory.read(filePath).sheet(sheetNo).head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（默认表头占一行，从第2行开始读）
     *
     * @param inputStream
     * @param clazz       模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo     sheet页号，从0开始
     */
    public static <T> List<T> syncReadModel(InputStream inputStream, Class clazz, Integer sheetNo) {
        return EasyExcelFactory.read(inputStream).sheet(sheetNo).head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（默认表头占一行，从第2行开始读）
     *
     * @param file
     * @param clazz   模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo sheet页号，从0开始
     */
    public static <T> List<T> syncReadModel(File file, Class clazz, Integer sheetNo) {
        return EasyExcelFactory.read(file).sheet(sheetNo).head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（指定sheet和表头占的行数）
     *
     * @param filePath
     * @param clazz      模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo    sheet页号，从0开始
     * @param headRowNum 表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> List<T> syncReadModel(String filePath, Class clazz, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(filePath).sheet(sheetNo).headRowNumber(headRowNum).head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（指定sheet和表头占的行数）
     *
     * @param inputStream
     * @param clazz       模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo     sheet页号，从0开始
     * @param headRowNum  表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> List<T> syncReadModel(InputStream inputStream, Class clazz, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(inputStream).sheet(sheetNo).headRowNumber(headRowNum).head(clazz).doReadSync();
    }

    /**
     * 同步按模型读（指定sheet和表头占的行数）
     *
     * @param file
     * @param clazz      模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo    sheet页号，从0开始
     * @param headRowNum 表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> List<T> syncReadModel(File file, Class clazz, Integer sheetNo, Integer headRowNum) {
        return EasyExcelFactory.read(file).sheet(sheetNo).headRowNumber(headRowNum).head(clazz).doReadSync();
    }

    /**
     * 异步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param filePath      表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncRead(String filePath, AnalysisEventListener<T> excelListener) {
        EasyExcelFactory.read(filePath, excelListener).sheet().doRead();
    }

    /**
     * 异步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param inputStream   表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncRead(InputStream inputStream, AnalysisEventListener<T> excelListener) {
        EasyExcelFactory.read(inputStream, excelListener).sheet().doRead();
    }

    /**
     * 异步无模型读（默认读取sheet0,从第2行开始读）
     *
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param file          表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncRead(File file, AnalysisEventListener<T> excelListener) {
        EasyExcelFactory.read(file, excelListener).sheet().doRead();
    }

    /**
     * 异步无模型读（默认表头占一行，从第2行开始读）
     *
     * @param filePath      表头占的行数，从0开始（如果要连表头一起读出来则传0）
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncRead(String filePath, AnalysisEventListener<T> excelListener, Integer sheetNo) {
        EasyExcelFactory.read(filePath, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步无模型读（默认表头占一行，从第2行开始读）
     *
     * @param inputStream   表头占的行数，从0开始（如果要连表头一起读出来则传0）
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncRead(InputStream inputStream, AnalysisEventListener<T> excelListener, Integer sheetNo) {
        EasyExcelFactory.read(inputStream, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步无模型读（默认表头占一行，从第2行开始读）
     *
     * @param file          表头占的行数，从0开始（如果要连表头一起读出来则传0）
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncRead(File file, AnalysisEventListener<T> excelListener, Integer sheetNo) {
        EasyExcelFactory.read(file, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步无模型读（指定sheet和表头占的行数）
     *
     * @param filePath
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     * @return
     */
    public static <T> void asyncRead(String filePath, AnalysisEventListener<T> excelListener, Integer sheetNo,
                                     Integer headRowNum) {
        EasyExcelFactory.read(filePath, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 异步无模型读（指定sheet和表头占的行数）
     *
     * @param inputStream
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncRead(InputStream inputStream, AnalysisEventListener<T> excelListener, Integer sheetNo,
                                     Integer headRowNum) {
        EasyExcelFactory.read(inputStream, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 异步无模型读（指定sheet和表头占的行数）
     *
     * @param file
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncRead(File file, AnalysisEventListener<T> excelListener, Integer sheetNo,
                                     Integer headRowNum) {
        EasyExcelFactory.read(file, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 异步按模型读取（默认读取sheet0,从第2行开始读）
     *
     * @param filePath
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> void asyncReadModel(String filePath, AnalysisEventListener<T> excelListener, Class clazz) {
        EasyExcelFactory.read(filePath, clazz, excelListener).sheet().doRead();
    }

    /**
     * 异步按模型读取（默认读取sheet0,从第2行开始读）
     *
     * @param inputStream
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> void asyncReadModel(InputStream inputStream, AnalysisEventListener<T> excelListener,
                                          Class clazz) {
        EasyExcelFactory.read(inputStream, clazz, excelListener).sheet().doRead();
    }

    /**
     * 异步按模型读取（默认读取sheet0,从第2行开始读）
     *
     * @param file
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     */
    public static <T> void asyncReadModel(File file, AnalysisEventListener<T> excelListener, Class clazz) {
        EasyExcelFactory.read(file, clazz, excelListener).sheet().doRead();
    }

    /**
     * 异步按模型读取（默认表头占一行，从第2行开始读）
     *
     * @param filePath
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncReadModel(String filePath, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo) {
        EasyExcelFactory.read(filePath, clazz, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步按模型读取（默认表头占一行，从第2行开始读）
     *
     * @param inputStream
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncReadModel(InputStream inputStream, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo) {
        EasyExcelFactory.read(inputStream, clazz, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步按模型读取（默认表头占一行，从第2行开始读）
     *
     * @param file
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     */
    public static <T> void asyncReadModel(File file, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo) {
        EasyExcelFactory.read(file, clazz, excelListener).sheet(sheetNo).doRead();
    }

    /**
     * 异步按模型读取
     *
     * @param filePath
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncReadModel(String filePath, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo, Integer headRowNum) {
        EasyExcelFactory.read(filePath, clazz, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 异步按模型读取
     *
     * @param inputStream
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncReadModel(InputStream inputStream, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo, Integer headRowNum) {
        EasyExcelFactory.read(inputStream, clazz, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 异步按模型读取
     *
     * @param file
     * @param excelListener 监听器，在监听器中可以处理行数据LinkedHashMap，表头数据，异常处理等
     * @param clazz         模型的类类型（excel数据会按该类型转换成对象）
     * @param sheetNo       sheet页号，从0开始
     * @param headRowNum    表头占的行数，从0开始（如果要连表头一起读出来则传0）
     */
    public static <T> void asyncReadModel(File file, AnalysisEventListener<T> excelListener, Class clazz,
                                          Integer sheetNo, Integer headRowNum) {
        EasyExcelFactory.read(file, clazz, excelListener).sheet(sheetNo).headRowNumber(headRowNum).doRead();
    }

    /**
     * 无模板写文件
     *
     * @param filePath
     * @param head     表头数据
     * @param data     表内容数据
     */
    public static void write(String filePath, List<List<String>> head, List<List<Object>> data) {
        EasyExcel.write(filePath).head(head).sheet().doWrite(data);
    }

    /**
     * 无模板写文件
     *
     * @param outputStream
     * @param head         表头数据
     * @param data         表内容数据
     */
    public static void write(OutputStream outputStream, List<List<String>> head, List<List<Object>> data) {
        EasyExcel.write(outputStream).head(head).sheet().doWrite(data);
    }

    /**
     * 无模板写文件
     *
     * @param filePath
     * @param head     表头数据
     * @param data     表内容数据
     * @param sheetNo  sheet页号，从0开始
     */
    public static void write(String filePath, List<List<String>> head, List<List<Object>> data, Integer sheetNo) {
        EasyExcel.write(filePath).head(head).sheet(sheetNo).doWrite(data);
    }

    /**
     * 无模板写文件
     *
     * @param outputStream
     * @param head         表头数据
     * @param data         表内容数据
     * @param sheetNo      sheet页号，从0开始
     */
    public static void write(OutputStream outputStream, List<List<String>> head, List<List<Object>> data,
                             Integer sheetNo) {
        EasyExcel.write(outputStream).head(head).sheet(sheetNo).doWrite(data);
    }

    /**
     * 无模板写文件
     *
     * @param filePath
     * @param head      表头数据
     * @param data      表内容数据
     * @param sheetNo   sheet页号，从0开始
     * @param sheetName sheet名称
     */
    public static void write(String filePath, List<List<String>> head, List<List<Object>> data, Integer sheetNo,
                             String sheetName) {
        EasyExcel.write(filePath).head(head).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 无模板写文件
     *
     * @param outputStream
     * @param head         表头数据
     * @param data         表内容数据
     * @param sheetNo      sheet页号，从0开始
     * @param sheetName    sheet名称
     */
    public static void write(OutputStream outputStream, List<List<String>> head, List<List<Object>> data,
                             Integer sheetNo, String sheetName) {
        EasyExcel.write(outputStream).head(head).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param filePath
     * @param templateFileName
     * @param data
     */
    public static void writeTemplate(String filePath, String templateFileName, List data) {
        EasyExcel.write(filePath).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param outputStream
     * @param templateFileName
     * @param data
     */
    public static void writeTemplate(OutputStream outputStream, String templateFileName, List data) {
        EasyExcel.write(outputStream).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param file
     * @param templateFileName
     * @param data
     */
    public static void writeTemplate(File file, String templateFileName, List data) {
        EasyExcel.write(file).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param filePath
     * @param templateFileName
     * @param headClazz
     * @param data
     */
    public static void writeTemplate(String filePath, String templateFileName, Class headClazz, List data) {
        EasyExcel.write(filePath, headClazz).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param outputStream
     * @param templateFileName
     * @param headClazz
     * @param data
     */
    public static void writeTemplate(OutputStream outputStream, String templateFileName, Class headClazz, List data) {
        EasyExcel.write(outputStream, headClazz).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 根据excel模板文件写入文件
     *
     * @param file
     * @param templateFileName
     * @param headClazz
     * @param data
     */
    public static void writeTemplate(File file, String templateFileName, Class headClazz, List data) {
        EasyExcel.write(file, headClazz).withTemplate(templateFileName).sheet().doFill(data);
    }

    /**
     * 按模板写文件
     *
     * @param filePath
     * @param headClazz 表头模板
     * @param data      数据
     */
    public static void write(String filePath, Class headClazz, List data) {
        EasyExcel.write(filePath, headClazz).sheet().doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param outputStream
     * @param headClazz    表头模板
     * @param data         数据
     */
    public static void write(OutputStream outputStream, Class headClazz, List data) {
        EasyExcel.write(outputStream, headClazz).sheet().doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param response
     * @param headClazz 表头模板
     * @param data      数据
     */
    public static void write(HttpServletResponse response, Class headClazz, List data) throws IOException {
        OutputStream outputStream = formatResponse(response);
        ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream, headClazz);
        buildCommonSheetBuilder(writerBuilder).doWrite(data);
    }

    /**
     * 格式化输出流，设置响应头，解决文件名乱码问题
     * @param response
     * @return
     * @throws IOException
     */
    private static OutputStream formatResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 使用通用工具类设置响应头，解决文件名乱码问题
        // fileName = fileName + "_" + DateUtils.dateTimeNow() + ".xlsx";
        // FileUtils.setAttachmentResponseHeader(response, fileName);

        return response.getOutputStream();
    }

    /**
     * 构建通用的Excel格式
     *
     * @param input Excel写入器构建器
     * @return 通用的Excel写入器构建器
     */
    private static ExcelWriterSheetBuilder buildCommonSheetBuilder(ExcelWriterBuilder input) {
        ExcelWriterSheetBuilder res = input.excelType(ExcelTypeEnum.XLSX)
            .registerWriteHandler(getHorizontalCellStyleStrategy())
            .registerWriteHandler(new SimpleColumnWidthStyleStrategy(DEFAULT_COLUMN_WIDTH)) // 设置列宽为20
            .registerWriteHandler(new SimpleRowHeightStyleStrategy(DEFAULT_HEADER_HEIGHT, DEFAULT_CONTENT_HEIGHT))
            .sheet()
            .sheetName("Sheet1");
        return res;
    }

    /**
     * 按模板写文件
     *
     * @param file
     * @param headClazz 表头模板
     * @param data      数据
     */
    public static void write(File file, Class headClazz, List data) {
        EasyExcel.write(file, headClazz).sheet().doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param filePath
     * @param headClazz 表头模板
     * @param data      数据
     * @param sheetNo   sheet页号，从0开始
     */
    public static void write(String filePath, Class headClazz, List data, Integer sheetNo) {
        EasyExcel.write(filePath, headClazz).sheet(sheetNo).doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param response
     * @param headClazz    表头模板
     * @param data         数据
     * @param sheetNo      sheet页号，从0开始
     */
//    public static void write(HttpServletResponse response, String fileName, Class headClazz, List data, Integer sheetNo)
//            throws IOException {
//        OutputStream outputStream = formatResponse(response, fileName);
//        EasyExcel.write(outputStream, headClazz)
//                .registerWriteHandler(getHorizontalCellStyleStrategy())
//                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(DEFAULT_COLUMN_WIDTH)) // 设置列宽为20
//                .registerWriteHandler(new SimpleRowHeightStyleStrategy(DEFAULT_HEADER_HEIGHT, DEFAULT_CONTENT_HEIGHT))
//                .sheet(sheetNo).doWrite(data);
//    }

    /**
     * 按模板写文件
     *
     * @param file
     * @param headClazz 表头模板
     * @param data      数据
     * @param sheetNo   sheet页号，从0开始
     */
    public static void write(File file, Class headClazz, List data, Integer sheetNo) {
        EasyExcel.write(file, headClazz).sheet(sheetNo).doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param filePath
     * @param headClazz 表头模板
     * @param data      数据
     * @param sheetNo   sheet页号，从0开始
     * @param sheetName sheet名称
     */
    public static void write(String filePath, Class headClazz, List data, Integer sheetNo, String sheetName) {
        EasyExcel.write(filePath, headClazz).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param outputStream
     * @param headClazz    表头模板
     * @param data         数据
     * @param sheetNo      sheet页号，从0开始
     * @param sheetName    sheet名称
     */
    public static void write(OutputStream outputStream, Class headClazz, List data, Integer sheetNo, String sheetName) {
        EasyExcel.write(outputStream, headClazz).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param file
     * @param headClazz 表头模板
     * @param data      数据
     * @param sheetNo   sheet页号，从0开始
     * @param sheetName sheet名称
     */
    public static void write(File file, Class headClazz, List data, Integer sheetNo, String sheetName) {
        EasyExcel.write(file, headClazz).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 按模板写文件
     *
     * @param filePath
     * @param headClazz    表头模板
     * @param data         数据
     * @param writeHandler 自定义的处理器，比如设置table样式，设置超链接、单元格下拉框等等功能都可以通过这个实现（需要注册多个则自己通过链式去调用）
     * @param sheetNo      sheet页号，从0开始
     * @param sheetName    sheet名称
     */
    public static void write(String filePath, Class headClazz, List data, WriteHandler writeHandler, Integer sheetNo,
                             String sheetName) {
        EasyExcel.write(filePath, headClazz).registerWriteHandler(writeHandler).sheet(sheetNo, sheetName).doWrite(data);
    }

    /**
     * 按模板写文件（包含某些字段）
     *
     * @param filePath
     * @param headClazz   表头模板
     * @param data        数据
     * @param includeCols 包含字段集合，根据字段名称显示
     * @param sheetNo     sheet页号，从0开始
     * @param sheetName   sheet名称
     */
    public static void writeInclude(String filePath, Class headClazz, List data, Set<String> includeCols,
                                    Integer sheetNo, String sheetName) {
        EasyExcel.write(filePath, headClazz).includeColumnFieldNames(includeCols).sheet(sheetNo, sheetName)
            .doWrite(data);
    }

    /**
     * 按模板写文件（排除某些字段）
     *
     * @param filePath
     * @param headClazz   表头模板
     * @param data        数据
     * @param excludeCols 过滤排除的字段，根据字段名称过滤
     * @param sheetNo     sheet页号，从0开始
     * @param sheetName   sheet名称
     */
    public static void writeExclude(String filePath, Class headClazz, List data, Set<String> excludeCols,
                                    Integer sheetNo, String sheetName) {
        EasyExcel.write(filePath, headClazz).excludeColumnFieldNames(excludeCols).sheet(sheetNo, sheetName)
            .doWrite(data);
    }

    /**
     * 多个sheet页的数据链式写入
     * ExcelUtil.writeWithSheets(outputStream)
     * .writeModel(ExcelModel.class, excelModelList, "sheetName1")
     * .write(headData, data,"sheetName2")
     * .finish();
     *
     * @param response
     */
    public static EasyExcelWriterFactory writeWithSheets(HttpServletResponse response) throws IOException {
        OutputStream outputStream = formatResponse(response);
        EasyExcelWriterFactory excelWriter = new EasyExcelWriterFactory(outputStream);
        return excelWriter;
    }

    /**
     * 多个sheet页的数据链式写入
     * ExcelUtil.writeWithSheets(file)
     * .writeModel(ExcelModel.class, excelModelList, "sheetName1")
     * .write(headData, data,"sheetName2")
     * .finish();
     *
     * @param file
     */
    public static EasyExcelWriterFactory writeWithSheets(File file) {
        EasyExcelWriterFactory excelWriter = new EasyExcelWriterFactory(file);
        return excelWriter;
    }

    /**
     * 多个sheet页的数据链式写入
     * ExcelUtil.writeWithSheets(filePath)
     * .writeModel(ExcelModel.class, excelModelList, "sheetName1")
     * .write(headData, data,"sheetName2")
     * .finish();
     *
     * @param filePath
     */
    public static EasyExcelWriterFactory writeWithSheets(String filePath) {
        EasyExcelWriterFactory excelWriter = new EasyExcelWriterFactory(filePath);
        return excelWriter;
    }

    /**
     * 多个sheet页的数据链式写入（失败了会返回一个有部分数据的Excel）
     * ExcelUtil.writeWithSheets(response, exportFileName)
     * .writeModel(ExcelModel.class, excelModelList, "sheetName1")
     * .write(headData, data,"sheetName2")
     * .finish();
     *
     * @param response
     * @param exportFileName 导出的文件名称
     */
    public static EasyExcelWriterFactory writeWithSheetsWeb(HttpServletResponse response, String exportFileName)
        throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = URLEncoder.encode(exportFileName, "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        EasyExcelWriterFactory excelWriter = new EasyExcelWriterFactory(response.getOutputStream());
        return excelWriter;
    }

    public static HorizontalCellStyleStrategy getHorizontalCellStyleStrategy() {
        // 头的策略
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // 背景设置为白色
        headWriteCellStyle.setFillForegroundColor(IndexedColors.ORANGE.index);
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        //边框
        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);
        //自动换行
        headWriteCellStyle.setWrapped(true);
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setBold(true);
        headWriteFont.setFontName("宋体");
        headWriteFont.setFontHeightInPoints((short) 12);
        headWriteCellStyle.setWriteFont(headWriteFont);


        // 内容的策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // 这里需要指定 FillPatternType 为FillPatternType.SOLID_FOREGROUND 不然无法显示背景颜色.头默认了 FillPatternType所以可以不指定
        contentWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        // 背景绿色
        contentWriteCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        //边框
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        //自动换行
        contentWriteCellStyle.setWrapped(true);
        //文字
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 12);
        contentWriteFont.setFontName("宋体");
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        // 这个策略是 头是头的样式 内容是内容的样式 其他的策略可以自己实现
        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }
}
