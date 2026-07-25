package com.lframework.xingyun.sc.service.sale;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitSummaryBo;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProfitTrendDto;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateStartResult;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateStepResult;
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

  /**
   * 查询销售利润汇总
   *
   * @param vo
   * @return
   */
  SaleOutSheetProfitSummaryBo queryProfitSummary(QuerySaleOutSheetVo vo);

  /**
   * 查询销售利润（按商品）汇总
   *
   * @param vo
   * @return
   */
  SaleOutSheetProductProfitSummaryBo queryProductProfitSummary(QuerySaleOutSheetVo vo);

  PageResult<QuerySaleOutSheetDetailDto> queryDetail(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetVo vo);

  /**
   * 查询产品询价不唯一的销售明细
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<QuerySaleOutSheetDetailDto> queryPriceCheckDetail(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetVo vo);

  PageResult<SaleOutSheetProductProfitDto> queryProductProfit(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetVo vo);

  List<SaleOutSheetProfitTrendDto> queryProfitTrend(QuerySaleOutSheetVo vo);

  List<SaleOutSheetProductProfitTrendDto> queryProductProfitTrend(QuerySaleOutSheetVo vo);

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
   * 修改备注
   *
   * @param vo
   */
  void updateDescription(UpdateSaleOutSheetDescriptionVo vo);

  /**
   * 批量调整销售出库明细售价
   *
   * @param vo
   */
  void batchUpdatePrice(BatchUpdateSaleOutSheetPriceVo vo);

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
   * 按源单结算状态与更新时间乐观锁设置为部分结算。
   *
   * @param id 单据ID
   * @param settleStatus 提交时读取到的结算状态
   * @param updateTime 提交时读取到的更新时间
   * @return 受影响行数
   */
  int setPartSettle(String id, SettleStatus settleStatus, LocalDateTime updateTime);

  /**
   * 设置成已结算
   *
   * @param id
   * @return
   */
  int setSettled(String id);

  /**
   * 按源单结算状态与更新时间乐观锁设置为已结算。
   *
   * @param id 单据ID
   * @param settleStatus 提交时读取到的结算状态
   * @param updateTime 提交时读取到的更新时间
   * @return 受影响行数
   */
  int setSettled(String id, SettleStatus settleStatus, LocalDateTime updateTime);

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

  /**
   * 是否开启产品询价唯一性检查
   *
   * @return
   */
  Boolean getPriceUniqueConfig();

  void marketBuySummary(QuerySaleOutSheetVo vo);

  /**
   * 导出买菜汇总2。
   *
   * @param vo 查询参数
   */
  void marketBuySummary2(QuerySaleOutSheetVo vo);

  void exportDetailDailySummary(QuerySaleOutSheetVo vo);

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
   * 月底成本重算 - 使用月加权平均法
   *
   * @param vo 重算参数（beginDate, endDate, scId）
   * @return 重算结果
   */
  MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo);

  /**
   * 月底成本重算（启动）—— 计算并缓存全范围月加权均价
   *
   * @param vo 启动参数（calcBeginDate, calcEndDate, scId）
   * @return 任务ID及总天数
   */
  MonthEndRecalculateStartResult startMonthEndRecalculate(MonthEndRecalculateStartVo vo);

  /**
   * 月底成本重算（逐天执行）—— 使用缓存的均价处理指定日期的单据
   *
   * @param vo 执行参数（taskId, processDate）
   * @return 当天执行结果
   */
  MonthEndRecalculateStepResult stepMonthEndRecalculate(MonthEndRecalculateStepVo vo);

  /**
   * 从销售出库查询页面导入并创建订单
   *
   * @param list 导入数据
   * @return 创建的订单ID列表
   */
  List<String> importByQuery(List<SaleOutSheetQueryImportModel> list);
}
