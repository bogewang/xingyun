package com.lframework.xingyun.sc.service.purchase;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.excel.purchase.ReceiveSheetQueryImportModel;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetImportModel;
import com.lframework.xingyun.sc.vo.purchase.receive.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReceiveSheetService extends BaseMpService<ReceiveSheet> {

  /**
   * 查询列表
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<ReceiveSheet> query(Integer pageIndex, Integer pageSize, QueryReceiveSheetVo vo);

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  List<ReceiveSheet> query(QueryReceiveSheetVo vo);

  PageResult<QueryReceiveSheetDetailDto> queryDetail(Integer pageIndex, Integer pageSize,
      QueryReceiveSheetVo vo);

  /**
   * 选择器
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<ReceiveSheet> selector(Integer pageIndex, Integer pageSize, ReceiveSheetSelectorVo vo);

  /**
   * 根据供应商ID查询默认付款日期
   *
   * @param supplierId
   */
  GetPaymentDateDto getPaymentDate(String supplierId);

  /**
   * 根据ID查询
   *
   * @param id
   * @return
   */
  ReceiveSheetFullDto getDetail(String id);

  /**
   * 根据ID查询（采购退货业务）
   *
   * @param id
   * @return
   */
  ReceiveSheetWithReturnDto getWithReturn(String id);

  /**
   * 查询列表（采购退货业务）
   *
   * @param pageIndex
   * @param pageSize
   * @param vo
   * @return
   */
  PageResult<ReceiveSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
      QueryReceiveSheetWithReturnVo vo);

  /**
   * 创建
   *
   * @param vo
   * @return
   */
  String create(CreateReceiveSheetVo vo);

  /**
   * 修改
   *
   * @param vo
   */
  void update(UpdateReceiveSheetVo vo);

  /**
   * 修改备注
   *
   * @param vo
   */
  void updateDescription(UpdateReceiveSheetDescriptionVo vo);

  /**
   * 审核通过
   *
   * @param vo
   */
  void approvePass(ApprovePassReceiveSheetVo vo);

  /**
   * 直接审核通过
   *
   * @param vo
   */
  String directApprovePass(CreateReceiveSheetVo vo);

  /**
   * 审核拒绝
   *
   * @param vo
   */
  void approveRefuse(ApproveRefuseReceiveSheetVo vo);

  /**
   * 根据ID删除
   *
   * @param id
   */
  void deleteById(String id);

  /**
   * 设置成待对账
   *
   * @param id
   * @return
   */
  int setUnCheckBill(String id);

  /**
   * 待结算
   *
   * @param id
   * @param settleCheckSheetDetailId
   * @return
   */
  int setUnSettle(String id, String settleCheckSheetDetailId);

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
   * 清除对账单关联
   *
   * @param id 单据ID
   */
  void clearSettleSheetDetailId(String id);

  /**
   * 执行结算
   *
   * @param id 单据ID
   * @param payAmount 实付金额
   * @param discountAmount 优惠金额
   */
  void settle(String id, BigDecimal payAmount, BigDecimal discountAmount);

  /**
   * 查询已审核列表
   *
   * @param supplierId
   * @param startTime
   * @param endTime
   * @return
   */
  List<ReceiveSheet> getApprovedList(String supplierId, LocalDateTime startTime,
      LocalDateTime endTime,
      SettleStatus settleStatus);

  List<ReceiveProductVo> checkImport(List<ReceiveSheetImportModel> list);
  /**
   * 从采购查询页面导入并创建订单
   *
   * @param list 导入数据
   * @return 创建的订单ID列表
   */
  List<String> importByQuery(List<ReceiveSheetQueryImportModel> list);
}
