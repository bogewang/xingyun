package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.entity.OrderTimeLine;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.OrderTimeLineService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.core.components.timeline.ReceiveOrderTimeLineBizType;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleReturn;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.sale.SaleReturnService;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.returned.QuerySaleReturnVo;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.settle.dto.sheet.customer.CustomerSettleSheetFullDto;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSaleSettleBizType;
import com.lframework.xingyun.settle.enums.SettleOpLogType;
import com.lframework.xingyun.settle.enums.CustomerSettleSheetStatus;
import com.lframework.xingyun.settle.mappers.CustomerSettleSheetMapper;
import com.lframework.xingyun.settle.service.CustomerSettleSheetDetailService;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.utils.SettleAmountAllocationUtil;
import com.lframework.xingyun.settle.vo.sheet.customer.CreateCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.CustomerSettleSheetItemVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleSheetVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * 客户结算单服务实现。
 */
@Service
public class CustomerSettleSheetServiceImpl extends
    BaseMpServiceImpl<CustomerSettleSheetMapper, CustomerSettleSheet>
    implements CustomerSettleSheetService {

  @Autowired
  private CustomerSettleSheetDetailService customerSettleSheetDetailService;

  @Autowired
  private GenerateCodeService generateCodeService;

  @Autowired
  private SaleOutSheetService saleOutSheetService;

  @Autowired
  private SaleReturnService saleReturnService;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private OrderTimeLineService orderTimeLineService;

  @Autowired
  private ReceiveOrderTimeLineBizType receiveOrderTimeLineBizType;

  /**
   * 查询客户销售业务单据结算工作台信息。
   *
   * @param vo 查询条件
   * @return 结算工作台分页数据
   */
  @Override
  public PageResult<CustomerSaleSettleInfoBo> querySaleSettleInfos(
      QueryCustomerSaleSettleInfoVo vo) {
    if (vo.getBizType() == null || (vo.getBizType() != 1 && vo.getBizType() != 2)) {
      throw new DefaultClientException("业务类型不正确！");
    }
    int pageIndex = vo.getPageIndex() == null || vo.getPageIndex() < 1 ? 1 : vo.getPageIndex();
    int pageSize = vo.getPageSize() == null || vo.getPageSize() < 1 ? 20 : vo.getPageSize();
    PageResult<CustomerSaleSettleInfoBo> result = vo.getBizType() == 1
        ? querySaleOutSettleInfos(pageIndex, pageSize, vo)
        : querySaleReturnSettleInfos(pageIndex, pageSize, vo);
    fillSettleAmounts(result.getDatas());
    return result;
  }

  /**
   * 查询销售出库单结算工作台信息。
   */
  private PageResult<CustomerSaleSettleInfoBo> querySaleOutSettleInfos(int pageIndex, int pageSize,
      QueryCustomerSaleSettleInfoVo vo) {
    QuerySaleOutSheetVo saleOutVo = new QuerySaleOutSheetVo();
    saleOutVo.setCode(vo.getCode());
    saleOutVo.setCustomerId(vo.getCustomerId());
    saleOutVo.setRequireTxIdNull(true);
    PageResult<SaleOutSheet> pageResult = saleOutSheetService.query(pageIndex, pageSize, saleOutVo);
    List<CustomerSaleSettleInfoBo> results = pageResult.getDatas().stream()
        .map(sheet -> buildSettleInfo(sheet.getId(), 1, sheet.getCode(), sheet.getCustomerId(),
            sheet.getTotalAmount(), sheet.getPaidAmount(), getSettleStatusCode(sheet.getSettleStatus())))
        .collect(Collectors.toList());
    return PageResultUtil.rebuild(pageResult, results);
  }

  /**
   * 查询销售退货单结算工作台信息。
   */
  private PageResult<CustomerSaleSettleInfoBo> querySaleReturnSettleInfos(int pageIndex,
      int pageSize, QueryCustomerSaleSettleInfoVo vo) {
    QuerySaleReturnVo saleReturnVo = new QuerySaleReturnVo();
    saleReturnVo.setCode(vo.getCode());
    saleReturnVo.setCustomerId(vo.getCustomerId());
    saleReturnVo.setRequireTxIdNull(true);
    PageResult<SaleReturn> pageResult = saleReturnService.query(pageIndex, pageSize, saleReturnVo);
    List<CustomerSaleSettleInfoBo> results = pageResult.getDatas().stream()
        .map(sheet -> buildSettleInfo(sheet.getId(), 2, sheet.getCode(), sheet.getCustomerId(),
            amountOrZero(sheet.getTotalAmount()).negate(), BigDecimal.ZERO,
            getSettleStatusCode(sheet.getSettleStatus())))
        .collect(Collectors.toList());
    return PageResultUtil.rebuild(pageResult, results);
  }

  /**
   * 获取结算状态编码。
   */
  private Integer getSettleStatusCode(SettleStatus settleStatus) {
    return settleStatus == null ? null : settleStatus.getCode();
  }

  /**
   * 构建销售单据结算信息。
   */
  private CustomerSaleSettleInfoBo buildSettleInfo(String id, int bizType, String code,
      String customerId, BigDecimal totalAmount, BigDecimal receivedAmount, Integer settleStatus) {
    CustomerSaleSettleInfoBo result = new CustomerSaleSettleInfoBo();
    result.setId(id);
    result.setBizType(bizType);
    result.setCode(code);
    result.setCustomerId(customerId);
    result.setTotalAmount(amountOrZero(totalAmount));
    result.setReceivedAmount(amountOrZero(receivedAmount));
    result.setSettleStatus(settleStatus);
    return result;
  }

  /**
   * 批量填充工作台结算金额。
   */
  private void fillSettleAmounts(List<CustomerSaleSettleInfoBo> results) {
    if (CollectionUtil.isEmpty(results)) {
      return;
    }
    Map<String, String> customerNameMap = customerService.listByIds(results.stream()
            .map(CustomerSaleSettleInfoBo::getCustomerId).filter(StringUtil::isNotBlank)
            .collect(Collectors.toSet()))
        .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
    Map<String, BigDecimal> settleAmountMap = querySettleAmountMap(results.stream()
        .map(CustomerSaleSettleInfoBo::getId).collect(Collectors.toList()));
    results.forEach(item -> {
      item.setCustomerName(customerNameMap.get(item.getCustomerId()));
      BigDecimal settleAmount = settleAmountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
      item.setSettleAmount(settleAmount);
      item.setUnSettleAmount(item.getTotalAmount().subtract(item.getReceivedAmount())
          .subtract(settleAmount));
    });
  }

  /**
   * 按业务单据批量汇总审核通过的客户结算金额。
   */
  private Map<String, BigDecimal> querySettleAmountMap(Collection<String> bizIds) {
    if (CollectionUtil.isEmpty(bizIds)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleSheetDetail> details = customerSettleSheetDetailService.list(
        Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
            .in(CustomerSettleSheetDetail::getBizId, bizIds));
    if (CollectionUtil.isEmpty(details)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleSheet> sheets = getBaseMapper().selectBatchIds(details.stream()
        .map(CustomerSettleSheetDetail::getSheetId).collect(Collectors.toSet()));
    if (CollectionUtil.isEmpty(sheets)) {
      return Collections.emptyMap();
    }
    Set<String> approvedSheetIds = sheets.stream()
        .filter(sheet -> sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS)
        .map(CustomerSettleSheet::getId).collect(Collectors.toSet());
    return details.stream().filter(detail -> approvedSheetIds.contains(detail.getSheetId()))
        .filter(detail -> detail.getPayAmount() != null)
        .collect(Collectors.groupingBy(CustomerSettleSheetDetail::getBizId,
            Collectors.reducing(BigDecimal.ZERO, CustomerSettleSheetDetail::getPayAmount,
                BigDecimal::add)));
  }

  /**
   * 查询客户结算记录。
   */
  @Override
  public PageResult<CustomerSettleSheet> query(Integer pageIndex, Integer pageSize,
      QueryCustomerSettleSheetVo vo) {
    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);
    PageHelperUtil.startPage(pageIndex, pageSize);
    return PageResultUtil.convert(new PageInfo<>(query(vo)));
  }

  /**
   * 查询客户结算记录列表。
   */
  @Override
  public List<CustomerSettleSheet> query(QueryCustomerSettleSheetVo vo) {
    return getBaseMapper().query(vo);
  }

  /**
   * 查询客户结算记录详情。
   */
  @Override
  public CustomerSettleSheetFullDto getDetail(String id) {
    CustomerSettleSheetFullDto result = getBaseMapper().getDetail(id);
    if (result == null || CollectionUtil.isEmpty(result.getDetails())) {
      return result;
    }
    Set<String> bizIds = result.getDetails().stream()
        .map(CustomerSettleSheetFullDto.SheetDetailDto::getBizId)
        .filter(StringUtil::isNotBlank).collect(Collectors.toSet());
    Map<String, String> bizCodeMap = new HashMap<>();
    Map<String, Integer> bizTypeMap = new HashMap<>();
    List<SaleOutSheet> saleOutSheets = saleOutSheetService.listByIds(bizIds);
    if (!CollectionUtil.isEmpty(saleOutSheets)) {
      saleOutSheets.forEach(item -> {
        bizCodeMap.put(item.getId(), item.getCode());
        bizTypeMap.put(item.getId(), CustomerSaleSettleBizType.OUT_SHEET.getCode());
      });
    }
    List<SaleReturn> saleReturns = saleReturnService.listByIds(bizIds);
    if (!CollectionUtil.isEmpty(saleReturns)) {
      saleReturns.forEach(item -> {
        bizCodeMap.put(item.getId(), item.getCode());
        bizTypeMap.put(item.getId(), CustomerSaleSettleBizType.SALE_RETURN.getCode());
      });
    }
    result.getDetails().forEach(item -> {
      item.setBizCode(bizCodeMap.get(item.getBizId()));
      item.setBizType(bizTypeMap.get(item.getBizId()));
    });
    return result;
  }

  /**
   * 创建已审核的客户直接结算单，并同步业务单据结算状态。
   *
   * @param vo 直接结算请求
   * @return 结算单ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  @OpLog(type = SettleOpLogType.class, name = "直接审核通过客户结算单，ID：{}",
      params = "#_result", autoSaveParams = true)
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result",
      name = "直接审核通过")
  public String directApprovePass(CreateCustomerSettleSheetVo vo) {
    List<DirectSettleBiz> bizItems = validateDirectSettle(vo);
    BigDecimal totalUnSettleAmount = bizItems.stream().map(DirectSettleBiz::getUnSettleAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    validateConfirmedAmount(vo.getSettleAmount(), totalUnSettleAmount);
    List<BigDecimal> amounts = SettleAmountAllocationUtil.allocateSigned(vo.getSettleAmount(),
        bizItems.stream().map(DirectSettleBiz::getUnSettleAmount).collect(Collectors.toList()));

    CustomerSettleSheet sheet = createApprovedSheet(vo);
    List<CustomerSettleSheetDetail> details = new ArrayList<>();
    for (int index = 0; index < bizItems.size(); index++) {
      DirectSettleBiz biz = bizItems.get(index);
      BigDecimal amount = amounts.get(index);
      updateBizSettleStatus(biz, amount);
      details.add(createDetail(sheet.getId(), biz.getBizId(), amount, index + 1));
    }
    if (!customerSettleSheetDetailService.saveBatch(details)) {
      throw new DefaultClientException("保存客户结算单明细失败！");
    }
    recordSaleSourceSettleTimeLine(sheet, details);
    return sheet.getId();
  }

  /**
   * 校验确认金额与所选业务净额的方向及范围一致。
   *
   * @param confirmedAmount 确认金额
   * @param totalUnSettleAmount 未结算净额
   */
  private void validateConfirmedAmount(BigDecimal confirmedAmount,
      BigDecimal totalUnSettleAmount) {
    if (totalUnSettleAmount.compareTo(BigDecimal.ZERO) == 0
        || confirmedAmount.signum() != totalUnSettleAmount.signum()
        || confirmedAmount.abs().compareTo(totalUnSettleAmount.abs()) > 0) {
      throw new DefaultClientException("确认结算金额与所选单据未结算净额方向或范围不一致！");
    }
  }

  /**
   * 校验直接结算请求，并批量加载源单据。
   */
  private List<DirectSettleBiz> validateDirectSettle(CreateCustomerSettleSheetVo vo) {
    if (vo == null || StringUtil.isBlank(vo.getCustomerId())) {
      throw new DefaultClientException("客户不能为空！");
    }
    if (vo.getSettleAmount() == null || vo.getSettleAmount().compareTo(BigDecimal.ZERO) == 0) {
      throw new DefaultClientException("确认结算金额不能为0！");
    }
    if (vo.getSettleAmount().scale() > 2) {
      throw new DefaultClientException("确认结算金额最多保留两位小数！");
    }
    if (CollectionUtil.isEmpty(vo.getItems())) {
      throw new DefaultClientException("结算项目不能为空！");
    }
    Set<String> saleOutIds = new HashSet<>();
    Set<String> saleReturnIds = new HashSet<>();
    Set<String> itemKeys = new HashSet<>();
    for (CustomerSettleSheetItemVo item : vo.getItems()) {
      if (item == null || StringUtil.isBlank(item.getBizId())
          || (item.getBizType() == null || (item.getBizType() != 1 && item.getBizType() != 2))) {
        throw new DefaultClientException("业务单据参数不正确！");
      }
      if (!itemKeys.add(item.getBizType() + ":" + item.getBizId())) {
        throw new DefaultClientException("业务单据不允许重复选择！");
      }
      if (item.getBizType() == 1) {
        saleOutIds.add(item.getBizId());
      } else {
        saleReturnIds.add(item.getBizId());
      }
    }
    Map<String, SaleOutSheet> saleOutMap = toSaleOutMap(saleOutIds);
    Map<String, SaleReturn> saleReturnMap = toSaleReturnMap(saleReturnIds);
    if (saleOutMap.size() != saleOutIds.size() || saleReturnMap.size() != saleReturnIds.size()) {
      throw new DefaultClientException("业务单据不存在或无权访问！");
    }
    Map<String, BigDecimal> settledAmountMap = querySettleAmountMap(itemKeys.stream()
        .map(key -> key.substring(key.indexOf(':') + 1)).collect(Collectors.toSet()));
    List<DirectSettleBiz> results = new ArrayList<>();
    for (CustomerSettleSheetItemVo item : vo.getItems()) {
      DirectSettleBiz biz = item.getBizType() == 1
          ? buildSaleOutBiz(item, saleOutMap.get(item.getBizId()), settledAmountMap)
          : buildSaleReturnBiz(item, saleReturnMap.get(item.getBizId()), settledAmountMap);
      if (!vo.getCustomerId().equals(biz.getCustomerId())) {
        throw new DefaultClientException("所选业务单据必须属于同一客户！");
      }
      if (biz.getUnSettleAmount().compareTo(BigDecimal.ZERO) == 0) {
        throw new DefaultClientException("单号：" + biz.getCode() + "不存在可结算金额！");
      }
      results.add(biz);
    }
    return results;
  }

  /**
   * 批量加载销售出库单。
   */
  private Map<String, SaleOutSheet> toSaleOutMap(Collection<String> ids) {
    if (CollectionUtil.isEmpty(ids)) {
      return Collections.emptyMap();
    }
    QuerySaleOutSheetVo queryVo = new QuerySaleOutSheetVo();
    queryVo.setIdList(new ArrayList<>(ids));
    queryVo.setRequireTxIdNull(true);
    List<SaleOutSheet> sheets = saleOutSheetService.query(queryVo);
    return sheets == null ? Collections.emptyMap() : sheets.stream()
        .collect(Collectors.toMap(SaleOutSheet::getId, sheet -> sheet, (a, b) -> a));
  }

  /**
   * 批量加载销售退货单。
   */
  private Map<String, SaleReturn> toSaleReturnMap(Collection<String> ids) {
    if (CollectionUtil.isEmpty(ids)) {
      return Collections.emptyMap();
    }
    QuerySaleReturnVo queryVo = new QuerySaleReturnVo();
    queryVo.setIdList(new ArrayList<>(ids));
    queryVo.setRequireTxIdNull(true);
    List<SaleReturn> sheets = saleReturnService.query(queryVo);
    return sheets == null ? Collections.emptyMap() : sheets.stream()
        .collect(Collectors.toMap(SaleReturn::getId, sheet -> sheet, (a, b) -> a));
  }

  /**
   * 构建销售出库单结算校验数据。
   */
  private DirectSettleBiz buildSaleOutBiz(CustomerSettleSheetItemVo item, SaleOutSheet sheet,
      Map<String, BigDecimal> settledAmountMap) {
    if (sheet == null) {
      throw new DefaultClientException("业务单据不存在或无权访问！");
    }
    if (!StringUtil.isBlank(sheet.getTxId())) {
      throw new DefaultClientException("单号：" + sheet.getCode() + "已被结算交易占用！");
    }
    validateSettleStatus(sheet.getCode(), sheet.getSettleStatus());
    return new DirectSettleBiz(item.getBizId(), item.getBizType(), sheet.getCode(),
        sheet.getCustomerId(), sheet.getSettleStatus(), sheet.getSettleVersion(),
        amountOrZero(sheet.getTotalAmount()).subtract(
            amountOrZero(sheet.getPaidAmount())).subtract(
            settledAmountMap.getOrDefault(item.getBizId(), BigDecimal.ZERO)));
  }

  /**
   * 构建销售退货单结算校验数据。
   */
  private DirectSettleBiz buildSaleReturnBiz(CustomerSettleSheetItemVo item, SaleReturn sheet,
      Map<String, BigDecimal> settledAmountMap) {
    if (sheet == null) {
      throw new DefaultClientException("业务单据不存在或无权访问！");
    }
    if (!StringUtil.isBlank(sheet.getTxId())) {
      throw new DefaultClientException("单号：" + sheet.getCode() + "已被结算交易占用！");
    }
    validateSettleStatus(sheet.getCode(), sheet.getSettleStatus());
    return new DirectSettleBiz(item.getBizId(), item.getBizType(), sheet.getCode(),
        sheet.getCustomerId(), sheet.getSettleStatus(), sheet.getSettleVersion(),
        amountOrZero(sheet.getTotalAmount()).negate().subtract(
            settledAmountMap.getOrDefault(item.getBizId(), BigDecimal.ZERO)));
  }

  /**
   * 校验业务单据处于可结算状态。
   */
  private void validateSettleStatus(String code, SettleStatus settleStatus) {
    if (settleStatus != SettleStatus.UN_SETTLE && settleStatus != SettleStatus.PART_SETTLE) {
      throw new DefaultClientException("单号：" + code + "不是待结算或部分结算状态！");
    }
  }

  /**
   * 创建已审核结算单主记录。
   */
  private CustomerSettleSheet createApprovedSheet(CreateCustomerSettleSheetVo vo) {
    CustomerSettleSheet sheet = new CustomerSettleSheet();
    sheet.setId(generateId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.CUSTOMER_SETTLE_SHEET));
    sheet.setCustomerId(vo.getCustomerId());
    sheet.setTotalAmount(vo.getSettleAmount());
    sheet.setTotalDiscountAmount(BigDecimal.ZERO);
    sheet.setDescription(StringUtil.isBlank(vo.getDescription()) ? "" : vo.getDescription());
    sheet.setRefuseReason("");
    sheet.setStatus(CustomerSettleSheetStatus.APPROVE_PASS);
    sheet.setApproveBy(getCurrentUserId());
    sheet.setApproveTime(LocalDateTime.now());
    if (getBaseMapper().insert(sheet) != 1) {
      throw new DefaultClientException("保存客户结算单失败！");
    }
    return sheet;
  }

  /**
   * 创建结算明细。
   */
  private CustomerSettleSheetDetail createDetail(String sheetId, String bizId, BigDecimal amount,
      int orderNo) {
    CustomerSettleSheetDetail detail = new CustomerSettleSheetDetail();
    detail.setId(generateId());
    detail.setSheetId(sheetId);
    detail.setBizId(bizId);
    detail.setPayAmount(amount);
    detail.setDiscountAmount(BigDecimal.ZERO);
    detail.setDescription("");
    detail.setOrderNo(orderNo);
    return detail;
  }

  /**
   * 按分摊结果回写业务单据结算状态。
   */
  private void updateBizSettleStatus(DirectSettleBiz biz, BigDecimal amount) {
    boolean settled = amount.abs().compareTo(biz.getUnSettleAmount().abs()) >= 0;
    int count;
    if (biz.getBizType() == 1) {
      count = settled ? saleOutSheetService.setSettled(biz.getBizId(), biz.getSettleStatus(),
          biz.getSettleVersion()) : saleOutSheetService.setPartSettle(biz.getBizId(),
          biz.getSettleStatus(), biz.getSettleVersion());
    } else {
      count = settled ? saleReturnService.setSettled(biz.getBizId(), biz.getSettleStatus(),
          biz.getSettleVersion()) : saleReturnService.setPartSettle(biz.getBizId(),
          biz.getSettleStatus(), biz.getSettleVersion());
    }
    if (count != 1) {
      throw new DefaultClientException("单号：" + biz.getCode() + "结算状态已变化，请刷新后重试！");
    }
  }

  /**
   * 将空金额按零处理。
   */
  private BigDecimal amountOrZero(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  /**
   * 生成结算记录ID。
   */
  private String generateId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 写入销售源单确认结算时间线。
   *
   * @param sheet 客户结算单
   * @param details 结算明细
   */
  private void recordSaleSourceSettleTimeLine(CustomerSettleSheet sheet,
      List<CustomerSettleSheetDetail> details) {
    if (CollectionUtil.isEmpty(details)) {
      return;
    }
    String description = StringUtil.isBlank(sheet.getDescription()) ? "无" : sheet.getDescription();
    String createById = getCurrentUserId();
    String createBy = getCurrentUserName();
    LocalDateTime createTime = sheet.getApproveTime() == null
        ? LocalDateTime.now() : sheet.getApproveTime();
    List<OrderTimeLine> timeLines = details.stream().map(detail -> {
      OrderTimeLine orderTimeLine = new OrderTimeLine();
      orderTimeLine.setId(generateId());
      orderTimeLine.setOrderId(detail.getBizId());
      orderTimeLine.setBizType(receiveOrderTimeLineBizType.getCode());
      orderTimeLine.setContent(String.format("确认结算，结算金额：%s，备注：%s",
          formatAmount(detail.getPayAmount()), description));
      orderTimeLine.setCreateById(createById);
      orderTimeLine.setCreateBy(createBy);
      orderTimeLine.setCreateTime(createTime);
      return orderTimeLine;
    }).collect(Collectors.toList());
    orderTimeLineService.saveBatch(timeLines);
  }

  /**
   * 获取当前用户 ID。
   *
   * @return 当前用户 ID
   */
  protected String getCurrentUserId() {
    return SecurityUtil.getCurrentUser().getId();
  }

  /**
   * 获取当前用户名称。
   *
   * @return 当前用户名称
   */
  protected String getCurrentUserName() {
    return SecurityUtil.getCurrentUser().getName();
  }

  /**
   * 将时间线金额格式化为两位小数。
   *
   * @param amount 金额
   * @return 两位小数字符串
   */
  private String formatAmount(BigDecimal amount) {
    return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  /**
   * 直接结算源单据。
   */
  private static class DirectSettleBiz {

    private final String bizId;
    private final Integer bizType;
    private final String code;
    private final String customerId;
    private final SettleStatus settleStatus;
    private final Long settleVersion;
    private final BigDecimal unSettleAmount;

    /**
     * 创建源单据结算信息。
     */
    DirectSettleBiz(String bizId, Integer bizType, String code, String customerId,
        SettleStatus settleStatus, Long settleVersion, BigDecimal unSettleAmount) {
      this.bizId = bizId;
      this.bizType = bizType;
      this.code = code;
      this.customerId = customerId;
      this.settleStatus = settleStatus;
      this.settleVersion = settleVersion;
      this.unSettleAmount = unSettleAmount;
    }

    /**
     * 获取业务单据ID。
     */
    String getBizId() {
      return bizId;
    }

    /**
     * 获取业务类型。
     */
    Integer getBizType() {
      return bizType;
    }

    /**
     * 获取业务单号。
     */
    String getCode() {
      return code;
    }

    /**
     * 获取客户ID。
     */
    String getCustomerId() {
      return customerId;
    }

    /**
     * 获取提交时的源单结算状态。
     */
    SettleStatus getSettleStatus() {
      return settleStatus;
    }

    /**
     * 获取提交时的源单结算版本号。
     */
    Long getSettleVersion() {
      return settleVersion;
    }

    /**
     * 获取当前未结算金额。
     */
    BigDecimal getUnSettleAmount() {
      return unSettleAmount;
    }
  }
}
