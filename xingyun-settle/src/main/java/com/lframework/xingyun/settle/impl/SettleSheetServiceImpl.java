package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.*;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.*;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.xingyun.sc.bo.purchase.receive.QueryReceiveSheetBo;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.settle.bo.sheet.ReceiveSheetSettleInfoBo;
import com.lframework.xingyun.settle.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.settle.dto.sheet.SettleBizItemDto;
import com.lframework.xingyun.settle.dto.sheet.SettleSheetFullDto;
import com.lframework.xingyun.settle.entity.SettleCheckSheet;
import com.lframework.xingyun.settle.entity.SettleCheckSheetDetail;
import com.lframework.xingyun.settle.entity.SettleSheet;
import com.lframework.xingyun.settle.entity.SettleSheetDetail;
import com.lframework.xingyun.settle.enums.SettleOpLogType;
import com.lframework.xingyun.settle.enums.SettleSheetStatus;
import com.lframework.xingyun.settle.mappers.SettleSheetMapper;
import com.lframework.xingyun.settle.service.SettleCheckSheetDetailService;
import com.lframework.xingyun.settle.service.SettleCheckSheetService;
import com.lframework.xingyun.settle.service.SettleSheetDetailService;
import com.lframework.xingyun.settle.service.SettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SettleSheetServiceImpl extends BaseMpServiceImpl<SettleSheetMapper, SettleSheet>
        implements SettleSheetService {

    @Autowired
    private SettleSheetDetailService settleSheetDetailService;

    @Autowired
    private SettleCheckSheetDetailService settleCheckSheetDetailService;

    @Autowired
    private GenerateCodeService generateCodeService;

    @Autowired
    private ReceiveSheetService receiveSheetService;

    @Autowired
    private SettleCheckSheetService settleCheckSheetService;


    @Override
    public PageResult<SettleSheet> query(Integer pageIndex, Integer pageSize, QuerySettleSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SettleSheet> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<SettleSheet> query(QuerySettleSheetVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public List<ReceiveSheetSettleInfoBo> queryReceiveSheetSettleInfos(List<ReceiveSheet> sheets) {

        if (CollectionUtil.isEmpty(sheets)) {
            return CollectionUtil.emptyList();
        }

        List<QueryReceiveSheetBo> sheetBoList = sheets.stream().map(QueryReceiveSheetBo::new).collect(Collectors.toList());
        List<String> ids = sheetBoList.stream().map(QueryReceiveSheetBo::getId).collect(Collectors.toList());
        // 对账单详情；
        Map<String, SettleCheckSheetDetail> checkDetailMap = queryCheckDetailMap(sheetBoList);
        Map<String, SettleCheckSheet> checkSheetMap = queryCheckMap(Lists.newArrayList(checkDetailMap.values()));

        // 结算单详情；
        Map<String, List<SettleSheetDetail>> settleDetailMap = querySettleDetailMap(ids);
        Map<String, SettleSheet> settleSheetMap = querySettleSheetMap(Lists.newArrayList(settleDetailMap.values())
                .stream().flatMap(Collection::stream).collect(Collectors.toList()));

        return sheetBoList.stream()
                .map(item -> buildReceiveSheetSettleInfo(item, checkDetailMap, checkSheetMap, settleDetailMap, settleSheetMap))
                .collect(Collectors.toList());
    }

    private Map<String, SettleCheckSheet> queryCheckMap(List<SettleCheckSheetDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return CollectionUtil.emptyMap();
        }

        List<String> sheetIds = details.stream().map(SettleCheckSheetDetail::getSheetId).collect(Collectors.toList());

        List<SettleCheckSheet> checkSheets = settleCheckSheetService.selectBatchIds(sheetIds);

        return checkSheets.stream()
                .collect(Collectors.toMap(SettleCheckSheet::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 查询对账单详情映射
     *
     * @param receiveSheets
     * @return
     */
    private Map<String, SettleCheckSheetDetail> queryCheckDetailMap(List<QueryReceiveSheetBo> receiveSheets) {

        List<String> checkDetailIds = receiveSheets.stream()
                .map(QueryReceiveSheetBo::getSettleCheckSheetDetailId)
                .filter(StringUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(checkDetailIds)) {
            return new HashMap<>();
        }

        return settleCheckSheetDetailService.listByIds(checkDetailIds).stream()
                .collect(Collectors.toMap(SettleCheckSheetDetail::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, List<SettleSheetDetail>> querySettleDetailMap(List<String> ids) {

        Wrapper<SettleSheetDetail> queryWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .in(SettleSheetDetail::getBizId, ids);
        // 按业务单据分组，便于后续逐个收货单汇总结算金额和备注。
        return settleSheetDetailService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(SettleSheetDetail::getBizId));
    }

    private Map<String, SettleSheet> querySettleSheetMap(List<SettleSheetDetail> details) {

        if (CollectionUtil.isEmpty(details)) {
            return CollectionUtil.emptyMap();
        }

        List<String> sheetIds = details.stream().map(SettleSheetDetail::getSheetId).distinct().collect(Collectors.toList());
        return this.listByIds(sheetIds).stream()
                .collect(Collectors.toMap(SettleSheet::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 构建收货单结算信息
     *
     * @param receiveSheet
     * @param checkDetailMap
     * @param checkSheetMap
     * @param settleDetailMap
     * @return
     */
    private ReceiveSheetSettleInfoBo buildReceiveSheetSettleInfo(QueryReceiveSheetBo receiveSheet,
                                                                 Map<String, SettleCheckSheetDetail> checkDetailMap,
                                                                 Map<String, SettleCheckSheet> checkSheetMap,
                                                                 Map<String, List<SettleSheetDetail>> settleDetailMap,
                                                                 Map<String, SettleSheet> settleSheetMap) {

        ReceiveSheetSettleInfoBo result = BeanUtil.copyProperties(receiveSheet, ReceiveSheetSettleInfoBo.class);
        result.setBizSheetId(receiveSheet.getId());

        fillCheckInfo(result, receiveSheet, checkDetailMap, checkSheetMap);
        fillSettleInfo(result, settleDetailMap.get(receiveSheet.getId()), settleSheetMap);

        return result;
    }

    /**
     * 填充对账信息
     *
     * @param result
     * @param receiveSheet
     * @param checkDetailMap
     * @param checkSheetMap
     */
    private void fillCheckInfo(ReceiveSheetSettleInfoBo result,
                               QueryReceiveSheetBo receiveSheet,
                               Map<String, SettleCheckSheetDetail> checkDetailMap,
                               Map<String, SettleCheckSheet> checkSheetMap) {

        if (StringUtil.isBlank(receiveSheet.getSettleCheckSheetDetailId())) {
            return;
        }

        SettleCheckSheetDetail checkDetail = checkDetailMap.get(receiveSheet.getSettleCheckSheetDetailId());
        if (checkDetail == null) {
            return;
        }

        SettleCheckSheet checkSheet = checkSheetMap.get(checkDetail.getSheetId());
        result.setCheckAmount(checkDetail.getPayAmount());
        if (checkSheet != null) {
            result.setCheckTime(checkSheet.getApproveTime() != null ? checkSheet.getApproveTime() : checkSheet.getCreateTime());
        }
        if (checkSheet != null && StringUtil.isNotBlank(checkSheet.getDescription())) {
            result.setCheckDescription(checkSheet.getDescription());
        }
    }

    /**
     * 填充结算信息
     *
     * @param result
     * @param settleDetails
     */
    private void fillSettleInfo(ReceiveSheetSettleInfoBo result,
                                List<SettleSheetDetail> settleDetails,
                                Map<String, SettleSheet> settleSheetMap) {

        if (CollectionUtil.isEmpty(settleDetails)) {
            return;
        }

        BigDecimal settleAmount = BigDecimal.ZERO;
        LinkedHashSet<String> descriptions = new LinkedHashSet<>();
        for (SettleSheetDetail detail : settleDetails) {
            // 结算金额需要累计实付金额和优惠金额，才能还原该收货单的实际结算总额。
            settleAmount = NumberUtil.add(settleAmount,
                    NumberUtil.add(detail.getPayAmount(), detail.getDiscountAmount()));
            SettleSheet settleSheet = settleSheetMap.get(detail.getSheetId());
            if (settleSheet != null) {
                result.setSettleTime(settleSheet.getApproveTime() != null ? settleSheet.getApproveTime() : settleSheet.getCreateTime());
            }
            if (StringUtil.isNotBlank(detail.getDescription())) {
                // 使用有序去重集合，避免重复备注，同时尽量保持用户录入时的展示顺序。
                descriptions.add(detail.getDescription());
            }
        }

        result.setSettleAmount(settleAmount);
        if (!descriptions.isEmpty()) {
            result.setSettleDescription(String.join("；", descriptions));
        }
    }

    @Override
    public SettleSheetFullDto getDetail(String id) {

        return getBaseMapper().getDetail(id);
    }

    @OpLog(type = SettleOpLogType.class, name = "创建供应商结算单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建结算单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateSettleSheetVo vo) {

        SettleSheet sheet = new SettleSheet();

        sheet.setId(IdUtil.getId());
        sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.SETTLE_SHEET));

        this.create(sheet, vo);

        sheet.setStatus(SettleSheetStatus.CREATED);

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);

        getBaseMapper().insert(sheet);

        return sheet.getId();
    }

    @OpLog(type = SettleOpLogType.class, name = "修改供应商结算单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改结算单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateSettleSheetVo vo) {

        SettleSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商结算单不存在！");
        }

        if (sheet.getStatus() != SettleSheetStatus.CREATED
                && sheet.getStatus() != SettleSheetStatus.APPROVE_REFUSE) {
            if (sheet.getStatus() == SettleSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商结算单已审核通过，无法修改！");
            } else {
                throw new DefaultClientException("供应商结算单无法修改！");
            }
        }

        // 将所有的单据的结算状态更新
        Wrapper<SettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getSheetId, sheet.getId()).orderByAsc(SettleSheetDetail::getOrderNo);
        List<SettleSheetDetail> sheetDetails = settleSheetDetailService.list(queryDetailWrapper);
        for (SettleSheetDetail sheetDetail : sheetDetails) {
            this.setBizItemUnSettle(sheetDetail.getBizId());
        }

        // 删除明细
        Wrapper<SettleSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getSheetId, sheet.getId());
        settleSheetDetailService.remove(deleteDetailWrapper);

        this.create(sheet, vo);

        sheet.setStatus(SettleSheetStatus.CREATED);

        List<SettleSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleSheetStatus.CREATED);
        statusList.add(SettleSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleSheet> updateWrapper = Wrappers.lambdaUpdate(SettleSheet.class)
                .set(SettleSheet::getApproveBy, null).set(SettleSheet::getApproveTime, null)
                .set(SettleSheet::getRefuseReason, StringPool.EMPTY_STR)
                .eq(SettleSheet::getId, sheet.getId())
                .in(SettleSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商结算单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SettleOpLogType.class, name = "审核通过供应商结算单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approvePass(ApprovePassSettleSheetVo vo) {

        SettleSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商结算单不存在！");
        }

        if (sheet.getStatus() != SettleSheetStatus.CREATED
                && sheet.getStatus() != SettleSheetStatus.APPROVE_REFUSE) {
            if (sheet.getStatus() == SettleSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商结算单已审核通过，不允许继续执行审核！");
            }
            throw new DefaultClientException("供应商结算单无法审核通过！");
        }

        sheet.setStatus(SettleSheetStatus.APPROVE_PASS);
        sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
        sheet.setApproveTime(LocalDateTime.now());
        if (!StringUtil.isBlank(vo.getDescription())) {
            sheet.setDescription(vo.getDescription());
        }

        List<SettleSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleSheetStatus.CREATED);
        statusList.add(SettleSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleSheet> updateWrapper = Wrappers.lambdaUpdate(SettleSheet.class)
                .eq(SettleSheet::getId, sheet.getId()).in(SettleSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商结算单信息已过期，请刷新重试！");
        }

        Wrapper<SettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getSheetId, sheet.getId())
                .orderByAsc(SettleSheetDetail::getOrderNo);
        List<SettleSheetDetail> details = settleSheetDetailService.list(queryDetailWrapper);
        for (SettleSheetDetail detail : details) {
            this.setBizItemSettled(detail.getBizId());
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String directApprovePass(CreateSettleSheetVo vo) {

        SettleSheetService thisService = getThis(this.getClass());

        String id = thisService.create(vo);

        ApprovePassSettleSheetVo approveVo = new ApprovePassSettleSheetVo();
        approveVo.setId(id);

        thisService.approvePass(approveVo);

        return id;
    }

    @OpLog(type = SettleOpLogType.class, name = "审核拒绝供应商结算单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approveRefuse(ApproveRefuseSettleSheetVo vo) {

        SettleSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商结算单不存在！");
        }

        if (sheet.getStatus() != SettleSheetStatus.CREATED) {
            if (sheet.getStatus() == SettleSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商结算单已审核通过，不允许继续执行审核！");
            }
            if (sheet.getStatus() == SettleSheetStatus.APPROVE_REFUSE) {
                throw new DefaultClientException("供应商结算单已审核拒绝，不允许继续执行审核！");
            }
            throw new DefaultClientException("供应商结算单无法审核拒绝！");
        }

        sheet.setStatus(SettleSheetStatus.APPROVE_REFUSE);
        sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
        sheet.setApproveTime(LocalDateTime.now());
        sheet.setRefuseReason(vo.getRefuseReason());

        List<SettleSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleSheetStatus.CREATED);
        statusList.add(SettleSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleSheet> updateWrapper = Wrappers.lambdaUpdate(SettleSheet.class)
                .eq(SettleSheet::getId, sheet.getId()).in(SettleSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商结算单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SettleOpLogType.class, name = "删除供应商结算单，单号：{}", params = "#code")
    @OrderTimeLineLog(orderId = "#id", delete = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        Assert.notBlank(id);
        SettleSheet sheet = getBaseMapper().selectById(id);
        if (sheet == null) {
            throw new InputErrorException("供应商结算单不存在！");
        }

        if (sheet.getStatus() != SettleSheetStatus.CREATED
                && sheet.getStatus() != SettleSheetStatus.APPROVE_REFUSE) {

            if (sheet.getStatus() == SettleSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("“审核通过”的供应商结算单不允许执行删除操作！");
            }

            throw new DefaultClientException("供应商结算单无法删除！");
        }

        // 将所有的单据的结算状态更新
        Wrapper<SettleSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getSheetId, sheet.getId()).orderByAsc(SettleSheetDetail::getOrderNo);
        List<SettleSheetDetail> sheetDetails = settleSheetDetailService.list(queryDetailWrapper);
        for (SettleSheetDetail sheetDetail : sheetDetails) {
            this.setBizItemUnSettle(sheetDetail.getBizId());
        }

        // 删除明细
        Wrapper<SettleSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getSheetId, sheet.getId());
        settleSheetDetailService.remove(deleteDetailWrapper);

        // 删除单据
        Wrapper<SettleSheet> deleteWrapper = Wrappers.lambdaUpdate(SettleSheet.class)
                .eq(SettleSheet::getId, id)
                .in(SettleSheet::getStatus, SettleSheetStatus.CREATED, SettleSheetStatus.APPROVE_REFUSE);
        if (!remove(deleteWrapper)) {
            throw new DefaultClientException("供应商结算单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
    }

    @Override
    public SettleBizItemDto getBizItem(String id) {
        // todo com.lframework.xingyun.settle.impl.SettleCheckSheetServiceImpl.getBizItem
        ReceiveSheet receiveSheet = receiveSheetService.getById(id);
        if (receiveSheet == null) {
            return null;
        }

        SettleBizItemDto result = new SettleBizItemDto();
        BigDecimal totalPayAmount = receiveSheet.getTotalAmount();
        BigDecimal paidAmount = getReceiveSheetSettledAmount(id);
        result.setId(receiveSheet.getId());
        result.setCode(receiveSheet.getCode());
        result.setTotalPayAmount(totalPayAmount);
        result.setTotalPayedAmount(paidAmount);
        result.setTotalDiscountAmount(BigDecimal.ZERO);
        result.setTotalUnPayAmount(NumberUtil.sub(totalPayAmount, paidAmount));
        result.setApproveTime(receiveSheet.getApproveTime());

        return result;
    }

    private BigDecimal getReceiveSheetSettledAmount(String bizId) {

        List<SettleSheetDetail> details = settleSheetDetailService.list(Wrappers.lambdaQuery(SettleSheetDetail.class)
                .eq(SettleSheetDetail::getBizId, bizId));
        if (CollectionUtil.isEmpty(details)) {
            return BigDecimal.ZERO;
        }

        BigDecimal settleAmount = BigDecimal.ZERO;
        for (SettleSheetDetail detail : details) {
            settleAmount = NumberUtil.add(settleAmount,
                    NumberUtil.add(detail.getPayAmount(), detail.getDiscountAmount()));
        }
        return settleAmount;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemUnSettle(String bizId) {
        List<SettleCheckSheetDetail> list = settleCheckSheetDetailService.listByBizIds(Lists.newArrayList(bizId));
        if (CollectionUtils.isEmpty(list)) {
            throw new InputErrorException("单据不存在！");
        }
        settleCheckSheetService.setBizItemUnSettle(bizId, list.get(0).getBizType());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemPartSettle(String bizId) {
        List<SettleCheckSheetDetail> list = settleCheckSheetDetailService.listByBizIds(Lists.newArrayList(bizId));
        if (CollectionUtils.isEmpty(list)) {
            throw new InputErrorException("单据不存在！");
        }
        settleCheckSheetService.setBizItemPartSettle(bizId, list.get(0).getBizType());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemSettled(String bizId) {
        List<SettleCheckSheetDetail> list = settleCheckSheetDetailService.listByBizIds(Lists.newArrayList(bizId));
        if (CollectionUtils.isEmpty(list)) {
            throw new InputErrorException("单据不存在！");
        }
        settleCheckSheetService.setBizItemSettled(bizId, list.get(0).getBizType());
    }

    private void create(SettleSheet sheet, CreateSettleSheetVo vo) {
        // 收货单ID列表
        List<String> receiveSheetIds = vo.getItems().stream().map(SettleSheetItemVo::getId).collect(Collectors.toList());

        // 分配结算金额
        this.allocateSettleAmount(vo);
        BigDecimal totalCheckAmt = vo.getItems().stream()
                .map(item -> item.getCheckAmt() == null ? BigDecimal.ZERO : item.getCheckAmt())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int orderNo = 1;
        for (SettleSheetItemVo itemVo : vo.getItems()) {
            SettleSheetDetail detail = this.buildDetail(sheet, itemVo, orderNo);
            settleSheetDetailService.save(detail);

            orderNo++;
        }

        this.buildSettleSheet(sheet, vo, receiveSheetIds, totalCheckAmt);
    }

    /**
     * 对账金额分配
     * @param vo
     * @return
     */
    private void allocateSettleAmount(CreateSettleSheetVo vo) {
        if (vo.getSettleAmount() == null || CollectionUtil.isEmpty(vo.getItems())) {
            return;
        }

        // 对账单汇总金额
        BigDecimal checkAmount = vo.getItems().stream()
                .map(item -> item.getCheckAmt() == null ? BigDecimal.ZERO : item.getCheckAmt())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 对账金额差额合计
        BigDecimal totalDiffAmount = NumberUtil.sub(vo.getSettleAmount(), checkAmount);
        // 将对账金额差额平均分摊给每个业务单据
        BigDecimal avgDiffAmount = NumberUtil.div(totalDiffAmount, BigDecimal.valueOf(vo.getItems().size()));

        vo.getItems().forEach(item -> {
            BigDecimal settleAmt = NumberUtil.add(item.getCheckAmt(), avgDiffAmount);
            if (NumberUtil.lt(settleAmt, BigDecimal.ZERO)) {
                throw new DefaultClientException("结算金额过小，分摊后会出现负数单据，请调整结算金额！");
            }
            item.setSettleAmount(settleAmt);
        });
    }

    /**
     * 构建结算单
     * @param sheet
     * @param vo
     * @param receiveSheetIds
     * @param checkTotalAmount
     */
    private void buildSettleSheet(SettleSheet sheet, CreateSettleSheetVo vo, List<String> receiveSheetIds, BigDecimal checkTotalAmount) {
        sheet.setSupplierId(vo.getSupplierId());
        sheet.setTotalAmount(vo.getSettleAmount());
        sheet.setCheckTotalAmount(checkTotalAmount);
        sheet.setTotalDiscountAmount(BigDecimal.ZERO);
        sheet.setBizSheetIds(String.join(StringPool.STR_SPLIT, receiveSheetIds));
        sheet.setDescription(vo.getDescription());
        sheet.setRefuseReason(StringPool.EMPTY_STR);
        sheet.setStartDate(vo.getStartDate());
        sheet.setEndDate(vo.getEndDate());
    }

    /**
     * 构建结算单详情
     * @param sheet
     * @param orderNo
     * @return
     */
    private SettleSheetDetail buildDetail(SettleSheet sheet, SettleSheetItemVo itemVo, int orderNo) {
        SettleSheetDetail res = new SettleSheetDetail();
        res.setSheetId(sheet.getId());
        res.setBizId(itemVo.getId());
        res.setPayAmount(itemVo.getSettleAmount());
        res.setDiscountAmount(BigDecimal.ZERO);
        res.setOrderNo(orderNo);
        return res;
    }
}
