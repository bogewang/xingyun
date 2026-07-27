package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
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
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSettleOverviewBo;
import com.lframework.xingyun.settle.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.settle.dto.sheet.customer.CustomerSettleSheetFullDto;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheetDetail;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.entity.CustomerSettleSheetDetail;
import com.lframework.xingyun.settle.enums.CustomerSaleSettleBizType;
import com.lframework.xingyun.settle.enums.CustomerSettleCheckSheetStatus;
import com.lframework.xingyun.settle.enums.CustomerSettleSheetStatus;
import com.lframework.xingyun.settle.enums.SettleOpLogType;
import com.lframework.xingyun.settle.mappers.CustomerSettleCheckSheetDetailMapper;
import com.lframework.xingyun.settle.mappers.CustomerSettleCheckSheetMapper;
import com.lframework.xingyun.settle.mappers.CustomerSettleSheetMapper;
import com.lframework.xingyun.settle.service.CustomerSettleSheetDetailService;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.utils.SettleAmountAllocationUtil;
import com.lframework.xingyun.settle.vo.sheet.customer.ApprovePassCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.ApproveRefuseCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.CreateCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.CustomerSettleSheetItemVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleOverviewVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.UpdateCustomerSettleSheetVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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

  @Autowired
  private CustomerSettleCheckSheetDetailMapper customerSettleCheckSheetDetailMapper;

  @Autowired
  private CustomerSettleCheckSheetMapper customerSettleCheckSheetMapper;

  /**
   * 查询客户结算总览。
   *
   * @param vo 查询条件
   * @return 客户结算总览分页数据
   */
  @Override
  public PageResult<CustomerSettleOverviewBo> querySettleOverviews(
      QueryCustomerSettleOverviewVo vo) {
    int pageIndex = vo.getPageIndex() == null || vo.getPageIndex() < 1 ? 1 : vo.getPageIndex();
    int pageSize = vo.getPageSize() == null || vo.getPageSize() < 1 ? 20 : vo.getPageSize();
    Map<String, CustomerSettleOverviewBo> overviewMap = new LinkedHashMap<>();
    saleOutSheetService.query(buildSaleOutOverviewQuery(vo)).forEach(sheet ->
        accumulateOverview(overviewMap, sheet.getCustomerId(), sheet.getSettleStatus(),
            amountOrZero(sheet.getTotalAmount())));
    saleReturnService.query(buildSaleReturnOverviewQuery(vo)).forEach(sheet ->
        accumulateOverview(overviewMap, sheet.getCustomerId(), sheet.getSettleStatus(),
            amountOrZero(sheet.getTotalAmount()).negate()));
    List<CustomerSettleOverviewBo> rows = new ArrayList<>(overviewMap.values());
    fillOverviewCustomers(rows);
    rows.sort(Comparator.comparing(CustomerSettleOverviewBo::getCustomerName,
        Comparator.nullsLast(String::compareTo)).thenComparing(CustomerSettleOverviewBo::getCustomerId,
        Comparator.nullsLast(String::compareTo)));
    int start = (int) Math.min((long) (pageIndex - 1) * pageSize, rows.size());
    int end = Math.min(start + pageSize, rows.size());
    return PageResultUtil.newInstance(pageIndex, pageSize, rows.size(),
        new ArrayList<>(rows.subList(start, end)));
  }

  /**
   * 构建销售出库单总览查询条件。
   */
  private QuerySaleOutSheetVo buildSaleOutOverviewQuery(QueryCustomerSettleOverviewVo vo) {
    QuerySaleOutSheetVo query = new QuerySaleOutSheetVo();
    query.setCustomerId(vo.getCustomerId());
    query.setRequireTxIdNull(true);
    return query;
  }

  /**
   * 构建销售退货单总览查询条件。
   */
  private QuerySaleReturnVo buildSaleReturnOverviewQuery(QueryCustomerSettleOverviewVo vo) {
    QuerySaleReturnVo query = new QuerySaleReturnVo();
    query.setCustomerId(vo.getCustomerId());
    query.setRequireTxIdNull(true);
    return query;
  }

  /**
   * 将业务单据累计到客户总览中。
   */
  private void accumulateOverview(Map<String, CustomerSettleOverviewBo> overviewMap,
      String customerId, SettleStatus settleStatus, BigDecimal amount) {
    if (settleStatus != SettleStatus.UN_CHECK_BILL && settleStatus != SettleStatus.UN_SETTLE
        && settleStatus != SettleStatus.PART_SETTLE && settleStatus != SettleStatus.SETTLED) {
      return;
    }
    CustomerSettleOverviewBo overview = overviewMap.computeIfAbsent(customerId,
        this::newCustomerSettleOverview);
    if (settleStatus == SettleStatus.UN_CHECK_BILL) {
      overview.setUnCheckCount(overview.getUnCheckCount() + 1);
      overview.setUnCheckAmount(overview.getUnCheckAmount().add(amount));
    } else if (settleStatus == SettleStatus.UN_SETTLE) {
      overview.setUnSettleCount(overview.getUnSettleCount() + 1);
      overview.setUnSettleAmount(overview.getUnSettleAmount().add(amount));
    } else if (settleStatus == SettleStatus.PART_SETTLE) {
      overview.setPartSettleCount(overview.getPartSettleCount() + 1);
      overview.setPartSettleAmount(overview.getPartSettleAmount().add(amount));
    } else {
      overview.setSettledCount(overview.getSettledCount() + 1);
      overview.setSettledAmount(overview.getSettledAmount().add(amount));
    }
  }

  /**
   * 初始化客户结算总览。
   */
  private CustomerSettleOverviewBo newCustomerSettleOverview(String customerId) {
    CustomerSettleOverviewBo overview = new CustomerSettleOverviewBo();
    overview.setCustomerId(customerId);
    overview.setUnCheckCount(0);
    overview.setUnCheckAmount(BigDecimal.ZERO);
    overview.setUnSettleCount(0);
    overview.setUnSettleAmount(BigDecimal.ZERO);
    overview.setPartSettleCount(0);
    overview.setPartSettleAmount(BigDecimal.ZERO);
    overview.setSettledCount(0);
    overview.setSettledAmount(BigDecimal.ZERO);
    return overview;
  }

  /**
   * 批量补齐总览客户信息。
   */
  private void fillOverviewCustomers(List<CustomerSettleOverviewBo> rows) {
    if (CollectionUtil.isEmpty(rows)) {
      return;
    }
    Map<String, Customer> customerMap = customerService.listByIds(rows.stream()
            .map(CustomerSettleOverviewBo::getCustomerId).filter(StringUtil::isNotBlank)
            .collect(Collectors.toSet()))
        .stream().collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
    rows.forEach(row -> {
      Customer customer = customerMap.get(row.getCustomerId());
      if (customer != null) {
        row.setCustomerCode(customer.getCode());
        row.setCustomerName(customer.getName());
      }
    });
  }

  /**
   * 查询客户销售业务单据结算工作台信息。
   *
   * @param vo 查询条件
   * @return 结算工作台分页数据
   */
  @Override
  public PageResult<CustomerSaleSettleInfoBo> querySaleSettleInfos(
      QueryCustomerSaleSettleInfoVo vo) {
    if (vo.getBizType() != null && vo.getBizType() != 1 && vo.getBizType() != 2) {
      throw new DefaultClientException("业务类型不正确！");
    }
    int pageIndex = vo.getPageIndex() == null || vo.getPageIndex() < 1 ? 1 : vo.getPageIndex();
    int pageSize = vo.getPageSize() == null || vo.getPageSize() < 1 ? 20 : vo.getPageSize();
    PageResult<CustomerSaleSettleInfoBo> result;
    if (vo.getBizType() == null) {
      PageResult<CustomerSaleSettleInfoBo> saleOutResult = querySaleOutSettleInfos(pageIndex,
          pageSize, vo);
      PageResult<CustomerSaleSettleInfoBo> saleReturnResult = querySaleReturnSettleInfos(
          pageIndex, pageSize, vo);
      List<CustomerSaleSettleInfoBo> datas = new ArrayList<>(saleOutResult.getDatas());
      datas.addAll(saleReturnResult.getDatas());
      result = PageResultUtil.newInstance(pageIndex, pageSize,
          saleOutResult.getTotalCount() + saleReturnResult.getTotalCount(), datas);
    } else {
      result = vo.getBizType() == 1
          ? querySaleOutSettleInfos(pageIndex, pageSize, vo)
          : querySaleReturnSettleInfos(pageIndex, pageSize, vo);
    }
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
    saleOutVo.setSettleStatus(vo.getSettleStatus());
    saleOutVo.setOrderDateStart(vo.getOrderDateStart());
    saleOutVo.setOrderDateEnd(vo.getOrderDateEnd());
    saleOutVo.setRequireTxIdNull(true);
    PageResult<SaleOutSheet> pageResult = saleOutSheetService.query(pageIndex, pageSize, saleOutVo);
    List<CustomerSaleSettleInfoBo> results = pageResult.getDatas().stream()
        .map(sheet -> buildSettleInfo(sheet.getId(), 1, sheet.getCode(), sheet.getCustomerId(),
            sheet.getTotalAmount(), sheet.getPaidAmount(), getSettleStatusCode(sheet.getSettleStatus()),
            sheet.getOrderDate() == null && sheet.getCreateTime() != null
                ? sheet.getCreateTime().toLocalDate() : sheet.getOrderDate(), sheet.getDescription()))
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
    saleReturnVo.setSettleStatus(vo.getSettleStatus());
    saleReturnVo.setCreateStartTime(vo.getOrderDateStart() == null ? null : vo.getOrderDateStart().atStartOfDay());
    saleReturnVo.setCreateEndTime(vo.getOrderDateEnd() == null ? null : vo.getOrderDateEnd().plusDays(1).atStartOfDay().minusNanos(1));
    saleReturnVo.setRequireTxIdNull(true);
    PageResult<SaleReturn> pageResult = saleReturnService.query(pageIndex, pageSize, saleReturnVo);
    List<CustomerSaleSettleInfoBo> results = pageResult.getDatas().stream()
        .map(sheet -> buildSettleInfo(sheet.getId(), 2, sheet.getCode(), sheet.getCustomerId(),
            amountOrZero(sheet.getTotalAmount()).negate(), BigDecimal.ZERO,
            getSettleStatusCode(sheet.getSettleStatus()), sheet.getCreateTime() == null
                ? null : sheet.getCreateTime().toLocalDate(), sheet.getDescription()))
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
      String customerId, BigDecimal totalAmount, BigDecimal receivedAmount, Integer settleStatus,
      LocalDate orderDate, String description) {
    CustomerSaleSettleInfoBo result = new CustomerSaleSettleInfoBo();
    result.setId(id);
    result.setBizType(bizType);
    result.setCode(code);
    result.setCustomerId(customerId);
    result.setOrderDate(orderDate);
    result.setDescription(description);
    result.setTotalAmount(amountOrZero(totalAmount));
    result.setReceivedAmount(amountOrZero(receivedAmount));
    result.setSettleStatus(settleStatus);
    return result;
  }

  /**
   * 批量填充工作台结算金额、对账金额。
   */
  private void fillSettleAmounts(List<CustomerSaleSettleInfoBo> results) {
    if (CollectionUtil.isEmpty(results)) {
      return;
    }
    Map<String, String> customerNameMap = customerService.listByIds(results.stream()
            .map(CustomerSaleSettleInfoBo::getCustomerId).filter(StringUtil::isNotBlank)
            .collect(Collectors.toSet()))
        .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
    List<String> bizIds = results.stream()
        .map(CustomerSaleSettleInfoBo::getId).collect(Collectors.toList());
    // 查询已审核结算明细汇总
    Map<String, BigDecimal> settleAmountMap = querySettleAmountMap(bizIds);
    Map<String, String> settleDescriptionMap = querySettleDescriptionMap(bizIds);
    Map<String, LocalDateTime> settleTimeMap = querySettleTimeMap(bizIds);
    // 查询对账明细
    Map<String, CustomerSettleCheckSheetDetail> checkDetailMap = queryCheckDetailMap(bizIds);
    // 查询对账单
    Map<String, CustomerSettleCheckSheet> checkSheetMap = queryCheckSheetMap(
        new ArrayList<>(checkDetailMap.values()));
    results.forEach(item -> {
      item.setCustomerName(customerNameMap.get(item.getCustomerId()));
      BigDecimal settleAmount = settleAmountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
      item.setSettleAmount(settleAmount);
      item.setSettleDescription(settleDescriptionMap.get(item.getId()));
      item.setSettleTime(settleTimeMap.get(item.getId()));
      // 填充对账信息
      fillCheckInfo(item, checkDetailMap.get(item.getId()), checkSheetMap);
      // 计算未结算金额：如果有对账金额，以对账金额为基数；否则以单据金额为基数
      BigDecimal baseAmount = item.getCheckAmount() != null
          ? item.getCheckAmount() : amountOrZero(item.getTotalAmount());
      BigDecimal unSettleAmount = baseAmount.subtract(amountOrZero(item.getReceivedAmount()))
          .subtract(settleAmount);
      if (item.getSettleStatus() != null
          && item.getSettleStatus() == SettleStatus.SETTLED.getCode()) {
        unSettleAmount = BigDecimal.ZERO;
      }
      item.setUnSettleAmount(unSettleAmount);
    });
  }

  /**
   * 填充单条工作台记录的对账信息。
   */
  private void fillCheckInfo(CustomerSaleSettleInfoBo result,
      CustomerSettleCheckSheetDetail checkDetail,
      Map<String, CustomerSettleCheckSheet> checkSheetMap) {
    if (checkDetail == null) {
      return;
    }
    result.setCheckAmount(checkDetail.getPayAmount());
    CustomerSettleCheckSheet checkSheet = checkSheetMap.get(checkDetail.getSheetId());
    if (checkSheet != null) {
      result.setCheckTime(checkSheet.getApproveTime());
      if (StringUtil.isNotBlank(checkSheet.getDescription())) {
        result.setCheckDescription(checkSheet.getDescription());
      }
    }
  }

  /**
   * 按业务单据ID批量查询对账单明细。
   *
   * @param bizIds 业务单据ID
   * @return bizId → 对账单明细
   */
  private Map<String, CustomerSettleCheckSheetDetail> queryCheckDetailMap(List<String> bizIds) {
    if (CollectionUtil.isEmpty(bizIds)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleCheckSheetDetail> details = customerSettleCheckSheetDetailMapper.selectList(
        Wrappers.lambdaQuery(CustomerSettleCheckSheetDetail.class)
            .in(CustomerSettleCheckSheetDetail::getBizId, bizIds));
    if (CollectionUtil.isEmpty(details)) {
      return Collections.emptyMap();
    }
    return details.stream().collect(Collectors.toMap(
        CustomerSettleCheckSheetDetail::getBizId, Function.identity(), (a, b) -> a));
  }

  /**
   * 按对账单明细批量查询对账单。
   *
   * @param details 对账单明细
   * @return sheetId → 对账单
   */
  private Map<String, CustomerSettleCheckSheet> queryCheckSheetMap(
      List<CustomerSettleCheckSheetDetail> details) {
    if (CollectionUtil.isEmpty(details)) {
      return Collections.emptyMap();
    }
    List<String> sheetIds = details.stream()
        .map(CustomerSettleCheckSheetDetail::getSheetId)
        .distinct()
        .collect(Collectors.toList());
    if (CollectionUtil.isEmpty(sheetIds)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleCheckSheet> sheets = customerSettleCheckSheetMapper.selectBatchIds(sheetIds);
    if (CollectionUtil.isEmpty(sheets)) {
      return Collections.emptyMap();
    }
    return sheets.stream().collect(Collectors.toMap(
        CustomerSettleCheckSheet::getId, Function.identity(), (a, b) -> a));
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

  /** 按业务单据查询最近一张审核通过结算单的备注。 */
  private Map<String, String> querySettleDescriptionMap(Collection<String> bizIds) {
    if (CollectionUtil.isEmpty(bizIds)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleSheetDetail> details = customerSettleSheetDetailService.list(
        Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
            .in(CustomerSettleSheetDetail::getBizId, bizIds));
    if (CollectionUtil.isEmpty(details)) {
      return Collections.emptyMap();
    }
    Map<String, CustomerSettleSheet> sheetMap = getBaseMapper().selectBatchIds(details.stream()
            .map(CustomerSettleSheetDetail::getSheetId).collect(Collectors.toSet()))
        .stream().filter(sheet -> sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS)
        .collect(Collectors.toMap(CustomerSettleSheet::getId, Function.identity(), (a, b) -> a));
    return details.stream().filter(detail -> sheetMap.containsKey(detail.getSheetId()))
        .collect(Collectors.toMap(CustomerSettleSheetDetail::getBizId,
            detail -> sheetMap.get(detail.getSheetId()).getDescription(), (a, b) -> b));
  }

  /** 按业务单据查询最近一张审核通过结算单的审核时间。 */
  private Map<String, LocalDateTime> querySettleTimeMap(Collection<String> bizIds) {
    if (CollectionUtil.isEmpty(bizIds)) {
      return Collections.emptyMap();
    }
    List<CustomerSettleSheetDetail> details = customerSettleSheetDetailService.list(
        Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
            .in(CustomerSettleSheetDetail::getBizId, bizIds));
    if (CollectionUtil.isEmpty(details)) {
      return Collections.emptyMap();
    }
    Map<String, CustomerSettleSheet> sheetMap = getBaseMapper().selectBatchIds(details.stream()
            .map(CustomerSettleSheetDetail::getSheetId).collect(Collectors.toSet()))
        .stream().filter(sheet -> sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS)
        .collect(Collectors.toMap(CustomerSettleSheet::getId, Function.identity(), (a, b) -> a));
    return details.stream().filter(detail -> sheetMap.containsKey(detail.getSheetId()))
        .collect(Collectors.toMap(CustomerSettleSheetDetail::getBizId,
            detail -> sheetMap.get(detail.getSheetId()).getApproveTime(), (a, b) -> b));
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
    List<CustomerSettleSheet> sheets = query(vo);
    fillDetailCounts(sheets);
    return PageResultUtil.convert(new PageInfo<>(sheets));
  }

  /**
   * 批量填充客户结算单关联业务单据数量。
   *
   * @param sheets 客户结算单列表
   */
  private void fillDetailCounts(List<CustomerSettleSheet> sheets) {
    if (CollectionUtil.isEmpty(sheets)) {
      return;
    }
    List<String> sheetIds = sheets.stream().map(CustomerSettleSheet::getId)
        .collect(Collectors.toList());
    Map<String, Long> countMap = customerSettleSheetDetailService.list(
            Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
                .in(CustomerSettleSheetDetail::getSheetId, sheetIds))
        .stream().collect(Collectors.groupingBy(CustomerSettleSheetDetail::getSheetId,
            Collectors.counting()));
    sheets.forEach(sheet -> sheet.setDetailCount(countMap.getOrDefault(sheet.getId(), 0L).intValue()));
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
   * 校验确认金额与所选业务净额方向一致。
   *
   * @param confirmedAmount 确认金额
   * @param totalUnSettleAmount 未结算净额
   */
  private void validateConfirmedAmount(BigDecimal confirmedAmount,
      BigDecimal totalUnSettleAmount) {
    if (totalUnSettleAmount.compareTo(BigDecimal.ZERO) == 0
        || confirmedAmount.signum() != totalUnSettleAmount.signum()) {
      throw new DefaultClientException("确认结算金额与所选单据未结算净额方向不一致！");
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
   * 创建客户结算单。
   *
   * @param vo 结算单创建请求
   * @return 结算单ID
   */
  @OpLog(type = SettleOpLogType.class, name = "创建客户结算单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建结算单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String create(CreateCustomerSettleSheetVo vo) {
    // 校验源单据合法性（存在性、归属、状态、金额方向等）
    validateDirectSettle(vo);

    CustomerSettleSheet sheet = new CustomerSettleSheet();
    sheet.setId(generateId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.CUSTOMER_SETTLE_SHEET));
    sheet.setStatus(CustomerSettleSheetStatus.CREATED);

    doCreate(sheet, vo);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);

    getBaseMapper().insert(sheet);
    return sheet.getId();
  }

  /**
   * 修改客户结算单。
   *
   * @param vo 结算单修改请求
   */
  @OpLog(type = SettleOpLogType.class, name = "修改客户结算单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改结算单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void update(UpdateCustomerSettleSheetVo vo) {
    CustomerSettleSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("客户结算单不存在！");
    }
    if (sheet.getStatus() != CustomerSettleSheetStatus.CREATED
        && sheet.getStatus() != CustomerSettleSheetStatus.APPROVE_REFUSE) {
      if (sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("客户结算单已审核通过，无法修改！");
      }
      throw new DefaultClientException("客户结算单无法修改！");
    }

    // 将原有明细的业务单据恢复为未结算
    Wrapper<CustomerSettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
            CustomerSettleSheetDetail.class)
        .eq(CustomerSettleSheetDetail::getSheetId, sheet.getId())
        .orderByAsc(CustomerSettleSheetDetail::getOrderNo);
    List<CustomerSettleSheetDetail> oldDetails = customerSettleSheetDetailService.list(
        queryDetailWrapper);
    for (CustomerSettleSheetDetail detail : oldDetails) {
      setBizItemUnSettle(detail.getBizId());
    }

    // 删除旧明细
    customerSettleSheetDetailService.remove(
        Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
            .eq(CustomerSettleSheetDetail::getSheetId, sheet.getId()));

    doCreate(sheet, vo);

    sheet.setStatus(CustomerSettleSheetStatus.CREATED);

    List<CustomerSettleSheetStatus> statusList = new ArrayList<>();
    statusList.add(CustomerSettleSheetStatus.CREATED);
    statusList.add(CustomerSettleSheetStatus.APPROVE_REFUSE);

    Wrapper<CustomerSettleSheet> updateWrapper = Wrappers.lambdaUpdate(CustomerSettleSheet.class)
        .set(CustomerSettleSheet::getApproveBy, null)
        .set(CustomerSettleSheet::getApproveTime, null)
        .set(CustomerSettleSheet::getRefuseReason, StringPool.EMPTY_STR)
        .eq(CustomerSettleSheet::getId, sheet.getId())
        .in(CustomerSettleSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("客户结算单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  /**
   * 审核通过客户结算单。
   *
   * @param vo 审核通过请求
   */
  @OpLog(type = SettleOpLogType.class, name = "审核通过客户结算单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id",
      name = "审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approvePass(ApprovePassCustomerSettleSheetVo vo) {
    CustomerSettleSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("客户结算单不存在！");
    }
    if (sheet.getStatus() != CustomerSettleSheetStatus.CREATED
        && sheet.getStatus() != CustomerSettleSheetStatus.APPROVE_REFUSE) {
      if (sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("客户结算单已审核通过，不允许继续执行审核！");
      }
      throw new DefaultClientException("客户结算单无法审核通过！");
    }

    sheet.setStatus(CustomerSettleSheetStatus.APPROVE_PASS);
    sheet.setApproveBy(getCurrentUserId());
    sheet.setApproveTime(LocalDateTime.now());
    if (!StringUtil.isBlank(vo.getDescription())) {
      sheet.setDescription(vo.getDescription());
    }

    List<CustomerSettleSheetStatus> statusList = new ArrayList<>();
    statusList.add(CustomerSettleSheetStatus.CREATED);
    statusList.add(CustomerSettleSheetStatus.APPROVE_REFUSE);

    Wrapper<CustomerSettleSheet> updateWrapper = Wrappers.lambdaUpdate(CustomerSettleSheet.class)
        .eq(CustomerSettleSheet::getId, sheet.getId())
        .in(CustomerSettleSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("客户结算单信息已过期，请刷新重试！");
    }

    // 更新业务单据结算状态为已结算
    Wrapper<CustomerSettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
            CustomerSettleSheetDetail.class)
        .eq(CustomerSettleSheetDetail::getSheetId, sheet.getId())
        .orderByAsc(CustomerSettleSheetDetail::getOrderNo);
    List<CustomerSettleSheetDetail> details = customerSettleSheetDetailService.list(
        queryDetailWrapper);
    for (CustomerSettleSheetDetail detail : details) {
      setBizItemSettled(detail.getBizId());
    }

    recordSaleSourceSettleTimeLine(sheet, details);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  /**
   * 直接审核通过客户结算单（创建并审核）。
   *
   * @param vo 结算单创建请求
   * @return 结算单ID
   */
  @OpLog(type = SettleOpLogType.class, name = "直接审核通过客户结算单，ID：{}",
      params = "#_result", autoSaveParams = true)
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result",
      name = "直接审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String directApprovePass(CreateCustomerSettleSheetVo vo) {
    // 校验源单据合法性（存在性、归属、状态、金额方向等），同时加载 DirectSettleBiz 列表
    List<DirectSettleBiz> bizItems = validateDirectSettle(vo);
    BigDecimal totalUnSettleAmount = bizItems.stream().map(DirectSettleBiz::getUnSettleAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    validateConfirmedAmount(vo.getSettleAmount(), totalUnSettleAmount);
    List<BigDecimal> amounts = SettleAmountAllocationUtil.allocateSigned(vo.getSettleAmount(),
        bizItems.stream().map(DirectSettleBiz::getUnSettleAmount).collect(Collectors.toList()));

    // 创建已审核的结算单
    CustomerSettleSheet sheet = new CustomerSettleSheet();
    sheet.setId(generateId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.CUSTOMER_SETTLE_SHEET));
    sheet.setCustomerId(vo.getCustomerId());
    sheet.setTotalAmount(vo.getSettleAmount());
    sheet.setTotalDiscountAmount(BigDecimal.ZERO);
    sheet.setDescription(StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR
        : vo.getDescription());
    sheet.setRefuseReason(StringPool.EMPTY_STR);
    sheet.setStatus(CustomerSettleSheetStatus.APPROVE_PASS);
    sheet.setApproveBy(getCurrentUserId());
    sheet.setApproveTime(LocalDateTime.now());
    if (getBaseMapper().insert(sheet) != 1) {
      throw new DefaultClientException("保存客户结算单失败！");
    }

    // 保存明细并回写业务单据结算状态
    List<CustomerSettleSheetDetail> details = new ArrayList<>();
    for (int index = 0; index < bizItems.size(); index++) {
      DirectSettleBiz biz = bizItems.get(index);
      BigDecimal amount = amounts.get(index);
      boolean settled = amount.abs().compareTo(biz.getUnSettleAmount().abs()) >= 0;
      if (biz.getBizType() == 1) {
        int count = settled
            ? saleOutSheetService.setSettled(biz.getBizId(), biz.getSettleStatus(),
                biz.getSettleVersion())
            : saleOutSheetService.setPartSettle(biz.getBizId(), biz.getSettleStatus(),
                biz.getSettleVersion());
        if (count != 1) {
          throw new DefaultClientException("单号：" + biz.getCode() + "结算状态已变化，请刷新后重试！");
        }
      } else {
        int count = settled
            ? saleReturnService.setSettled(biz.getBizId(), biz.getSettleStatus(),
                biz.getSettleVersion())
            : saleReturnService.setPartSettle(biz.getBizId(), biz.getSettleStatus(),
                biz.getSettleVersion());
        if (count != 1) {
          throw new DefaultClientException("单号：" + biz.getCode() + "结算状态已变化，请刷新后重试！");
        }
      }
      CustomerSettleSheetDetail detail = new CustomerSettleSheetDetail();
      detail.setId(generateId());
      detail.setSheetId(sheet.getId());
      detail.setBizId(biz.getBizId());
      detail.setPayAmount(amount);
      detail.setDiscountAmount(BigDecimal.ZERO);
      detail.setDescription(StringPool.EMPTY_STR);
      detail.setOrderNo(index + 1);
      details.add(detail);
    }
    if (!customerSettleSheetDetailService.saveBatch(details)) {
      throw new DefaultClientException("保存客户结算单明细失败！");
    }
    recordSaleSourceSettleTimeLine(sheet, details);
    return sheet.getId();
  }

  /**
   * 审核拒绝客户结算单。
   *
   * @param vo 审核拒绝请求
   */
  @OpLog(type = SettleOpLogType.class, name = "审核拒绝客户结算单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id",
      name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approveRefuse(ApproveRefuseCustomerSettleSheetVo vo) {
    CustomerSettleSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new DefaultClientException("客户结算单不存在！");
    }
    if (sheet.getStatus() != CustomerSettleSheetStatus.CREATED) {
      if (sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("客户结算单已审核通过，不允许继续执行审核！");
      }
      if (sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_REFUSE) {
        throw new DefaultClientException("客户结算单已审核拒绝，不允许继续执行审核！");
      }
      throw new DefaultClientException("客户结算单无法审核拒绝！");
    }

    sheet.setStatus(CustomerSettleSheetStatus.APPROVE_REFUSE);
    sheet.setApproveBy(getCurrentUserId());
    sheet.setApproveTime(LocalDateTime.now());
    sheet.setRefuseReason(vo.getRefuseReason());

    List<CustomerSettleSheetStatus> statusList = new ArrayList<>();
    statusList.add(CustomerSettleSheetStatus.CREATED);
    statusList.add(CustomerSettleSheetStatus.APPROVE_REFUSE);

    Wrapper<CustomerSettleSheet> updateWrapper = Wrappers.lambdaUpdate(CustomerSettleSheet.class)
        .eq(CustomerSettleSheet::getId, sheet.getId())
        .in(CustomerSettleSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
      throw new DefaultClientException("客户结算单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  /**
   * 删除客户结算单。
   *
   * @param id 结算单ID
   */
  @OpLog(type = SettleOpLogType.class, name = "删除客户结算单，单号：{}", params = "#code")
  @OrderTimeLineLog(orderId = "#id", delete = true)
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteById(String id) {
    Assert.notBlank(id);
    CustomerSettleSheet sheet = getBaseMapper().selectById(id);
    if (sheet == null) {
      throw new InputErrorException("客户结算单不存在！");
    }
    if (sheet.getStatus() != CustomerSettleSheetStatus.CREATED
        && sheet.getStatus() != CustomerSettleSheetStatus.APPROVE_REFUSE) {
      if (sheet.getStatus() == CustomerSettleSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("审核通过的客户结算单不允许执行删除操作！");
      }
      throw new DefaultClientException("客户结算单无法删除！");
    }

    // 将业务单据结算状态恢复为未结算
    Wrapper<CustomerSettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
            CustomerSettleSheetDetail.class)
        .eq(CustomerSettleSheetDetail::getSheetId, sheet.getId())
        .orderByAsc(CustomerSettleSheetDetail::getOrderNo);
    List<CustomerSettleSheetDetail> details = customerSettleSheetDetailService.list(
        queryDetailWrapper);
    for (CustomerSettleSheetDetail detail : details) {
      setBizItemUnSettle(detail.getBizId());
    }

    // 删除明细
    customerSettleSheetDetailService.remove(
        Wrappers.lambdaQuery(CustomerSettleSheetDetail.class)
            .eq(CustomerSettleSheetDetail::getSheetId, sheet.getId()));

    // 删除主单
    Wrapper<CustomerSettleSheet> deleteWrapper = Wrappers.lambdaUpdate(CustomerSettleSheet.class)
        .eq(CustomerSettleSheet::getId, id)
        .in(CustomerSettleSheet::getStatus, CustomerSettleSheetStatus.CREATED,
            CustomerSettleSheetStatus.APPROVE_REFUSE);
    if (!remove(deleteWrapper)) {
      throw new DefaultClientException("客户结算单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
  }

  /**
   * 更新业务单据为未结算状态。
   *
   * @param bizId 业务单据ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void setBizItemUnSettle(String bizId) {
    SaleOutSheet saleOutSheet = saleOutSheetService.getById(bizId);
    if (saleOutSheet != null) {
      saleOutSheetService.setUnSettle(bizId);
      return;
    }
    SaleReturn saleReturn = saleReturnService.getById(bizId);
    if (saleReturn != null) {
      saleReturnService.setUnSettle(bizId);
      return;
    }
    throw new InputErrorException("业务单据不存在！");
  }

  /**
   * 更新业务单据为部分结算状态。
   *
   * @param bizId 业务单据ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void setBizItemPartSettle(String bizId) {
    SaleOutSheet saleOutSheet = saleOutSheetService.getById(bizId);
    if (saleOutSheet != null) {
      saleOutSheetService.setPartSettle(bizId, saleOutSheet.getSettleStatus(),
          saleOutSheet.getSettleVersion());
      return;
    }
    SaleReturn saleReturn = saleReturnService.getById(bizId);
    if (saleReturn != null) {
      saleReturnService.setPartSettle(bizId, saleReturn.getSettleStatus(),
          saleReturn.getSettleVersion());
      return;
    }
    throw new InputErrorException("业务单据不存在！");
  }

  /**
   * 更新业务单据为已结算状态。
   *
   * @param bizId 业务单据ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void setBizItemSettled(String bizId) {
    SaleOutSheet saleOutSheet = saleOutSheetService.getById(bizId);
    if (saleOutSheet != null) {
      saleOutSheetService.setSettled(bizId, saleOutSheet.getSettleStatus(),
          saleOutSheet.getSettleVersion());
      return;
    }
    SaleReturn saleReturn = saleReturnService.getById(bizId);
    if (saleReturn != null) {
      saleReturnService.setSettled(bizId, saleReturn.getSettleStatus(),
          saleReturn.getSettleVersion());
      return;
    }
    throw new InputErrorException("业务单据不存在！");
  }

  /**
   * 执行结算金额分摊（支持销售出库和退货混合场景的含负数分配）。
   *
   * @param vo 结算请求
   */
  private void allocateSettleAmount(CreateCustomerSettleSheetVo vo) {
    if (vo.getSettleAmount() == null || CollectionUtil.isEmpty(vo.getItems())) {
      return;
    }
    List<BigDecimal> amounts = SettleAmountAllocationUtil.allocateSigned(vo.getSettleAmount(),
        vo.getItems().stream().map(
                item -> item.getUnSettleAmount() == null ? BigDecimal.ZERO : item.getUnSettleAmount())
            .collect(Collectors.toList()));
    for (int index = 0; index < vo.getItems().size(); index++) {
      vo.getItems().get(index).setSettleAmount(amounts.get(index));
    }
  }

  /**
   * 构建结算单明细和主单。
   *
   * @param sheet 结算单
   * @param vo 结算请求
   */
  private void doCreate(CustomerSettleSheet sheet, CreateCustomerSettleSheetVo vo) {
    // 分配结算金额
    allocateSettleAmount(vo);
    BigDecimal totalUnSettleAmt = vo.getItems().stream()
        .map(item -> item.getUnSettleAmount() == null ? BigDecimal.ZERO : item.getUnSettleAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCheckAmt = vo.getItems().stream()
        .map(item -> item.getCheckAmount() == null ? BigDecimal.ZERO : item.getCheckAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    int orderNo = 1;
    for (CustomerSettleSheetItemVo itemVo : vo.getItems()) {
      CustomerSettleSheetDetail detail = buildDetail(sheet, itemVo, orderNo);
      customerSettleSheetDetailService.save(detail);
      // 回写业务单据部分结算状态
      if (itemVo.getSettleAmount() != null
          && itemVo.getUnSettleAmount() != null
          && itemVo.getSettleAmount().abs().compareTo(itemVo.getUnSettleAmount().abs()) < 0) {
        setBizItemPartSettle(itemVo.getBizId());
      }
      orderNo++;
    }

    buildSettleSheet(sheet, vo, totalUnSettleAmt, totalCheckAmt);
  }

  /**
   * 构建结算单明细。
   */
  private CustomerSettleSheetDetail buildDetail(CustomerSettleSheet sheet,
      CustomerSettleSheetItemVo itemVo, int orderNo) {
    CustomerSettleSheetDetail detail = new CustomerSettleSheetDetail();
    detail.setId(generateId());
    detail.setSheetId(sheet.getId());
    detail.setBizId(itemVo.getBizId());
    detail.setPayAmount(itemVo.getSettleAmount());
    detail.setDiscountAmount(BigDecimal.ZERO);
    detail.setDescription(StringPool.EMPTY_STR);
    detail.setOrderNo(orderNo);
    return detail;
  }

  /**
   * 构建结算单主记录。
   */
  private void buildSettleSheet(CustomerSettleSheet sheet, CreateCustomerSettleSheetVo vo,
      BigDecimal totalUnSettleAmt, BigDecimal totalCheckAmt) {
    sheet.setCustomerId(vo.getCustomerId());
    sheet.setTotalAmount(vo.getSettleAmount());
    sheet.setTotalDiscountAmount(BigDecimal.ZERO);
    sheet.setDescription(StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR
        : vo.getDescription());
    sheet.setRefuseReason(StringPool.EMPTY_STR);
    sheet.setStartDate(vo.getStartDate());
    sheet.setEndDate(vo.getEndDate());
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
