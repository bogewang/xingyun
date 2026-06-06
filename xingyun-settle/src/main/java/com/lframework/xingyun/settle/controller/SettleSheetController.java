package com.lframework.xingyun.settle.controller;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.mq.core.utils.ExportTaskUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.QueryReceiveSheetVo;
import com.lframework.xingyun.settle.bo.sheet.GetSettleSheetBo;
import com.lframework.xingyun.settle.bo.sheet.QuerySettleSheetBo;
import com.lframework.xingyun.settle.bo.sheet.ReceiveSheetSettleInfoBo;
import com.lframework.xingyun.settle.bo.sheet.SettleSheetSummaryBo;
import com.lframework.xingyun.settle.excel.sheet.ReceiveSheetSettleInfoExportTaskWorker;
import com.lframework.xingyun.settle.dto.sheet.SettleSheetFullDto;
import com.lframework.xingyun.settle.entity.SettleSheet;
import com.lframework.xingyun.settle.excel.sheet.SettleSheetExportTaskWorker;
import com.lframework.xingyun.settle.service.SettleSheetService;
import com.lframework.xingyun.settle.service.SettleSheetSummaryService;
import com.lframework.xingyun.settle.vo.sheet.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商结算单
 *
 * @author zmj
 */
@Api(tags = "供应商结算单")
@Validated
@RestController
@RequestMapping("/settle/sheet")
@Slf4j
public class SettleSheetController extends DefaultBaseController {

    @Autowired
    private SettleSheetService settleSheetService;

    @Autowired
    private SettleSheetSummaryService settleSheetSummaryService;

    @Autowired
    private ReceiveSheetService receiveSheetService;

    /**
     * 供应商结算单列表
     */
    @ApiOperation("供应商结算单列表")
    @HasPermission({"settle:sheet:query"})
    @GetMapping("/query")
    public InvokeResult<PageResult<QuerySettleSheetBo>> query(@Valid QuerySettleSheetVo vo) {

        PageResult<SettleSheet> pageResult = settleSheetService.query(getPageIndex(vo), getPageSize(vo),
                vo);

        List<SettleSheet> datas = pageResult.getDatas();
        List<QuerySettleSheetBo> results = null;

        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(QuerySettleSheetBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 供应商结算汇总
     */
    @ApiOperation("供应商结算汇总")
    @HasPermission({"settle:sheet:query"})
    @GetMapping("/summary")
    public InvokeResult<List<SettleSheetSummaryBo>> summary(@Valid QuerySettleSheetSummaryVo vo) {

        try {
            return InvokeResultBuilder.success(settleSheetSummaryService.query(vo));
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @ApiOperation("查询收货单对账结算扩展信息")
    @HasPermission({"settle:sheet:query"})
    @PostMapping("/receive-sheet-settle-infos")
    public InvokeResult<List<ReceiveSheetSettleInfoBo>> queryReceiveSheetSettleInfos(@RequestBody @Valid QueryReceiveSheetVo vo) {
        try {
            PageResult<ReceiveSheet> pageResult = receiveSheetService.query(getPageIndex(vo), getPageSize(vo), vo);

            List<ReceiveSheetSettleInfoBo> data = settleSheetService.queryReceiveSheetSettleInfos(pageResult.getDatas());
            return InvokeResultBuilder.success(data);
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    /**
     * 导出收货单对账结算扩展信息
     */
    @ApiOperation("导出收货单对账结算扩展信息")
    @HasPermission({"settle:sheet:export"})
    @PostMapping("/export-receive-sheet-settle-infos")
    public InvokeResult<Void> exportReceiveSheetSettleInfos(@Valid QueryReceiveSheetVo vo) {

        ExportTaskUtil.exportTask("供应商结算工作台", ReceiveSheetSettleInfoExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 导出
     */
    @ApiOperation("导出")
    @HasPermission({"settle:sheet:export"})
    @PostMapping("/export")
    public InvokeResult<Void> export(@Valid QuerySettleSheetVo vo) {

        ExportTaskUtil.exportTask("供应商结算单信息", SettleSheetExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 根据ID查询
     */
    @ApiOperation("根据ID查询")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"settle:sheet:query"})
    @GetMapping
    public InvokeResult<GetSettleSheetBo> findById(
            @NotBlank(message = "供应商结算单ID不能为空！") String id) {

        SettleSheetFullDto data = settleSheetService.getDetail(id);

        GetSettleSheetBo result = new GetSettleSheetBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 创建供应商结算单
     */
    @ApiOperation("创建供应商结算单")
    @HasPermission({"settle:sheet:add"})
    @PostMapping
    public InvokeResult<String> create(@RequestBody @Valid CreateSettleSheetVo vo) {

        vo.validate();

        String id = settleSheetService.create(vo);

        return InvokeResultBuilder.success(id);
    }

    /**
     * 修改供应商结算单
     */
    @ApiOperation("修改供应商结算单")
    @HasPermission({"settle:sheet:modify"})
    @PutMapping
    public InvokeResult<Void> update(@RequestBody @Valid UpdateSettleSheetVo vo) {

        vo.validate();

        settleSheetService.update(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 审核通过供应商结算单
     */
    @ApiOperation("审核通过供应商结算单")
    @HasPermission({"settle:sheet:approve"})
    @PatchMapping("/approve/pass")
    public InvokeResult<Void> approvePass(@RequestBody @Valid ApprovePassSettleSheetVo vo) {

        settleSheetService.approvePass(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 直接审核通过供应商结算单
     */
    @ApiOperation("直接审核通过供应商结算单")
    @HasPermission({"settle:sheet:approve"})
    @PostMapping("/approve/pass/direct")
    public InvokeResult<Void> directApprovePass(@RequestBody @Valid CreateSettleSheetVo vo) {
        try {
            vo.validate();

            settleSheetService.directApprovePass(vo);

            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    /**
     * 审核拒绝供应商结算单
     */
    @ApiOperation("审核拒绝供应商结算单")
    @HasPermission({"settle:sheet:approve"})
    @PatchMapping("/approve/refuse")
    public InvokeResult<Void> approveRefuse(@RequestBody @Valid ApproveRefuseSettleSheetVo vo) {

        settleSheetService.approveRefuse(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 删除供应商结算单
     */
    @ApiOperation("删除供应商结算单")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"settle:sheet:delete"})
    @DeleteMapping
    public InvokeResult<Void> deleteById(@NotBlank(message = "供应商结算单ID不能为空！") String id) {

        settleSheetService.deleteById(id);

        return InvokeResultBuilder.success();
    }

}
