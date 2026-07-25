package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
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
    PageResult<SaleReturn> pageResult = saleReturnService.query(pageIndex, pageSize, saleReturnVo);
    List<CustomerSaleSettleInfoBo> results = pageResult.getDatas().stream()
        .map(sheet -> buildSettleInfo(sheet.getId(), 2, sheet.getCode(), sheet.getCustomerId(),
            sheet.getTotalAmount(), BigDecimal.ZERO, getSettleStatusCode(sheet.getSettleStatus())))
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
          .subtract(settleAmount).max(BigDecimal.ZERO));
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
        bizTypeMap.put(item.getId(), 1);
      });
    }
    List<SaleReturn> saleReturns = saleReturnService.listByIds(bizIds);
    if (!CollectionUtil.isEmpty(saleReturns)) {
      saleReturns.forEach(item -> {
        bizCodeMap.put(item.getId(), item.getCode());
        bizTypeMap.put(item.getId(), 2);
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
  public String directApprovePass(CreateCustomerSettleSheetVo vo) {
    List<DirectSettleBiz> bizItems = validateDirectSettle(vo);
    BigDecimal totalUnSettleAmount = bizItems.stream().map(DirectSettleBiz::getUnSettleAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (vo.getSettleAmount().compareTo(totalUnSettleAmount) > 0) {
      throw new DefaultClientException("确认结算金额不能大于所选单据的未结算总额！");
    }
    List<BigDecimal> amounts = SettleAmountAllocationUtil.allocate(vo.getSettleAmount(), bizItems
        .stream().map(DirectSettleBiz::getUnSettleAmount).collect(Collectors.toList()));

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
    return sheet.getId();
  }

  /**
   * 校验直接结算请求，并批量加载源单据。
   */
  private List<DirectSettleBiz> validateDirectSettle(CreateCustomerSettleSheetVo vo) {
    if (vo == null || StringUtil.isBlank(vo.getCustomerId())) {
      throw new DefaultClientException("客户不能为空！");
    }
    if (vo.getSettleAmount() == null || vo.getSettleAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new DefaultClientException("确认结算金额必须大于0！");
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
      if (biz.getUnSettleAmount().compareTo(BigDecimal.ZERO) <= 0) {
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
    List<SaleOutSheet> sheets = saleOutSheetService.listByIds(ids);
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
    List<SaleReturn> sheets = saleReturnService.listByIds(ids);
    return sheets == null ? Collections.emptyMap() : sheets.stream()
        .collect(Collectors.toMap(SaleReturn::getId, sheet -> sheet, (a, b) -> a));
  }

  /**
   * 构建销售出库单结算校验数据。
   */
  private DirectSettleBiz buildSaleOutBiz(CustomerSettleSheetItemVo item, SaleOutSheet sheet,
      Map<String, BigDecimal> settledAmountMap) {
    if (sheet == null) {
      throw new DefaultClientException("销售出库单不存在！");
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
      throw new DefaultClientException("销售退货单不存在！");
    }
    validateSettleStatus(sheet.getCode(), sheet.getSettleStatus());
    return new DirectSettleBiz(item.getBizId(), item.getBizType(), sheet.getCode(),
        sheet.getCustomerId(), sheet.getSettleStatus(), sheet.getSettleVersion(),
        amountOrZero(sheet.getTotalAmount()).subtract(
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
    boolean settled = amount.compareTo(biz.getUnSettleAmount()) >= 0;
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
