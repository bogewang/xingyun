package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.DefaultSysException;
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
import com.lframework.starter.web.core.utils.*;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.entity.OrderTimeLine;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.OrderTimeLineService;
import com.lframework.xingyun.core.components.timeline.ReceiveOrderTimeLineBizType;
import com.lframework.xingyun.sc.entity.PurchaseReturn;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.service.purchase.PurchaseReturnService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetDetailService;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.settle.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.settle.dto.check.SettleCheckBizItemDto;
import com.lframework.xingyun.settle.dto.check.SettleCheckSheetFullDto;
import com.lframework.xingyun.settle.entity.*;
import com.lframework.xingyun.settle.enums.*;
import com.lframework.xingyun.settle.mappers.SettleCheckSheetMapper;
import com.lframework.xingyun.settle.service.*;
import com.lframework.xingyun.settle.utils.SettleAmountAllocationUtil;
import com.lframework.xingyun.settle.vo.check.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettleCheckSheetServiceImpl extends
        BaseMpServiceImpl<SettleCheckSheetMapper, SettleCheckSheet>
        implements SettleCheckSheetService {

    @Autowired
    private SettleCheckSheetDetailService settleCheckSheetDetailService;

    @Autowired
    private SettleSheetDetailService settleSheetDetailService;

    @Autowired
    private GenerateCodeService generateCodeService;

    @Autowired
    private ReceiveSheetService receiveSheetService;

    @Autowired
    private PurchaseReturnService purchaseReturnService;

    @Autowired
    private SettleFeeSheetService settleFeeSheetService;

    @Autowired
    private SettlePreSheetService settlePreSheetService;

    @Autowired
    private OrderTimeLineService orderTimeLineService;

    @Autowired
    private ReceiveOrderTimeLineBizType receiveOrderTimeLineBizType;

    @Override
    public PageResult<SettleCheckSheet> query(Integer pageIndex, Integer pageSize,
                                              QuerySettleCheckSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<SettleCheckSheet> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<SettleCheckSheet> query(QuerySettleCheckSheetVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public SettleCheckSheetFullDto getDetail(String id) {

        return getBaseMapper().getDetail(id);
    }

    private String generateCode() {
        while (true) {
            String code = generateCodeService.generate(GenerateCodeTypePool.SETTLE_CHECK_SHEET);
            QuerySettleCheckSheetVo vo = new QuerySettleCheckSheetVo();
            vo.setCode(code);
            List<SettleCheckSheet> list = query(vo);
            if (CollectionUtils.isEmpty(list)) {
                return code;
            }
        }
    }

    @OpLog(type = SettleOpLogType.class, name = "创建供应商对账单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建对账单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateSettleCheckSheetVo vo) {

        SettleCheckSheet sheet = new SettleCheckSheet();

        sheet.setId(IdUtil.getId());
        sheet.setCode(generateCode());

        this.create(sheet, vo);

        sheet.setStatus(SettleCheckSheetStatus.CREATED);

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);

        getBaseMapper().insert(sheet);

        return sheet.getId();
    }

    @OpLog(type = SettleOpLogType.class, name = "修改供应商对账单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改对账单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateSettleCheckSheetVo vo) {

        SettleCheckSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商对账单不存在！");
        }

        if (sheet.getStatus() != SettleCheckSheetStatus.CREATED
                && sheet.getStatus() != SettleCheckSheetStatus.APPROVE_REFUSE) {
            if (sheet.getStatus() == SettleCheckSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商对账单已审核通过，无法修改！");
            } else {
                throw new DefaultClientException("供应商对账单无法修改！");
            }
        }

        // 将所有的单据的结算状态更新
        Wrapper<SettleCheckSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
                        SettleCheckSheetDetail.class)
                .eq(SettleCheckSheetDetail::getSheetId, sheet.getId())
                .orderByAsc(SettleCheckSheetDetail::getOrderNo);
        List<SettleCheckSheetDetail> sheetDetails = settleCheckSheetDetailService.list(
                queryDetailWrapper);
        for (SettleCheckSheetDetail sheetDetail : sheetDetails) {
            this.setBizItemUnSettle(sheetDetail.getBizId(), sheetDetail.getBizType());
        }

        // 删除明细
        Wrapper<SettleCheckSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(
                        SettleCheckSheetDetail.class)
                .eq(SettleCheckSheetDetail::getSheetId, sheet.getId());
        settleCheckSheetDetailService.remove(deleteDetailWrapper);

        this.create(sheet, vo);

        sheet.setStatus(SettleCheckSheetStatus.CREATED);

        List<SettleCheckSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleCheckSheetStatus.CREATED);
        statusList.add(SettleCheckSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
                .set(SettleCheckSheet::getApproveBy, null).set(SettleCheckSheet::getApproveTime, null)
                .set(SettleCheckSheet::getRefuseReason, StringPool.EMPTY_STR)
                .eq(SettleCheckSheet::getId, sheet.getId())
                .in(SettleCheckSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商对账单信息已过期，请刷新重试！");
        }

        this.recordReceiveSheetCheckTimeLine(sheet);

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SettleOpLogType.class, name = "审核通过供应商对账单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approvePass(ApprovePassSettleCheckSheetVo vo) {

        SettleCheckSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商对账单不存在！");
        }

        if (sheet.getStatus() != SettleCheckSheetStatus.CREATED
                && sheet.getStatus() != SettleCheckSheetStatus.APPROVE_REFUSE) {
            if (sheet.getStatus() == SettleCheckSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商对账单已审核通过，不允许继续执行审核！");
            }
            throw new DefaultClientException("供应商对账单无法审核通过！");
        }

        sheet.setStatus(SettleCheckSheetStatus.APPROVE_PASS);
        sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
        sheet.setApproveTime(LocalDateTime.now());
        if (!StringUtil.isBlank(vo.getDescription())) {
            sheet.setDescription(vo.getDescription());
        }

        List<SettleCheckSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleCheckSheetStatus.CREATED);
        statusList.add(SettleCheckSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
                .eq(SettleCheckSheet::getId, sheet.getId()).in(SettleCheckSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商对账单信息已过期，请刷新重试！");
        }

        this.recordReceiveSheetCheckTimeLine(sheet);

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String directApprovePass(CreateSettleCheckSheetVo vo) {

        SettleCheckSheetService thisService = getThis(this.getClass());

        String id = thisService.create(vo);

        ApprovePassSettleCheckSheetVo approveVo = new ApprovePassSettleCheckSheetVo();
        approveVo.setId(id);

        thisService.approvePass(approveVo);

        return id;
    }

    @OpLog(type = SettleOpLogType.class, name = "审核拒绝供应商对账单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approveRefuse(ApproveRefuseSettleCheckSheetVo vo) {

        SettleCheckSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new DefaultClientException("供应商对账单不存在！");
        }

        if (sheet.getStatus() != SettleCheckSheetStatus.CREATED) {
            if (sheet.getStatus() == SettleCheckSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("供应商对账单已审核通过，不允许继续执行审核！");
            }
            if (sheet.getStatus() == SettleCheckSheetStatus.APPROVE_REFUSE) {
                throw new DefaultClientException("供应商对账单已审核拒绝，不允许继续执行审核！");
            }
            throw new DefaultClientException("供应商对账单无法审核拒绝！");
        }

        sheet.setStatus(SettleCheckSheetStatus.APPROVE_REFUSE);
        sheet.setApproveBy(SecurityUtil.getCurrentUser().getId());
        sheet.setApproveTime(LocalDateTime.now());
        sheet.setRefuseReason(vo.getRefuseReason());

        List<SettleCheckSheetStatus> statusList = new ArrayList<>();
        statusList.add(SettleCheckSheetStatus.CREATED);
        statusList.add(SettleCheckSheetStatus.APPROVE_REFUSE);

        Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
                .eq(SettleCheckSheet::getId, sheet.getId()).in(SettleCheckSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateWrapper) != 1) {
            throw new DefaultClientException("供应商对账单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = SettleOpLogType.class, name = "删除供应商对账单，单号：{}", params = "#code")
    @OrderTimeLineLog(orderId = "#id", delete = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        Assert.notBlank(id);
        SettleCheckSheet sheet = getBaseMapper().selectById(id);
        if (sheet == null) {
            throw new InputErrorException("供应商对账单不存在！");
        }

        if (sheet.getStatus() != SettleCheckSheetStatus.CREATED
                && sheet.getStatus() != SettleCheckSheetStatus.APPROVE_REFUSE) {

            if (sheet.getStatus() == SettleCheckSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("“审核通过”的供应商对账单不允许执行删除操作！");
            }

            throw new DefaultClientException("供应商对账单无法删除！");
        }

        // 将所有的单据的结算状态更新
        Wrapper<SettleCheckSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
                        SettleCheckSheetDetail.class)
                .eq(SettleCheckSheetDetail::getSheetId, sheet.getId())
                .orderByAsc(SettleCheckSheetDetail::getOrderNo);
        List<SettleCheckSheetDetail> sheetDetails = settleCheckSheetDetailService.list(
                queryDetailWrapper);
        for (SettleCheckSheetDetail sheetDetail : sheetDetails) {
            this.setBizItemUnSettle(sheetDetail.getBizId(), sheetDetail.getBizType());
        }

        // 删除明细
        Wrapper<SettleCheckSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(
                        SettleCheckSheetDetail.class)
                .eq(SettleCheckSheetDetail::getSheetId, sheet.getId());
        settleCheckSheetDetailService.remove(deleteDetailWrapper);

        // 删除单据
        Wrapper<SettleCheckSheet> deleteWrapper = Wrappers.lambdaQuery(SettleCheckSheet.class)
                .eq(SettleCheckSheet::getId, id)
                .in(SettleCheckSheet::getStatus, SettleCheckSheetStatus.CREATED,
                        SettleCheckSheetStatus.APPROVE_REFUSE);
        if (!remove(deleteWrapper)) {
            throw new DefaultClientException("供应商对账单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
    }

    @Override
    public SettleCheckBizItemDto getBizItem(String id, SettleCheckSheetBizType bizType) {

        SettleCheckBizItemDto result = new SettleCheckBizItemDto();

        switch (bizType) {
            case RECEIVE_SHEET: {
                ReceiveSheet receiveSheet = receiveSheetService.getById(id);

                result.setId(receiveSheet.getId());
                result.setCode(receiveSheet.getCode());
                result.setTotalAmount(receiveSheet.getTotalAmount());
                result.setPaidAmount(this.getReceiveSheetSettledAmount(receiveSheet.getId()));
                result.setApproveTime(receiveSheet.getApproveTime());
                result.setCalcType(SettleCheckSheetCalcType.ADD);
                break;
            }
            case PURCHASE_RETURN: {
                PurchaseReturn purchaseReturn = purchaseReturnService.getById(id);

                result.setId(purchaseReturn.getId());
                result.setCode(purchaseReturn.getCode());
                result.setTotalAmount(purchaseReturn.getTotalAmount());
                // result.setPaidAmount(purchaseReturn.getPaidAmount());
                result.setApproveTime(purchaseReturn.getApproveTime());
                result.setCalcType(SettleCheckSheetCalcType.SUB);
                break;
            }
            case SETTLE_FEE_SHEET: {
                SettleFeeSheet feeSheet = settleFeeSheetService.getById(id);

                result.setId(feeSheet.getId());
                result.setCode(feeSheet.getCode());
                result.setTotalAmount(feeSheet.getTotalAmount());
                result.setApproveTime(feeSheet.getApproveTime());
                result.setCalcType(feeSheet.getSheetType() == SettleFeeSheetType.PAY ? SettleCheckSheetCalcType.ADD
                        : SettleCheckSheetCalcType.SUB);
                break;
            }
            case SETTLE_PRE_SHEET: {
                SettlePreSheet preSheet = settlePreSheetService.getById(id);

                result.setId(preSheet.getId());
                result.setCode(preSheet.getCode());
                result.setTotalAmount(preSheet.getTotalAmount());
                result.setApproveTime(preSheet.getApproveTime());
                result.setCalcType(SettleCheckSheetCalcType.SUB);
                break;
            }
            default: {
                throw new DefaultSysException("未知的SettleCheckSheetBizType");
            }
        }

        result.setBizType(bizType);
        if (result.getCalcType() == SettleCheckSheetCalcType.SUB) {
            result.setTotalAmount(result.getTotalAmount().negate());
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemUnSettle(String id, SettleCheckSheetBizType bizType) {

        SettleCheckBizItemDto item = this.getBizItem(id, bizType);

        switch (bizType) {
            case RECEIVE_SHEET: {
                int count = receiveSheetService.setUnSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case PURCHASE_RETURN: {
                int count = purchaseReturnService.setUnSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case SETTLE_FEE_SHEET: {
                int count = settleFeeSheetService.setUnSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case SETTLE_PRE_SHEET: {
                int count = settlePreSheetService.setUnSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            default: {
                throw new DefaultSysException("未知的SettleCheckSheetBizType");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemPartSettle(String id, SettleCheckSheetBizType bizType) {

        SettleCheckBizItemDto item = this.getBizItem(id, bizType);

        switch (bizType) {
            case RECEIVE_SHEET: {
                int count = receiveSheetService.setPartSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case PURCHASE_RETURN: {
                int count = purchaseReturnService.setPartSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case SETTLE_FEE_SHEET: {
                int count = settleFeeSheetService.setPartSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            case SETTLE_PRE_SHEET: {
                int count = settlePreSheetService.setPartSettle(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，业务无法进行！");
                }
                break;
            }
            default: {
                throw new DefaultSysException("未知的SettleCheckSheetBizType");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setBizItemSettled(String id, SettleCheckSheetBizType bizType) {

        SettleCheckBizItemDto item = this.getBizItem(id, bizType);

        switch (bizType) {
            case RECEIVE_SHEET: {
                int count = receiveSheetService.setSettled(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，无法重复结算！");
                }
                break;
            }
            case PURCHASE_RETURN: {
                int count = purchaseReturnService.setSettled(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，无法重复结算！");
                }
                break;
            }
            case SETTLE_FEE_SHEET: {
                int count = settleFeeSheetService.setSettled(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，无法重复结算！");
                }
                break;
            }
            case SETTLE_PRE_SHEET: {
                int count = settlePreSheetService.setSettled(id);
                if (count != 1) {
                    throw new DefaultClientException(
                            "单号：" + item.getCode() + "，" + bizType.getDesc() + "已结算，无法重复结算！");
                }
                break;
            }
            default: {
                throw new DefaultSysException("未知的SettleCheckSheetBizType");
            }
        }
    }

    @Override
    public List<SettleCheckBizItemDto> getUnCheckBizItems(QueryUnCheckBizItemVo vo) {

        List<SettleCheckBizItemDto> results = new ArrayList<>();

        List<ReceiveSheet> receiveSheetList = receiveSheetService.getApprovedList(vo.getSupplierId(),
                vo.getStartTime(),
                vo.getEndTime(), SettleStatus.UN_SETTLE);

        List<PurchaseReturn> purchaseReturnList = purchaseReturnService.getApprovedList(
                vo.getSupplierId(),
                vo.getStartTime(), vo.getEndTime(), SettleStatus.UN_SETTLE);

        List<SettleFeeSheet> feeSheetList = settleFeeSheetService.getApprovedList(vo.getSupplierId(),
                vo.getStartTime(),
                vo.getEndTime(), SettleStatus.UN_SETTLE);

        List<SettlePreSheet> preSheetList = settlePreSheetService.getApprovedList(vo.getSupplierId(),
                vo.getStartTime(),
                vo.getEndTime(), SettleStatus.UN_SETTLE);

        if (!CollectionUtil.isEmpty(receiveSheetList)) {
            for (ReceiveSheet item : receiveSheetList) {
                SettleCheckBizItemDto result = new SettleCheckBizItemDto();
                result.setId(item.getId());
                result.setCode(item.getCode());
                result.setTotalAmount(item.getTotalAmount());
                result.setApproveTime(item.getApproveTime());
                result.setBizType(SettleCheckSheetBizType.RECEIVE_SHEET);
                result.setCalcType(SettleCheckSheetCalcType.ADD);

                results.add(result);
            }
        }

        if (!CollectionUtil.isEmpty(purchaseReturnList)) {
            for (PurchaseReturn item : purchaseReturnList) {
                SettleCheckBizItemDto result = new SettleCheckBizItemDto();
                result.setId(item.getId());
                result.setCode(item.getCode());
                result.setTotalAmount(item.getTotalAmount());
                result.setApproveTime(item.getApproveTime());
                result.setBizType(SettleCheckSheetBizType.PURCHASE_RETURN);
                result.setCalcType(SettleCheckSheetCalcType.SUB);

                results.add(result);
            }
        }

        if (!CollectionUtil.isEmpty(feeSheetList)) {
            for (SettleFeeSheet item : feeSheetList) {
                SettleCheckBizItemDto result = new SettleCheckBizItemDto();
                result.setId(item.getId());
                result.setCode(item.getCode());
                result.setTotalAmount(item.getTotalAmount());
                result.setApproveTime(item.getApproveTime());
                result.setBizType(SettleCheckSheetBizType.SETTLE_FEE_SHEET);
                result.setCalcType(item.getSheetType() == SettleFeeSheetType.PAY ? SettleCheckSheetCalcType.ADD
                        : SettleCheckSheetCalcType.SUB);

                results.add(result);
            }
        }

        if (!CollectionUtil.isEmpty(preSheetList)) {
            for (SettlePreSheet item : preSheetList) {
                SettleCheckBizItemDto result = new SettleCheckBizItemDto();
                result.setId(item.getId());
                result.setCode(item.getCode());
                result.setTotalAmount(item.getTotalAmount());
                result.setApproveTime(item.getApproveTime());
                result.setBizType(SettleCheckSheetBizType.SETTLE_PRE_SHEET);
                result.setCalcType(SettleCheckSheetCalcType.SUB);

                results.add(result);
            }
        }

        results.stream().filter(t -> t.getCalcType() == SettleCheckSheetCalcType.SUB)
                .forEach(t -> t.setTotalAmount(t.getTotalAmount().negate()));

        return results;
    }

    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public int setUnSettle(String id) {
    //
    // Wrapper<SettleCheckSheet> updateWrapper =
    // Wrappers.lambdaUpdate(SettleCheckSheet.class)
    // .set(SettleCheckSheet::getSettleStatus, SettleStatus.UN_SETTLE)
    // .eq(SettleCheckSheet::getId, id)
    // .eq(SettleCheckSheet::getSettleStatus, SettleStatus.PART_SETTLE);
    // int count = getBaseMapper().update(updateWrapper);
    //
    // if (count == 1) {
    // Wrapper<SettleCheckSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
    // SettleCheckSheetDetail.class)
    // .eq(SettleCheckSheetDetail::getSheetId,
    // id).orderByAsc(SettleCheckSheetDetail::getOrderNo);
    // List<SettleCheckSheetDetail> sheetDetails =
    // settleCheckSheetDetailService.list(
    // queryDetailWrapper);
    // for (SettleCheckSheetDetail sheetDetail : sheetDetails) {
    // if (sheetDetail.getBizType() == SettleCheckSheetBizType.RECEIVE_SHEET) {
    // receiveSheetService.setUnSettle(sheetDetail.getBizId(),
    // }
    // }
    // }
    //
    // return count;
    // }

    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public int setPartSettle(String id) {
    //
    //     Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
    //             .set(SettleCheckSheet::getSettleStatus, SettleStatus.PART_SETTLE)
    //             .eq(SettleCheckSheet::getId, id)
    //             .in(SettleCheckSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    //     int count = getBaseMapper().update(updateWrapper);
    //
    //     if (count == 1) {
    //         Wrapper<SettleCheckSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
    //                         SettleCheckSheetDetail.class)
    //                 .eq(SettleCheckSheetDetail::getSheetId, id).orderByAsc(SettleCheckSheetDetail::getOrderNo);
    //         List<SettleCheckSheetDetail> sheetDetails = settleCheckSheetDetailService.list(
    //                 queryDetailWrapper);
    //         for (SettleCheckSheetDetail sheetDetail : sheetDetails) {
    //             if (sheetDetail.getBizType() == SettleCheckSheetBizType.RECEIVE_SHEET) {
    //                 receiveSheetService.setPartSettle(sheetDetail.getBizId());
    //             }
    //         }
    //     }
    //
    //     return count;
    // }

    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public int setSettled(String id) {
    //
    //     Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
    //             .set(SettleCheckSheet::getSettleStatus, SettleStatus.SETTLED)
    //             .eq(SettleCheckSheet::getId, id)
    //             .in(SettleCheckSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    //     int count = getBaseMapper().update(updateWrapper);
    //
    //     // 将所有的单据的结算状态更新
    //     Wrapper<SettleCheckSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
    //                     SettleCheckSheetDetail.class)
    //             .eq(SettleCheckSheetDetail::getSheetId, id).orderByAsc(SettleCheckSheetDetail::getOrderNo);
    //     List<SettleCheckSheetDetail> sheetDetails = settleCheckSheetDetailService.list(
    //             queryDetailWrapper);
    //     for (SettleCheckSheetDetail sheetDetail : sheetDetails) {
    //         this.setBizItemSettled(sheetDetail.getBizId(), sheetDetail.getBizType());
    //     }
    //
    //     return count;
    // }

    @Override
    public List<SettleCheckSheet> getApprovedList(String supplierId, LocalDateTime startTime,
                                                  LocalDateTime endTime) {

        return getBaseMapper().getApprovedList(supplierId, startTime, endTime);
    }

    private void recordReceiveSheetCheckTimeLine(SettleCheckSheet sheet) {

        List<SettleCheckSheetDetail> details = settleCheckSheetDetailService.list(
                Wrappers.lambdaQuery(SettleCheckSheetDetail.class)
                        .eq(SettleCheckSheetDetail::getSheetId, sheet.getId())
                        .orderByAsc(SettleCheckSheetDetail::getOrderNo));
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        String description = StringUtil.isBlank(sheet.getDescription()) ? "无" : sheet.getDescription();
        String createById = SecurityUtil.getCurrentUser().getId();
        String createBy = SecurityUtil.getCurrentUser().getName();
        LocalDateTime createTime = sheet.getApproveTime() != null ? sheet.getApproveTime() : LocalDateTime.now();

        details.stream()
                .filter(detail -> SettleCheckSheetBizType.RECEIVE_SHEET == detail.getBizType())
                .forEach(detail -> {
                    OrderTimeLine orderTimeLine = new OrderTimeLine();
                    orderTimeLine.setId(IdUtil.getId());
                    orderTimeLine.setOrderId(detail.getBizId());
                    orderTimeLine.setBizType(receiveOrderTimeLineBizType.getCode());
                    orderTimeLine.setContent(String.format("确认对账，对账金额：%s，备注：%s",
                            formatAmount(detail.getPayAmount()), description));
                    orderTimeLine.setCreateById(createById);
                    orderTimeLine.setCreateBy(createBy);
                    orderTimeLine.setCreateTime(createTime);
                    orderTimeLineService.save(orderTimeLine);
                });
    }

    private String formatAmount(BigDecimal amount) {

        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    // @Transactional(rollbackFor = Exception.class)
    // @Override
    // public void setSettleAmount(String id, BigDecimal totalPayedAmount,
    //                             BigDecimal totalDiscountAmount) {
    //
    //     SettleCheckSheet checkSheet = getBaseMapper().selectById(id);
    //     BigDecimal remainTotalPayAmount = NumberUtil.sub(checkSheet.getTotalPayAmount(),
    //             checkSheet.getTotalPayedAmount(), checkSheet.getTotalDiscountAmount(), totalPayedAmount,
    //             totalDiscountAmount);
    //     BigDecimal totalPayAmount = NumberUtil.sub(checkSheet.getTotalPayAmount(),
    //             checkSheet.getTotalPayedAmount(),
    //             checkSheet.getTotalDiscountAmount());
    //     if (NumberUtil.lt(checkSheet.getTotalPayAmount(), 0)) {
    //         if (NumberUtil.gt(remainTotalPayAmount, 0)) {
    //             throw new DefaultClientException(
    //                     "对账单：" + checkSheet.getCode() + "，剩余付款金额为" + totalPayAmount + "元，本次付款金额为"
    //                             + NumberUtil.add(
    //                             totalPayedAmount, totalDiscountAmount)
    //                             + "元，无法结算！");
    //         }
    //     }
    //     if (NumberUtil.gt(checkSheet.getTotalPayAmount(), 0)) {
    //         if (NumberUtil.lt(remainTotalPayAmount, 0)) {
    //             throw new DefaultClientException(
    //                     "对账单：" + checkSheet.getCode() + "，剩余付款金额为" + totalPayAmount + "元，本次付款金额为"
    //                             + NumberUtil.add(
    //                             totalPayedAmount, totalDiscountAmount)
    //                             + "元，无法结算！");
    //         }
    //     }
    //     Wrapper<SettleCheckSheet> updateWrapper = Wrappers.lambdaUpdate(SettleCheckSheet.class)
    //             .set(SettleCheckSheet::getTotalPayedAmount,
    //                     NumberUtil.add(totalPayedAmount, checkSheet.getTotalPayedAmount()))
    //             .set(SettleCheckSheet::getTotalDiscountAmount,
    //                     NumberUtil.add(totalDiscountAmount, checkSheet.getTotalDiscountAmount()))
    //             .eq(SettleCheckSheet::getId, id)
    //             .eq(SettleCheckSheet::getTotalPayedAmount, checkSheet.getTotalPayedAmount())
    //             .eq(SettleCheckSheet::getTotalDiscountAmount, checkSheet.getTotalDiscountAmount());
    //     if (getBaseMapper().update(updateWrapper) != 1) {
    //         throw new DefaultClientException(
    //                 "结账单：" + checkSheet.getCode() + "，信息已过期，请刷新重试！");
    //     }
    //
    //     if (NumberUtil.equal(remainTotalPayAmount, 0)) {
    //         this.setSettled(id);
    //     }
    // }

    private void create(SettleCheckSheet sheet, CreateSettleCheckSheetVo vo) {
        // 收货单ID列表
        List<String> receiveSheetIds = vo.getItems().stream().map(SettleCheckSheetItemVo::getId).collect(Collectors.toList());

        // 对账单中包含的收货单，需要更新收货单的结算状态
        this.allocateCheckAmount(vo);
        BigDecimal bizTotalAmount = vo.getItems().stream()
                .map(item -> item.getBizAmount() == null ? BigDecimal.ZERO : item.getBizAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int orderNo = 1;
        for (SettleCheckSheetItemVo item : vo.getItems()) {
            SettleCheckSheetDetail detail = buildDetail(sheet, item, orderNo);
            settleCheckSheetDetailService.save(detail);

            // 收货单的结算状态更新
            this.setBizItemUnSettle(detail.getBizId(), detail.getBizType());

            orderNo++;
        }

        buildSettleCheckSheet(sheet, vo, receiveSheetIds, bizTotalAmount);
    }

    /**
     * 构建对账单
     * @param sheet
     * @param vo
     * @param receiveSheetIds
     * @param bizTotalAmount
     */
    private void buildSettleCheckSheet(SettleCheckSheet sheet,
                                              CreateSettleCheckSheetVo vo,
                                              List<String> receiveSheetIds,
                                              BigDecimal bizTotalAmount) {
        sheet.setSupplierId(vo.getSupplierId());
        sheet.setTotalPayAmount(vo.getCheckAmt());
        sheet.setTotalPayedAmount(BigDecimal.ZERO);
        sheet.setTotalAmount(NumberUtil.add(vo.getCheckAmt(), sheet.getTotalPayedAmount()));
        sheet.setTotalDiscountAmount(BigDecimal.ZERO);
        sheet.setBizSheetIds(String.join(StringPool.STR_SPLIT, receiveSheetIds));
        sheet.setBizTotalAmount(bizTotalAmount);
        sheet.setDescription(vo.getDescription());
        sheet.setRefuseReason(StringPool.EMPTY_STR);
        sheet.setSettleStatus(SettleStatus.UN_SETTLE);
        sheet.setStartDate(vo.getStartDate());
        sheet.setEndDate(vo.getEndDate());
    }

    /**
     * 构建对账单详情
     *
     * @param sheet
     * @param item
     * @param orderNo
     * @return
     */
    private static SettleCheckSheetDetail buildDetail(SettleCheckSheet sheet,
                                                      SettleCheckSheetItemVo item,
                                                      int orderNo) {
        SettleCheckSheetDetail detail = new SettleCheckSheetDetail();

        detail.setId(IdUtil.getId());
        detail.setSheetId(sheet.getId());
        detail.setBizId(item.getId());
        detail.setBizType(SettleCheckSheetBizType.fromCode(item.getBizType()));
        // todo 计算方式默认加法
        detail.setCalcType(SettleCheckSheetCalcType.ADD);
        detail.setPayAmount(item.getCheckAmt());
        // 主单存了备注了，明细单就不存了
        // detail.setDescription(vo.getDescription());
        detail.setOrderNo(orderNo);
        return detail;
    }

    /**
     * 将对账金额进行分摊，分配给每个业务单据
     *
     * @param vo
     * @return
     */
    private void allocateCheckAmount(CreateSettleCheckSheetVo vo) {
        if (vo.getCheckAmt() == null || CollectionUtil.isEmpty(vo.getItems())) {
            return;
        }

        List<BigDecimal> amounts = SettleAmountAllocationUtil.allocate(vo.getCheckAmt(),
                vo.getItems().stream().map(SettleCheckSheetItemVo::getBizAmount)
                        .collect(Collectors.toList()));
        for (int index = 0; index < vo.getItems().size(); index++) {
            vo.getItems().get(index).setCheckAmt(amounts.get(index));
        }
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

    @Override
    public List<SettleCheckSheet> selectBatchIds(List<String> sheetIds) {
        return getBaseMapper().selectBatchIds(sheetIds);
    }
}
