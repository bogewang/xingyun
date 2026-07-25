package com.lframework.xingyun.sc.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.starter.web.core.annotations.permission.DataPermission;
import com.lframework.starter.web.core.annotations.permission.DataPermissions;
import com.lframework.starter.web.core.annotations.sort.Sort;
import com.lframework.starter.web.core.annotations.sort.Sorts;
import com.lframework.starter.web.inner.components.permission.OrderDataPermissionDataPermissionType;
import com.lframework.xingyun.sc.dto.sale.out.QuerySaleOutSheetDetailDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProductProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetProfitTrendDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProductProfitSummaryBo;
import com.lframework.xingyun.sc.bo.sale.out.SaleOutSheetProfitSummaryBo;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetWithReturnVo;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutSheetSelectorVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author zmj
 * @since 2021-10-26
 */
public interface SaleOutSheetMapper extends BaseMapper<SaleOutSheet> {

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  @Sorts({
      @Sort(value = "code", alias = "s", autoParse = true),
      @Sort(value = "createTime", alias = "s", autoParse = true),
      @Sort(value = "approveTime", alias = "s", autoParse = true),
  })
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheet> query(@Param("vo") QuerySaleOutSheetVo vo);

  /**
   * 查询销售利润汇总
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  SaleOutSheetProfitSummaryBo queryProfitSummary(@Param("vo") QuerySaleOutSheetVo vo);

  /**
   * 查询销售利润（按商品）汇总
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  SaleOutSheetProductProfitSummaryBo queryProductProfitSummary(@Param("vo") QuerySaleOutSheetVo vo);

  @Sorts({
      @Sort(value = "productName", alias = "g", autoParse = true),
      @Sort(value = "saleNum", autoParse = false),
      @Sort(value = "salesAmount", autoParse = false),
      @Sort(value = "salesCost", autoParse = false),
      @Sort(value = "salesProfit", autoParse = false),
  })
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheetProductProfitDto> queryProductProfit(@Param("vo") QuerySaleOutSheetVo vo);

  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheetProfitTrendDto> queryProfitTrend(@Param("vo") QuerySaleOutSheetVo vo);

  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheetProductProfitTrendDto> queryProductProfitTrend(@Param("vo") QuerySaleOutSheetVo vo);

  @Sorts({
      @Sort(value = "code", alias = "s", autoParse = true),
      @Sort(value = "orderDate", alias = "s", autoParse = true),
      @Sort(value = "createTime", alias = "s", autoParse = true),
      @Sort(value = "approveTime", alias = "s", autoParse = true),
  })
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<QuerySaleOutSheetDetailDto> queryDetail(@Param("vo") QuerySaleOutSheetVo vo);

  @Sorts({
      @Sort(value = "code", alias = "s", autoParse = true),
      @Sort(value = "orderDate", alias = "s", autoParse = true),
      @Sort(value = "createTime", alias = "s", autoParse = true),
      @Sort(value = "approveTime", alias = "s", autoParse = true),
  })
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<QuerySaleOutSheetDetailDto> queryPriceCheckDetail(@Param("vo") QuerySaleOutSheetVo vo);

  /**
   * 选择器
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheet> selector(@Param("vo") SaleOutSheetSelectorVo vo);

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
  SaleOutSheetWithReturnDto getWithReturn(@Param("id") String id,
      @Param("requireOut") Boolean requireOut);

  /**
   * 查询列表（销售退货业务）
   *
   * @param vo
   * @return
   */
  @DataPermissions(type = OrderDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "order", alias = "s")
  })
  List<SaleOutSheet> queryWithReturn(@Param("vo") QuerySaleOutSheetWithReturnVo vo,
      @Param("multipleRelate") boolean multipleRelate);

  /**
   * 查询已审核列表
   *
   * @param customerId
   * @param startTime
   * @param endTime
   * @return
   */
  List<SaleOutSheet> getApprovedList(@Param("customerId") String customerId,
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
      @Param("settleStatus") SettleStatus settleStatus);

  /**
   * 按结算状态和版本号原子更新结算状态。
   *
   * @param id 单据ID
   * @param expectedStatus 预期结算状态
   * @param targetStatus 目标结算状态
   * @param settleVersion 预期结算版本号
   * @return 受影响行数
   */
  int updateSettleStatusWithVersion(@Param("id") String id,
      @Param("expectedStatus") SettleStatus expectedStatus,
      @Param("targetStatus") SettleStatus targetStatus,
      @Param("settleVersion") Long settleVersion);
}
