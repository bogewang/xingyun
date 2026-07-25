package com.lframework.xingyun.settle.controller;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.mq.core.utils.ExportTaskUtil;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.bo.sheet.customer.GetCustomerSettleSheetBo;
import com.lframework.xingyun.settle.bo.sheet.customer.QueryCustomerSettleSheetBo;
import com.lframework.xingyun.settle.dto.sheet.customer.CustomerSettleSheetFullDto;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.excel.sheet.customer.CustomerSettleSheetExportTaskWorker;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.customer.CreateCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户结算单
 *
 * @author zmj
 */
@Api(tags = "客户结算单")
@Validated
@RestController
@RequestMapping("/customer/settle/sheet")
@Slf4j
public class CustomerSettleSheetController extends DefaultBaseController {

  @Autowired
  private CustomerSettleSheetService customerSettleSheetService;

  /**
   * 查询客户销售业务单据结算工作台信息。
   *
   * @param vo 查询条件
   * @return 工作台分页数据
   */
  @ApiOperation("查询客户销售结算工作台")
  @HasPermission({"customer-settle:sheet:query"})
  @PostMapping("/sale-settle-infos")
  public InvokeResult<PageResult<CustomerSaleSettleInfoBo>> querySaleSettleInfos(
      @RequestBody @Valid QueryCustomerSaleSettleInfoVo vo) {
    try {
      return InvokeResultBuilder.success(customerSettleSheetService.querySaleSettleInfos(vo));
    } catch (Exception e) {
      log.error("查询客户结算工作台失败", e);
      return InvokeResultBuilder.fail(e.getMessage(), null);
    }
  }

  /**
   * 客户结算单列表
   */
  @ApiOperation("客户结算单列表")
  @HasPermission({"customer-settle:sheet:query"})
  @GetMapping("/query")
  public InvokeResult<PageResult<QueryCustomerSettleSheetBo>> query(
      @Valid QueryCustomerSettleSheetVo vo) {

    PageResult<CustomerSettleSheet> pageResult = customerSettleSheetService.query(getPageIndex(vo),
        getPageSize(vo), vo);

    List<CustomerSettleSheet> datas = pageResult.getDatas();
    List<QueryCustomerSettleSheetBo> results = null;

    if (!CollectionUtil.isEmpty(datas)) {
      results = datas.stream().map(QueryCustomerSettleSheetBo::new).collect(Collectors.toList());
    }

    return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
  }

  /**
   * 导出
   */
  @ApiOperation("导出")
  @HasPermission({"customer-settle:sheet:export"})
  @PostMapping("/export")
  public InvokeResult<Void> export(@Valid QueryCustomerSettleSheetVo vo) {

    ExportTaskUtil.exportTask("客户结算单信息", CustomerSettleSheetExportTaskWorker.class, vo);

    return InvokeResultBuilder.success();
  }

  /**
   * 根据ID查询
   */
  @ApiOperation("根据ID查询")
  @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
  @HasPermission({"customer-settle:sheet:query"})
  @GetMapping
  public InvokeResult<GetCustomerSettleSheetBo> findById(
      @NotBlank(message = "客户结算单ID不能为空！") String id) {

    CustomerSettleSheetFullDto data = customerSettleSheetService.getDetail(id);

    GetCustomerSettleSheetBo result = new GetCustomerSettleSheetBo(data);

    return InvokeResultBuilder.success(result);
  }

  /**
   * 直接审核通过客户结算单
   */
  @ApiOperation("直接审核通过客户结算单")
  @HasPermission({"customer-settle:sheet:approve"})
  @PostMapping("/approve/pass/direct")
  public InvokeResult<Void> directApprovePass(@RequestBody @Valid CreateCustomerSettleSheetVo vo) {
    try {
      vo.validate();
      customerSettleSheetService.directApprovePass(vo);
      return InvokeResultBuilder.success();
    } catch (Exception e) {
      log.error("客户直接结算失败", e);
      return InvokeResultBuilder.fail(e.getMessage(), null);
    }
  }
}
