package com.lframework.xingyun.sc.controller.purchase;

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
import com.lframework.xingyun.sc.bo.purchase.*;
import com.lframework.xingyun.sc.converter.PurchaseOrderConverter;
import com.lframework.xingyun.sc.dto.purchase.PurchaseOrderFullDto;
import com.lframework.xingyun.sc.dto.purchase.PurchaseOrderWithReceiveDto;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.entity.PurchaseOrder;
import com.lframework.xingyun.sc.excel.purchase.PurchaseOrderDetailExportTaskWorker;
import com.lframework.xingyun.sc.excel.purchase.PurchaseOrderExportTaskWorker;
import com.lframework.xingyun.sc.excel.purchase.PurchaseOrderImportModel;
import com.lframework.xingyun.sc.service.purchase.PurchaseOrderService;
import com.lframework.xingyun.sc.vo.purchase.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购订单管理
 *
 * @author zmj
 */
@Slf4j
@Api(tags = "采购订单管理")
@Validated
@RestController
@RequestMapping("/purchase/order")
public class PurchaseOrderController extends DefaultBaseController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * 打印
     */
    @ApiOperation("打印")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"purchase:order:query"})
    @GetMapping("/print")
    public InvokeResult<PrintPurchaseOrderBo> print(
            @NotBlank(message = "订单ID不能为空！") String id) {

        PurchaseOrderFullDto data = purchaseOrderService.getDetail(id, false);
        if (data == null) {
            throw new DefaultClientException("订单不存在！");
        }

        PrintPurchaseOrderBo result = new PrintPurchaseOrderBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 订单列表
     */
    @ApiOperation("订单列表")
    @HasPermission({"purchase:order:query"})
    @GetMapping("/query")
    public InvokeResult<PageResult<QueryPurchaseOrderBo>> query(@Valid QueryPurchaseOrderVo vo) {

        PageResult<PurchaseOrder> pageResult = purchaseOrderService.query(getPageIndex(vo),
                getPageSize(vo), vo);

        List<PurchaseOrder> datas = pageResult.getDatas();
        List<QueryPurchaseOrderBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {

            results = datas.stream().map(QueryPurchaseOrderBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 导出
     */
    @ApiOperation("导出")
    @HasPermission({"purchase:order:export"})
    @PostMapping("/export")
    public InvokeResult<Void> export(@Valid QueryPurchaseOrderVo vo) {

        ExportTaskUtil.exportTask("采购单信息", PurchaseOrderExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 明细导出
     */
    @ApiOperation("明细导出")
    @HasPermission({"purchase:order:export"})
    @PostMapping("/exportDetail")
    public InvokeResult<Void> exportDetail(@RequestBody @Valid QueryPurchaseOrderVo vo) {

        ExportTaskUtil.exportTask("采购单明细", PurchaseOrderDetailExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 根据ID查询
     */
    @ApiOperation("根据ID查询")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true),
            @ApiImplicitParam(value = "isForm", name = "是否为表单数据", paramType = "query", defaultValue = "false")
    })
    @HasPermission({"purchase:order:query"})
    @GetMapping
    public InvokeResult<GetPurchaseOrderBo> findById(
            @NotBlank(message = "订单ID不能为空！") String id, Boolean isForm) {

        PurchaseOrderFullDto data = purchaseOrderService.getDetail(id, isForm);

        GetPurchaseOrderBo result = new GetPurchaseOrderBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 根据ID查询（收货业务）
     */
    @ApiOperation("根据ID查询（收货业务）")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"purchase:receive:add", "purchase:receive:modify"})
    @GetMapping("/receive")
    public InvokeResult<PurchaseOrderWithReceiveBo> getWithReceive(
            @NotBlank(message = "订单ID不能为空！") String id) {

        PurchaseOrderWithReceiveDto data = purchaseOrderService.getWithReceive(id);
        PurchaseOrderWithReceiveBo result = new PurchaseOrderWithReceiveBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 查询列表（收货业务）
     */
    @ApiOperation("查询列表（收货业务）")
    @HasPermission({"purchase:receive:add", "purchase:receive:modify"})
    @GetMapping("/query/receive")
    public InvokeResult<PageResult<QueryPurchaseOrderWithReceiveBo>> queryWithReceive(
            @Valid QueryPurchaseOrderWithReceiveVo vo) {

        PageResult<PurchaseOrder> pageResult = purchaseOrderService.queryWithReceive(getPageIndex(vo),
                getPageSize(vo),
                vo);
        List<PurchaseOrder> datas = pageResult.getDatas();

        List<QueryPurchaseOrderWithReceiveBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(QueryPurchaseOrderWithReceiveBo::new)
                    .collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 加载列表（收货业务）
     */
    @ApiOperation("加载列表（收货业务）")
    @HasPermission({"purchase:receive:add", "purchase:receive:modify"})
    @PostMapping("/query/receive/load")
    public InvokeResult<List<QueryPurchaseOrderWithReceiveBo>> loadWithReceive(
            @RequestBody(required = false) List<String> ids) {

        List<PurchaseOrder> datas = purchaseOrderService.listByIds(ids);

        List<QueryPurchaseOrderWithReceiveBo> results = datas.stream()
                .map(QueryPurchaseOrderWithReceiveBo::new)
                .collect(Collectors.toList());

        return InvokeResultBuilder.success(results);
    }

    /**
     * 创建订单
     */
    @ApiOperation("创建订单")
    @HasPermission({"purchase:order:add"})
    @PostMapping
    public InvokeResult<String> create(@RequestBody @Valid CreatePurchaseOrderVo vo) {

        vo.validate();

        String id = purchaseOrderService.create(vo);

        return InvokeResultBuilder.success(id);
    }

    /**
     * 修改订单
     */
    @ApiOperation("修改订单")
    @HasPermission({"purchase:order:modify"})
    @PutMapping
    public InvokeResult<Void> update(@RequestBody @Valid UpdatePurchaseOrderVo vo) {

        vo.validate();

        purchaseOrderService.update(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核通过订单
     */
    @ApiOperation("审核通过订单")
    @HasPermission({"purchase:order:approve"})
    @PatchMapping("/approve/pass")
    public InvokeResult<Void> approvePass(@RequestBody @Valid ApprovePassPurchaseOrderVo vo) {

        purchaseOrderService.approvePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 直接审核通过订单
     */
    @ApiOperation("直接审核通过订单")
    @HasPermission({"purchase:order:approve"})
    @PostMapping("/approve/pass/direct")
    public InvokeResult<Void> directApprovePass(@RequestBody @Valid CreatePurchaseOrderVo vo) {

        vo.validate();

        purchaseOrderService.directApprovePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核拒绝订单
     */
    @ApiOperation("审核拒绝订单")
    @HasPermission({"purchase:order:approve"})
    @PatchMapping("/approve/refuse")
    public InvokeResult<Void> approveRefuse(@RequestBody @Valid ApproveRefusePurchaseOrderVo vo) {

        purchaseOrderService.approveRefuse(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 删除订单
     */
    @ApiOperation("删除订单")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"purchase:order:delete"})
    @DeleteMapping
    public InvokeResult<Void> deleteById(@NotBlank(message = "订单ID不能为空！") String id) {

        purchaseOrderService.deleteById(id);

        return InvokeResultBuilder.success();
    }

    /**
     * 取消审核订单
     */
    @ApiOperation("取消审核订单")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"purchase:order:approve"})
    @PatchMapping("/approve/cancel")
    public InvokeResult<Void> cancelApprovePass(@NotBlank(message = "订单ID不能为空！") String id) {

        purchaseOrderService.cancelApprovePass(id);

        return InvokeResultBuilder.success();
    }

    @ApiOperation("下载导入模板")
    @HasPermission({"purchase:order:import"})
    @GetMapping("/import/template")
    public void downloadImportTemplate() {
        ExcelUtil.export("采购订单导入模板", PurchaseOrderImportModel.class);
    }

    @ApiOperation("导入")
    @HasPermission({"purchase:order:import"})
    @PostMapping("/import")
    public InvokeResult<List<PurchaseOrderImportModel>> importExcel(@NotNull(message = "请上传文件") MultipartFile file) {
        try {
            List<PurchaseOrderImportModel> list = EasyExcelUtils.syncReadModel(file.getInputStream(),
                    PurchaseOrderImportModel.class);
            List<PurchaseOrderImportModel> data = purchaseOrderService.checkImport(list);

            return InvokeResultBuilder.success(data);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    /**
     * 根据关键字查询商品
     */
    @ApiOperation("根据关键字查询可采购商品")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "仓库ID", name = "scId", paramType = "query"),
            @ApiImplicitParam(value = "关键字", name = "condition", paramType = "query", required = true)})
    @HasPermission({"purchase:order:add", "purchase:order:modify", "purchase:receive:add",
            "purchase:receive:modify", "purchase:return:add", "purchase:return:modify"})
    @GetMapping("/product/search")
    public InvokeResult<List<PurchaseProductBo>> searchPurchaseProducts(
            String scId, String condition, Boolean isReturn) {

        if (isReturn == null) {
            isReturn = false;
        }

        if (StringUtil.isBlank(condition)) {
            return InvokeResultBuilder.success(CollectionUtil.emptyList());
        }

        PageResult<PurchaseProductDto> pageResult = purchaseOrderService.queryPurchaseByCondition(
                getPageIndex(), getPageSize(), condition, isReturn);
        List<PurchaseProductDto> datas = pageResult.getDatas();
        if (CollectionUtil.isNotEmpty(datas)) {
            return InvokeResultBuilder.success(PurchaseOrderConverter.purchaseProductDto2Bos(scId, datas));
        }

        return InvokeResultBuilder.success(CollectionUtil.emptyList());
    }

    /**
     * 查询商品列表
     */
    @ApiOperation("查询可采购商品列表")
    @HasPermission({"purchase:order:add", "purchase:order:modify", "purchase:receive:add",
            "purchase:receive:modify", "purchase:return:add", "purchase:return:modify"})
    @GetMapping("/product/list")
    public InvokeResult<PageResult<PurchaseProductBo>> queryPurchaseProductList(
            @Valid QueryPurchaseProductVo vo) {

        PageResult<PurchaseProductDto> pageResult = purchaseOrderService.queryPurchaseList(getPageIndex(vo),
                getPageSize(vo), vo);
        List<PurchaseProductDto> datas = pageResult.getDatas();
        if (CollectionUtils.isEmpty(datas)) {
            return InvokeResultBuilder.success(new PageResult<>());
        }

        List<PurchaseProductBo> results = datas.stream().map(t -> new PurchaseProductBo(vo.getScId(), t))
                .collect(Collectors.toList());

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }
}
