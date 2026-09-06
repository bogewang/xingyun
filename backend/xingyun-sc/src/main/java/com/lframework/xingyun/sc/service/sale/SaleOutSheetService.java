package com.lframework.xingyun.sc.service.sale;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitSummaryBo;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProfitTrendDto;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetQueryImportModel;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetInvoiceDetailExportModel;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.excel.sale.out.SaleOutSheetImportModel;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateStartResult;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateStepResult;
import com.lframework.xingyun.sc.vo.sale.out.*;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteProductVo;
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
   * 合并销售出库单
   *
   * @param vo 合并参数
   * @return 合并后保留的销售出库单ID
   */
  String merge(MergeSaleOutSheetVo vo);

  /**
   * 修改备注
   *
   * @param vo
   */
  void updateDescription(UpdateSaleOutSheetDescriptionVo vo);

  /**
   * 批量更新销售出库单备注。
   *
   * @param vo 批量更新参数
   */
  void batchUpdateDescription(BatchUpdateSaleOutSheetDescriptionVo vo);

  /**
   * 批量调整销售出库明细售价
   *
   * @param vo
   */
  void batchUpdatePrice(BatchUpdateSaleOutSheetPriceVo vo);

  /**
   * 批量标记销售出库单为已送货
   *
   * @param vo 销售出库单ID列表
   */
  void batchDelivery(BatchDeliverySaleOutSheetVo vo);

  /**
   * 按订单日期同步询价商品销售价。
   *
   * @param vo 日期范围
   */
  void syncInquirySalePrice(SyncInquirySalePriceVo vo);

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
   * 按源单结算状态与结算版本号乐观锁设置为未结算。
   *
   * @param id 单据ID
   * @param settleStatus 提交时读取到的结算状态
   * @param settleVersion 提交时读取到的结算版本号
   * @return 受影响行数
   */
  int setUnSettle(String id, SettleStatus settleStatus, Long settleVersion);

  /**
   * 设置成结算中
   *
   * @param id
   * @return
   */
  int setPartSettle(String id);

  /**
   * 按源单结算状态与结算版本号乐观锁设置为部分结算。
   *
   * @param id 单据ID
   * @param settleStatus 提交时读取到的结算状态
   * @param settleVersion 提交时读取到的结算版本号
   * @return 受影响行数
   */
  int setPartSettle(String id, SettleStatus settleStatus, Long settleVersion);

  /**
   * 设置成已结算
   *
   * @param id
   * @return
   */
  int setSettled(String id);

  /**
   * 按源单结算状态与结算版本号乐观锁设置为已结算。
   *
   * @param id 单据ID
   * @param settleStatus 提交时读取到的结算状态
   * @param settleVersion 提交时读取到的结算版本号
   * @return 受影响行数
   */
  int setSettled(String id, SettleStatus settleStatus, Long settleVersion);

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

  /**
   * 校验销售出库导入数据，并按订单日期限定可导入的询价商品。
   *
   * @param list 导入数据
   * @param orderDate 订单日期
   * @return 校验后的销售出库商品
   */
  List<SaleOutProductVo> checkImport(List<SaleOutSheetImportModel> list, LocalDate orderDate);

  List<PrintSaleTagBo> tagPrint(QuerySaleOutSheetVo vo);

  /**
   * 是否开启产品询价唯一性检查
   *
   * @return
   */
  Boolean getPriceUniqueConfig();

  /**
   * 是否展示销售出库计划日期。
   *
   * @return 参数未配置时默认展示
   */
  Boolean getPlanDateDisplayConfig();

  /**
   * 按订单日期查询可用于销售出库的报价商品。
   *
   * @param vo 查询参数
   * @return 已启用报价单中的商品
   */
  List<QuoteProductBo> queryQuoteProducts(QueryQuoteProductVo vo);

  /**
   * 唯一报价模式下，将可销售商品过滤为单据日期报价单内的商品，并用报价价覆盖售价。
   *
   * @param products 可销售商品
   * @param orderDate 单据日期
   * @return 过滤后的商品列表；未开启唯一报价或日期为空时返回 null，表示无需过滤
   */
  List<SaleProductDto> applyQuoteFilter(List<SaleProductDto> products, String orderDate);

  void marketBuySummary(QuerySaleOutSheetVo vo);

  /**
   * 导出买菜汇总2。
   *
   * @param vo 查询参数
   */
  void marketBuySummary2(QuerySaleOutSheetVo vo);

  void exportDetailDailySummary(QuerySaleOutSheetVo vo);

  /**
   * 查询已按商品和单位汇总的开票明细。
   *
   * @param vo 查询参数
   * @return 开票明细
   */
  List<SaleOutSheetInvoiceDetailExportModel> queryInvoiceDetail(QuerySaleOutSheetVo vo);

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
