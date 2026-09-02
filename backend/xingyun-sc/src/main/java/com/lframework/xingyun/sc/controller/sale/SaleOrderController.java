package com.lframework.xingyun.sc.controller.sale;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.mq.core.utils.ExportTaskUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.sc.bo.sale.*;
import com.lframework.xingyun.sc.converter.SaleOrderConverter;
import com.lframework.xingyun.sc.converter.SaleOutSheetConverter;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
import com.lframework.xingyun.sc.dto.sale.SaleOrderWithOutDto;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.excel.sale.SaleOrderDetailExportTaskWorker;
import com.lframework.xingyun.sc.excel.sale.SaleOrderExportTaskWorker;
import com.lframework.xingyun.sc.excel.sale.SaleOrderImportModel;
import com.lframework.xingyun.sc.service.ProductHotnessService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.github.pagehelper.PageInfo;
import com.lframework.xingyun.sc.vo.sale.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * 销售订单管理
 *
 * @author zmj
 */
@Slf4j
@Api(tags = "销售订单管理")
@Validated
@RestController
@RequestMapping("/sale/order")
public class SaleOrderController extends DefaultBaseController {

    @Autowired
    private SaleOrderService saleOrderService;

    @Resource
    private StoreCenterService storeCenterService;

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    /**
     * 打印
     */
    @ApiOperation("打印")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:order:query" })
    @GetMapping("/print")
    public InvokeResult<PrintSaleOrderBo> print(@NotBlank(message = "订单ID不能为空！") String id) {

        SaleOrderFullDto data = saleOrderService.getDetail(id);
        if (data == null) {
            throw new DefaultClientException("订单不存在！");
        }

        PrintSaleOrderBo res = SaleOrderConverter.fullDTO2PrintBO(data);

        return InvokeResultBuilder.success(res);
    }

    /**
     * 订单列表
     */
    @ApiOperation("订单列表")
    @HasPermission({ "sale:order:query" })
    @GetMapping("/query")
    public InvokeResult<PageResult<QuerySaleOrderBo>> query(@Valid QuerySaleOrderVo vo) {

        PageResult<SaleOrder> pageResult = saleOrderService.query(getPageIndex(vo), getPageSize(vo),
                vo);

        List<SaleOrder> datas = pageResult.getDatas();
        List<QuerySaleOrderBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(QuerySaleOrderBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 标签打印
     */
    @ApiOperation("标签打印")
    @HasPermission({ "sale:order:query" })
    @GetMapping("/tagPrint")
    public InvokeResult<List<PrintSaleTagBo>> tagPrint(@Valid QuerySaleOrderVo vo) {
        List<PrintSaleTagBo> data = saleOrderService.tagPrint(vo);
        return InvokeResultBuilder.success(data);
    }

    /**
     * 导出
     */
    @ApiOperation("导出")
    @HasPermission({ "sale:order:export" })
    @PostMapping("/export")
    public InvokeResult<Void> export(@Valid QuerySaleOrderVo vo) {

        ExportTaskUtil.exportTask("销售单信息", SaleOrderExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 明细导出
     */
    @ApiOperation("明细导出")
    @HasPermission({ "sale:order:export" })
    @PostMapping("/exportDetail")
    public InvokeResult<Void> exportDetail(@RequestBody @Valid QuerySaleOrderVo vo) {

        ExportTaskUtil.exportTask("销售单明细", SaleOrderDetailExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 根据ID查询
     */
    @ApiOperation("根据ID查询")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:order:query" })
    @GetMapping
    public InvokeResult<GetSaleOrderBo> findById(@NotBlank(message = "订单ID不能为空！") String id) {

        SaleOrderFullDto data = saleOrderService.getDetail(id);

        GetSaleOrderBo result = new GetSaleOrderBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 根据ID查询（出库业务）
     */
    @ApiOperation("根据ID查询（出库业务）")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:out:add", "sale:out:modify" })
    @GetMapping("/out")
    public InvokeResult<SaleOrderWithOutBo> getWithOut(
            @NotBlank(message = "订单ID不能为空！") String id) {

        SaleOrderWithOutDto data = saleOrderService.getWithOut(id);
        SaleOrderWithOutBo result = new SaleOrderWithOutBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 查询列表（出库业务）
     */
    @ApiOperation("查询列表（出库业务）")
    @HasPermission({ "sale:out:add", "sale:out:modify" })
    @GetMapping("/query/out")
    public InvokeResult<PageResult<QuerySaleOrderWithOutBo>> queryWithOut(
            @Valid QuerySaleOrderWithOutVo vo) {

        PageResult<SaleOrder> pageResult = saleOrderService.queryWithOut(getPageIndex(vo),
                getPageSize(vo), vo);
        List<SaleOrder> datas = pageResult.getDatas();

        List<QuerySaleOrderWithOutBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(QuerySaleOrderWithOutBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 加载列表（出库业务）
     */
    @ApiOperation("加载列表（出库业务）")
    @HasPermission({ "sale:out:add", "sale:out:modify" })
    @PostMapping("/query/out/load")
    public InvokeResult<List<QuerySaleOrderWithOutBo>> getWithOut(
            @RequestBody(required = false) List<String> ids) {

        List<SaleOrder> datas = saleOrderService.listByIds(ids);

        List<QuerySaleOrderWithOutBo> results = datas.stream()
                .map(QuerySaleOrderWithOutBo::new)
                .collect(Collectors.toList());

        return InvokeResultBuilder.success(results);
    }

    /**
     * 创建订单
     */
    @ApiOperation("创建订单")
    @HasPermission({ "sale:order:add" })
    @PostMapping
    public InvokeResult<String> create(@RequestBody @Valid CreateSaleOrderVo vo) {

        vo.validate();

        String id = saleOrderService.create(vo);

        return InvokeResultBuilder.success(id);
    }

    /**
     * 修改订单
     */
    @ApiOperation("修改订单")
    @HasPermission({ "sale:order:modify" })
    @PutMapping
    public InvokeResult<Void> update(@RequestBody @Valid UpdateSaleOrderVo vo) {

        vo.validate();

        saleOrderService.update(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核通过订单
     */
    @ApiOperation("审核通过订单")
    @HasPermission({ "sale:order:approve" })
    @PatchMapping("/approve/pass")
    public InvokeResult<Void> approvePass(@RequestBody @Valid ApprovePassSaleOrderVo vo) {

        saleOrderService.approvePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 直接审核通过订单
     */
    @ApiOperation("直接审核通过订单")
    @HasPermission({ "sale:order:approve" })
    @PostMapping("/approve/pass/direct")
    public InvokeResult<Void> directApprovePass(@RequestBody @Valid CreateSaleOrderVo vo) {

        vo.validate();

        saleOrderService.directApprovePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核拒绝订单
     */
    @ApiOperation("审核拒绝订单")
    @HasPermission({ "sale:order:approve" })
    @PatchMapping("/approve/refuse")
    public InvokeResult<Void> approveRefuse(@RequestBody @Valid ApproveRefuseSaleOrderVo vo) {

        saleOrderService.approveRefuse(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 删除订单
     */
    @ApiOperation("删除订单")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({ "sale:order:delete" })
    @DeleteMapping
    public InvokeResult<Void> deleteById(@NotBlank(message = "订单ID不能为空！") String id) {

        saleOrderService.deleteById(id);

        return InvokeResultBuilder.success();
    }

    /**
     * 根据关键字查询商品
     */
    @ApiOperation("根据关键字查询可销售商品")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "仓库ID", name = "scId", paramType = "query"),
            @ApiImplicitParam(value = "关键字", name = "condition", paramType = "query", required = true),
            @ApiImplicitParam(value = "单据日期，唯一报价模式下用于过滤报价商品", name = "orderDate", paramType = "query") })
    @HasPermission({ "sale:order:add", "sale:order:modify", "sale:out:add", "sale:out:modify",
            "sale:return:add", "sale:return:modify" })
    @GetMapping("/product/search")
    public InvokeResult<List<SaleProductBo>> searchSaleProducts(
            String scId, String condition, Boolean isReturn, String orderDate) {

        if (isReturn == null) {
            isReturn = false;
        }
        if (StringUtil.isBlank(condition)) {
            return InvokeResultBuilder.success(CollectionUtil.emptyList());
        }
        if (StringUtils.isBlank(scId)) {
            scId = storeCenterService.getDefaultStoreId();
        }

        PageResult<SaleProductDto> pageResult = saleOrderService.querySaleByCondition(getPageIndex(),
                1000, scId, condition, isReturn);
        List<SaleProductDto> datas = pageResult.getDatas();
        if (CollectionUtil.isNotEmpty(datas)) {
            // 唯一报价模式下按单据日期过滤为报价商品，并用报价价覆盖售价
            List<SaleProductDto> quoteFiltered = saleOutSheetService.applyQuoteFilter(datas, orderDate);
            if (quoteFiltered != null) {
                datas = quoteFiltered;
            }
            if (CollectionUtil.isNotEmpty(datas)) {
                return InvokeResultBuilder.success(SaleOutSheetConverter.saleOutProductDto2Bos(scId, datas));
            }
        }

        return InvokeResultBuilder.success(CollectionUtil.emptyList());
    }

    /**
     * 查询商品列表
     */
    @ApiOperation("查询可销售商品列表")
    @HasPermission({ "sale:order:add", "sale:order:modify", "sale:out:add", "sale:out:modify",
            "sale:return:add", "sale:return:modify" })
    @GetMapping("/product/list")
    public InvokeResult<PageResult<SaleProductBo>> querySaleProductList(@Valid QuerySaleProductVo vo) {
        if (StringUtils.isBlank(vo.getScId())) {
            vo.setScId(storeCenterService.getDefaultStoreId());
        }
        PageResult<SaleProductDto> pageResult = saleOrderService.querySaleList(getPageIndex(vo),
                getPageSize(vo), vo);

        if (CollectionUtil.isEmpty(pageResult.getDatas())) {
            return InvokeResultBuilder.success(new PageResult<>());
        }

        // 唯一报价模式下按单据日期过滤为报价商品，并用报价价覆盖售价
        List<SaleProductDto> quoteFiltered = saleOutSheetService.applyQuoteFilter(pageResult.getDatas(),
                vo.getOrderDate());
        if (quoteFiltered != null) {
            pageResult = PageResultUtil.convert(new PageInfo<>(quoteFiltered));
        }

        List<SaleProductBo> results = pageResult.getDatas().stream().map(
                t -> new SaleProductBo(vo.getScId(), t))
                    .collect(Collectors.toList());

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    @ApiOperation("下载导入模板")
    @HasPermission({ "sale:order:import" })
    @GetMapping("/import/template")
    public void downloadImportTemplate() {
        ExcelUtil.export("销售单导入模板", SaleOrderImportModel.class);
    }

    /**
     * 返回前端, 前端处理后再保存
     *
     * @param file
     * @return
     */
    @ApiOperation("导入")
    @HasPermission({ "sale:order:import" })
    @PostMapping("/import")
    public InvokeResult<List<SaleProductVo>> importExcel(@NotNull(message = "请上传文件") MultipartFile file,
                                                          @RequestParam @NotNull(message = "请先选择订单日期！") LocalDate orderDate) {
        try {

            List<SaleOrderImportModel> list = EasyExcelUtils.syncReadModel(file.getInputStream(),
                    SaleOrderImportModel.class);
            List<SaleProductVo> data = saleOrderService.checkImport(list, orderDate);

            return InvokeResultBuilder.success(data);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }
}
