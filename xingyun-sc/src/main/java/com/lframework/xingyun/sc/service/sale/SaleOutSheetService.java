package com.lframework.xingyun.sc.service.sale;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.excel.sale.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.vo.sale.out.*;
import javax.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleOutSheetService extends BaseMpService<SaleOutSheet> {

  /**
   * 查询列表
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<SaleOutSheet> query(Integer pageIndex, Integer pageSize, QuerySaleOutSheetVo vo);

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  List<SaleOutSheet> query(QuerySaleOutSheetVo vo);

  PageResult<QuerySaleOutSheetDetailDto> queryDetail(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetVo vo);

  /**
   * 选择器
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<SaleOutSheet> selector(Integer pageIndex, Integer pageSize, SaleOutSheetSelectorVo vo);

  /**
   * 根据客户ID查询默认付款日期
   *
   * @param customerId
   */
  GetPaymentDateDto getPaymentDate(String customerId);

  /**
   * 根据ID查询
   *
   * @param id
   * @return
   */
  SaleOutSheetFullDto getDetail(String id);

  /**
   * 根据ID查询（销售退货业务）
   *
   * @param id
   * @return
   */
  SaleOutSheetWithReturnDto getWithReturn(String id);

  /**
   * 查询列表（销售退货业务）
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<SaleOutSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetWithReturnVo vo);

  /**
   * 创建
   *
   * @param vo
   * @return
   */
  String create(CreateSaleOutSheetVo vo);

  /**
   * 修改
   *
   * @param vo
   */
  void update(UpdateSaleOutSheetVo vo);

  /**
   * 审核通过
   *
   * @param vo
   */
  void approvePass(ApprovePassSaleOutSheetVo vo);

  /**
   * 直接审核通过
   *
   * @param vo
   */
  String directApprovePass(CreateSaleOutSheetVo vo);

  /**
   * 审核拒绝
   *
   * @param vo
   */
  void approveRefuse(ApproveRefuseSaleOutSheetVo vo);

  /**
   * 根据ID删除
   *
   * @param id
   */
  void deleteById(String id);

  /**
   * 设置成未结算
   *
   * @param id
   * @return
   */
  int setUnSettle(String id);

  /**
   * 设置成结算中
   *
   * @param id
   * @return
   */
  int setPartSettle(String id);

  /**
   * 设置成已结算
   *
   * @param id
   * @return
   */
  int setSettled(String id);

  /**
   * 查询已审核列表
   *
   * @param customerId
   * @param startTime
   * @param endTime
   * @return
   */
  List<SaleOutSheet> getApprovedList(String customerId, LocalDateTime startTime,
      LocalDateTime endTime,
      SettleStatus settleStatus);

  List<SaleOutProductVo> checkImport(List<SaleOutSheetImportModel> list);

  List<PrintSaleTagBo> tagPrint(QuerySaleOutSheetVo vo);

  void marketBuySummary(QuerySaleOutSheetVo vo);

  void exportSales(QuerySaleOutSheetVo vo, HttpServletResponse response);

  /**
   * 刷新成本(当天成本价）
   * @param orderDate
   */
  void refreshCostPrice(LocalDate orderDate);

  /**
   * 按销售单刷新成本
   * @param orderId
   */
  void refreshCostPrice(String orderId);

  /**
   * 从销售出库查询页面导入并创建订单
   *
   * @param list 导入数据
   * @return 创建的订单ID列表
   */
  List<String> importByQuery(List<SaleOutSheetQueryImportModel> list);
}
