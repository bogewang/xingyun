package com.lframework.xingyun.basedata.excel.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetService;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteSheetVo;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 报价单商品明细导出任务处理器。 */
public class QuoteSheetDetailExportTaskWorker implements
    ExportTaskWorker<QueryQuoteSheetVo, QuoteSheetDetailExportDto, QuoteSheetDetailExportModel> {

  /**
   * 解析导出筛选条件。
   *
   * @param json 导出参数 JSON
   * @return 报价单查询条件
   */
  @Override
  public QueryQuoteSheetVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryQuoteSheetVo.class);
  }

  /**
   * 分页查询报价单，并批量组装对应的商品明细。
   *
   * @param pageIndex 页码
   * @param pageSize 每页数量
   * @param params 查询条件
   * @return 待导出的报价单商品明细
   */
  @Override
  public PageResult<QuoteSheetDetailExportDto> getDataList(int pageIndex, int pageSize,
      QueryQuoteSheetVo params) {
    QuoteSheetService quoteSheetService = ApplicationUtil.getBean(QuoteSheetService.class);
    PageResult<QuoteSheet> sheets = quoteSheetService.query(pageIndex, pageSize, params);
    if (sheets.getDatas().isEmpty()) {
      return PageResultUtil.convert(new PageInfo<QuoteSheetDetailExportDto>());
    }

    List<String> quoteSheetIds = sheets.getDatas().stream().map(QuoteSheet::getId)
        .collect(Collectors.toList());
    QuoteSheetDetailMapper detailMapper = ApplicationUtil.getBean(QuoteSheetDetailMapper.class);
    List<QuoteSheetDetail> details = detailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class)
        .in(QuoteSheetDetail::getQuoteSheetId, quoteSheetIds));
    if (details.isEmpty()) {
      return PageResultUtil.convert(new PageInfo<QuoteSheetDetailExportDto>());
    }

    Map<String, QuoteSheet> sheetMap = sheets.getDatas().stream()
        .collect(Collectors.toMap(QuoteSheet::getId, Function.identity()));
    Map<String, Product> productMap = loadProducts(details);
    Map<String, String> unitNameMap = loadUnitNames(productMap.values());
    List<QuoteSheetDetailExportDto> data = details.stream()
        .map(detail -> toExportDto(detail, sheetMap.get(detail.getQuoteSheetId()),
            productMap.get(detail.getProductId()), unitNameMap))
        .collect(Collectors.toList());
    return PageResultUtil.convert(new PageInfo<>(data));
  }

  /**
   * 将导出数据转换为 Excel 行数据。
   *
   * @param data 报价单商品明细导出数据
   * @return Excel 行数据
   */
  @Override
  public QuoteSheetDetailExportModel exportData(QuoteSheetDetailExportDto data) {
    return new QuoteSheetDetailExportModel(data);
  }

  /**
   * 获取 Excel 行数据类型。
   *
   * @return Excel 行数据类型
   */
  @Override
  public Class<QuoteSheetDetailExportModel> getModelClass() {
    return QuoteSheetDetailExportModel.class;
  }

  /** 批量读取商品快照，兼容历史明细缺少快照的场景。 */
  private Map<String, Product> loadProducts(List<QuoteSheetDetail> details) {
    Map<String, Product> snapshotProducts = details.stream()
        .filter(detail -> StringUtil.isNotBlank(detail.getProductSnapshot()))
        .map(detail -> JsonUtil.parseObject(detail.getProductSnapshot(), Product.class))
        .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left));
    Set<String> missingProductIds = details.stream().map(QuoteSheetDetail::getProductId)
        .filter(productId -> !snapshotProducts.containsKey(productId)).collect(Collectors.toSet());
    if (missingProductIds.isEmpty()) {
      return snapshotProducts;
    }
    ProductService productService = ApplicationUtil.getBean(ProductService.class);
    snapshotProducts.putAll(productService.listByIds(missingProductIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity())));
    return snapshotProducts;
  }

  /** 批量读取单位字典名称，兼容快照中已保存的历史单位文本。 */
  private Map<String, String> loadUnitNames(Collection<Product> products) {
    Set<String> unitIds = products.stream().map(Product::getUnit).filter(StringUtil::isNotBlank)
        .collect(Collectors.toSet());
    if (unitIds.isEmpty()) {
      return Collections.emptyMap();
    }
    UnitService unitService = ApplicationUtil.getBean(UnitService.class);
    return unitService.list(Wrappers.lambdaQuery(Unit.class).in(Unit::getId, unitIds))
        .stream().collect(Collectors.toMap(Unit::getId, Unit::getName));
  }

  /** 将报价单、商品和报价明细组装为导出数据。 */
  private QuoteSheetDetailExportDto toExportDto(QuoteSheetDetail detail, QuoteSheet sheet,
      Product product, Map<String, String> unitNameMap) {
    QuoteSheetDetailExportDto result = new QuoteSheetDetailExportDto();
    result.setQuoteSheetName(sheet.getName());
    result.setStartDate(String.valueOf(sheet.getStartDate()));
    result.setEndDate(String.valueOf(sheet.getEndDate()));
    result.setStatus(sheet.getStatus().getDesc());
    result.setDescription(sheet.getDescription());
    result.setProductCode(product == null ? null : product.getCode());
    result.setProductName(product == null ? null : product.getName());
    result.setShortName(product == null ? null : product.getShortName());
    result.setSpec(product == null ? null : product.getSpec());
    result.setUnit(product == null ? null : resolveUnitName(product.getUnit(), unitNameMap));
    result.setSalePrice(detail.getSalePrice());
    result.setInquiryProduct(detail.getInquiryProduct());
    return result;
  }

  /** 将商品保存的单位 ID 转换为单位字典名称。 */
  static String resolveUnitName(String unitId, Map<String, String> unitNameMap) {
    return unitNameMap.getOrDefault(unitId, unitId);
  }
}
