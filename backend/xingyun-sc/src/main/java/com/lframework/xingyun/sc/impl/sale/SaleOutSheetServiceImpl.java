package com.lframework.xingyun.sc.impl.sale;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.*;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.*;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.entity.SysParameter;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.system.SysParameterService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.starter.web.inner.vo.system.parameter.QuerySysParameterVo;
import com.lframework.xingyun.basedata.entity.*;
import com.lframework.xingyun.basedata.enums.SettleType;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.basedata.vo.customer.QueryCustomerVo;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.bo.sale.out.GetSaleOutSheetBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitSummaryBo;
import com.lframework.xingyun.sc.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.*;
import com.lframework.xingyun.sc.dto.stock.ProductStockChangeDto;
import com.lframework.xingyun.sc.entity.*;
import com.lframework.xingyun.sc.enums.*;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetDetailExportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetInvoiceDetailExportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetSalesExportHelper;
import com.lframework.xingyun.sc.mappers.ProductStockMapper;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import com.lframework.xingyun.sc.service.ProductHotnessService;
import com.lframework.xingyun.sc.service.logistics.LogisticsSheetDetailService;
import com.lframework.xingyun.sc.service.sale.*;
import com.lframework.xingyun.sc.service.stock.ProductStockPendingCostService;
import com.lframework.xingyun.sc.service.stock.ProductStockLogService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.vo.sale.out.*;
import com.lframework.xingyun.sc.vo.stock.AddProductStockVo;
import com.lframework.xingyun.sc.vo.stock.SubProductStockVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lframework.xingyun.sc.impl.sale.SaleOutSheetMarketBuySummaryFormatter.formatUnit;

@Service
@Slf4j
public class SaleOutSheetServiceImpl extends
        BaseMpServiceImpl<SaleOutSheetMapper, SaleOutSheet> implements SaleOutSheetService {

    private static final String COST_PRICE_SOURCE_USE_STOCK_PRICE_PM_KEY = "sale_out_cost_price_use_stock_price";
    private static final String PRODUCT_SALE_PRICE_UNIQUE_PM_KEY = "sale_out_price_use_unique_price";
    private static final DateTimeFormatter QUERY_IMPORT_ACTUAL_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd");

    /**
     * 成本重算任务缓存（内存），key = taskId
     */
    private final ConcurrentHashMap<String, RecalculateTask> recalculateTaskCache = new ConcurrentHashMap<>();

    /**
     * 任务有效期（分钟）
     */
    private static final int TASK_EXPIRE_MINUTES = 30;

    /**
     * 成本重算任务（内部缓存模型）
     */
    @Data
    @AllArgsConstructor
    private static class RecalculateTask {
        private String taskId;
        private LocalDate calcBeginDate;
        private LocalDate calcEndDate;
        private String scId;
        private Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap;
        private Map<String, QueryReceiveSheetDetailDto> fallbackMap;
        private int totalDays;
        private LocalDateTime createdAt;
    }

    @Autowired
    private SaleOutSheetDetailService saleOutSheetDetailService;

    @Autowired
    private SaleOutSheetDetailLotService saleOutSheetDetailLotService;

    @Autowired
    private SaleOutSheetDetailBundleService saleOutSheetDetailBundleService;

    @Autowired
    private StoreCenterService storeCenterService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SaleOrderService saleOrderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductUnitService productUnitService;

    @Autowired
    private UnitService unitService;

    @Autowired
    private ProductLatestPriceCacheService productLatestPriceCacheService;

    @Autowired
    private ProductHotnessService productHotnessService;

    @Autowired
    private SysParameterService sysParameterService;

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private GenerateCodeService generateCodeService;

    @Autowired
    private SaleConfigService saleConfigService;

    @Autowired
    private SaleOrderDetailService saleOrderDetailService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockLogService productStockLogService;

    @Autowired
    private ProductStockPendingCostService productStockPendingCostService;

    @Autowired
    private LogisticsSheetDetailService logisticsSheetDetailService;

    @Autowired
    private ReceiveSheetDetailMapper receiveSheetDetailMapper;

    @Autowired
    private ProductStockMapper productStockMapper;

    @Autowired
    private SupplierService supplierService;

    @Override
    public PageResult<SaleOutSheet> query(Integer pageIndex, Integer pageSize,
            QuerySaleOutSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SaleOutSheet> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<SaleOutSheet> query(QuerySaleOutSheetVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public SaleOutSheetProfitSummaryBo queryProfitSummary(QuerySaleOutSheetVo vo) {

        return getBaseMapper().queryProfitSummary(vo);
    }

    @Override
    public SaleOutSheetProductProfitSummaryBo queryProductProfitSummary(QuerySaleOutSheetVo vo) {

        return getBaseMapper().queryProductProfitSummary(vo);
    }

    @Override
    public PageResult<QuerySaleOutSheetDetailDto> queryDetail(Integer pageIndex, Integer pageSize,
            QuerySaleOutSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<QuerySaleOutSheetDetailDto> datas = getBaseMapper().queryDetail(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public PageResult<QuerySaleOutSheetDetailDto> queryPriceCheckDetail(Integer pageIndex,
            Integer pageSize,
            QuerySaleOutSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<QuerySaleOutSheetDetailDto> datas = getBaseMapper().queryPriceCheckDetail(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public PageResult<SaleOutSheetProductProfitDto> queryProductProfit(Integer pageIndex,
            Integer pageSize,
            QuerySaleOutSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SaleOutSheetProductProfitDto> datas = getBaseMapper().queryProductProfit(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<SaleOutSheetProfitTrendDto> queryProfitTrend(QuerySaleOutSheetVo vo) {

        return getBaseMapper().queryProfitTrend(vo);
    }

    @Override
    public List<SaleOutSheetProductProfitTrendDto> queryProductProfitTrend(QuerySaleOutSheetVo vo) {

        return getBaseMapper().queryProductProfitTrend(vo);
    }

    @Override
    public List<PrintSaleTagBo> tagPrint(QuerySaleOutSheetVo vo) {

        PageResult<SaleOutSheet> result = this.query(1, Integer.MAX_VALUE, vo);
        if (CollectionUtils.isEmpty(result.getDatas())) {
            return Lists.newArrayList();
        }

        List<PrintSaleTagBo> res = Lists.newArrayList();
        result.getDatas().forEach(item -> {
            Customer customer = customerService.findById(item.getCustomerId());
            List<SaleOutSheetDetail> details = getSheetDetails(item.getId());
            details = filterTagPrintDetails(details, vo.getDetailIdList());
            if (CollectionUtils.isEmpty(details)) {
                return;
            }

            List<String> productIds = details.stream()
                    .map(SaleOutSheetDetail::getProductId)
                    .collect(Collectors.toList());
            // 组装成打印数据；若指定了分类筛选则只保留选中分类的商品
            Map<String, Product> productMap = productService.getBaseMapper().selectBatchIds(productIds).stream()
                    .filter(product -> {
                        // 如果指定了分类筛选，只保留选中分类的商品
                        if (CollectionUtils.isNotEmpty(vo.getCategoryIdList())) {
                            return vo.getCategoryIdList().contains(product.getCategoryId());
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(Product::getId, r -> r, (v1, v2) -> v2));

            // 每条销售明细单独生成标签，确保数量和备注与原始明细一一对应。
            List<PrintSaleTagBo> collect = details.stream()
                    .filter(detail -> productMap.containsKey(detail.getProductId()))
                    .map(detail -> {
                        Product product = productMap.get(detail.getProductId());

                        PrintSaleTagBo bo = new PrintSaleTagBo();
                        bo.setCustomerSimpleName(
                                customer.getNickName() == null ? customer.getName() : customer.getNickName());
                        bo.setProductName(product.getName());

                        String format = formatTagPrintNum(detail.getBusinessNum());
                        String unitName = detail.getUnitName();
                        // 添加备注
                        if (StringUtils.isNotBlank(detail.getDescription())) {
                            unitName = String.format("%s（%s）", unitName, detail.getDescription());
                        }
                        bo.setOrderNum(String.format("%s%s", format, unitName));
                        bo.setOrderDate(item.getOrderDate().toString());
                        bo.setCategoryId(product.getCategoryId());

                        return bo;
                    }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(collect)) {
                res.addAll(collect);
            }
        });

        return res.stream()
                .sorted(Comparator.comparing(PrintSaleTagBo::getCategoryId)
                        .thenComparing(PrintSaleTagBo::getProductName)
                        .thenComparing(PrintSaleTagBo::getOrderDate))
                .collect(Collectors.toList());
    }

    /**
     * 格式化标签打印数量：保留一位小数进行四舍五入，并移除末尾无意义的零。
     *
     * @param num 数量
     * @return 用于标签展示的数量
     */
    static String formatTagPrintNum(BigDecimal num) {
        return num.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * 根据指定明细ID过滤标签打印数据；未指定时保留单据全部明细。
     *
     * @param details 单据明细
     * @param detailIdList 指定的明细ID列表
     * @return 可用于标签打印的明细
     */
    static List<SaleOutSheetDetail> filterTagPrintDetails(List<SaleOutSheetDetail> details,
            List<String> detailIdList) {
        if (CollectionUtils.isEmpty(detailIdList)) {
            return details;
        }
        Set<String> detailIds = new HashSet<>(detailIdList);
        return details.stream().filter(detail -> detailIds.contains(detail.getId())).collect(Collectors.toList());
    }

    @Override
    public Boolean getPriceUniqueConfig() {

        QuerySysParameterVo sysParameterVo = new QuerySysParameterVo();
        sysParameterVo.setPmKey(PRODUCT_SALE_PRICE_UNIQUE_PM_KEY);
        List<SysParameter> list = sysParameterService.query(sysParameterVo);
        if (CollectionUtil.isEmpty(list)) {
            return Boolean.FALSE;
        }

        return BooleanUtil.toBoolean(list.get(0).getPmValue());
    }

    @Override
    public void marketBuySummary(QuerySaleOutSheetVo vo) {
        validateMarketBuySummaryIds(vo);

        boolean groupByDate = Boolean.TRUE.equals(vo.getGroupByDate());
        boolean mergeSameDayCustomerProduct = Boolean.TRUE.equals(
                vo.getMergeSameDayCustomerProduct());
        Map<String, String> headerMap = buildMarketBuySummaryHeaders(groupByDate);

        List<SaleOutSheet> sheets = this.query(vo);
        if (CollectionUtils.isEmpty(sheets)) {
            exportMarketBuySummary(headerMap, new ArrayList<>());
            return;
        }

        Map<String, SaleOutSheet> sheetMap = sheets.stream().collect(Collectors.toMap(
                SaleOutSheet::getId, item -> item, (v1, v2) -> v2));
        LinkedHashMap<String, String> customerNameMap = buildCustomerNameMap(sheets);

        List<SaleOutSheetDetail> details = queryMarketBuySummaryDetails(sheets);
        if (CollectionUtils.isEmpty(details)) {
            exportMarketBuySummary(headerMap, new ArrayList<>());
            return;
        }

        Map<String, Product> productMap = buildProductMap(details);
        Map<String, ProductCategory> categoryMap = buildCategoryMap(productMap);
        Map<String, String> productUnitNameMap = buildProductUnitNameMap(productMap);
        List<SummaryRow> summaryRows = buildSummaryRows(details, sheetMap, productMap, categoryMap,
                productUnitNameMap, groupByDate, mergeSameDayCustomerProduct);

        List<Map<String, String>> data = new ArrayList<>();
        for (SummaryRow row : summaryRows) {
            Map<String, String> map = new LinkedHashMap<>();
            if (groupByDate) {
                map.put("date", SaleOutSheetMarketBuySummaryFormatter.formatOrderDate(row.orderDate));
            }
            map.put("productName", row.productName);
            map.put("spec", row.spec);
            map.put("category", row.categoryName);
            map.put("total", SaleOutSheetMarketBuySummaryFormatter.formatTotalWithUnit(
                    row.total, row.unit));
            map.put("detail", buildMarketBuySummaryDetail(row, customerNameMap));
            data.add(map);
        }

        exportMarketBuySummary(headerMap, data);
    }

    /**
     * 导出按客户动态列展示的买菜汇总2。
     *
     * @param vo 查询参数
     */
    @Override
    public void marketBuySummary2(QuerySaleOutSheetVo vo) {
        validateMarketBuySummaryIds(vo);

        List<SaleOutSheet> sheets = sortMarketBuySummary2SheetsBySelection(this.query(vo),
                vo.getIdList());
        LinkedHashMap<String, String> customerNameMap = CollectionUtils.isEmpty(sheets)
                ? new LinkedHashMap<>() : buildCustomerNameMap(sheets);
        Map<String, String> headerMap = buildMarketBuySummary2Headers(customerNameMap);
        if (CollectionUtils.isEmpty(sheets)) {
            exportMarketBuySummary2(headerMap, new ArrayList<>());
            return;
        }

        Map<String, SaleOutSheet> sheetMap = sheets.stream().collect(Collectors.toMap(
                SaleOutSheet::getId, item -> item, (v1, v2) -> v2));
        List<SaleOutSheetDetail> details = queryMarketBuySummaryDetails(sheets);
        if (CollectionUtils.isEmpty(details)) {
            exportMarketBuySummary2(headerMap, new ArrayList<>());
            return;
        }

        Map<String, Product> productMap = buildProductMap(details);
        Map<String, ProductCategory> categoryMap = buildCategoryMap(productMap);
        Map<String, String> productUnitNameMap = buildProductUnitNameMap(productMap);
        List<SummaryRow> summaryRows = buildSummaryRows(details, sheetMap, productMap, categoryMap,
                productUnitNameMap);

        List<Map<String, String>> data = new ArrayList<>();
        for (SummaryRow row : summaryRows) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("date", SaleOutSheetMarketBuySummaryFormatter.formatOrderDate(row.orderDate));
            map.put("category", row.categoryName);
            map.put("productName", row.productName);
            map.put("spec", row.spec);

            map.put("unit", formatUnit(row.unit));
            for (String customerId : customerNameMap.keySet()) {
                SummaryCell cell = row.cells.get(customerId);
                map.put("customer-" + customerId, cell == null ? StringPool.EMPTY_STR
                        : SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(
                                cell.orderNum, cell.descriptions));
            }
            map.put("total", formatNumber(row.total));
            data.add(map);
        }

        exportMarketBuySummary2(headerMap, data);
    }

    /**
     * 校验买菜汇总必须基于勾选的销售出库单ID。
     *
     * @param vo 查询参数
     */
    static void validateMarketBuySummaryIds(QuerySaleOutSheetVo vo) {
        if (vo == null || CollectionUtils.isEmpty(vo.getIdList())) {
            throw new DefaultClientException("请选择要汇总的销售出库单！");
        }
    }

    /**
     * 按勾选单据ID的首次出现顺序重排买菜汇总2单据，忽略查询结果中不存在的ID。
     *
     * @param sheets 数据库查询出的销售出库单
     * @param idList 勾选的销售出库单ID列表
     * @return 按勾选顺序重排后的销售出库单
     */
    static List<SaleOutSheet> sortMarketBuySummary2SheetsBySelection(List<SaleOutSheet> sheets,
            List<String> idList) {
        if (CollectionUtils.isEmpty(sheets) || CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }

        Map<String, SaleOutSheet> sheetMap = sheets.stream().collect(Collectors.toMap(
                SaleOutSheet::getId, item -> item, (v1, v2) -> v1));
        List<SaleOutSheet> sortedSheets = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        for (String id : idList) {
            if (selectedIds.add(id) && sheetMap.containsKey(id)) {
                sortedSheets.add(sheetMap.get(id));
            }
        }
        return sortedSheets;
    }

    @Override
    public void exportDetailDailySummary(QuerySaleOutSheetVo vo) {
        List<QuerySaleOutSheetDetailDto> details = getBaseMapper().queryDetail(vo);
        if (CollectionUtils.isEmpty(details)) {
            MultiSheetsData<SaleOutSheetDetailExportModel> sheetData = new MultiSheetsData<>();
            sheetData.setSheetName("明细");
            sheetData.setHeadClazz(SaleOutSheetDetailExportModel.class);
            sheetData.setData(new ArrayList<>());
            ExcelUtil.writeWithSheets("销售出库单明细按天汇总", Collections.singletonList(sheetData));
            return;
        }

        Map<String, List<QuerySaleOutSheetDetailDto>> detailGroup = details.stream()
                .collect(Collectors.groupingBy(QuerySaleOutSheetDetailDto::getOrderDate,
                        LinkedHashMap::new, Collectors.toList()));

        List<MultiSheetsData<SaleOutSheetDetailExportModel>> sheetDatas = new ArrayList<>(detailGroup.size());
        detailGroup.forEach((orderDate, dayDetails) -> {
            MultiSheetsData<SaleOutSheetDetailExportModel> sheetData = new MultiSheetsData<>();
            sheetData.setSheetName(orderDate);
            sheetData.setHeadClazz(SaleOutSheetDetailExportModel.class);
            sheetData.setData(buildDailySummaryExportModels(dayDetails));
            sheetDatas.add(sheetData);
        });
        List<MultiSheetsData<SaleOutSheetDetailExportModel>> sortedDatas = sheetDatas.stream()
                .sorted(Comparator.comparing(MultiSheetsData::getSheetName))
                .collect(Collectors.toList());

        ExcelUtil.writeWithSheets("销售出库单明细按天汇总", sortedDatas);
    }

    /**
     * 查询开票明细；同一商品的不同单位分别汇总。
     *
     * @param vo 查询参数
     * @return 开票明细
     */
    @Override
    public List<SaleOutSheetInvoiceDetailExportModel> queryInvoiceDetail(QuerySaleOutSheetVo vo) {
        List<QuerySaleOutSheetDetailDto> details = getBaseMapper().queryDetail(vo);
        return buildInvoiceDetailExportModels(details, useProductSalePriceForInvoiceDetail());
    }

    @Override
    public void exportSales(QuerySaleOutSheetVo vo, HttpServletResponse response) {
        List<SaleOutSheet> sheets = this.query(vo);
        if (CollectionUtils.isEmpty(sheets)) {
            throw new DefaultClientException("未查询到可导出的销售出库单！");
        }

        List<SaleOutSheetSalesExportHelper.SheetData> exportDatas = sheets.stream()
                .map(this::buildSalesExportSheetData)
                .collect(Collectors.toList());

        try {
            SaleOutSheetSalesExportHelper.export(exportDatas, response);
        } catch (IOException e) {
            throw new DefaultClientException("销售导出失败！");
        }
    }

    private SaleOutSheetSalesExportHelper.SheetData buildSalesExportSheetData(SaleOutSheet sheet) {
        SaleOutSheetFullDto detail = this.getDetail(sheet.getId());
        GetSaleOutSheetBo detailBo = new GetSaleOutSheetBo(detail);
        Customer customer = customerService.findById(detail.getCustomerId());

        SaleOutSheetSalesExportHelper.SheetData data = new SaleOutSheetSalesExportHelper.SheetData();
        data.setCode(sheet.getCode());
        data.setTitle("销售单");
        data.setCustomerName(detailBo.getCustomerName());
        data.setAddress(customer == null ? StringPool.EMPTY_STR : customer.getAddress());
        data.setOrderDate(detail.getOrderDate());
        data.setTotalQty(detailBo.getTotalNum());
        data.setTotalAmount(detailBo.getTotalAmount());
        data.setTotalConfirmQty(detail.getConfirmNum());
        data.setTotalConfirmAmount(detail.getConfirmAmt());

        if (!CollectionUtils.isEmpty(detailBo.getDetails())) {
            List<SaleOutSheetSalesExportHelper.DetailData> details = buildSalesExportDetails(detailBo.getDetails(),
                    detail.getDetails(), detail.getCode());
            data.setDetails(details);
        }

        return data;
    }

    private List<SaleOutSheetDetailExportModel> buildDailySummaryExportModels(
            List<QuerySaleOutSheetDetailDto> details) {
        if (CollectionUtils.isEmpty(details)) {
            return new ArrayList<>();
        }

        List<String> supplierIds = details.stream().map(QuerySaleOutSheetDetailDto::getSupplierId)
                .collect(Collectors.toList());
        List<Supplier> suppliers = supplierService.selectByIds(supplierIds);
        Map<String, String> supplierMap = suppliers.stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName));

        Map<String, SaleOutSheetDetailExportModel> summaryMap = new LinkedHashMap<>();
        for (QuerySaleOutSheetDetailDto detail : details) {
            String productKey = StringUtil.isBlank(detail.getProductId()) ? detail.getProductCode()
                    : detail.getProductId();
            SaleOutSheetDetailExportModel current = new SaleOutSheetDetailExportModel(detail);
            SaleOutSheetDetailExportModel summary = summaryMap.get(productKey);
            if (summary == null) {
                summaryMap.put(productKey, current);
                continue;
            }

            summary.setOrderNum(NumberUtil.add(defaultValue(summary.getOrderNum()),
                    defaultValue(current.getOrderNum())));
            summary.setConfirmNum(NumberUtil.add(defaultValue(summary.getConfirmNum()),
                    defaultValue(current.getConfirmNum())));
            summary.setCostAmount(NumberUtil.add(defaultValue(summary.getCostAmount()),
                    defaultValue(current.getCostAmount())));
            summary.setTaxAmount(NumberUtil.add(defaultValue(summary.getTaxAmount()),
                    defaultValue(current.getTaxAmount())));
            summary.setConfirmAmt(NumberUtil.add(defaultValue(summary.getConfirmAmt()),
                    defaultValue(current.getConfirmAmt())));
            summary.setProfitRate(buildProfitRate(summary.getTaxAmount(), summary.getCostAmount()));
            summary.setSupplierName(supplierMap.get(detail.getSupplierId()));
        }

        return new ArrayList<>(summaryMap.values());
    }

    /**
     * 构建开票明细导出行，按商品标识和交易单位汇总。
     *
     * @param details 销售出库明细
     * @return 开票明细导出行
     */
    static List<SaleOutSheetInvoiceDetailExportModel> buildInvoiceDetailExportModels(
            List<QuerySaleOutSheetDetailDto> details, boolean useProductSalePrice) {
        if (CollectionUtils.isEmpty(details)) {
            return Collections.emptyList();
        }

        Map<InvoiceDetailKey, SaleOutSheetInvoiceDetailExportModel> summaryMap = new LinkedHashMap<>();
        for (QuerySaleOutSheetDetailDto detail : details) {
            InvoiceDetailKey key = new InvoiceDetailKey(buildInvoiceProductKey(detail), detail.getUnit());
            SaleOutSheetInvoiceDetailExportModel summary = summaryMap.get(key);
            if (summary == null) {
                summary = SaleOutSheetInvoiceDetailExportModel.builder()
                        .productCode(detail.getProductCode())
                        .unit(detail.getUnit())
                        .productName(detail.getProductName())
                        .spec(detail.getSpec())
                        .categoryName(detail.getCategoryName())
                        .price(defaultValue(detail.getProductSalePrice()))
                        .quantity(BigDecimal.ZERO)
                        .amount(BigDecimal.ZERO)
                        .build();
                summaryMap.put(key, summary);
            }
            summary.setQuantity(NumberUtil.add(summary.getQuantity(), resolveInvoiceQuantity(detail)));
            summary.setAmount(NumberUtil.add(summary.getAmount(), resolveInvoiceAmount(detail)));
        }

        List<SaleOutSheetInvoiceDetailExportModel> models = summaryMap.values().stream()
                .sorted(Comparator.comparing(SaleOutSheetInvoiceDetailExportModel::getProductName,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(SaleOutSheetInvoiceDetailExportModel::getUnit,
                                Comparator.nullsFirst(String::compareTo)))
                .collect(Collectors.toList());
        if (!useProductSalePrice) {
            models.forEach(model -> model.setPrice(resolveInvoicePrice(model.getAmount(), model.getQuantity())));
        }
        return models;
    }

    /**
     * 计算开票明细的单价，数量为零时返回零避免除零异常。
     *
     * @param amount 汇总金额
     * @param quantity 汇总数量
     * @return 单价，保留六位小数
     */
    private static BigDecimal resolveInvoicePrice(BigDecimal amount, BigDecimal quantity) {
        if (!isPositive(quantity)) {
            return BigDecimal.ZERO;
        }
        return defaultValue(amount).divide(quantity, 6, RoundingMode.HALF_UP);
    }

    /**
     * 是否使用商品售价作为开票明细单价。
     *
     * @return 参数 product_sale_price_unique 为 true 时返回 true
     */
    private boolean useProductSalePriceForInvoiceDetail() {
        QuerySysParameterVo parameterVo = new QuerySysParameterVo();
        parameterVo.setPmKey(PRODUCT_SALE_PRICE_UNIQUE_PM_KEY);
        List<SysParameter> parameters = sysParameterService.query(parameterVo);
        return !CollectionUtil.isEmpty(parameters) && BooleanUtil.toBoolean(parameters.get(0).getPmValue());
    }

    /**
     * 获取用于开票明细汇总的商品标识，缺失商品 ID 时依次回退到商品编码和名称。
     *
     * @param detail 销售出库明细
     * @return 商品标识
     */
    private static String buildInvoiceProductKey(QuerySaleOutSheetDetailDto detail) {
        if (StringUtils.isNotBlank(detail.getProductId())) {
            return detail.getProductId();
        }
        if (StringUtils.isNotBlank(detail.getProductCode())) {
            return detail.getProductCode();
        }
        return detail.getProductName();
    }

    /**
     * 开票数量优先使用大于零的验收数量。
     *
     * @param detail 销售出库明细
     * @return 开票数量
     */
    private static BigDecimal resolveInvoiceQuantity(QuerySaleOutSheetDetailDto detail) {
        return isPositive(detail.getConfirmNum()) ? detail.getConfirmNum() : defaultValue(detail.getOrderNum());
    }

    /**
     * 开票金额优先使用大于零的验收金额。
     *
     * @param detail 销售出库明细
     * @return 开票金额
     */
    private static BigDecimal resolveInvoiceAmount(QuerySaleOutSheetDetailDto detail) {
        return isPositive(detail.getConfirmAmt()) ? detail.getConfirmAmt() : defaultValue(detail.getTaxAmount());
    }

    /**
     * 判断金额是否大于零。
     *
     * @param value 金额或数量
     * @return 是否大于零
     */
    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 开票明细汇总键。
     */
    @Data
    @AllArgsConstructor
    private static class InvoiceDetailKey {
        private String productKey;
        private String unit;
    }

    /**
     * 构建销售单导出明细，按明细ID或商品ID显式匹配验收数据。
     *
     * @param orderDetails 销售单明细
     * @param confirmDetails 验收明细
     * @param sheetCode 单据号
     * @return 导出明细
     */
    private List<SaleOutSheetSalesExportHelper.DetailData> buildSalesExportDetails(
            List<GetSaleOutSheetBo.OrderDetailBo> orderDetails,
            List<SaleOutSheetFullDto.SheetDetailDto> confirmDetails,
            String sheetCode) {

        List<SaleOutSheetSalesExportHelper.DetailData> res = new ArrayList<>();
        for (GetSaleOutSheetBo.OrderDetailBo orderDetail : orderDetails) {
            SaleOutSheetFullDto.SheetDetailDto confirmDetail = findConfirmDetail(orderDetail, confirmDetails,
                    sheetCode);
            res.add(buildSalesExportDetailData(orderDetail, confirmDetail));
        }

        if (!CollectionUtils.isEmpty(confirmDetails) && res.size() != confirmDetails.size()) {
            throw new DefaultClientException("销售出库单【" + sheetCode + "】明细验收数据不完整，请刷新后重试！");
        }

        return res;
    }

    /**
     * 按明细ID或商品ID查找对应验收明细。
     *
     * @param orderDetail 销售单明细
     * @param confirmDetails 验收明细
     * @param sheetCode 单据号
     * @return 对应验收明细
     */
    private SaleOutSheetFullDto.SheetDetailDto findConfirmDetail(GetSaleOutSheetBo.OrderDetailBo orderDetail,
            List<SaleOutSheetFullDto.SheetDetailDto> confirmDetails, String sheetCode) {

        if (CollectionUtils.isEmpty(confirmDetails)) {
            throw new DefaultClientException("销售出库单【" + sheetCode + "】缺少验收明细，请刷新后重试！");
        }

        List<SaleOutSheetFullDto.SheetDetailDto> matched = confirmDetails.stream()
                .filter(item -> StringUtils.isNotBlank(orderDetail.getId()) && StringUtils.equals(orderDetail.getId(),
                        item.getId()))
                .collect(Collectors.toList());
        if (matched.size() == 1) {
            return matched.get(0);
        }

        matched = confirmDetails.stream()
                .filter(item -> StringUtils.isNotBlank(orderDetail.getProductId())
                        && StringUtils.equals(orderDetail.getProductId(), item.getProductId()))
                .collect(Collectors.toList());
        if (matched.size() == 1) {
            return matched.get(0);
        }

        if (matched.isEmpty()) {
            throw new DefaultClientException("销售出库单【" + sheetCode + "】商品【" + orderDetail.getProductName()
                    + "】缺少验收数据，请刷新后重试！");
        }

        throw new DefaultClientException("销售出库单【" + sheetCode + "】商品【" + orderDetail.getProductName()
                + "】验收数据存在重复，请刷新后重试！");
    }

    private SaleOutSheetSalesExportHelper.DetailData buildSalesExportDetailData(
            GetSaleOutSheetBo.OrderDetailBo detail, SaleOutSheetFullDto.SheetDetailDto confirmDetail) {
        SaleOutSheetSalesExportHelper.DetailData data = new SaleOutSheetSalesExportHelper.DetailData();
        data.setProductName(detail.getProductName());
        data.setSpec(detail.getSpec());
        data.setUnit(detail.getUnit());
        data.setQty(detail.getOutNum());
        data.setPrice(detail.getTaxPrice());
        if (detail.getTaxPrice() != null && detail.getOutNum() != null) {
            data.setAmount(detail.getTaxPrice().multiply(detail.getOutNum()).setScale(2,
                    RoundingMode.HALF_UP));
        }
        data.setConfirmQty(confirmDetail.getConfirmNum());
        data.setConfirmAmount(confirmDetail.getConfirmAmt());
        data.setRemark(detail.getDescription());
        return data;
    }

    private static BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildProfitRate(BigDecimal taxAmount, BigDecimal costAmount) {
        if (taxAmount == null || BigDecimal.ZERO.compareTo(taxAmount) == 0) {
            return "0.00%";
        }
        BigDecimal totalProfit = defaultValue(taxAmount).subtract(defaultValue(costAmount));
        return totalProfit.multiply(new BigDecimal("100"))
                .divide(taxAmount, 2, RoundingMode.HALF_UP) + "%";
    }

    /**
     * 生成买菜汇总固定表头，保证导出列顺序稳定。
     *
     * @param groupByDate 是否按日期汇总
     * @return 买菜汇总表头
     */
    static Map<String, String> buildMarketBuySummaryHeaders(boolean groupByDate) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        if (groupByDate) {
            headerMap.put("date", "日期");
        }
        headerMap.put("category", "分类名称");
        headerMap.put("productName", "商品名称");
        headerMap.put("spec", "规格");
        headerMap.put("total", "总重量");
        headerMap.put("detail", "明细数量");
        return headerMap;
    }

    /**
     * 生成默认不按日期汇总的买菜汇总表头。
     *
     * @return 买菜汇总表头
     */
    static Map<String, String> buildMarketBuySummaryHeaders() {
        return buildMarketBuySummaryHeaders(false);
    }

    /**
     * 生成买菜汇总2的动态表头，客户列按查询结果的首次出现顺序排列。
     *
     * @param customerNameMap 客户ID到展示名称的有序映射
     * @return 买菜汇总2表头
     */
    static Map<String, String> buildMarketBuySummary2Headers(
            LinkedHashMap<String, String> customerNameMap) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        headerMap.put("date", "日期");
        headerMap.put("category", "分类");
        headerMap.put("productName", "商品名称");
        headerMap.put("spec", "规格");
        headerMap.put("unit", "单位");
        for (Map.Entry<String, String> customer : customerNameMap.entrySet()) {
            headerMap.put("customer-" + customer.getKey(), customer.getValue());
        }
        headerMap.put("total", "总计");
        return headerMap;
    }

    /**
     * 按查询结果中的客户顺序生成客户展示名称映射。
     *
     * @param sheets 销售出库单
     * @return 客户ID到展示名称的有序映射
     */
    private LinkedHashMap<String, String> buildCustomerNameMap(List<SaleOutSheet> sheets) {
        List<String> customerIds = sheets.stream().map(SaleOutSheet::getCustomerId).distinct()
                .collect(Collectors.toList());
        Map<String, Customer> customerMap = customerService.listByIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, item -> item, (v1, v2) -> v2));

        LinkedHashMap<String, String> customerNameMap = new LinkedHashMap<>();
        for (SaleOutSheet sheet : sheets) {
            if (customerNameMap.containsKey(sheet.getCustomerId())) {
                continue;
            }

            Customer customer = customerMap.get(sheet.getCustomerId());
            customerNameMap.put(sheet.getCustomerId(),
                    SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer));
        }

        return customerNameMap;
    }

    /**
     * 查询参与买菜汇总的出库明细。
     * <p>
     * 这里按明细排序号升序查询，保证同一商品下备注合并时的顺序与录单顺序尽量一致。
     */
    private List<SaleOutSheetDetail> queryMarketBuySummaryDetails(List<SaleOutSheet> sheets) {
        List<String> sheetIds = sheets.stream().map(SaleOutSheet::getId).collect(Collectors.toList());
        return saleOutSheetDetailService.list(Wrappers.lambdaQuery(SaleOutSheetDetail.class)
                .in(SaleOutSheetDetail::getSheetId, sheetIds)
                .orderByAsc(SaleOutSheetDetail::getOrderNo));
    }

    /**
     * 批量加载商品基础信息，避免在汇总循环里逐条查询。
     */
    private Map<String, Product> buildProductMap(List<SaleOutSheetDetail> details) {
        List<String> productIds = details.stream().map(SaleOutSheetDetail::getProductId).distinct()
                .collect(Collectors.toList());
        return productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, item -> item, (v1, v2) -> v2));
    }

    /**
     * 批量加载商品分类信息，供导出“分类”列使用。
     */
    private Map<String, ProductCategory> buildCategoryMap(Map<String, Product> productMap) {
        List<String> categoryIds = productMap.values().stream().map(Product::getCategoryId)
                .filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        return productCategoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, item -> item, (v1, v2) -> v2));
    }

    /**
     * 批量将商品主单位 ID 转换为单位名称，供买菜汇总导出使用。
     */
    private Map<String, String> buildProductUnitNameMap(Map<String, Product> productMap) {
        List<String> unitIds = productMap.values().stream().map(Product::getUnit)
                .filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(unitIds)) {
            return Collections.emptyMap();
        }

        return unitService.listByIds(unitIds).stream()
                .collect(Collectors.toMap(Unit::getId, Unit::getName, (v1, v2) -> v2));
    }

    /**
     * 将原始出库明细聚合成导出行。
     * <p>
     * 聚合维度：
     * 1. 同日期同商品归并为一行
     * 2. 同日期同客户同商品数量累加
     * 3. 同日期同客户同商品备注去重后按出现顺序拼接
     * <p>
     * 输出前按“分类 -> 商品名称 -> 日期”升序排序，满足导出展示要求。
     */
    private List<SummaryRow> buildSummaryRows(List<SaleOutSheetDetail> details,
            Map<String, SaleOutSheet> sheetMap,
            Map<String, Product> productMap,
            Map<String, ProductCategory> categoryMap,
            Map<String, String> productUnitNameMap) {
        return buildSummaryRows(details, sheetMap, productMap, categoryMap, productUnitNameMap, true);
    }

    /**
     * 将原始出库明细按指定日期维度聚合成导出行。
     *
     * @param details 出库明细
     * @param sheetMap 出库单映射
     * @param productMap 商品映射
     * @param categoryMap 商品分类映射
     * @param productUnitNameMap 商品单位名称映射
     * @param groupByDate 是否按日期汇总
     * @return 汇总行
     */
    private List<SummaryRow> buildSummaryRows(List<SaleOutSheetDetail> details,
            Map<String, SaleOutSheet> sheetMap,
            Map<String, Product> productMap,
            Map<String, ProductCategory> categoryMap,
            Map<String, String> productUnitNameMap,
            boolean groupByDate) {
        Map<String, SummaryRow> summaryMap = new LinkedHashMap<>();
        for (SaleOutSheetDetail detail : details) {
            SaleOutSheet sheet = sheetMap.get(detail.getSheetId());
            Product product = productMap.get(detail.getProductId());
            if (sheet == null || product == null) {
                continue;
            }

            String summaryKey = buildMarketBuySummaryRowKey(
                    sheet.getOrderDate(), product.getId(), groupByDate);
            SummaryRow row = summaryMap.computeIfAbsent(summaryKey,
                    key -> new SummaryRow(sheet.getOrderDate(), getCategoryName(product, categoryMap),
                            product.getName(), product.getSpec(),
                            productUnitNameMap.getOrDefault(product.getUnit(), product.getUnit())));
            appendSummaryDetail(row, sheet, detail);
        }
        return sortMarketBuySummaryRows(summaryMap);
    }

    /**
     * 将原始出库明细按日期、客户、商品及合并选项聚合成导出行。
     *
     * @param details 出库明细
     * @param sheetMap 出库单映射
     * @param productMap 商品映射
     * @param categoryMap 商品分类映射
     * @param productUnitNameMap 商品单位名称映射
     * @param groupByDate 是否按日期汇总
     * @param mergeSameDayCustomerProduct 是否合并同一天、同一客户的相同商品
     * @return 汇总行
     */
    private List<SummaryRow> buildSummaryRows(List<SaleOutSheetDetail> details,
            Map<String, SaleOutSheet> sheetMap,
            Map<String, Product> productMap,
            Map<String, ProductCategory> categoryMap,
            Map<String, String> productUnitNameMap,
            boolean groupByDate,
            boolean mergeSameDayCustomerProduct) {
        Map<String, SummaryRow> summaryMap = new LinkedHashMap<>();
        int detailIndex = 0;
        for (SaleOutSheetDetail detail : details) {
            SaleOutSheet sheet = sheetMap.get(detail.getSheetId());
            Product product = productMap.get(detail.getProductId());
            if (sheet == null || product == null) {
                continue;
            }

            // 商品仍汇总到同一行；日期选项决定是否按日期拆行。
            String summaryKey = buildMarketBuySummaryRowKey(
                    sheet.getOrderDate(), product.getId(), groupByDate);
            detailIndex++;
            SummaryRow row = summaryMap.computeIfAbsent(summaryKey,
                    key -> new SummaryRow(sheet.getOrderDate(), getCategoryName(product, categoryMap),
                            product.getName(),
                            product.getSpec(),
                            productUnitNameMap.getOrDefault(product.getUnit(), product.getUnit())));

            // 未勾选合并时保留每条明细；勾选后以“日期 + 客户 + 商品”为维度累加。
            String customerDetailKey = buildMarketBuySummaryRowKey(sheet.getOrderDate(),
                    sheet.getCustomerId(), product.getId(), detail.getId(), detailIndex,
                    mergeSameDayCustomerProduct);
            SummaryDetail customerDetail = row.marketBuySummaryDetails.computeIfAbsent(
                    customerDetailKey, key -> new SummaryDetail(sheet.getCustomerId()));
            appendSummaryCell(customerDetail.cell, detail);
            BigDecimal orderNum = detail.getOrderNum() == null ? BigDecimal.ZERO : detail.getOrderNum();
            row.total = NumberUtil.add(row.total, orderNum);
        }

        return sortMarketBuySummaryRows(summaryMap);
    }

    /**
     * 将销售出库明细的数量及备注追加到买菜汇总行。
     *
     * @param row 买菜汇总行
     * @param sheet 销售出库单
     * @param detail 销售出库单明细
     */
    private void appendSummaryDetail(SummaryRow row, SaleOutSheet sheet, SaleOutSheetDetail detail) {
        SummaryCell cell = row.cells.computeIfAbsent(sheet.getCustomerId(), key -> new SummaryCell());
        appendSummaryCell(cell, detail);
        BigDecimal orderNum = detail.getOrderNum() == null ? BigDecimal.ZERO : detail.getOrderNum();
        row.total = NumberUtil.add(row.total, orderNum);
    }

    /**
     * 将销售出库明细的数量及备注追加到客户明细单元。
     *
     * @param cell 客户明细单元
     * @param detail 销售出库单明细
     */
    private void appendSummaryCell(SummaryCell cell, SaleOutSheetDetail detail) {
        BigDecimal orderNum = detail.getOrderNum() == null ? BigDecimal.ZERO : detail.getOrderNum();
        cell.orderNum = NumberUtil.add(cell.orderNum, orderNum);
        if (StringUtils.isNotBlank(detail.getDescription())) {
            cell.descriptions.add(detail.getDescription());
            cell.quantityByDescription.merge(detail.getDescription(), orderNum, NumberUtil::add);
        } else {
            cell.quantityWithoutDescription = NumberUtil.add(cell.quantityWithoutDescription, orderNum);
        }
    }

    /**
     * 按分类、商品名称及日期排序买菜汇总行。
     *
     * @param summaryMap 买菜汇总行映射
     * @return 排序后的买菜汇总行
     */
    private List<SummaryRow> sortMarketBuySummaryRows(Map<String, SummaryRow> summaryMap) {
        return summaryMap.values().stream()
                .sorted(Comparator.comparing((SummaryRow item) -> buildMarketBuySummarySortKey(
                        item.categoryName, item.productName, item.orderDate)))
                .collect(Collectors.toList());
    }

    /**
     * 构造买菜汇总行键，确保同一商品跨日期分别汇总。
     *
     * @param orderDate 订单日期
     * @param productId 商品ID
     * @return 日期和商品组合键
     */
    static String buildMarketBuySummaryRowKey(LocalDate orderDate, String productId) {
        return buildMarketBuySummaryRowKey(orderDate, productId, true);
    }

    /**
     * 根据日期汇总选项构造买菜汇总行键。
     *
     * @param orderDate 订单日期
     * @param productId 商品ID
     * @param groupByDate 是否按日期汇总
     * @return 汇总行键
     */
    static String buildMarketBuySummaryRowKey(LocalDate orderDate, String productId,
            boolean groupByDate) {
        if (!groupByDate) {
            return String.valueOf(productId);
        }
        return String.valueOf(orderDate) + '\u0000' + String.valueOf(productId);
    }

    /**
     * 根据是否合并同日同客户商品构造买菜汇总行键。
     *
     * @param orderDate 订单日期
     * @param customerId 客户ID
     * @param productId 商品ID
     * @param detailId 出库明细ID
     * @param detailIndex 出库明细顺序，作为空明细ID的兜底唯一值
     * @param mergeSameDayCustomerProduct 是否合并同一天、同一客户的相同商品
     * @return 汇总行键
     */
    static String buildMarketBuySummaryRowKey(LocalDate orderDate, String customerId,
            String productId, String detailId, int detailIndex,
            boolean mergeSameDayCustomerProduct) {
        String baseKey = String.valueOf(orderDate) + '\u0000' + String.valueOf(customerId)
                + '\u0000' + String.valueOf(productId);
        if (mergeSameDayCustomerProduct) {
            return baseKey;
        }
        return baseKey + '\u0000' + (StringUtils.isBlank(detailId) ? detailIndex : detailId);
    }

    /**
     * 构造买菜汇总排序键，排序顺序为品类、商品名称、日期。
     *
     * @param categoryName 品类名称
     * @param productName 商品名称
     * @param orderDate 订单日期
     * @return 品类、商品名称和日期组合排序键
     */
    static String buildMarketBuySummarySortKey(String categoryName, String productName,
            LocalDate orderDate) {
        return defaultSortValue(categoryName) + '\u0000' + defaultSortValue(productName)
                + '\u0000' + defaultSortValue(orderDate == null ? null : orderDate.toString());
    }

    /**
     * 将排序字段的空值转换为空字符串。
     *
     * @param value 排序字段
     * @return 非空排序字段
     */
    private static String defaultSortValue(String value) {
        return value == null ? StringPool.EMPTY_STR : value;
    }

    /**
     * 读取商品分类名称；未配置分类时返回空字符串，避免导出空指针。
     */
    private String getCategoryName(Product product, Map<String, ProductCategory> categoryMap) {
        if (StringUtils.isBlank(product.getCategoryId())) {
            return StringPool.EMPTY_STR;
        }

        ProductCategory category = categoryMap.get(product.getCategoryId());
        return category == null ? StringPool.EMPTY_STR : category.getName();
    }

    private String defaultString(String value) {
        return value == null ? StringPool.EMPTY_STR : value;
    }

    /**
     * 统一执行无模型导出。
     */
    private void exportMarketBuySummary(Map<String, String> headerMap, List<Map<String, String>> data) {
        ExcelUtil.exportNoModel("买菜汇总", headerMap, data);
    }

    /**
     * 导出买菜汇总2的动态列数据。
     *
     * @param headerMap 表头
     * @param data 导出数据
     */
    private void exportMarketBuySummary2(Map<String, String> headerMap, List<Map<String, String>> data) {
        ExcelUtil.exportNoModel("买菜汇总2", headerMap, data);
    }

    /**
     * 将商品下的客户数量明细合并为单列文本。
     *
     * @param row 商品汇总行
     * @param customerNameMap 客户ID到展示名称的有序映射
     * @return 合并后的客户数量明细
     */
    private String buildMarketBuySummaryDetail(SummaryRow row,
            LinkedHashMap<String, String> customerNameMap) {
        if (!row.marketBuySummaryDetails.isEmpty()) {
            return row.marketBuySummaryDetails.values().stream()
                    .map(detail -> SaleOutSheetMarketBuySummaryFormatter
                            .formatCustomerDetailByDescription(
                                    customerNameMap.get(detail.customerId), row.unit,
                                    detail.cell.quantityWithoutDescription,
                                    detail.cell.quantityByDescription))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining("+"));
        }

        List<String> details = new ArrayList<>();
        for (Map.Entry<String, String> customer : customerNameMap.entrySet()) {
            SummaryCell cell = row.cells.get(customer.getKey());
            if (cell == null) {
                continue;
            }

            details.add(SaleOutSheetMarketBuySummaryFormatter.formatCustomerDetailByDescription(
                    customer.getValue(), row.unit, cell.quantityWithoutDescription,
                    cell.quantityByDescription));
        }
        return details.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining("+"));
    }

    /**
     * 去掉数量尾部无意义的 0，便于导出展示。
     */
    private String formatNumber(BigDecimal num) {
        if (num == null) {
            return "0";
        }

        return num.stripTrailingZeros().toPlainString();
    }

    private static class SummaryRow {
        private LocalDate orderDate;
        private String categoryName;
        private String productName;
        private String spec;
        private String unit;
        private BigDecimal total = BigDecimal.ZERO;

        // key: customerId，value: 当前商品在该客户下的汇总数量与备注。
        private Map<String, SummaryCell> cells = new HashMap<>();

        // key: 同日同客户商品合并键或明细唯一键，value: 买菜汇总明细数量展示段。
        private Map<String, SummaryDetail> marketBuySummaryDetails = new LinkedHashMap<>();

        private SummaryRow(LocalDate orderDate, String categoryName, String productName, String spec,
                String unit) {
            this.orderDate = orderDate;
            this.categoryName = categoryName;
            this.productName = productName;
            this.spec = spec;
            this.unit = unit;
        }
    }

    private static class SummaryCell {
        private BigDecimal orderNum = BigDecimal.ZERO;

        // 无备注的数量单独汇总，保持原有“数量/单位”的展示方式。
        private BigDecimal quantityWithoutDescription = BigDecimal.ZERO;

        // key: 备注，value: 对应数量；按备注首次出现顺序输出。
        private Map<String, BigDecimal> quantityByDescription = new LinkedHashMap<>();

        // 使用 LinkedHashSet 去重并保持备注原始顺序，导出时展示更稳定。
        private Set<String> descriptions = new LinkedHashSet<>();
    }

    /**
     * 买菜汇总单行中的客户明细数量展示段。
     */
    private static class SummaryDetail {
        private String customerId;
        private SummaryCell cell = new SummaryCell();

        private SummaryDetail(String customerId) {
            this.customerId = customerId;
        }
    }

    @Override
    public PageResult<SaleOutSheet> selector(Integer pageIndex, Integer pageSize,
            SaleOutSheetSelectorVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SaleOutSheet> datas = getBaseMapper().selector(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public GetPaymentDateDto getPaymentDate(String customerId) {

        // 默认为当前日期的30天后，如当天为2021-10-01，则付款日期默认为2021-11-01
        // （1）客户的结算方式为“任意指定”，则付款日期按照以上规则展示默认值，允许用户更改，但仅能选择当天及当天之后的日期。
        // （2）客户的结算方式为“货到付款”（这个参数的名字后期会改，如“货销付款”），则付款日期默认为此刻，且不允许修改，即出库单的创建时间，可能会遇到跨日的问题，但付款日期，均赋值为出库单的创建日期。

        Customer customer = customerService.findById(customerId);

        GetPaymentDateDto result = new GetPaymentDateDto();
        result.setAllowModify(customer.getSettleType() == SettleType.ARBITRARILY);

        if (customer.getSettleType() == SettleType.ARBITRARILY) {
            result.setPaymentDate(LocalDate.now().plusMonths(1));
        } else if (customer.getSettleType() == SettleType.CASH_ON_DELIVERY) {
            result.setPaymentDate(LocalDate.now());
        }

        return result;
    }

    @Override
    public SaleOutSheetFullDto getDetail(String id) {

        return getBaseMapper().getDetail(id);
    }

    @Override
    public SaleOutSheetWithReturnDto getWithReturn(String id) {

        SaleConfig saleConfig = saleConfigService.get();

        SaleOutSheetWithReturnDto sheet = getBaseMapper().getWithReturn(id,
                saleConfig.getSaleReturnRequireOutStock());
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }

        return sheet;
    }

    @Override
    public PageResult<SaleOutSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
            QuerySaleOutSheetWithReturnVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        SaleConfig saleConfig = saleConfigService.get();

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SaleOutSheet> datas = getBaseMapper().queryWithReturn(vo,
                saleConfig.getSaleReturnMultipleRelateOutStock());

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    private String generateCode() {
        while (true) {
            String code = generateCodeService.generate(GenerateCodeTypePool.SALE_OUT_SHEET);
            QuerySaleOutSheetVo vo = new QuerySaleOutSheetVo();
            vo.setCode(code);
            List<SaleOutSheet> list = query(vo);
            if (CollectionUtils.isEmpty(list)) {
                return code;
            }
        }
    }

    @OpLog(type = SaleOpLogType.class, name = "创建销售出库单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建出库单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateSaleOutSheetVo vo) {

        productService.assertAvailable(vo.getProducts().stream()
                .map(SaleOutProductVo::getProductId).collect(Collectors.toList()));

        SaleOutSheet sheet = new SaleOutSheet();
        sheet.setId(IdUtil.getId());
        sheet.setCode(generateCode());

        this.create(sheet, vo);

        sheet.setStatus(SaleOutSheetStatus.CREATED);

        getBaseMapper().insert(sheet);

        this.adjustCustomerAmount(sheet.getCustomerId());
        refreshCostPrice(sheet.getId(), vo.getFillAllCost(), Boolean.TRUE.equals(vo.getFillAllCostModified()));

        subStock(sheet);
        productHotnessService.increment(
                vo.getProducts().stream().map(SaleOutProductVo::getProductId).collect(Collectors.toList()));

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);

        return sheet.getId();
    }

    @OpLog(type = SaleOpLogType.class, name = "修改销售出库单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改出库单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateSaleOutSheetVo vo) {

        SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }

        checkApproveStatus(sheet, "销售出库单已审核通过，无法修改！", "销售出库单无法修改！");
        if (Arrays.asList(SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE,
                SettleStatus.SETTLED).contains(sheet.getSettleStatus())) {
            throw new DefaultClientException("销售出库单已对账或已结算，无法修改！");
        }

        String oldCustomerId = sheet.getCustomerId();
        List<SaleOutSheetDetail> oldDetails = getSheetDetails(sheet.getId());
        boolean stockSynced = hasStockSynced(sheet.getId());
        if (stockSynced) {
            rollbackStock(sheet, oldDetails);
        }

        deleteSheetDetail(sheet.getId());

        removeDetailLots(oldDetails);

        this.create(sheet, vo);

        sheet.setStatus(SaleOutSheetStatus.CREATED);

        clearApproveStatus(sheet);

        this.adjustCustomerAmount(oldCustomerId);
        if (!StringUtil.equals(oldCustomerId, sheet.getCustomerId())) {
            this.adjustCustomerAmount(sheet.getCustomerId());
        }

        refreshCostPrice(sheet.getId(), vo.getFillAllCost(), Boolean.TRUE.equals(vo.getFillAllCostModified()));
        subStock(sheet);

        productHotnessService.increment(
                vo.getProducts().stream().map(SaleOutProductVo::getProductId).collect(Collectors.toList()));

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    /**
     * 合并销售出库单，保留创建时间最早的单据并追加其他单据明细。
     *
     * @param vo 合并参数
     * @return 合并后保留的销售出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String merge(MergeSaleOutSheetVo vo) {

        if (vo == null || CollectionUtil.isEmpty(vo.getIds()) || vo.getIds().size() < 2) {
            throw new DefaultClientException("请选择两张及以上销售出库单进行合并！");
        }

        List<String> distinctIds = normalizeMergeSheetIds(vo.getIds());

        List<SaleOutSheet> sheets = listByIds(distinctIds);
        if (sheets.size() != distinctIds.size()) {
            throw new DefaultClientException("部分销售出库单不存在，请刷新后重试！");
        }

        sortMergeSheets(sheets);

        SaleOutSheet target = sheets.get(0);
        validateMergeSheets(target, sheets);

        UpdateSaleOutSheetVo updateVo = buildMergeUpdateVo(target, sheets);
        updateVo.validate();

        SaleOutSheetService thisService = getThis(this.getClass());
        thisService.update(updateVo);

        for (SaleOutSheet sheet : sheets) {
            if (!StringUtil.equals(target.getId(), sheet.getId())) {
                thisService.deleteById(sheet.getId());
            }
        }

        return target.getId();
    }

    /**
     * 规范化待合并销售出库单ID。
     *
     * @param ids 待合并销售出库单ID列表
     * @return 去空白、去重后的销售出库单ID列表
     */
    static List<String> normalizeMergeSheetIds(List<String> ids) {
        List<String> distinctIds = ids.stream().filter(StringUtils::isNotBlank).distinct()
                .collect(Collectors.toList());
        if (distinctIds.size() < 2) {
            throw new DefaultClientException("请选择两张及以上销售出库单进行合并！");
        }
        return distinctIds;
    }

    /**
     * 按合并保留规则排序销售出库单。
     *
     * @param sheets 待排序销售出库单列表
     */
    static void sortMergeSheets(List<SaleOutSheet> sheets) {
        sheets.sort(Comparator.comparing(SaleOutSheet::getCreateTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(SaleOutSheet::getCode, Comparator.nullsLast(String::compareTo)));
    }

    /**
     * 校验销售出库单是否满足合并条件。
     *
     * @param target 保留的销售出库单
     * @param sheets 待合并销售出库单列表
     */
    void validateMergeSheets(SaleOutSheet target, List<SaleOutSheet> sheets) {
        for (SaleOutSheet sheet : sheets) {
            checkApproveStatus(sheet, "销售出库单已审核通过，无法合并！", "销售出库单无法合并！");
            if (Arrays.asList(SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE,
                    SettleStatus.SETTLED).contains(sheet.getSettleStatus())) {
                throw new DefaultClientException("销售出库单已对账或已结算，无法合并！");
            }

            if (!StringUtil.equals(target.getCustomerId(), sheet.getCustomerId())) {
                throw new DefaultClientException("仅允许合并相同客户的销售出库单！");
            }
            if (!StringUtil.equals(target.getScId(), sheet.getScId())) {
                throw new DefaultClientException("仅允许合并相同仓库的销售出库单！");
            }
            if (!StringUtil.equals(target.getSaleOrderId(), sheet.getSaleOrderId())) {
                throw new DefaultClientException("仅允许合并相同销售订单来源的销售出库单！");
            }
        }
    }

    /**
     * 构建合并保存参数。
     *
     * @param target 保留的销售出库单
     * @param sheets 待合并销售出库单列表
     * @return 销售出库单修改参数
     */
    private UpdateSaleOutSheetVo buildMergeUpdateVo(SaleOutSheet target, List<SaleOutSheet> sheets) {
        UpdateSaleOutSheetVo updateVo = new UpdateSaleOutSheetVo();
        updateVo.setId(target.getId());
        updateVo.setScId(target.getScId());
        updateVo.setCustomerId(target.getCustomerId());
        updateVo.setSalerId(target.getSalerId());
        updateVo.setOrderDate(target.getOrderDate());
        updateVo.setPaymentDate(target.getPaymentDate());
        updateVo.setSaleOrderId(target.getSaleOrderId());
        updateVo.setDescription(target.getDescription());
        updateVo.setRequired(StringUtil.isNotBlank(target.getSaleOrderId()));
        updateVo.setPaidAmount(sumSheetAmount(sheets, SaleOutSheet::getPaidAmount));

        List<SaleOutProductVo> products = new ArrayList<>();
        int seq = 1;
        for (SaleOutSheet sheet : sheets) {
            List<SaleOutSheetDetail> details = getSheetDetails(sheet.getId());
            for (SaleOutSheetDetail detail : details) {
                products.add(toMergeProductVo(detail, sheet.getOrderDate(), seq++));
            }
        }
        updateVo.setProducts(products);
        return updateVo;
    }

    /**
     * 将销售出库明细转换为合并保存商品参数。
     *
     * @param detail 销售出库明细
     * @param orderDate 原销售出库单订单日期
     * @param seq 行号
     * @return 销售出库商品参数
     */
    static SaleOutProductVo toMergeProductVo(SaleOutSheetDetail detail, LocalDate orderDate,
            int seq) {
        SaleOutProductVo productVo = new SaleOutProductVo();
        productVo.setSeq(seq);
        productVo.setProductId(detail.getProductId());
        productVo.setUnitId(detail.getUnitId());
        productVo.setUnit(detail.getUnitName());
        productVo.setOriPrice(detail.getOriPrice());
        productVo.setTaxPrice(detail.getTaxPrice());
        productVo.setDiscountRate(detail.getDiscountRate());
        productVo.setOrderNum(detail.getBusinessNum());
        productVo.setConfirmNum(detail.getConfirmNum());
        productVo.setDescription(detail.getDescription());
        productVo.setCostPrice(detail.getCostPrice());
        productVo.setSaleOrderDetailId(detail.getSaleOrderDetailId());
        productVo.setActualDate(detail.getActualDate());
        productVo.setPlanDate(orderDate);
        return productVo;
    }

    /**
     *
     * @param sheet
     * @param msg
     * @param msg1
     */
    private void checkApproveStatus(SaleOutSheet sheet, String msg, String msg1) {
        if (sheet.getStatus() != SaleOutSheetStatus.CREATED
                && sheet.getStatus() != SaleOutSheetStatus.APPROVE_REFUSE) {

            if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException(msg);
            }

            throw new DefaultClientException(msg1);
        }
    }

    /**
     * 清空审核相关信息
     * @param sheet
     */
    private void clearApproveStatus(SaleOutSheet sheet) {
        List<SaleOutSheetStatus> statusList = new ArrayList<>();
        statusList.add(SaleOutSheetStatus.CREATED);
        statusList.add(SaleOutSheetStatus.APPROVE_REFUSE);

        Wrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getApproveBy, null)
                .set(SaleOutSheet::getApproveTime, null)
                .set(SaleOutSheet::getRefuseReason, StringPool.EMPTY_STR)
                .eq(SaleOutSheet::getId, sheet.getId())
                .in(SaleOutSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }
    }

    /**
     * 删除出库单明细
     * @param sheetId
     */
    private void deleteSheetDetail(String sheetId) {
        Wrapper<SaleOutSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
                .eq(SaleOutSheetDetail::getSheetId, sheetId);
        saleOutSheetDetailService.remove(deleteDetailWrapper);
    }

    @OpLog(type = SaleOpLogType.class, name = "修改销售出库单备注，单号：{}", params = "#code")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDescription(UpdateSaleOutSheetDescriptionVo vo) {

        SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }
        Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getDescription, vo.getDescription())
                .eq(SaleOutSheet::getId, sheet.getId());
        if (getBaseMapper().update(updateWrapper) != 1) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    /**
     * 批量更新销售出库单备注。
     *
     * @param vo 批量更新参数
     */
    @OpLog(type = SaleOpLogType.class, name = "批量修改销售出库单备注")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchUpdateDescription(BatchUpdateSaleOutSheetDescriptionVo vo) {

        List<SaleOutSheet> sheets = getBaseMapper().selectBatchIds(vo.getIds());
        if (sheets.size() != vo.getIds().size()) {
            throw new InputErrorException("部分销售出库单不存在，请刷新后重试！");
        }
        Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getDescription, vo.getDescription())
                .in(SaleOutSheet::getId, vo.getIds());
        if (getBaseMapper().update(updateWrapper) != vo.getIds().size()) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }

        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SaleOpLogType.class, name = "批量调整销售出库明细售价")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchUpdatePrice(BatchUpdateSaleOutSheetPriceVo vo) {

        if (NumberUtil.lt(vo.getTaxPrice(), BigDecimal.ZERO)) {
            throw new InputErrorException("销售价不允许小于0！");
        }

        if (!NumberUtil.isNumberPrecision(vo.getTaxPrice(), 6)) {
            throw new InputErrorException("销售价最多允许6位小数！");
        }

        List<SaleOutSheetDetail> details = saleOutSheetDetailService.listByIds(vo.getDetailIds());
        if (CollectionUtils.isEmpty(details)) {
            throw new DefaultClientException("未查询到需要调整售价的明细数据！");
        }

        if (details.size() != vo.getDetailIds().size()) {
            throw new DefaultClientException("部分销售出库明细不存在，请刷新后重试！");
        }

        String productId = null;
        for (SaleOutSheetDetail detail : details) {
            if (productId == null) {
                productId = detail.getProductId();
                continue;
            }
            if (!StringUtil.equals(productId, detail.getProductId())) {
                throw new DefaultClientException("一次只能调整同一种商品的售价！");
            }
        }

        Set<String> customerIds = new LinkedHashSet<>();
        Set<String> sheetIds = new LinkedHashSet<>();
        for (SaleOutSheetDetail detail : details) {
            validateBatchUpdatePriceDetail(detail);

            detail.setTaxPrice(vo.getTaxPrice());
            detail.setTaxAmount(SaleOutSheetAmtCalculator.calculateLineAmount(vo.getTaxPrice(),
                    detail.getBusinessNum()));
            detail.setConfirmAmt(SaleOutSheetAmtCalculator.calculateLineAmount(vo.getTaxPrice(),
                    detail.getConfirmNum()));

            saleOutSheetDetailService.updateById(detail);

            productLatestPriceCacheService.updateLatestPrice(detail.getProductId(), vo.getTaxPrice(), null);
            sheetIds.add(detail.getSheetId());
        }

        for (String sheetId : sheetIds) {
            SaleOutSheet sheet = getBaseMapper().selectById(sheetId);
            if (sheet == null) {
                throw new DefaultClientException("销售出库单不存在，请刷新后重试！");
            }

            BigDecimal totalAmount = calcSheetTotalAmount(sheetId);
            List<SaleOutSheetDetail> sheetDetails = saleOutSheetDetailService.getBySheetId(sheetId);
            SaleOutSheetAmtCalculator.calculateSheet(sheet, sheetDetails);
            BigDecimal paidAmount = sheet.getPaidAmount() == null ? BigDecimal.ZERO : sheet.getPaidAmount();
            if (NumberUtil.gt(paidAmount.abs(), totalAmount.abs())) {
                throw new DefaultClientException("单据号：" + sheet.getCode() + " 的已付金额绝对值大于调整后的单据金额绝对值，不允许调整售价！");
            }

            LambdaUpdateWrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                    .set(SaleOutSheet::getTotalAmount, totalAmount)
                    .set(SaleOutSheet::getConfirmNum, sheet.getConfirmNum())
                    .set(SaleOutSheet::getConfirmAmt, sheet.getConfirmAmt())
                    .eq(SaleOutSheet::getId, sheetId);
            if (!this.update(updateWrapper)) {
                throw new DefaultClientException("销售出库单金额更新失败，请重试！");
            }

            customerIds.add(sheet.getCustomerId());
        }

        sheetIds.forEach(this::refreshCostPrice);
        customerIds.forEach(this::adjustCustomerAmount);

        OpLogUtil.setExtra(vo);
    }

    /**
     * 批量标记销售出库单为已送货。
     *
     * @param vo 销售出库单ID列表
     */
    @OpLog(type = SaleOpLogType.class, name = "批量送货")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDelivery(BatchDeliverySaleOutSheetVo vo) {

        List<SaleOutSheet> sheets = listByIds(vo.getIds());
        if (sheets.size() != vo.getIds().size()) {
            throw new DefaultClientException("部分销售出库单不存在，请刷新后重试！");
        }

        LambdaUpdateWrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getDelivered, true)
                .in(SaleOutSheet::getId, vo.getIds());
        if (!update(updateWrapper)) {
            throw new DefaultClientException("销售出库单送货状态更新失败，请刷新后重试！");
        }
    }

    /**
     * 按销售出库订单日期同步询价商品售价及相关金额。
     *
     * @param vo 日期范围
     */
    @OpLog(type = SaleOpLogType.class, name = "同步询价商品销售价")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncInquirySalePrice(SyncInquirySalePriceVo vo) {
        if (vo.getStartDate().isAfter(vo.getEndDate())) {
            throw new DefaultClientException("订单开始日期不能晚于结束日期！");
        }

        List<SaleOutSheet> sheets = this.list(Wrappers.lambdaQuery(SaleOutSheet.class)
                .between(SaleOutSheet::getOrderDate, vo.getStartDate(), vo.getEndDate()));
        if (CollectionUtils.isEmpty(sheets)) {
            throw new DefaultClientException("所选订单日期范围内没有销售出库单！");
        }

        List<String> sheetIds = sheets.stream().map(SaleOutSheet::getId).collect(Collectors.toList());
        List<SaleOutSheetDetail> details = saleOutSheetDetailService.list(
                Wrappers.lambdaQuery(SaleOutSheetDetail.class)
                        .in(SaleOutSheetDetail::getSheetId, sheetIds));
        if (CollectionUtils.isEmpty(details)) {
            throw new DefaultClientException("所选销售出库单没有商品明细！");
        }

        Set<String> productIds = details.stream().map(SaleOutSheetDetail::getProductId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, Product> inquiryProductMap = productService.list(
                        Wrappers.lambdaQuery(Product.class)
                                .in(Product::getId, productIds)
                                .eq(Product::getInquiryProduct, Boolean.TRUE))
                .stream().collect(Collectors.toMap(Product::getId, item -> item));

        List<SaleOutSheetDetail> changedDetails = details.stream()
                .filter(item -> inquiryProductMap.containsKey(item.getProductId()))
                .filter(item -> !Boolean.TRUE.equals(item.getIsGift()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(changedDetails)) {
            throw new DefaultClientException("所选订单日期范围内没有可同步的询价商品明细！");
        }

        for (SaleOutSheetDetail detail : changedDetails) {
            if (detail.getSettleStatus() != SettleStatus.UN_CHECK_BILL) {
                throw new DefaultClientException("仅支持同步未对账、未结算的销售出库明细！");
            }

            Product product = inquiryProductMap.get(detail.getProductId());
            validateInquirySalePrice(product);
            applyInquirySalePrice(detail, product.getSalePrice());
        }
        if (!saleOutSheetDetailService.updateBatchById(changedDetails)) {
            throw new DefaultClientException("询价商品明细金额更新失败，请重试！");
        }

        Map<String, List<SaleOutSheetDetail>> detailMap = details.stream()
                .collect(Collectors.groupingBy(SaleOutSheetDetail::getSheetId));
        Set<String> customerIds = new LinkedHashSet<>();
        for (SaleOutSheet sheet : sheets) {
            List<SaleOutSheetDetail> sheetDetails = detailMap.getOrDefault(sheet.getId(), Collections.emptyList());
            BigDecimal totalAmount = sumDetailAmount(sheetDetails, SaleOutSheetDetail::getTaxAmount);
            BigDecimal confirmAmt = sumDetailAmount(sheetDetails, SaleOutSheetDetail::getConfirmAmt);
            BigDecimal totalProfit = sumDetailAmount(sheetDetails, SaleOutSheetDetail::getTotalProfit);
            BigDecimal paidAmount = NumberUtil.getDefaultValue(sheet.getPaidAmount());
            if (NumberUtil.gt(paidAmount.abs(), totalAmount.abs())) {
                throw new DefaultClientException("单据号：" + sheet.getCode()
                        + " 的已付金额绝对值大于同步后的单据金额绝对值，不允许同步售价！");
            }

            sheet.setTotalAmount(totalAmount);
            sheet.setConfirmAmt(confirmAmt);
            sheet.setTotalProfit(totalProfit);
            customerIds.add(sheet.getCustomerId());
        }
        if (!this.updateBatchById(sheets)) {
            throw new DefaultClientException("销售出库单金额更新失败，请重试！");
        }

        adjustCustomerAmounts(customerIds);
        OpLogUtil.setExtra(vo);
    }

    /**
     * 校验询价商品销售价。
     *
     * @param product 询价商品
     */
    private void validateInquirySalePrice(Product product) {
        if (product == null || product.getSalePrice() == null) {
            throw new DefaultClientException("询价商品销售价不能为空！");
        }
        if (NumberUtil.lt(product.getSalePrice(), BigDecimal.ZERO)) {
            throw new DefaultClientException("询价商品销售价不允许小于0！");
        }
        if (!NumberUtil.isNumberPrecision(product.getSalePrice(), 6)) {
            throw new DefaultClientException("询价商品销售价最多允许6位小数！");
        }
    }

    /**
     * 将商品销售价应用到销售出库明细并重算金额、利润。
     *
     * @param detail   销售出库明细
     * @param salePrice 商品销售价
     */
    static void applyInquirySalePrice(SaleOutSheetDetail detail, BigDecimal salePrice) {
        detail.setTaxPrice(salePrice);
        detail.setTaxAmount(SaleOutSheetAmtCalculator.calculateLineAmount(salePrice,
                detail.getBusinessNum()));
        detail.setConfirmAmt(SaleOutSheetAmtCalculator.calculateLineAmount(salePrice,
                detail.getConfirmNum()));
        detail.setTotalProfit(calculateSyncedProfit(detail));
    }

    /**
     * 使用同步后的销售金额和原成本计算明细利润。
     *
     * @param detail 销售出库明细
     * @return 明细利润，成本为空时返回空
     */
    static BigDecimal calculateSyncedProfit(SaleOutSheetDetail detail) {
        if (detail == null || detail.getCostPrice() == null) {
            return null;
        }
        BigDecimal costAmount = NumberUtil.calculateAmount(detail.getCostPrice(), resolveCostNum(detail));
        BigDecimal incomeAmount = detail.getConfirmAmt() != null
                && detail.getConfirmAmt().compareTo(BigDecimal.ZERO) > 0
                ? detail.getConfirmAmt() : NumberUtil.getDefaultValue(detail.getTaxAmount());
        return NumberUtil.getNumber(NumberUtil.sub(incomeAmount, costAmount), NumberUtil.AMT_PRECISION);
    }

    /**
     * 汇总明细金额字段。
     *
     * @param details 明细列表
     * @param getter  金额字段读取器
     * @return 汇总金额
     */
    private BigDecimal sumDetailAmount(List<SaleOutSheetDetail> details,
            Function<SaleOutSheetDetail, BigDecimal> getter) {
        return details.stream().map(getter).map(NumberUtil::getDefaultValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 批量刷新客户已付和未付金额。
     *
     * @param customerIds 客户ID集合
     */
    private void adjustCustomerAmounts(Set<String> customerIds) {
        Set<String> validCustomerIds = customerIds.stream().filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(validCustomerIds)) {
            return;
        }

        List<SaleOutSheet> customerSheets = this.list(Wrappers.lambdaQuery(SaleOutSheet.class)
                .in(SaleOutSheet::getCustomerId, validCustomerIds));
        Map<String, List<SaleOutSheet>> customerSheetMap = customerSheets.stream()
                .collect(Collectors.groupingBy(SaleOutSheet::getCustomerId));
        List<Customer> customers = new ArrayList<>();
        for (String customerId : validCustomerIds) {
            List<SaleOutSheet> currentSheets = customerSheetMap.getOrDefault(customerId,
                    Collections.emptyList());
            BigDecimal paidAmount = sumSheetAmount(currentSheets, SaleOutSheet::getPaidAmount);
            BigDecimal totalAmount = sumSheetAmount(currentSheets, SaleOutSheet::getTotalAmount);

            Customer customer = new Customer();
            customer.setId(customerId);
            customer.setPaidAmount(paidAmount);
            customer.setUnpaidAmount(NumberUtil.sub(totalAmount, paidAmount));
            customers.add(customer);
        }
        if (!customerService.updateBatchById(customers)) {
            throw new DefaultClientException("客户金额更新失败，请重试！");
        }
    }

    /**
     * 汇总销售出库单金额字段。
     *
     * @param sheets 销售出库单列表
     * @param getter 金额字段读取器
     * @return 汇总金额
     */
    private BigDecimal sumSheetAmount(List<SaleOutSheet> sheets,
            Function<SaleOutSheet, BigDecimal> getter) {
        return sheets.stream().map(getter).map(NumberUtil::getDefaultValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @OpLog(type = SaleOpLogType.class, name = "审核通过销售出库单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approvePass(ApprovePassSaleOutSheetVo vo) {

        SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }

        checkApproveStatus(sheet, "销售出库单已审核通过，不允许继续执行审核！", "销售出库单无法审核通过！");

        SaleConfig saleConfig = saleConfigService.get();

        if (!saleConfig.getOutStockMultipleRelateSale()) {
            Wrapper<SaleOutSheet> checkWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
                    .eq(SaleOutSheet::getSaleOrderId, sheet.getSaleOrderId())
                    .ne(SaleOutSheet::getId, sheet.getId());
            if (getBaseMapper().selectCount(checkWrapper) > 0) {
                SaleOrder purchaseOrder = saleOrderService.getById(sheet.getSaleOrderId());
                throw new DefaultClientException("销售订单号：" + purchaseOrder.getCode()
                        + "，已关联其他销售出库单，不允许关联多个销售出库单！");
            }
        }

        if (saleConfig.getOutStockRequireLogistics()) {
            // 关联物流单
            LogisticsSheetDetail logisticsSheetDetail = logisticsSheetDetailService.getByBizId(
                    sheet.getId(), LogisticsSheetDetailBizType.SALE_OUT_SHEET);
            if (logisticsSheetDetail == null) {
                throw new DefaultClientException("销售出库单尚未发货，无法审核通过！");
            }
        }

        sheet.setStatus(SaleOutSheetStatus.APPROVE_PASS);

        List<SaleOutSheetStatus> statusList = new ArrayList<>();
        statusList.add(SaleOutSheetStatus.CREATED);
        statusList.add(SaleOutSheetStatus.APPROVE_REFUSE);

        LambdaUpdateWrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
                .set(SaleOutSheet::getApproveTime, LocalDateTime.now())
                .eq(SaleOutSheet::getId, sheet.getId()).in(SaleOutSheet::getStatus, statusList);
        if (!StringUtil.isBlank(vo.getDescription())) {
            updateOrderWrapper.set(SaleOutSheet::getDescription, vo.getDescription());
        }
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String directApprovePass(CreateSaleOutSheetVo vo) {

        SaleOutSheetService thisService = getThis(this.getClass());

        String sheetId = thisService.create(vo);

        ApprovePassSaleOutSheetVo approvePassVo = new ApprovePassSaleOutSheetVo();
        approvePassVo.setId(sheetId);
        approvePassVo.setDescription(vo.getDescription());

        thisService.approvePass(approvePassVo);

        return sheetId;
    }

    @OpLog(type = SaleOpLogType.class, name = "审核拒绝销售出库单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approveRefuse(ApproveRefuseSaleOutSheetVo vo) {

        SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }

        if (sheet.getStatus() != SaleOutSheetStatus.CREATED) {

            if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("销售出库单已审核通过，不允许继续执行审核！");
            }

            if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_REFUSE) {
                throw new DefaultClientException("销售出库单已审核拒绝，不允许继续执行审核！");
            }

            throw new DefaultClientException("销售出库单无法审核拒绝！");
        }

        sheet.setStatus(SaleOutSheetStatus.APPROVE_REFUSE);

        LambdaUpdateWrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
                .set(SaleOutSheet::getApproveTime, LocalDateTime.now())
                .set(SaleOutSheet::getRefuseReason, vo.getRefuseReason())
                .eq(SaleOutSheet::getId, sheet.getId())
                .eq(SaleOutSheet::getStatus, SaleOutSheetStatus.CREATED);
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SaleOpLogType.class, name = "删除销售出库单，单号：{}", params = "#code")
    @OrderTimeLineLog(orderId = "#id", delete = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        Assert.notBlank(id);
        SaleOutSheet sheet = getBaseMapper().selectById(id);
        if (sheet == null) {
            throw new InputErrorException("销售出库单不存在！");
        }

        checkApproveStatus(sheet, "“审核通过”的销售出库单不允许执行删除操作！", "销售出库单无法删除！");
        if (Arrays.asList(SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE,
                SettleStatus.SETTLED).contains(sheet.getSettleStatus())) {
            throw new DefaultClientException("销售出库单已对账或已结算，不允许执行删除操作！");
        }

        if (hasStockSynced(sheet.getId())) {
            // 查询销售出库单明细
            List<SaleOutSheetDetail> details = getSheetDetails(sheet.getId());
            rollbackStock(sheet, details);
        }

        // 删除订单明细
        deleteSheetDetail(sheet.getId());

        // 删除订单
        deleteSheet(id);

        this.adjustCustomerAmount(sheet.getCustomerId());

        OpLogUtil.setVariable("code", sheet.getCode());
    }

    /**
     * 删除销售出库单
     * @param id
     */
    private void deleteSheet(String id) {
        Wrapper<SaleOutSheet> deleteWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
                .in(SaleOutSheet::getId, id)
                .in(SaleOutSheet::getStatus, SaleOutSheetStatus.CREATED, SaleOutSheetStatus.APPROVE_REFUSE);
        if (!remove(deleteWrapper)) {
            throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setUnSettle(String id) {

        return updateSettleStatus(id, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    }

    /**
     * 按提交时源单版本设置为未结算，避免并发对账重复确认。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setUnSettle(String id, SettleStatus settleStatus, Long settleVersion) {

        return getBaseMapper().updateSettleStatusWithVersion(id, settleStatus,
                SettleStatus.UN_SETTLE, settleVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setPartSettle(String id) {

        return updateSettleStatus(id, SettleStatus.PART_SETTLE, SettleStatus.UN_SETTLE,
                SettleStatus.PART_SETTLE);
    }

    /**
     * 按提交时源单版本设置为部分结算，避免并发结算重复占用余额。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setPartSettle(String id, SettleStatus settleStatus, Long settleVersion) {

        return getBaseMapper().updateSettleStatusWithVersion(id, settleStatus,
                SettleStatus.PART_SETTLE, settleVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setSettled(String id) {

        return updateSettleStatus(id, SettleStatus.SETTLED, SettleStatus.UN_SETTLE,
                SettleStatus.PART_SETTLE);
    }

    /**
     * 按提交时源单版本设置为已结算，避免并发结算重复占用余额。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setSettled(String id, SettleStatus settleStatus, Long settleVersion) {

        return getBaseMapper().updateSettleStatusWithVersion(id, settleStatus,
                SettleStatus.SETTLED, settleVersion);
    }

    /**
     * 按当前结算状态和版本号原子回写历史结算入口的状态。
     */
    private int updateSettleStatus(String id, SettleStatus targetStatus,
            SettleStatus... allowedStatuses) {

        SaleOutSheet sheet = getById(id);
        if (sheet == null || !Arrays.asList(allowedStatuses).contains(sheet.getSettleStatus())) {
            return 0;
        }
        Long settleVersion = sheet.getSettleVersion() == null ? 0L : sheet.getSettleVersion();
        return getBaseMapper().updateSettleStatusWithVersion(id, sheet.getSettleStatus(),
                targetStatus, settleVersion);
    }

    @Override
    public List<SaleOutSheet> getApprovedList(String customerId, LocalDateTime startTime,
            LocalDateTime endTime, SettleStatus settleStatus) {

        return getBaseMapper().getApprovedList(customerId, startTime, endTime, settleStatus);
    }

    private void create(SaleOutSheet sheet, CreateSaleOutSheetVo vo) {

        handleScId(sheet, vo);
        Customer customer = customerService.findById(vo.getCustomerId());
        if (customer == null) {
            throw new InputErrorException("客户不存在！");
        }
        sheet.setCustomerId(vo.getCustomerId());
        sheet.setOrderDate(vo.getOrderDate());

        List<SaleOutSheetDetail> details = new ArrayList<>(vo.getProducts().size());
        for (SaleOutProductVo productVo : vo.getProducts()) {
            Product product = productService.findById(productVo.getProductId());
            Assert.notNull(product, "第" + productVo.getSeq() + "行商品不存在！");

            SaleOutSheetDetail detail = buildDetail(sheet, productVo, product, customer);

            saleOutSheetDetailService.save(detail);
            updateProductPrice(product, detail);
            productLatestPriceCacheService.updateLatestPrice(product.getId(),
                    toBasePrice(detail.getTaxPrice(), detail.getConversionRate()),
                    null);
            details.add(detail);
        }
        sheet.setDescription(vo.getDescription());
        sheet.setSettleStatus(this.getInitSettleStatus(customer));
        SaleOutSheetAmtCalculator.calculateSheet(sheet, details);
        sheet.setPaidAmount(this.normalizePaidAmount(vo.getPaidAmount(), sheet.getTotalAmount()));
    }

    private SaleOutSheetDetail buildDetail(SaleOutSheet sheet,
                                           SaleOutProductVo productVo,
                                           Product product,
                                           Customer customer) {
        ProductUnit unit = resolveUnit(product, productVo.getUnitId(), productVo.getUnit());
        BigDecimal baseNum = NumberUtil.mul(productVo.getOrderNum(), unit.getConversionRate());
        BigDecimal price = productVo.getTaxPrice() == null ? NumberUtil.mul(getDefaultSalePrice(product), unit.getConversionRate()) : productVo.getTaxPrice();

        SaleOutSheetDetail detail = new SaleOutSheetDetail();
        detail.setId(IdUtil.getId());
        detail.setSheetId(sheet.getId());

        detail.setProductId(productVo.getProductId());
        detail.setOrderNum(baseNum);
        detail.setUnitId(unit.getId());
        detail.setUnitName(unit.getUnitName());
        detail.setConversionRate(unit.getConversionRate());
        detail.setBusinessNum(productVo.getOrderNum());
        detail.setConfirmNum(productVo.getConfirmNum());
        detail.setOriPrice(productVo.getOriPrice());
        detail.setTaxPrice(price);
        detail.setDiscountRate(productVo.getDiscountRate());
        detail.setTaxRate(product.getSaleTaxRate());
        detail.setDescription(productVo.getDescription());
        detail.setOrderNo(productVo.getSeq());
        detail.setActualDate(productVo.getActualDate());
        detail.setPlanDate(productVo.getPlanDate());
        detail.setSettleStatus(this.getInitSettleStatus(customer));
        detail.setTaxAmount(SaleOutSheetAmtCalculator.calculateLineAmount(price,
                detail.getBusinessNum()));
        detail.setConfirmAmt(SaleOutSheetAmtCalculator.calculateLineAmount(price,
                detail.getConfirmNum()));
        return detail;
    }

    private void handleScId(SaleOutSheet sheet, CreateSaleOutSheetVo vo) {
        if (StringUtil.isNotBlank(vo.getScId())) {
            StoreCenter sc = storeCenterService.findById(vo.getScId());
            if (sc == null) {
                throw new InputErrorException("仓库不存在！");
            }

            sheet.setScId(vo.getScId());
        }
    }

    private ProductUnit resolveUnit(Product product, String unitId, String unitName) {
        ProductUnit unit = StringUtil.isNotBlank(unitId)
                ? productUnitService.getAvailableById(product.getId(), unitId) : null;
        if (unit == null) {
            unit = StringUtil.isBlank(unitName)
                    ? productUnitService.getAvailableByProductId(product.getId()).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getBaseUnit())).findFirst().orElse(null)
                    : productUnitService.getAvailableByUnitName(product.getId(), unitName);
        }
        if (unit == null) {
            throw new InputErrorException("商品单位不存在或已停用！");
        }
        return unit;
    }

    private void subStock(SaleOutSheet sheet) {
        List<SaleOutSheetDetail> details = getSheetDetails(sheet.getId());

        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        int orderNo = 1;
        for (SaleOutSheetDetail detail : details) {
            if (detail.getOrderNum() == null || detail.getOrderNum().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            SubProductStockVo subProductStockVo = new SubProductStockVo();
            subProductStockVo.setProductId(detail.getProductId());
            subProductStockVo.setScId(sheet.getScId());
            subProductStockVo.setStockNum(detail.getOrderNum());
            subProductStockVo.setBizId(sheet.getId());
            subProductStockVo.setBizDetailId(detail.getId());
            subProductStockVo.setBizCode(sheet.getCode());
            subProductStockVo.setBizType(ProductStockBizType.SALE.getCode());
            ProductStockChangeDto stockChange = productStockService.subStock(subProductStockVo);
            orderNo = createSaleOutDetailLots(sheet, detail, stockChange, orderNo);
        }
    }

    private int createSaleOutDetailLots(SaleOutSheet sheet, SaleOutSheetDetail detail,
            ProductStockChangeDto stockChange, int orderNo) {

        if (NumberUtil.gt(defaultValue(stockChange.getCostNum()), BigDecimal.ZERO)) {
            SaleOutSheetDetailLot costedLot = new SaleOutSheetDetailLot();
            costedLot.setId(IdUtil.getId());
            costedLot.setDetailId(detail.getId());
            costedLot.setOrderNum(stockChange.getCostNum());
            costedLot.setCostTaxAmount(stockChange.getTaxAmount());
            costedLot.setSettledCostNum(stockChange.getCostNum());
            costedLot.setCostStatus(StockCostStatus.FINAL);
            costedLot.setSettleStatus(detail.getSettleStatus());
            costedLot.setOrderNo(orderNo++);
            saleOutSheetDetailLotService.save(costedLot);
        }

        if (NumberUtil.gt(defaultValue(stockChange.getPendingNum()), BigDecimal.ZERO)) {
            SaleOutSheetDetailLot pendingLot = new SaleOutSheetDetailLot();
            pendingLot.setId(IdUtil.getId());
            pendingLot.setDetailId(detail.getId());
            pendingLot.setOrderNum(stockChange.getPendingNum());
            pendingLot.setCostTaxAmount(null);
            pendingLot.setSettledCostNum(BigDecimal.ZERO);
            pendingLot.setCostStatus(StockCostStatus.PENDING);
            pendingLot.setSettleStatus(detail.getSettleStatus());
            pendingLot.setOrderNo(orderNo++);
            saleOutSheetDetailLotService.save(pendingLot);

            productStockPendingCostService.create(sheet.getScId(), detail.getProductId(), sheet.getId(),
                    detail.getId(), ProductStockBizType.SALE, pendingLot.getId(), stockChange.getPendingNum(),
                    stockChange.getCreateTime());
        }

        return orderNo;
    }

    private void rollbackStock(SaleOutSheet sheet, List<SaleOutSheetDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        Map<String, BigDecimal> lotCostMap = getDetailLotCostMap(details);
        for (SaleOutSheetDetail detail : details) {
            if (detail.getOrderNum() == null || detail.getOrderNum().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Product product = productService.findById(detail.getProductId());

            AddProductStockVo addProductStockVo = new AddProductStockVo();
            addProductStockVo.setProductId(detail.getProductId());
            addProductStockVo.setScId(sheet.getScId());
            addProductStockVo.setStockNum(detail.getOrderNum());
            addProductStockVo.setTaxAmount(lotCostMap.get(detail.getId()));
            addProductStockVo.setDefaultTaxAmount(NumberUtil.getNumber(
                    NumberUtil.mul(product.getPurchasePrice(), detail.getOrderNum()), 2));
            addProductStockVo.setBizId(sheet.getId());
            addProductStockVo.setBizDetailId(detail.getId());
            addProductStockVo.setBizCode(sheet.getCode());
            addProductStockVo.setBizType(ProductStockBizType.SALE.getCode());

            productStockService.addStock(addProductStockVo);
        }

        removeDetailLots(details);
    }

    private Map<String, BigDecimal> getDetailLotCostMap(List<SaleOutSheetDetail> details) {
        List<String> detailIds = details.stream().map(SaleOutSheetDetail::getId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(detailIds)) {
            return new HashMap<>();
        }

        Wrapper<SaleOutSheetDetailLot> queryWrapper = Wrappers.lambdaQuery(SaleOutSheetDetailLot.class)
                .in(SaleOutSheetDetailLot::getDetailId, detailIds);
        List<SaleOutSheetDetailLot> lots = saleOutSheetDetailLotService.list(queryWrapper);
        if (CollectionUtil.isEmpty(lots)) {
            return new HashMap<>();
        }

        return lots.stream().collect(Collectors.groupingBy(SaleOutSheetDetailLot::getDetailId,
                Collectors.mapping(item -> item.getCostTaxAmount() == null ? BigDecimal.ZERO : item.getCostTaxAmount(),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private void removeDetailLots(List<SaleOutSheetDetail> details) {
        List<String> detailIds = details.stream().map(SaleOutSheetDetail::getId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(detailIds)) {
            return;
        }

        Wrapper<SaleOutSheetDetailLot> deleteDetailLotWrapper = Wrappers.lambdaQuery(
                SaleOutSheetDetailLot.class).in(SaleOutSheetDetailLot::getDetailId, detailIds);
        saleOutSheetDetailLotService.remove(deleteDetailLotWrapper);
    }

    private boolean hasStockSynced(String sheetId) {
        Wrapper<ProductStockLog> queryWrapper = Wrappers.lambdaQuery(ProductStockLog.class)
                .eq(ProductStockLog::getBizId, sheetId)
                .eq(ProductStockLog::getBizType, ProductStockBizType.SALE);
        return productStockLogService.count(queryWrapper) > 0;
    }

    private List<SaleOutSheetDetail> getSheetDetails(String sheetId) {
        Wrapper<SaleOutSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
                .eq(SaleOutSheetDetail::getSheetId, sheetId)
                .orderByAsc(SaleOutSheetDetail::getOrderNo);
        return saleOutSheetDetailService.list(queryDetailWrapper);
    }

    /**
     * 更新商品价格
     * 
     * @param product
     * @param detail
     */
    private void updateProductPrice(Product product, SaleOutSheetDetail detail) {
        QuerySysParameterVo sysParameterVo = new QuerySysParameterVo();
        sysParameterVo.setPmKey("latest_price_override_product_price");
        List<SysParameter> list = sysParameterService.query(sysParameterVo);
        if (CollectionUtil.isEmpty(list)) {
            return;
        }

        boolean override = BooleanUtil.toBoolean(list.get(0).getPmValue());
        if (override) {
            productService.updatePrice(product.getId(),
                    toBasePrice(detail.getTaxPrice(), detail.getConversionRate()), null);
        }
    }

    private BigDecimal toBasePrice(BigDecimal price, BigDecimal conversionRate) {
        BigDecimal rate = conversionRate == null ? BigDecimal.ONE : conversionRate;
        return price.divide(rate, NumberUtil.AMT_PRECISION, RoundingMode.HALF_UP);
    }

    /**
     * 根据客户获取初始结算状态
     *
     * @param customer
     * @return
     */
    private SettleStatus getInitSettleStatus(Customer customer) {

        return SettleStatus.UN_CHECK_BILL;
    }

    private BigDecimal normalizePaidAmount(BigDecimal paidAmount, BigDecimal totalAmount) {

        BigDecimal actualPaidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        // 付款金额允许负数，不再校验不小于0

        if (!NumberUtil.isNumberPrecision(actualPaidAmount, NumberUtil.AMT_PRECISION)) {
            throw new InputErrorException("付款金额最多允许6位小数！");
        }

        if (NumberUtil.gt(actualPaidAmount.abs(), totalAmount.abs())) {
            throw new InputErrorException("付款金额绝对值不允许大于单据总金额绝对值！");
        }

        return actualPaidAmount;
    }

    private void validateBatchUpdatePriceDetail(SaleOutSheetDetail detail) {

        if (detail == null) {
            throw new DefaultClientException("销售出库明细不存在，请刷新后重试！");
        }

        if (Boolean.TRUE.equals(detail.getIsGift())) {
            throw new DefaultClientException("商品明细为赠品，不允许调整售价！");
        }

        if (detail.getSettleStatus() != SettleStatus.UN_CHECK_BILL) {
            throw new DefaultClientException("仅支持调整未结算的商品明细售价！");
        }
    }

    private BigDecimal calcSheetTotalAmount(String sheetId) {

        List<SaleOutSheetDetail> sheetDetails = saleOutSheetDetailService.getBySheetId(sheetId);
        return sheetDetails.stream()
                .map(item -> item.getTaxAmount() == null ? BigDecimal.ZERO : item.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void adjustCustomerAmount(String customerId) {

        if (StringUtil.isBlank(customerId)) {
            return;
        }

        List<SaleOutSheet> sheets = this.list(Wrappers.lambdaQuery(SaleOutSheet.class)
                .eq(SaleOutSheet::getCustomerId, customerId));

        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        for (SaleOutSheet item : sheets) {
            BigDecimal itemPaidAmount = item.getPaidAmount() == null ? BigDecimal.ZERO : item.getPaidAmount();
            BigDecimal itemTotalAmount = item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount();
            paidAmount = NumberUtil.add(paidAmount, itemPaidAmount);
            unpaidAmount = NumberUtil.add(unpaidAmount, NumberUtil.sub(itemTotalAmount, itemPaidAmount));
        }

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setPaidAmount(paidAmount);
        customer.setUnpaidAmount(unpaidAmount);
        if (!customerService.updateById(customer)) {
            throw new DefaultClientException("客户金额更新失败，请重试！");
        }
    }

    @Override
    public List<SaleOutProductVo> checkImport(List<SaleOutSheetImportModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }

        handleSeq(list);

        // 匹配编号
        List<String> errors = checkImportData(list);
        Assert.isTrue(CollectionUtils.isEmpty(errors), StringUtils.join(errors, ";\r\n"));

        return list.stream()
                .map(item -> BeanUtil.copyProperties(item, SaleOutProductVo.class))
                .collect(Collectors.toList());
    }

    private void handleSeq(List<SaleOutSheetImportModel> list) {
        for (int i = 0; i < list.size(); i++) {
            SaleOutSheetImportModel model = list.get(i);
            if (model.getSeq() == null) {
                model.setSeq(i + 2);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<String> importByQuery(List<SaleOutSheetQueryImportModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }

        SaleOutSheetService thisService = getThis(this.getClass());

        for (int i = 0; i < list.size(); i++) {
            SaleOutSheetQueryImportModel model = list.get(i);
            model.setSeq(i + 2);
            normalizeQueryImportNumbers(model);
        }
        Map<String, List<SaleOutSheetQueryImportModel>> map = list.stream().collect(
                Collectors.groupingBy(item -> item.getOrderDate() + "|" + item.getCustomerName()));

        return map.keySet().stream()
                .map(item -> thisService.create(buildCreateVo(map.get(item))))
                .collect(Collectors.toList());
    }

    static void normalizeQueryImportNumbers(SaleOutSheetQueryImportModel model) {
        if (model.getOrderNum() == null) {
            model.setOrderNum(BigDecimal.ZERO);
        }
        if (model.getConfirmNum() == null) {
            model.setConfirmNum(BigDecimal.ZERO);
        }
    }

    private CreateSaleOutSheetVo buildCreateVo(List<SaleOutSheetQueryImportModel> list) {
        SaleOutSheetQueryImportModel model = list.get(0);
        Customer customer = getImportCustomer(model.getCustomerName());

        CreateSaleOutSheetVo res = new CreateSaleOutSheetVo();
        res.setCustomerId(customer.getId());
        res.setOrderDate(DateUtil.parseDate(model.getOrderDate(), "yyyyMMdd"));
        res.setRequired(Boolean.FALSE);
        res.setProducts(buildImportProducts(list));
        res.setScId(storeCenterService.getDefaultStoreId());

        return res;
    }

    private Customer getImportCustomer(String customerName) {
        if (StringUtil.isBlank(customerName)) {
            throw new InputErrorException("客户不能为空！");
        }

        QueryCustomerVo queryCustomerVo = new QueryCustomerVo();
        queryCustomerVo.setName(customerName);
        List<Customer> customers = customerService.query(queryCustomerVo).stream()
                .filter(item -> customerName.equals(item.getName()))
                .collect(Collectors.toList());
        Assert.notEmpty(customers, "客户不存在：" + customerName);
        if (customers.size() > 1) {
            throw new InputErrorException("存在多个同名客户，请先整理基础资料后再导入：" + customerName);
        }

        return customers.get(0);
    }

    private List<SaleOutProductVo> buildImportProducts(List<SaleOutSheetQueryImportModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }

        List<SaleOutSheetImportModel> collect = list.stream()
                .map(item -> BeanUtil.copyProperties(item, SaleOutSheetImportModel.class))
                .collect(Collectors.toList());
        List<SaleOutProductVo> checked = checkImport(collect);
        List<SaleOutProductVo> products = checked.stream()
                .map(item -> BeanUtil.copyProperties(item, SaleOutProductVo.class))
                .collect(Collectors.toList());

        for (int i = 0; i < products.size(); i++) {
            products.get(i).setActualDate(parseActualDate(list.get(i).getActualDate(), list.get(i).getSeq()));
        }

        return products;
    }

    private LocalDate parseActualDate(String value, Integer rowIndex) {
        if (StringUtil.isBlank(value)) {
            return null;
        }

        String text = value.trim();
        int blankIndex = text.indexOf(' ');
        if (blankIndex > 0) {
            text = text.substring(0, blankIndex);
        }
        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            text = text.substring(0, tIndex);
        }

        try {
            return LocalDate.parse(text, QUERY_IMPORT_ACTUAL_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DefaultClientException("第" + rowIndex + "行“配送日期”格式错误，正确格式为yyyy-MM-dd");
        }
    }

    private List<String> checkImportData(List<SaleOutSheetImportModel> list) {
        List<String> productNames = list.stream().map(SaleOutSheetImportModel::getProductName)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productService.selectByProductName(productNames);
        Map<String, List<Product>> nameUnitMap = new HashMap<>();
        for (Product product : products) {
            productUnitService.getAvailableByProductId(product.getId()).stream()
                    .forEach(unit -> nameUnitMap.computeIfAbsent(
                            buildProductImportKey(product.getName(), unit.getUnitName()), key -> new ArrayList<>())
                            .add(product));
        }

        List<String> errors = Lists.newArrayList();
        for (int i = 0; i < list.size(); i++) {
            SaleOutSheetImportModel data = list.get(i);
            int rowIndex = data.getSeq();

            if (StringUtils.isBlank(data.getProductName())) {
                errors.add("第" + rowIndex + "行“商品名称”不能为空");
            }
            if (StringUtils.isBlank(data.getUnit())) {
                errors.add("第" + rowIndex + "行“单位”不能为空");
            }
            errors.addAll(validateImportNumbers(data));

            Product product = matchImportProduct(data, nameUnitMap);
            if (product != null) {
                ProductUnit unit = productUnitService.getAvailableByUnitName(product.getId(),
                        StringUtils.trim(data.getUnit()));
                if (unit == null) {
                    continue;
                }
                data.setProductCode(product.getCode());
                data.setProductId(product.getId());
                data.setUnitId(unit.getId());
                data.setSpec(product.getSpec());
                data.setUnit(unit.getUnitName());
                data.setInquiryProduct(product.getInquiryProduct());
                data.setOriPrice(product.getSalePrice() == null ? BigDecimal.ZERO
                        : NumberUtil.mul(product.getSalePrice(), unit.getConversionRate()));
                // 导入时，如果指定销售价，则以销售价为准
                if (data.getTaxPrice() == null) {
                    data.setTaxPrice(NumberUtil.mul(getDefaultSalePrice(product), unit.getConversionRate()));
                }
            }
        }
        return errors;
    }

    static List<String> validateImportNumbers(SaleOutSheetImportModel data) {
        List<String> errors = Lists.newArrayList();
        int rowIndex = data.getSeq();
        // 数量允许负数，不再校验不小于0
        if (data.getOrderNum() != null && !NumberUtil.isNumberPrecision(data.getOrderNum(), 8)) {
            errors.add("第" + rowIndex + "行“数量”最多允许8位小数");
        }
        if (data.getTaxPrice() != null && NumberUtil.lt(data.getTaxPrice(), BigDecimal.ZERO)) {
            errors.add("第" + rowIndex + "行“单价”不允许小于0");
        }
        if (data.getTaxPrice() != null && !NumberUtil.isNumberPrecision(data.getTaxPrice(), 6)) {
            errors.add("第" + rowIndex + "行“单价”最多允许6位小数");
        }
        if (data.getConfirmNum() != null && NumberUtil.lt(data.getConfirmNum(), BigDecimal.ZERO)) {
            errors.add("第" + rowIndex + "行“验收数量”不允许小于0");
        }
        if (data.getConfirmNum() != null && !NumberUtil.isNumberPrecision(data.getConfirmNum(), 6)) {
            errors.add("第" + rowIndex + "行“验收数量”最多允许6位小数");
        }
        return errors;
    }

    private String buildProductImportKey(String productName, String unit) {
        return StringUtils.trimToEmpty(productName) + StringPool.STR_SPLIT
                + StringUtils.trimToEmpty(unit);
    }

    private Product matchImportProduct(SaleOutSheetImportModel data,
            Map<String, List<Product>> nameUnitMap) {
        if (StringUtils.isBlank(data.getProductName()) || StringUtils.isBlank(data.getUnit())) {
            return null;
        }

        List<Product> candidates = nameUnitMap.get(buildProductImportKey(data.getProductName(), data.getUnit()));
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        String spec = StringUtils.trimToEmpty(data.getSpec());
        List<Product> specMatchedProducts = candidates.stream()
                .filter(item -> StringUtils.equals(StringUtils.trimToEmpty(item.getSpec()), spec))
                .collect(Collectors.toList());

        return specMatchedProducts.size() == 1 ? specMatchedProducts.get(0) : null;
    }

    @Override
    public void refreshCostPrice(LocalDate orderDate) {
        LambdaQueryWrapper<SaleOutSheet> orderDateQuery = Wrappers.lambdaQuery(SaleOutSheet.class)
                .eq(SaleOutSheet::getOrderDate, orderDate);
        List<SaleOutSheet> list = getBaseMapper().selectList(orderDateQuery);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(item -> {
            refreshCostPrice(item.getId());
        });
    }

    @Override
    public void refreshCostPrice(String orderId) {
        refreshCostPrice(orderId, null, false);
    }

    private void refreshCostPrice(String orderId, Boolean manualFillAllCost, boolean overrideFillAllCost) {
        log.info("refreshCostPrice start, orderId: {}, manualFillAllCost: {}, overrideFillAllCost: {}", orderId,
                manualFillAllCost, overrideFillAllCost);
        SaleOutSheet saleOutSheet = getBaseMapper().selectById(orderId);
        if (saleOutSheet == null) {
            return;
        }
        LocalDate orderDate = saleOutSheet.getOrderDate();
        // 1. 查询当天的所有采购单，同一商品按照总金额、总数量，计算采购价；
        // 2. 获取销售明细，补齐cost_price,total_profit
        // 3. 汇总单据明细，补齐单据cost_price,total_profit
        // 4. 如果所有的商品采购成本都已经录入，则标记单据fill_all_cost=true, 单据查询页面也展示该字段
        Map<String, QueryReceiveSheetDetailDto> receiveCostPriceMap = getCostPriceMap(
                saleOutSheet.getScId(), orderDate);

        List<SaleOutSheetDetail> saleDetails = saleOutSheetDetailService.getBySheetId(orderId);

        boolean fillAllCost = true;
        BigDecimal totalCostAmount = BigDecimal.ZERO;
        for (SaleOutSheetDetail saleDetail : saleDetails) {
            QueryReceiveSheetDetailDto receiveDetail = receiveCostPriceMap.get(saleDetail.getProductId());
            if (receiveDetail == null) {
                fillAllCost = false;
                saleDetail.setCostPrice(null);
                saleDetail.setTotalProfit(null);
                saleOutSheetDetailService.saveOrUpdateAllColumn(saleDetail);
                continue;
            }
            // receiveDetail.getTaxPrice() 在sql中已经换算成基本单位对应采购价了。
            BigDecimal detailCostAmount = NumberUtil.calculateAmount(
                    NumberUtil.getDefaultValue(receiveDetail.getTaxPrice()), resolveCostNum(saleDetail));
            BigDecimal detailTotalProfit = NumberUtil.getNumber(
                    NumberUtil.sub(resolveConfirmAmt(saleDetail), detailCostAmount),
                    NumberUtil.AMT_PRECISION);
            totalCostAmount = NumberUtil.add(totalCostAmount, detailCostAmount);

            saleDetail.setCostPrice(receiveDetail.getTaxPrice());
            saleDetail.setTotalProfit(detailTotalProfit);
            saleDetail.setSupplierId(receiveDetail.getSupplierId());
            saleOutSheetDetailService.saveOrUpdateAllColumn(saleDetail);
        }

        // 总利润由明细汇总
        BigDecimal totalProfit = saleDetails.stream()
                .map(item -> NumberUtil.getDefaultValue(item.getTotalProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Boolean finalFillAllCost = overrideFillAllCost ? manualFillAllCost : fillAllCost;

        LambdaUpdateWrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                .set(SaleOutSheet::getTotalCost, totalCostAmount)
                .set(SaleOutSheet::getTotalProfit, totalProfit)
                .set(SaleOutSheet::getFillAllCost, finalFillAllCost)
                .eq(SaleOutSheet::getId, orderId);
        this.update(updateWrapper);
        log.info("refreshCostPrice end, orderId: {}", orderId);
    }

    /**
     * 获取计算成本金额使用的数量。
     *
     * @param saleDetail 销售出库单明细
     * @return 成本计算数量
     */
    static BigDecimal resolveCostNum(SaleOutSheetDetail saleDetail) {
        if (saleDetail == null) {
            return BigDecimal.ZERO;
        }

        return saleDetail.getConfirmNum() == null || saleDetail.getConfirmNum().compareTo(BigDecimal.ZERO) == 0
                ? NumberUtil.getDefaultValue(saleDetail.getOrderNum())
                : saleDetail.getConfirmNum();
    }

    private BigDecimal resolveConfirmAmt(SaleOutSheetDetail saleDetail) {
        if (saleDetail == null) {
            return BigDecimal.ZERO;
        }

        return saleDetail.getConfirmAmt() != null && saleDetail.getConfirmAmt().compareTo(BigDecimal.ZERO) > 0
                ? saleDetail.getConfirmAmt()
                : NumberUtil.getDefaultValue(saleDetail.getTaxAmount());
    }

    /**
     * 查询当天的所有采购单, 获取商品的采购价
     *
     * @param orderDate
     * @return
     */
    private Map<String, QueryReceiveSheetDetailDto> getCostPriceMap(String scId, LocalDate orderDate) {
        if (useStockPriceAsLatestCostPrice()) {
            return getCostPriceMapFromProductStock(scId);
        }

        return getCostPriceMapFromReceiveSheet(orderDate);
    }

    /**
     * 采用最近的采购价
     * 
     * @param orderDate
     * @return
     */
    private Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromReceiveSheet(LocalDate orderDate) {
        LocalDate beginDate = orderDate.plusMonths(-6);
        List<QueryReceiveSheetDetailDto> latestCostPrices = receiveSheetDetailMapper.getLatestCostPriceList(beginDate,
                orderDate);

        return toCostPriceMap(latestCostPrices);
    }

    /**
     * 采用库存表成本价
     * 
     * @param scId
     * @return
     */
    private Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromProductStock(String scId) {
        List<ProductStock> productStocks = productStockMapper.getPositiveStockListByScId(scId);
        if (CollectionUtils.isEmpty(productStocks)) {
            return new HashMap<>();
        }

        List<QueryReceiveSheetDetailDto> latestCostPrices = productStocks.stream().map(item -> {
            QueryReceiveSheetDetailDto dto = new QueryReceiveSheetDetailDto();
            dto.setProductId(item.getProductId());
            dto.setTaxPrice(item.getTaxPrice());
            return dto;
        }).collect(Collectors.toList());

        return toCostPriceMap(latestCostPrices);
    }

    /**
     * 计算月加权均价
     * <p>
     * 月加权均价 = SUM(采购总金额) / SUM(采购总数量)
     * 已过滤赠品（is_gift = 0），数据在 SQL 中已汇总
     *
     * @param beginDate 采购时间范围起
     * @param endDate   采购时间范围止
     * @return productId -> 月加权均价
     */
    private Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromMonthWtdAvg(
            LocalDate beginDate, LocalDate endDate) {
        List<QueryReceiveSheetDetailDto> list =
                receiveSheetDetailMapper.getMonthWtdAvgCostPriceList(beginDate, endDate);
        return toCostPriceMap(list);
    }

    /**
     * 月底成本重算 - 使用月加权平均法
     * <p>
     * 1. 计算时间范围内的月加权均价（采购总金额 / 采购总数量）
     * 2. 当月无采购的商品回退到最近一次采购价
     * 3. 遍历时间范围内所有销售出库单，更新 costPrice / totalProfit
     *
     * @param vo 重算参数
     * @return 重算结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo) {
        LocalDate beginDate = vo.getBeginDate();
        LocalDate endDate = vo.getEndDate();

        log.info("月底成本重算开始, beginDate: {}, endDate: {}, scId: {}", beginDate, endDate, vo.getScId());

        // 1. 计算月加权均价
        Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap =
                getCostPriceMapFromMonthWtdAvg(beginDate, endDate);

        // 2. 回退方案：当月无采购的商品用最近一次采购价
        Map<String, QueryReceiveSheetDetailDto> fallbackMap =
                getCostPriceMapFromReceiveSheet(endDate);

        // 3. 查询时间范围内所有销售出库单
        List<SaleOutSheet> sheets = querySaleOutSheets(vo.getScId(), beginDate, endDate);

        if (CollectionUtils.isEmpty(sheets)) {
            log.info("月底成本重算：时间范围内无销售出库单");
            return new MonthEndRecalculateResult(0, 0, 0);
        }

        // 4. 逐单据更新成本
        return processSheetsWithCostPrice(sheets, monthWtdAvgMap, fallbackMap);
    }


    /**
     * 月底成本重算（启动）—— 计算并缓存全范围月加权均价
     * <p>
     * 计算 calcBeginDate~calcEndDate 范围内的月加权均价及回退价格，
     * 缓存到内存中并返回 taskId，供后续 step 调用逐天执行。
     *
     * @param vo 启动参数（calcBeginDate, calcEndDate, scId）
     * @return 任务ID及总天数
     */
    @Override
    public MonthEndRecalculateStartResult startMonthEndRecalculate(MonthEndRecalculateStartVo vo) {
        LocalDate calcBeginDate = vo.getCalcBeginDate();
        LocalDate calcEndDate = vo.getCalcEndDate();

        log.info("成本重算启动, calcBeginDate: {}, calcEndDate: {}, scId: {}", calcBeginDate, calcEndDate, vo.getScId());

        // 1. 计算月加权均价
        Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap =
                getCostPriceMapFromMonthWtdAvg(calcBeginDate, calcEndDate);

        // 2. 回退方案：当月无采购的商品用最近一次采购价
        Map<String, QueryReceiveSheetDetailDto> fallbackMap =
                getCostPriceMapFromReceiveSheet(calcEndDate);

        // 3. 构建缓存任务
        String taskId = UUID.randomUUID().toString().replace("-", "");
        int totalDays = (int) ChronoUnit.DAYS.between(calcBeginDate, calcEndDate) + 1;

        RecalculateTask task = new RecalculateTask(
                taskId, calcBeginDate, calcEndDate, vo.getScId(),
                monthWtdAvgMap, fallbackMap, totalDays, LocalDateTime.now());
        recalculateTaskCache.put(taskId, task);

        // 清理过期任务
        cleanExpiredTasks();

        log.info("成本重算任务已创建, taskId: {}, totalDays: {}", taskId, totalDays);
        return new MonthEndRecalculateStartResult(taskId, totalDays);
    }

    /**
     * 月底成本重算（逐天执行）—— 使用缓存的均价处理指定日期的单据
     *
     * @param vo 执行参数（taskId, processDate）
     * @return 当天执行结果（含 hasError 标识）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthEndRecalculateStepResult stepMonthEndRecalculate(MonthEndRecalculateStepVo vo) {
        String taskId = vo.getTaskId();
        LocalDate processDate = vo.getProcessDate();

        // 1. 从缓存取任务
        RecalculateTask task = recalculateTaskCache.get(taskId);
        if (task == null) {
            return new MonthEndRecalculateStepResult(0, 0, 0, processDate, true,
                    "任务已过期，请重新发起成本重算");
        }

        // 2. 检查任务是否过期
        if (ChronoUnit.MINUTES.between(task.getCreatedAt(), LocalDateTime.now()) > TASK_EXPIRE_MINUTES) {
            recalculateTaskCache.remove(taskId);
            return new MonthEndRecalculateStepResult(0, 0, 0, processDate, true,
                    "任务已过期（超过" + TASK_EXPIRE_MINUTES + "分钟），请重新发起成本重算");
        }

        try {
            // 3. 查询指定日期的销售出库单
            List<SaleOutSheet> sheets = querySaleOutSheets(task.getScId(), processDate, processDate);

            if (CollectionUtils.isEmpty(sheets)) {
                log.info("成本重算 step：日期 {} 无销售出库单", processDate);
                return new MonthEndRecalculateStepResult(0, 0, 0, processDate, false, null);
            }

            // 4. 使用缓存的均价更新单据
            MonthEndRecalculateResult result = processSheetsWithCostPrice(
                    sheets, task.getMonthWtdAvgMap(), task.getFallbackMap());

            log.info("成本重算 step 完成, 日期: {}, 单据数: {}, 明细数: {}",
                    processDate, result.getUpdatedSheetCount(), result.getUpdatedDetailCount());

            return new MonthEndRecalculateStepResult(
                    result.getUpdatedSheetCount(), result.getUpdatedDetailCount(),
                    result.getNotFilledCount(), processDate, false, null);

        } catch (Exception e) {
            log.error("成本重算 step 失败, 日期: {}, taskId: {}", processDate, taskId, e);
            return new MonthEndRecalculateStepResult(0, 0, 0, processDate, true, e.getMessage());
        }
    }

    /**
     * 用给定的成本单价逐单据更新成本（提取的公共方法）
     *
     * @param sheets          待更新单据列表
     * @param monthWtdAvgMap  月加权均价表
     * @param fallbackMap     回退采购价表
     * @return 更新结果
     */
    private MonthEndRecalculateResult processSheetsWithCostPrice(
            List<SaleOutSheet> sheets,
            Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap,
            Map<String, QueryReceiveSheetDetailDto> fallbackMap) {

        int detailCount = 0;
        int notFilledCount = 0;
        for (SaleOutSheet sheet : sheets) {
            List<SaleOutSheetDetail> details = saleOutSheetDetailService.getBySheetId(sheet.getId());
            if (CollectionUtils.isEmpty(details)) {
                continue;
            }

            boolean fillAllCost = true;
            BigDecimal totalCostAmount = BigDecimal.ZERO;
            for (SaleOutSheetDetail detail : details) {
                // 优先月加权均价，无则回退到最近采购价
                QueryReceiveSheetDetailDto costDto = monthWtdAvgMap.get(detail.getProductId());
                if (costDto == null) {
                    costDto = fallbackMap.get(detail.getProductId());
                }

                if (costDto == null) {
                    fillAllCost = false;
                    notFilledCount++;
                    detail.setCostPrice(null);
                    detail.setTotalProfit(null);
                    saleOutSheetDetailService.saveOrUpdateAllColumn(detail);
                    continue;
                }

                BigDecimal detailCostAmount = NumberUtil.calculateAmount(
                        NumberUtil.getDefaultValue(costDto.getTaxPrice()), resolveCostNum(detail));
                BigDecimal detailTotalProfit = NumberUtil.getNumber(
                        NumberUtil.sub(resolveConfirmAmt(detail), detailCostAmount),
                        NumberUtil.AMT_PRECISION);
                totalCostAmount = NumberUtil.add(totalCostAmount, detailCostAmount);

                detail.setCostPrice(costDto.getTaxPrice());
                detail.setTotalProfit(detailTotalProfit);
                saleOutSheetDetailService.saveOrUpdateAllColumn(detail);
                detailCount++;
            }

            // 汇总单据总利润
            BigDecimal totalProfit = details.stream()
                    .map(item -> NumberUtil.getDefaultValue(item.getTotalProfit()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 成本重算同时修正单据头验收汇总，保证与明细已保存的验收数据一致。
            BigDecimal confirmNum = sumDetailAmount(details, SaleOutSheetDetail::getConfirmNum);
            BigDecimal confirmAmt = sumDetailAmount(details, SaleOutSheetDetail::getConfirmAmt);

            LambdaUpdateWrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                    .set(SaleOutSheet::getTotalCost, totalCostAmount)
                    .set(SaleOutSheet::getTotalProfit, totalProfit)
                    .set(SaleOutSheet::getConfirmNum, confirmNum)
                    .set(SaleOutSheet::getConfirmAmt, confirmAmt)
                    .set(SaleOutSheet::getFillAllCost, fillAllCost)
                    .eq(SaleOutSheet::getId, sheet.getId());
            this.update(updateWrapper);
        }

        log.info("成本更新完成, 单据数: {}, 明细数: {}, 未填充数: {}",
                sheets.size(), detailCount, notFilledCount);
        return new MonthEndRecalculateResult(sheets.size(), detailCount, notFilledCount);
    }

    /**
     * 清理过期的缓存任务
     */
    private void cleanExpiredTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TASK_EXPIRE_MINUTES);
        recalculateTaskCache.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(threshold));
    }

    /**
     * 查询时间段内销售单
     * @param scId
     * @param beginDate
     * @param endDate
     * @return
     */
    private List<SaleOutSheet> querySaleOutSheets(String scId, LocalDate beginDate, LocalDate endDate) {
        LambdaQueryWrapper<SaleOutSheet> queryWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
                .ge(SaleOutSheet::getOrderDate, beginDate)
                .le(SaleOutSheet::getOrderDate, endDate);
        if (StringUtils.isNotBlank(scId)) {
            queryWrapper.eq(SaleOutSheet::getScId, scId);
        }
        List<SaleOutSheet> sheets = getBaseMapper().selectList(queryWrapper);
        return sheets;
    }

    /**
     *
     * @param latestCostPrices
     * @return
     */
    private Map<String, QueryReceiveSheetDetailDto> toCostPriceMap(
            List<QueryReceiveSheetDetailDto> latestCostPrices) {
        if (CollectionUtils.isEmpty(latestCostPrices)) {
            return new HashMap<>();
        }

        return latestCostPrices.stream().collect(Collectors.toMap(QueryReceiveSheetDetailDto::getProductId,
                item -> item, (v1, v2) -> v1, HashMap::new));
    }

    /**
     * 是否优先使用库存表成本价
     * 
     * @return
     */
    private boolean useStockPriceAsLatestCostPrice() {
        QuerySysParameterVo sysParameterVo = new QuerySysParameterVo();
        sysParameterVo.setPmKey(COST_PRICE_SOURCE_USE_STOCK_PRICE_PM_KEY);
        List<SysParameter> list = sysParameterService.query(sysParameterVo);
        if (CollectionUtil.isEmpty(list)) {
            return false;
        }

        return BooleanUtil.toBoolean(list.get(0).getPmValue());
    }

    /**
     * 获取商品售价
     * @param product
     * @return
     */
    private BigDecimal getDefaultSalePrice(Product product) {
        if (useUniquePriceAsSalePrice()) {
            return product.getSalePrice() == null ? BigDecimal.ZERO : product.getSalePrice();
        }
        BigDecimal latestSalePrice = productLatestPriceCacheService.getLatestSalePrice(product.getId());
        return latestSalePrice == null ? BigDecimal.ZERO : latestSalePrice;
    }

    /**
     * 是否优先使用询价作为售价
     * 
     * @return
     */
    private boolean useUniquePriceAsSalePrice() {
        QuerySysParameterVo sysParameterVo = new QuerySysParameterVo();
        sysParameterVo.setPmKey(PRODUCT_SALE_PRICE_UNIQUE_PM_KEY);
        List<SysParameter> list = sysParameterService.query(sysParameterVo);
        if (CollectionUtil.isEmpty(list)) {
            return false;
        }

        return BooleanUtil.toBoolean(list.get(0).getPmValue());
    }
}
