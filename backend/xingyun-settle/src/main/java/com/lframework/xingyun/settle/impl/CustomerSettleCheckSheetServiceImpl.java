package com.lframework.xingyun.settle.impl;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleReturn;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.returned.QuerySaleReturnVo;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetBizType;
import com.lframework.xingyun.settle.enums.SettleCheckSheetCalcType;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetStatus;
import com.lframework.xingyun.settle.mappers.CustomerSettleCheckSheetMapper;
import com.lframework.xingyun.settle.service.CustomerSettleCheckSheetDetailService;
import com.lframework.xingyun.settle.service.CustomerSettleCheckSheetService;
import com.lframework.xingyun.settle.utils.SettleAmountAllocationUtil;
import com.lframework.xingyun.settle.vo.check.customer.CreateCustomerSettleCheckSheetVo;
import com.lframework.xingyun.settle.vo.check.customer.CustomerSettleCheckSheetItemVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户对账单服务实现。
 */
@Service
public class CustomerSettleCheckSheetServiceImpl extends
    BaseMpServiceImpl<CustomerSettleCheckSheetMapper, CustomerSettleCheckSheet>
    implements CustomerSettleCheckSheetService {

  @Autowired
  private CustomerSettleCheckSheetDetailService customerSettleCheckSheetDetailService;

  @Autowired
  private SaleOutSheetService saleOutSheetService;

  @Autowired
  private SaleReturnService saleReturnService;

  /**
   * 直接确认客户对账单，并在一个事务内保存明细和回写源单状态。
   *
   * @param vo 客户对账确认请求
   * @return 已确认对账单ID
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String directApprovePass(CreateCustomerSettleCheckSheetVo vo) {
    validateRequest(vo);
    CustomerSettleCheckSheetBizType bizType = resolveBizType(vo.getItems());
    List<CheckBiz> bizItems = loadBizItems(vo, bizType);
    List<BigDecimal> allocatedAmounts = SettleAmountAllocationUtil.allocate(vo.getCheckAmount(),
        bizItems.stream().map(CheckBiz::getOriginalAmount).collect(Collectors.toList()));
    validateAllocatedAmounts(vo.getCheckAmount(), allocatedAmounts);

    CustomerSettleCheckSheet sheet = createConfirmedSheet(vo, bizItems);
    if (getBaseMapper().insert(sheet) != 1) {
      throw new DefaultClientException("保存客户对账单失败！");
    }
    List<CustomerSettleCheckSheetDetail> details = createDetails(sheet, bizItems,
        allocatedAmounts);
    if (!customerSettleCheckSheetDetailService.saveBatch(details)) {
      throw new DefaultClientException("保存客户对账单明细失败！");
    }
    updateSourceSettleStatus(bizItems, bizType);
    return sheet.getId();
  }

  /**
   * 校验确认请求基础参数。
   *
   * @param vo 客户对账确认请求
   */
  private void validateRequest(CreateCustomerSettleCheckSheetVo vo) {
    if (vo == null || StringUtil.isBlank(vo.getCustomerId())) {
      throw new DefaultClientException("客户不能为空！");
    }
    if (vo.getCheckAmount() == null || vo.getCheckAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new DefaultClientException("确认对账金额必须大于0！");
    }
    if (vo.getCheckAmount().scale() > 2) {
      throw new DefaultClientException("确认对账金额最多保留两位小数！");
    }
    if (vo.getItems() == null || vo.getItems().isEmpty()) {
      throw new DefaultClientException("对账业务项不能为空！");
    }
  }

  /**
   * 解析并校验同批业务类型。
   *
   * @param items 对账业务项
   * @return 统一业务类型
   */
  private CustomerSettleCheckSheetBizType resolveBizType(
      List<CustomerSettleCheckSheetItemVo> items) {
    CustomerSettleCheckSheetBizType result = null;
    Set<String> bizIds = new HashSet<>();
    for (CustomerSettleCheckSheetItemVo item : items) {
      if (item == null || StringUtil.isBlank(item.getBizId()) || item.getBizType() == null) {
        throw new DefaultClientException("业务单据参数不正确！");
      }
      CustomerSettleCheckSheetBizType bizType = getBizType(item.getBizType());
      if (result != null && result != bizType) {
        throw new DefaultClientException("同批对账业务单据类型必须一致！");
      }
      if (!bizIds.add(item.getBizId())) {
        throw new DefaultClientException("业务单据不允许重复选择！");
      }
      result = bizType;
    }
    return result;
  }

  /**
   * 根据请求业务类型加载并校验源单据。
   *
   * @param vo 客户对账确认请求
   * @param bizType 统一业务类型
   * @return 有序的源单据对账数据
   */
  private List<CheckBiz> loadBizItems(CreateCustomerSettleCheckSheetVo vo,
      CustomerSettleCheckSheetBizType bizType) {
    List<String> bizIds = vo.getItems().stream().map(CustomerSettleCheckSheetItemVo::getBizId)
        .collect(Collectors.toList());
    Map<String, CheckBiz> bizMap = bizType == CustomerSettleCheckSheetBizType.OUT_SHEET
        ? loadSaleOutBizMap(bizIds) : loadSaleReturnBizMap(bizIds);
    if (bizMap.size() != bizIds.size()) {
      throw new DefaultClientException("业务单据不存在！");
    }
    List<CheckBiz> results = new ArrayList<>();
    for (String bizId : bizIds) {
      CheckBiz biz = bizMap.get(bizId);
      validateBiz(vo.getCustomerId(), biz);
      biz.setBizType(bizType);
      results.add(biz);
    }
    return results;
  }

  /**
   * 批量加载销售出库单。
   *
   * @param bizIds 销售出库单ID
   * @return 按ID索引的业务单据
   */
  private Map<String, CheckBiz> loadSaleOutBizMap(Collection<String> bizIds) {
    QuerySaleOutSheetVo queryVo = new QuerySaleOutSheetVo();
    queryVo.setIdList(new ArrayList<>(bizIds));
    queryVo.setRequireTxIdNull(true);
    List<SaleOutSheet> sheets = saleOutSheetService.query(queryVo);
    Map<String, CheckBiz> results = new HashMap<>();
    if (sheets != null) {
      for (SaleOutSheet sheet : sheets) {
        results.put(sheet.getId(), new CheckBiz(sheet.getId(), sheet.getCustomerId(),
            sheet.getTotalAmount(), sheet.getSettleStatus(), sheet.getSettleVersion()));
      }
    }
    return results;
  }

  /**
   * 批量加载销售退货单。
   *
   * @param bizIds 销售退货单ID
   * @return 按ID索引的业务单据
   */
  private Map<String, CheckBiz> loadSaleReturnBizMap(Collection<String> bizIds) {
    QuerySaleReturnVo queryVo = new QuerySaleReturnVo();
    queryVo.setIdList(new ArrayList<>(bizIds));
    queryVo.setRequireTxIdNull(true);
    List<SaleReturn> sheets = saleReturnService.query(queryVo);
    Map<String, CheckBiz> results = new HashMap<>();
    if (sheets != null) {
      for (SaleReturn sheet : sheets) {
        results.put(sheet.getId(), new CheckBiz(sheet.getId(), sheet.getCustomerId(),
            sheet.getTotalAmount(), sheet.getSettleStatus(), sheet.getSettleVersion()));
      }
    }
    return results;
  }

  /**
   * 校验源单据属于当前客户且可以对账。
   *
   * @param customerId 客户ID
   * @param biz 源单据数据
   */
  private void validateBiz(String customerId, CheckBiz biz) {
    if (biz == null || !customerId.equals(biz.getCustomerId())) {
      throw new DefaultClientException("业务单据不存在或不属于当前客户！");
    }
    if (biz.getSettleStatus() != SettleStatus.UN_CHECK_BILL) {
      throw new DefaultClientException("业务单据不是待对账状态！");
    }
    if (biz.getOriginalAmount() == null || biz.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new DefaultClientException("业务单据金额必须大于0！");
    }
  }

  /**
   * 校验分摊结果金额合法且合计准确。
   *
   * @param checkAmount 确认对账金额
   * @param allocatedAmounts 分摊结果
   */
  private void validateAllocatedAmounts(BigDecimal checkAmount, List<BigDecimal> allocatedAmounts) {
    if (allocatedAmounts.isEmpty() || allocatedAmounts.stream()
        .anyMatch(amount -> amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
        || allocatedAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
        .compareTo(checkAmount) != 0) {
      throw new DefaultClientException("对账金额分摊结果不正确！");
    }
  }

  /**
   * 创建已确认客户对账单主记录。
   *
   * @param vo 客户对账确认请求
   * @param bizItems 源单据数据
   * @return 客户对账单
   */
  private CustomerSettleCheckSheet createConfirmedSheet(CreateCustomerSettleCheckSheetVo vo,
      List<CheckBiz> bizItems) {
    CustomerSettleCheckSheet sheet = new CustomerSettleCheckSheet();
    sheet.setId(generateId());
    sheet.setCode(generateId());
    sheet.setCustomerId(vo.getCustomerId());
    sheet.setTotalAmount(bizItems.stream().map(CheckBiz::getOriginalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
    sheet.setTotalPayAmount(vo.getCheckAmount());
    sheet.setTotalPayedAmount(BigDecimal.ZERO);
    sheet.setTotalDiscountAmount(BigDecimal.ZERO);
    sheet.setDescription(vo.getDescription());
    sheet.setApproveBy(SecurityUtil.getCurrentUser() == null ? null
        : SecurityUtil.getCurrentUser().getId());
    sheet.setApproveTime(LocalDateTime.now());
    sheet.setStatus(CustomerSettleCheckSheetStatus.CONFIRMED);
    return sheet;
  }

  /**
   * 创建客户对账单明细。
   *
   * @param sheet 客户对账单
   * @param bizItems 源单据数据
   * @param allocatedAmounts 分摊后的最终对账金额
   * @return 客户对账单明细
   */
  private List<CustomerSettleCheckSheetDetail> createDetails(CustomerSettleCheckSheet sheet,
      List<CheckBiz> bizItems, List<BigDecimal> allocatedAmounts) {
    List<CustomerSettleCheckSheetDetail> results = new ArrayList<>();
    for (int index = 0; index < bizItems.size(); index++) {
      CustomerSettleCheckSheetDetail detail = new CustomerSettleCheckSheetDetail();
      detail.setId(generateId());
      detail.setSheetId(sheet.getId());
      detail.setBizId(bizItems.get(index).getBizId());
      detail.setBizType(bizItems.get(index).getBizType());
      detail.setCalcType(SettleCheckSheetCalcType.ADD);
      detail.setPayAmount(allocatedAmounts.get(index));
      detail.setDescription(sheet.getDescription());
      detail.setOrderNo(index + 1);
      results.add(detail);
    }
    return results;
  }

  /**
   * 以源单状态与版本号乐观锁回写为未结算。
   *
   * @param bizItems 源单据数据
   * @param bizType 业务类型
   */
  private void updateSourceSettleStatus(List<CheckBiz> bizItems,
      CustomerSettleCheckSheetBizType bizType) {
    for (CheckBiz biz : bizItems) {
      int count = bizType == CustomerSettleCheckSheetBizType.OUT_SHEET
          ? saleOutSheetService.setUnSettle(biz.getBizId(), biz.getSettleStatus(),
              biz.getSettleVersion())
          : saleReturnService.setUnSettle(biz.getBizId(), biz.getSettleStatus(),
              biz.getSettleVersion());
      if (count != 1) {
        throw new DefaultClientException("业务单据状态已变化，请刷新后重试！");
      }
    }
  }

  /**
   * 解析客户对账业务类型。
   *
   * @param bizType 业务类型编码
   * @return 客户对账业务类型
   */
  private CustomerSettleCheckSheetBizType getBizType(Integer bizType) {
    if (CustomerSettleCheckSheetBizType.OUT_SHEET.getCode().equals(bizType)) {
      return CustomerSettleCheckSheetBizType.OUT_SHEET;
    }
    if (CustomerSettleCheckSheetBizType.SALE_RETURN.getCode().equals(bizType)) {
      return CustomerSettleCheckSheetBizType.SALE_RETURN;
    }
    throw new DefaultClientException("业务类型不正确！");
  }

  /**
   * 生成不含连接符的ID。
   *
   * @return 唯一ID
   */
  private String generateId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 客户对账源单据数据。
   */
  private static class CheckBiz {

    private final String bizId;
    private final String customerId;
    private final BigDecimal originalAmount;
    private final SettleStatus settleStatus;
    private final Long settleVersion;
    private CustomerSettleCheckSheetBizType bizType;

    /**
     * 创建客户对账源单据数据。
     *
     * @param bizId 业务单据ID
     * @param customerId 客户ID
     * @param originalAmount 原始金额
     * @param settleStatus 源单结算状态
     * @param settleVersion 源单结算版本号
     */
    CheckBiz(String bizId, String customerId, BigDecimal originalAmount,
        SettleStatus settleStatus, Long settleVersion) {
      this.bizId = bizId;
      this.customerId = customerId;
      this.originalAmount = originalAmount;
      this.settleStatus = settleStatus;
      this.settleVersion = settleVersion == null ? 0L : settleVersion;
    }

    /**
     * 获取业务单据ID。
     *
     * @return 业务单据ID
     */
    String getBizId() {
      return bizId;
    }

    /**
     * 获取客户ID。
     *
     * @return 客户ID
     */
    String getCustomerId() {
      return customerId;
    }

    /**
     * 获取原始金额。
     *
     * @return 原始金额
     */
    BigDecimal getOriginalAmount() {
      return originalAmount;
    }

    /**
     * 获取源单结算状态。
     *
     * @return 源单结算状态
     */
    SettleStatus getSettleStatus() {
      return settleStatus;
    }

    /**
     * 获取源单结算版本号。
     *
     * @return 源单结算版本号
     */
    Long getSettleVersion() {
      return settleVersion;
    }

    /**
     * 获取业务类型。
     *
     * @return 业务类型
     */
    CustomerSettleCheckSheetBizType getBizType() {
      return bizType;
    }

    /**
     * 设置业务类型。
     *
     * @param bizType 业务类型
     */
    void setBizType(CustomerSettleCheckSheetBizType bizType) {
      this.bizType = bizType;
    }
  }
}
