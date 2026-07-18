package com.lframework.xingyun.sc.controller.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.mq.core.utils.ExportTaskUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.bo.purchase.receive.GetPaymentDateBo;
import com.lframework.xingyun.sc.bo.sale.PrintSaleOrderBo;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.bo.sale.out.GetSaleOutSheetBo;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetDetailBo;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetBo;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetWithReturnBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitTrendBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitTrendBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetWithReturnBo;
import com.lframework.xingyun.sc.converter.SaleOutSheetConverter;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetDetailExportTaskWorker;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetExportTaskWorker;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetProductProfitExportTaskWorker;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetProfitExportTaskWorker;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售出库单管理
 *
 * @author zmj
 */
@Slf4j
@Api(tags = "销售出库单管理")
@Validated
@RestController
@RequestMapping("/sale/out/sheet")
public class SaleOutSheetController extends DefaultBaseController {

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    /**
     * 打印
     */
    @ApiOperation("打印")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:out:query" })
    @GetMapping("/print")
    public InvokeResult<PrintSaleOrderBo> print(
            @NotBlank(message = "订单ID不能为空！") String id) {

        SaleOutSheetFullDto data = saleOutSheetService.getDetail(id);
        if (data == null) {
            throw new DefaultClientException("销售出库单不存在！");
        }

        PrintSaleOrderBo result = SaleOutSheetConverter.fullDTO2PrintBO(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 订单列表
     */
    @ApiOperation("订单列表")
    @HasPermission({ "sale:out:query" })
    @GetMapping("/query")
    public InvokeResult<PageResult<QuerySaleOutSheetBo>> query(@Valid QuerySaleOutSheetVo vo) {

        PageResult<SaleOutSheet> pageResult = saleOutSheetService.query(getPageIndex(vo),
                getPageSize(vo), vo);

        List<QuerySaleOutSheetBo> results = null;
        if (!CollectionUtil.isEmpty(pageResult.getDatas())) {
            results = pageResult.getDatas().stream().map(QuerySaleOutSheetBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    @ApiOperation("订单明细列表")
    @HasPermission({ "sale:out:query" })
    @GetMapping("/query/detail")
    public InvokeResult<PageResult<QuerySaleOutSheetDetailBo>> queryDetail(@Valid QuerySaleOutSheetVo vo) {

        PageResult<QuerySaleOutSheetDetailDto> pageResult = saleOutSheetService.queryDetail(
                getPageIndex(vo), getPageSize(vo), vo);

        List<QuerySaleOutSheetDetailBo> results = null;
        if (!CollectionUtil.isEmpty(pageResult.getDatas())) {
            results = pageResult.getDatas().stream().map(QuerySaleOutSheetDetailBo::new)
                    .collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    @ApiOperation("查询产品询价不唯一的销售明细")
    @HasPermission({ "sale:out:query" })
    @GetMapping("/query/priceCheck")
    public InvokeResult<PageResult<QuerySaleOutSheetDetailBo>> queryPriceCheckDetail(@Valid QuerySaleOutSheetVo vo) {
        try {
            PageResult<QuerySaleOutSheetDetailDto> pageResult = saleOutSheetService.queryPriceCheckDetail(
                    getPageIndex(vo), getPageSize(vo), vo);

            if (CollectionUtil.isEmpty(pageResult.getDatas())) {
                return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, null));
            }

            List<QuerySaleOutSheetDetailBo> results = pageResult.getDatas().stream().map(QuerySaleOutSheetDetailBo::new)
                        .collect(Collectors.toList());
            return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("是否开启产品询价唯一性检查")
    @HasPermission({ "sale:out:query" })
    @GetMapping("/price/unique/config")
    public InvokeResult<Boolean> getPriceUniqueConfig() {

        try {
            return InvokeResultBuilder.success(saleOutSheetService.getPriceUniqueConfig());
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售利润列表")
    @HasPermission({ "report:sale-profit:query" })
    @GetMapping("/profit/query")
    public InvokeResult<PageResult<QuerySaleOutSheetBo>> queryProfit(@Valid QuerySaleOutSheetVo vo) {

        try {
            PageResult<SaleOutSheet> pageResult = saleOutSheetService.query(getPageIndex(vo),
                    getPageSize(vo), vo);

            List<SaleOutSheet> datas = pageResult.getDatas();
            List<QuerySaleOutSheetBo> results = null;

            if (!CollectionUtil.isEmpty(datas)) {
                results = datas.stream().map(QuerySaleOutSheetBo::new).collect(Collectors.toList());
            }

            return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售利润汇总")
    @HasPermission({ "report:sale-profit:query" })
    @GetMapping("/profit/summary")
    public InvokeResult<SaleOutSheetProfitSummaryBo> queryProfitSummary(
            @Valid QuerySaleOutSheetVo vo) {

        try {
            SaleOutSheetProfitSummaryBo result = saleOutSheetService.queryProfitSummary(vo);

            return InvokeResultBuilder.success(result);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售利润（按商品）列表")
    @HasPermission({ "report:sale-profit:product:query" })
    @GetMapping("/profit/product/query")
    public InvokeResult<PageResult<SaleOutSheetProductProfitBo>> queryProductProfit(
            @Valid QuerySaleOutSheetVo vo) {

        try {
            PageResult<SaleOutSheetProductProfitDto> pageResult = saleOutSheetService
                    .queryProductProfit(getPageIndex(vo), getPageSize(vo), vo);

            List<SaleOutSheetProductProfitBo> results = null;
            if (!CollectionUtil.isEmpty(pageResult.getDatas())) {
                results = pageResult.getDatas().stream().map(SaleOutSheetProductProfitBo::new)
                        .collect(Collectors.toList());
            }

            return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售利润（按商品）汇总")
    @HasPermission({ "report:sale-profit:product:query" })
    @GetMapping("/profit/product/summary")
    public InvokeResult<SaleOutSheetProductProfitSummaryBo> queryProductProfitSummary(
            @Valid QuerySaleOutSheetVo vo) {

        try {
            SaleOutSheetProductProfitSummaryBo result = saleOutSheetService
                    .queryProductProfitSummary(vo);

            return InvokeResultBuilder.success(result);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售趋势")
    @HasPermission({ "report:sale-trend:query" })
    @GetMapping("/profit/trend")
    public InvokeResult<List<SaleOutSheetProfitTrendBo>> queryProfitTrend(
            @Valid QuerySaleOutSheetVo vo) {

        try {
            List<SaleOutSheetProfitTrendDto> datas = saleOutSheetService.queryProfitTrend(vo);

            List<SaleOutSheetProfitTrendBo> results = null;
            if (!CollectionUtil.isEmpty(datas)) {
                results = datas.stream().map(SaleOutSheetProfitTrendBo::new)
                        .collect(Collectors.toList());
            }

            return InvokeResultBuilder.success(results);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("销售利润（按商品）趋势")
    @HasPermission({ "report:sale-profit:product:query" })
    @GetMapping("/profit/product/trend")
    public InvokeResult<List<SaleOutSheetProductProfitTrendBo>> queryProductProfitTrend(
            @Valid QuerySaleOutSheetVo vo) {

        try {
            List<SaleOutSheetProductProfitTrendDto> datas = saleOutSheetService
                    .queryProductProfitTrend(vo);

            List<SaleOutSheetProductProfitTrendBo> results = null;
            if (!CollectionUtil.isEmpty(datas)) {
                results = datas.stream().map(SaleOutSheetProductProfitTrendBo::new)
                        .collect(Collectors.toList());
            }

            return InvokeResultBuilder.success(results);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    /**
     * 标签打印
     */
    @ApiOperation("标签打印")
    @HasPermission({ "sale:out:query" })
    @PostMapping("/tagPrint")
    public InvokeResult<List<PrintSaleTagBo>> tagPrint(@RequestBody @Valid QuerySaleOutSheetVo vo) {

        List<PrintSaleTagBo> data = saleOutSheetService.tagPrint(vo);

        return InvokeResultBuilder.success(data);
    }

    /**
     * 买菜汇总导出
     */
    @ApiOperation("买菜汇总导出")
    @HasPermission({ "sale:out:query" })
    @GetMapping("/export/marketBuySummary")
    public void exportMarketBuySummary(@Valid QuerySaleOutSheetVo vo) {
        try {
            saleOutSheetService.marketBuySummary(vo);
        } catch (Exception e) {
            log.error("导出买菜汇总失败", e);
            throw new DefaultClientException(e.getMessage());
        }
    }

    /**
     * 导出
     */
    @ApiOperation("导出")
    @HasPermission({ "sale:out:export" })
    @PostMapping("/export")
    public InvokeResult<Void> export(@Valid QuerySaleOutSheetVo vo) {

        ExportTaskUtil.exportTask("销售出库单信息", SaleOutSheetExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 明细导出
     */
    @ApiOperation("明细导出")
    @HasPermission({ "sale:out:export" })
    @PostMapping("/exportDetail")
    public InvokeResult<Void> exportDetail(@RequestBody @Valid QuerySaleOutSheetVo vo) {

        ExportTaskUtil.exportTask("销售出库单明细", SaleOutSheetDetailExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 按天汇总导出明细
     */
    @ApiOperation("按天汇总导出明细")
    @HasPermission({ "sale:out:export" })
    @PostMapping("/exportDetail/dailySummary")
    public void exportDetailDailySummary(@RequestBody @Valid QuerySaleOutSheetVo vo) {
        try {
            saleOutSheetService.exportDetailDailySummary(vo);
        } catch (DefaultClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("按天汇总导出销售出库明细失败", e);
            throw new DefaultClientException(e.getMessage());
        }
    }

    /**
     * 销售利润（按单据）导出
     */
    @ApiOperation("销售利润（按单据）导出")
    @HasPermission({ "report:sale-profit:export" })
    @PostMapping("/profit/export")
    public InvokeResult<Void> exportProfit(@Valid QuerySaleOutSheetVo vo) {

        ExportTaskUtil.exportTask("销售利润（按单据）", SaleOutSheetProfitExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 销售利润（按商品）导出
     */
    @ApiOperation("销售利润（按商品）导出")
    @HasPermission({ "report:sale-profit:product:export" })
    @PostMapping("/profit/product/export")
    public InvokeResult<Void> exportProductProfit(@Valid QuerySaleOutSheetVo vo) {

        ExportTaskUtil.exportTask("销售利润（按商品）", SaleOutSheetProductProfitExportTaskWorker.class,
                vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 文山销售单导出
     */
    @ApiOperation("文山销售单导出")
    @HasPermission({ "wenshan:sale:out:saleexport" })
    @PostMapping("/export/sales")
    public void exportSales(@RequestBody @Valid QuerySaleOutSheetVo vo,
                            HttpServletResponse response) {
        try {
            saleOutSheetService.exportSales(vo, response);
        } catch (DefaultClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("销售导出失败", e);
            throw new DefaultClientException(e.getMessage());
        }
    }

    /**
     * 根据ID查询
     */
    @ApiOperation("根据ID查询")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:out:query" })
    @GetMapping
    public InvokeResult<GetSaleOutSheetBo> findById(
            @NotBlank(message = "订单ID不能为空！") String id) {

        SaleOutSheetFullDto data = saleOutSheetService.getDetail(id);

        GetSaleOutSheetBo result = new GetSaleOutSheetBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 根据客户ID查询默认付款日期
     */
    @ApiOperation("根据客户ID查询默认付款日期")
    @ApiImplicitParam(value = "客户ID", name = "customerId", paramType = "query", required = true)
    @HasPermission({ "sale:out:add", "sale:out:modify" })
    @GetMapping("/paymentdate")
    public InvokeResult<GetPaymentDateBo> getPaymentDate(
            @NotBlank(message = "客户ID不能为空！") String customerId) {

        GetPaymentDateDto data = saleOutSheetService.getPaymentDate(customerId);

        GetPaymentDateBo result = new GetPaymentDateBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 根据ID查询（销售退货业务）
     */
    @ApiOperation("根据ID查询（销售退货业务）")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:return:add", "sale:return:modify" })
    @GetMapping("/return")
    public InvokeResult<SaleOutSheetWithReturnBo> getWithReturn(
            @NotBlank(message = "出库单ID不能为空！") String id) {

        SaleOutSheetWithReturnDto data = saleOutSheetService.getWithReturn(id);
        SaleOutSheetWithReturnBo result = new SaleOutSheetWithReturnBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 查询列表（销售退货业务）
     */
    @ApiOperation("查询列表（销售退货业务）")
    @HasPermission({ "sale:return:add", "sale:return:modify" })
    @GetMapping("/query/return")
    public InvokeResult<PageResult<QuerySaleOutSheetWithReturnBo>> queryWithReturn(
            @Valid QuerySaleOutSheetWithReturnVo vo) {

        PageResult<SaleOutSheet> pageResult = saleOutSheetService.queryWithReturn(getPageIndex(vo),
                getPageSize(vo),
                vo);
        List<SaleOutSheet> datas = pageResult.getDatas();

        List<QuerySaleOutSheetWithReturnBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(QuerySaleOutSheetWithReturnBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 加载列表（销售退货业务）
     */
    @ApiOperation("加载列表（销售退货业务）")
    @HasPermission({ "sale:return:add", "sale:return:modify" })
    @PostMapping("/query/return/load")
    public InvokeResult<List<QuerySaleOutSheetWithReturnBo>> loadWithReturn(
            @RequestBody(required = false) List<String> ids) {

        List<SaleOutSheet> datas = saleOutSheetService.listByIds(ids);

        List<QuerySaleOutSheetWithReturnBo> results = datas.stream()
                .map(QuerySaleOutSheetWithReturnBo::new)
                .collect(Collectors.toList());

        return InvokeResultBuilder.success(results);
    }

    /**
     * 创建
     */
    @ApiOperation("创建")
    @HasPermission({ "sale:out:add" })
    @PostMapping
    public InvokeResult<String> create(@RequestBody @Valid CreateSaleOutSheetVo vo) {

        try {
            vo.validate();

            String id = saleOutSheetService.create(vo);

            return InvokeResultBuilder.success(id);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @HasPermission({ "sale:out:modify" })
    @PutMapping
    public InvokeResult<Void> update(@RequestBody @Valid UpdateSaleOutSheetVo vo) {

        try {
            vo.validate();

            saleOutSheetService.update(vo);

            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 修改备注
     */
    @ApiOperation("修改备注")
    @HasPermission({ "sale:out:modify" })
    @PatchMapping("/description")
    public InvokeResult<Void> updateDescription(@RequestBody @Valid UpdateSaleOutSheetDescriptionVo vo) {
        try {
            saleOutSheetService.updateDescription(vo);

            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 批量调整售价
     */
    @ApiOperation("批量调整售价")
    @HasPermission({ "sale:out:modify" })
    @PatchMapping("/price")
    public InvokeResult<Void> batchUpdatePrice(
            @RequestBody @Valid BatchUpdateSaleOutSheetPriceVo vo) {

        saleOutSheetService.batchUpdatePrice(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核通过
     */
    @ApiOperation("审核通过")
    @HasPermission({ "sale:out:approve" })
    @PatchMapping("/approve/pass")
    public InvokeResult<Void> approvePass(@RequestBody @Valid ApprovePassSaleOutSheetVo vo) {

        saleOutSheetService.approvePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 直接审核通过
     */
    @ApiOperation("直接审核通过")
    @HasPermission({ "sale:out:approve" })
    @PostMapping("/approve/pass/direct")
    public InvokeResult<Void> directApprovePass(@RequestBody @Valid CreateSaleOutSheetVo vo) {

        vo.validate();

        saleOutSheetService.directApprovePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核拒绝
     */
    @ApiOperation("审核拒绝")
    @HasPermission({ "sale:out:approve" })
    @PatchMapping("/approve/refuse")
    public InvokeResult<Void> approveRefuse(@RequestBody @Valid ApproveRefuseSaleOutSheetVo vo) {

        saleOutSheetService.approveRefuse(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:out:delete" })
    @DeleteMapping
    public InvokeResult<Void> deleteById(@NotBlank(message = "销售出库单ID不能为空！") String id) {

        try {
            saleOutSheetService.deleteById(id);

            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("下载导入模板")
    @HasPermission({ "sale:out:add" })
    @GetMapping("/import/template")
    public void downloadImportTemplate() {
        ExcelUtil.export("销售出库单导入模板", SaleOutSheetImportModel.class);
    }

    /**
     * 返回前端, 前端处理后再保存
     * 
     * @param file
     * @return
     */
    @ApiOperation("导入")
    @HasPermission({ "sale:out:add" })
    @PostMapping("/import")
    public InvokeResult<List<SaleOutProductVo>> importExcel(@NotNull(message = "请上传文件") MultipartFile file) {
        try {

            List<SaleOutSheetImportModel> list = EasyExcelUtils.syncReadModel(file.getInputStream(),
                    SaleOutSheetImportModel.class);
            List<SaleOutProductVo> data = saleOutSheetService.checkImport(list);

            return InvokeResultBuilder.success(data);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("下载销售出库查询导入模板")
    @HasPermission({ "sale:out:add" })
    @GetMapping("/import/query/template")
    public void downloadQueryImportTemplate() {
        ExcelUtil.export("销售出库查询导入模板", SaleOutSheetQueryImportModel.class);
    }

    @ApiOperation("销售出库查询页面导入并创建订单")
    @HasPermission({ "sale:out:add" })
    @PostMapping("/import/query")
    public InvokeResult<List<String>> importByQuery(@NotNull(message = "请上传文件") MultipartFile file) {
        try {
            List<SaleOutSheetQueryImportModel> list = EasyExcelUtils.syncReadModel(file.getInputStream(), SaleOutSheetQueryImportModel.class);
            List<String> data = saleOutSheetService.importByQuery(list);

            return InvokeResultBuilder.success(data);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }
}
