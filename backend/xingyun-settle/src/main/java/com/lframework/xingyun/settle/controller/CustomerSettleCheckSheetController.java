package com.lframework.xingyun.settle.controller;

import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.xingyun.settle.service.CustomerSettleCheckSheetService;
import com.lframework.xingyun.settle.vo.check.customer.CreateCustomerSettleCheckSheetVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户对账单。
 *
 * @author zmj
 */
@Api(tags = "客户对账单")
@Validated
@RestController
@RequestMapping("/customer/settle/checksheet")
@Slf4j
public class CustomerSettleCheckSheetController extends DefaultBaseController {

  @Autowired
  private CustomerSettleCheckSheetService customerSettleCheckSheetService;

  /**
   * 直接确认客户对账单。
   *
   * @param vo 客户对账确认请求
   * @return 对账单ID
   */
  @ApiOperation("直接确认客户对账单")
  @HasPermission({"customer-settle:sheet:approve"})
  @PostMapping("/approve/pass/direct")
  public InvokeResult<String> directApprovePass(
      @RequestBody @Valid CreateCustomerSettleCheckSheetVo vo) {
    try {
      String id = customerSettleCheckSheetService.directApprovePass(vo);
      return InvokeResultBuilder.success(id);
    } catch (Exception e) {
      log.error("客户对账确认失败", e);
      return InvokeResultBuilder.fail(e.getMessage(), null);
    }
  }
}
