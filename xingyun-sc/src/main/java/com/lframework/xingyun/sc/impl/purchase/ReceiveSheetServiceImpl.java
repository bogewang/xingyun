package com.lframework.xingyun.sc.impl.purchase;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.lframework.starter.web.inner.entity.SysParameter;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.system.SysParameterService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.starter.web.inner.vo.system.parameter.QuerySysParameterVo;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.enums.ManageType;
import com.lframework.xingyun.basedata.enums.SettleType;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.basedata.service.supplier.SupplierService;
import com.lframework.xingyun.sc.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.sc.dto.stock.ProductStockPendingCostResolveDto;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetFullDto;
import com.lframework.xingyun.sc.dto.purchase.receive.ReceiveSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.*;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.PurchaseOpLogType;
import com.lframework.xingyun.sc.enums.ReceiveSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetDetailExportModel;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetImportModel;
import com.lframework.xingyun.sc.excel.purchase.receive.ReceiveSheetQueryImportModel;
import com.lframework.xingyun.sc.mappers.ReceiveSheetMapper;
import com.lframework.xingyun.sc.service.ProductHotnessService;
import com.lframework.xingyun.sc.service.purchase.*;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.stock.ProductStockPendingCostService;
import com.lframework.xingyun.sc.service.stock.ProductStockLogService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.vo.purchase.receive.*;
import com.lframework.xingyun.sc.vo.stock.AddProductStockVo;
import com.lframework.xingyun.sc.vo.stock.SubProductStockVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;

@Service
public class ReceiveSheetServiceImpl extends BaseMpServiceImpl<ReceiveSheetMapper, ReceiveSheet>
        implements ReceiveSheetService {

    private static final DateTimeFormatter QUERY_IMPORT_ACTUAL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ReceiveSheetDetailService receiveSheetDetailService;

    @Autowired
    private GenerateCodeService generateCodeService;

    @Autowired
    private StoreCenterService storeCenterService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductUnitService productUnitService;

    @Autowired
    private ProductLatestPriceCacheService productLatestPriceCacheService;

    @Autowired
    private ProductHotnessService productHotnessService;

    @Autowired
    private PurchaseConfigService purchaseConfigService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockLogService productStockLogService;

    @Autowired
    private ProductStockPendingCostService productStockPendingCostService;

    @Autowired
    private ReceiveSheetDetailBundleService receiveSheetDetailBundleService;
    @Autowired
    private SaleOutSheetService saleOutSheetService;

    @Autowired
    private SysParameterService sysParameterService;

    @Override
    public PageResult<ReceiveSheet> query(Integer pageIndex, Integer pageSize,
            QueryReceiveSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<ReceiveSheet> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<ReceiveSheet> query(QueryReceiveSheetVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public PageResult<QueryReceiveSheetDetailDto> queryDetail(Integer pageIndex, Integer pageSize,
            QueryReceiveSheetVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<QueryReceiveSheetDetailDto> datas = getBaseMapper().queryDetail(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public PageResult<ReceiveSheet> selector(Integer pageIndex, Integer pageSize,
            ReceiveSheetSelectorVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<ReceiveSheet> datas = getBaseMapper().selector(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public GetPaymentDateDto getPaymentDate(String supplierId) {

        // 付款日期默认为当前日期的30天后，如当天为2021-10-01，则付款日期默认为2021-11-01
        // （1）供应商的经营方式为“经销”，且结算方式为“任意指定”，则付款日期按照以上规则展示默认值，允许用户更改，但仅能选择当天及当天之后的日期。
        // （2）供应商的经营方式为“经销”，且结算方式为“货到付款”，则付款日期默认为此刻，即收货单的创建时间，可能会遇到跨日的问题，但付款日期，均赋值为收货单的创建日期。
        // （3）供应商的经营方式为非经销模式时，收货单、退货单不涉及付款，则付款日期字段置灰，为空，且不可点击。

        Supplier supplier = supplierService.findById(supplierId);

        GetPaymentDateDto result = new GetPaymentDateDto();

        result.setAllowModify(supplier.getManageType() == ManageType.DISTRIBUTION
                && supplier.getSettleType() == SettleType.ARBITRARILY);
        if (supplier.getManageType() == ManageType.DISTRIBUTION
                && supplier.getSettleType() == SettleType.ARBITRARILY) {
            result.setPaymentDate(LocalDate.now().plusMonths(1));
        } else if (supplier.getManageType() == ManageType.DISTRIBUTION
                && supplier.getSettleType() == SettleType.CASH_ON_DELIVERY) {
            result.setPaymentDate(LocalDate.now());
        }

        return result;
    }

    @Override
    public ReceiveSheetFullDto getDetail(String id) {

        return getBaseMapper().getDetail(id);
    }

    @Override
    public ReceiveSheetWithReturnDto getWithReturn(String id) {

        PurchaseConfig purchaseConfig = purchaseConfigService.get();

        ReceiveSheetWithReturnDto sheet = getBaseMapper().getWithReturn(id,
                purchaseConfig.getPurchaseReturnRequireReceive());
        if (sheet == null) {
            throw new InputErrorException("收货单不存在！");
        }
        return sheet;
    }

    @Override
    public PageResult<ReceiveSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
            QueryReceiveSheetWithReturnVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PurchaseConfig purchaseConfig = purchaseConfigService.get();

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<ReceiveSheet> datas = getBaseMapper().queryWithReturn(vo,
                purchaseConfig.getPurchaseReturnMultipleRelateReceive());

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    private String generateCode() {
        while (true) {
            String code = generateCodeService.generate(GenerateCodeTypePool.SALE_OUT_SHEET);
            QueryReceiveSheetVo vo = new QueryReceiveSheetVo();
            vo.setCode(code);
            List<ReceiveSheet> list = query(vo);
            if (CollectionUtils.isEmpty(list)) {
                return code;
            }
        }
    }

    @OpLog(type = PurchaseOpLogType.class, name = "创建采购收货单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建收货单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateReceiveSheetVo vo) {

        ReceiveSheet sheet = new ReceiveSheet();
        sheet.setId(IdUtil.getId());
        sheet.setCode(generateCode());

        this.create(sheet, vo);

        sheet.setStatus(ReceiveSheetStatus.CREATED);

        getBaseMapper().insert(sheet);
        List<ReceiveSheetDetail> details = receiveSheetDetailService.getBySheetId(sheet.getId());
        addStock(sheet, details);
        saleOutSheetService.refreshCostPrice(vo.getOrderDate());
        productHotnessService.increment(
                vo.getProducts().stream().map(ReceiveProductVo::getProductId).collect(Collectors.toList()));

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);

        return sheet.getId();
    }

    @OpLog(type = PurchaseOpLogType.class, name = "修改采购收货单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改收货单")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateReceiveSheetVo vo) {

        ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("采购收货单不存在！");
        }

        if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
            throw new DefaultClientException("采购收货单已审核通过，无法修改！");
        }

        if (sheet.getSettleStatus() != SettleStatus.UN_CHECK_BILL) {
            throw new DefaultClientException("采购收货单已进入对账/结算流程，无法修改！");
        }

        if (sheet.getStatus() != ReceiveSheetStatus.CREATED
                && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {
            throw new DefaultClientException("采购收货单无法修改！");
        }

        List<ReceiveSheetDetail> oldDetails = receiveSheetDetailService.getBySheetId(sheet.getId());
        boolean stockSynced = hasStockSynced(sheet.getId());
        if (stockSynced) {
            rollbackStock(sheet, oldDetails);
        }

        // 删除采购收货单明细
        Wrapper<ReceiveSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(ReceiveSheetDetail.class)
                .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
        receiveSheetDetailService.remove(deleteDetailWrapper);

        this.create(sheet, vo);

        sheet.setStatus(ReceiveSheetStatus.CREATED);

        List<ReceiveSheetStatus> statusList = new ArrayList<>();
        statusList.add(ReceiveSheetStatus.CREATED);
        statusList.add(ReceiveSheetStatus.APPROVE_REFUSE);

        Wrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getApproveBy, null).set(ReceiveSheet::getApproveTime, null)
                .set(ReceiveSheet::getRefuseReason, StringPool.EMPTY_STR)
                .eq(ReceiveSheet::getId, sheet.getId()).in(ReceiveSheet::getStatus, statusList);
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
        }

        List<ReceiveSheetDetail> details = receiveSheetDetailService.getBySheetId(sheet.getId());
        addStock(sheet, details);
        saleOutSheetService.refreshCostPrice(vo.getOrderDate());
        productHotnessService.increment(
                vo.getProducts().stream().map(ReceiveProductVo::getProductId).collect(Collectors.toList()));

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = PurchaseOpLogType.class, name = "修改采购收货单备注，单号：{}", params = "#code")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDescription(UpdateReceiveSheetDescriptionVo vo) {

        ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("采购收货单不存在！");
        }

        Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getDescription, vo.getDescription())
                .eq(ReceiveSheet::getId, sheet.getId());
        if (getBaseMapper().update(updateWrapper) != 1) {
            throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = PurchaseOpLogType.class, name = "审核通过采购收货单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approvePass(ApprovePassReceiveSheetVo vo) {

        ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("采购收货单不存在！");
        }

        if (sheet.getStatus() != ReceiveSheetStatus.CREATED
                && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {

            if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("采购收货单已审核通过，不允许继续执行审核！");
            }

            throw new DefaultClientException("采购收货单无法审核通过！");
        }

        sheet.setStatus(ReceiveSheetStatus.APPROVE_PASS);

        List<ReceiveSheetStatus> statusList = new ArrayList<>();
        statusList.add(ReceiveSheetStatus.CREATED);
        statusList.add(ReceiveSheetStatus.APPROVE_REFUSE);

        LambdaUpdateWrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
                .set(ReceiveSheet::getApproveTime, LocalDateTime.now())
                .eq(ReceiveSheet::getId, sheet.getId()).in(ReceiveSheet::getStatus, statusList);
        if (!StringUtil.isBlank(vo.getDescription())) {
            updateOrderWrapper.set(ReceiveSheet::getDescription, vo.getDescription());
        }
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @Transactional(rollbackFor = Exception.class)
    @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
    @Override
    public String directApprovePass(CreateReceiveSheetVo vo) {

        ReceiveSheetService thisService = getThis(this.getClass());

        String sheetId = thisService.create(vo);

        ApprovePassReceiveSheetVo approvePassVo = new ApprovePassReceiveSheetVo();
        approvePassVo.setId(sheetId);
        approvePassVo.setDescription(vo.getDescription());

        thisService.approvePass(approvePassVo);

        return sheetId;
    }

    @OpLog(type = PurchaseOpLogType.class, name = "审核拒绝采购收货单，单号：{}", params = "#code")
    @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approveRefuse(ApproveRefuseReceiveSheetVo vo) {

        ReceiveSheet sheet = getBaseMapper().selectById(vo.getId());
        if (sheet == null) {
            throw new InputErrorException("采购收货单不存在！");
        }

        if (sheet.getStatus() != ReceiveSheetStatus.CREATED) {

            if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("采购收货单已审核通过，不允许继续执行审核！");
            }

            if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_REFUSE) {
                throw new DefaultClientException("采购收货单已审核拒绝，不允许继续执行审核！");
            }

            throw new DefaultClientException("采购收货单无法审核拒绝！");
        }

        sheet.setStatus(ReceiveSheetStatus.APPROVE_REFUSE);

        LambdaUpdateWrapper<ReceiveSheet> updateOrderWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
                .set(ReceiveSheet::getApproveTime, LocalDateTime.now())
                .set(ReceiveSheet::getRefuseReason, vo.getRefuseReason())
                .eq(ReceiveSheet::getId, sheet.getId())
                .eq(ReceiveSheet::getStatus, ReceiveSheetStatus.CREATED);
        if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
            throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
        OpLogUtil.setExtra(vo);
    }

    @OpLog(type = PurchaseOpLogType.class, name = "删除采购收货单，单号：{}", params = "#code")
    @OrderTimeLineLog(orderId = "#id", delete = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(String id) {

        Assert.notBlank(id);
        ReceiveSheet sheet = getBaseMapper().selectById(id);
        if (sheet == null) {
            throw new InputErrorException("采购收货单不存在！");
        }

        if (sheet.getStatus() != ReceiveSheetStatus.CREATED
                && sheet.getStatus() != ReceiveSheetStatus.APPROVE_REFUSE) {

            if (sheet.getStatus() == ReceiveSheetStatus.APPROVE_PASS) {
                throw new DefaultClientException("“审核通过”的采购收货单不允许执行删除操作！");
            }

            throw new DefaultClientException("采购收货单无法删除！");
        }

        if (hasStockSynced(sheet.getId())) {
            rollbackStock(sheet, receiveSheetDetailService.getBySheetId(sheet.getId()));
        }

        // 删除订单明细
        Wrapper<ReceiveSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(ReceiveSheetDetail.class)
                .eq(ReceiveSheetDetail::getSheetId, sheet.getId());
        receiveSheetDetailService.remove(deleteDetailWrapper);

        // 删除组合商品明细
        Wrapper<ReceiveSheetDetailBundle> deleteBundleWrapper = Wrappers.lambdaQuery(
                ReceiveSheetDetailBundle.class).eq(ReceiveSheetDetailBundle::getSheetId, sheet.getId());
        receiveSheetDetailBundleService.remove(deleteBundleWrapper);

        // 删除订单
        Wrapper<ReceiveSheet> deleteWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .eq(ReceiveSheet::getId, id)
                .in(ReceiveSheet::getStatus, ReceiveSheetStatus.CREATED, ReceiveSheetStatus.APPROVE_REFUSE);
        if (!remove(deleteWrapper)) {
            throw new DefaultClientException("采购收货单信息已过期，请刷新重试！");
        }

        OpLogUtil.setVariable("code", sheet.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setUnCheckBill(String id) {

        Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getSettleStatus, SettleStatus.UN_CHECK_BILL)
                .eq(ReceiveSheet::getId, id);
        return getBaseMapper().update(updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setUnSettle(String id) {

        Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE)
                .eq(ReceiveSheet::getId, id);
        int count = getBaseMapper().update(updateWrapper);

        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setPartSettle(String id) {

        Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getSettleStatus, SettleStatus.PART_SETTLE).eq(ReceiveSheet::getId, id)
                .in(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
        int count = getBaseMapper().update(updateWrapper);

        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setSettled(String id) {

        Wrapper<ReceiveSheet> updateWrapper = Wrappers.lambdaUpdate(ReceiveSheet.class)
                .set(ReceiveSheet::getSettleStatus, SettleStatus.SETTLED)
                .eq(ReceiveSheet::getId, id)
                .in(ReceiveSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
        int count = getBaseMapper().update(updateWrapper);

        return count;
    }

    @Override
    public List<ReceiveSheet> getApprovedList(String supplierId, LocalDateTime startTime,
            LocalDateTime endTime, SettleStatus settleStatus) {

        return getBaseMapper().getApprovedList(supplierId, startTime, endTime, settleStatus);
    }

    @Override
    public void exportDetailDailySummary(QueryReceiveSheetVo vo) {
        List<QueryReceiveSheetDetailDto> details = getBaseMapper().queryDetail(vo);
        if (CollectionUtils.isEmpty(details)) {
            MultiSheetsData<ReceiveSheetDetailExportModel> sheetData = new MultiSheetsData<>();
            sheetData.setSheetName("明细");
            sheetData.setHeadClazz(ReceiveSheetDetailExportModel.class);
            sheetData.setData(new ArrayList<>());
            ExcelUtil.writeWithSheets("采购收货单明细按天汇总", Lists.newArrayList(sheetData));
            return;
        }

        Map<String, List<QueryReceiveSheetDetailDto>> detailGroup = details.stream()
                .collect(Collectors.groupingBy(QueryReceiveSheetDetailDto::getOrderDate,
                        LinkedHashMap::new, Collectors.toList()));

        List<MultiSheetsData<ReceiveSheetDetailExportModel>> sheetDatas = new ArrayList<>(detailGroup.size());
        detailGroup.forEach((orderDate, dayDetails) -> {
            MultiSheetsData<ReceiveSheetDetailExportModel> sheetData = new MultiSheetsData<>();
            sheetData.setSheetName(orderDate);
            sheetData.setHeadClazz(ReceiveSheetDetailExportModel.class);
            sheetData.setData(buildDailySummaryExportModels(dayDetails));
            sheetDatas.add(sheetData);
        });

        List<MultiSheetsData<ReceiveSheetDetailExportModel>> sortedDatas = sheetDatas.stream()
                .sorted(Comparator.comparing(MultiSheetsData::getSheetName))
                .collect(Collectors.toList());
        ExcelUtil.writeWithSheets("采购收货单明细按天汇总", sortedDatas);
    }

    private void create(ReceiveSheet sheet, CreateReceiveSheetVo vo) {

        if (!StringUtil.isBlank(vo.getScId())) {
            StoreCenter sc = storeCenterService.findById(vo.getScId());
            if (sc == null) {
                throw new InputErrorException("仓库不存在！");
            }

            sheet.setScId(vo.getScId());
        }

        Supplier supplier = supplierService.findById(vo.getSupplierId());
        if (supplier == null) {
            throw new InputErrorException("供应商不存在！");
        }
        sheet.setSupplierId(vo.getSupplierId());

        if (!StringUtil.isBlank(vo.getPurchaserId())) {
            SysUser purchaser = userService.findById(vo.getPurchaserId());
            if (purchaser == null) {
                throw new InputErrorException("采购员不存在！");
            }

            sheet.setPurchaserId(vo.getPurchaserId());
        }

        sheet.setOrderDate(vo.getOrderDate());
        sheet.setReceiveDate(vo.getReceiveDate());

        BigDecimal purchaseNum = BigDecimal.ZERO;
        BigDecimal businessTotalNum = BigDecimal.ZERO;
        BigDecimal giftNum = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReceiveProductVo productVo : vo.getProducts()) {
            Product product = productService.findById(productVo.getProductId());
            if (product == null) {
                throw new InputErrorException("第" + productVo.getSeq() + "行商品不存在！");
            }
            ProductUnit unit = resolveUnit(product, productVo.getUnitId(), productVo.getUnit());
            BigDecimal baseNum = NumberUtil.mul(productVo.getReceiveNum(), unit.getConversionRate());
            purchaseNum = NumberUtil.add(purchaseNum, baseNum);
            businessTotalNum = NumberUtil.add(businessTotalNum, productVo.getReceiveNum());
            BigDecimal taxAmount = NumberUtil.getNumber(
                    NumberUtil.mul(productVo.getReceiveNum(), productVo.getPurchasePrice()), 2);
            totalAmount = NumberUtil.add(totalAmount, taxAmount);

            ReceiveSheetDetail detail = new ReceiveSheetDetail();
            detail.setId(IdUtil.getId());
            detail.setSheetId(sheet.getId());

            if (!NumberUtil.isNumberPrecision(productVo.getPurchasePrice(), 6)) {
                throw new InputErrorException("第" + productVo.getSeq() + "行商品采购价最多允许6位小数！");
            }

            if (!NumberUtil.isNumberPrecision(productVo.getReceiveNum(), 8)) {
                throw new InputErrorException("第" + productVo.getSeq() + "行商品收货数量最多允许8位小数！");
            }

            detail.setProductId(productVo.getProductId());
            detail.setOrderNum(baseNum);
            detail.setUnitId(unit.getId());
            detail.setUnitName(unit.getUnitName());
            detail.setConversionRate(unit.getConversionRate());
            detail.setBusinessNum(productVo.getReceiveNum());
            detail.setTaxPrice(productVo.getPurchasePrice());
            detail.setTaxAmount(taxAmount);
            detail.setTaxRate(product.getTaxRate());
            detail.setDescription(StringUtil.isBlank(productVo.getDescription()) ? StringPool.EMPTY_STR
                    : productVo.getDescription());
            detail.setOrderNo(productVo.getSeq());
            detail.setActualDate(productVo.getActualDate());
            receiveSheetDetailService.save(detail);
            updateProductPrice(product, detail);
            productLatestPriceCacheService.updateLatestPrice(product.getId(), null,
                    toBasePrice(detail.getTaxPrice(), detail.getConversionRate()));
        }
        BigDecimal actualTotalAmount = this.normalizeTotalAmount(vo.getTotalAmount(), totalAmount);
        sheet.setTotalNum(businessTotalNum);
        sheet.setTotalGiftNum(giftNum);
        sheet.setTotalAmount(actualTotalAmount);
        sheet.setPaidAmount(this.normalizePaidAmount(vo.getPaidAmount(), actualTotalAmount));
        sheet.setDescription(StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription());
        sheet.setSettleStatus(this.getInitSettleStatus(supplier));
    }

    private ProductUnit resolveUnit(Product product, String unitId, String unitName) {
        ProductUnit unit = StringUtil.isNotBlank(unitId)
                ? productUnitService.getAvailableById(product.getId(), unitId) : null;
        if (unit == null) {
            unit = StringUtil.isBlank(unitName)
                    ? productUnitService.getAvailableByProductId(product.getId()).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getBaseUnit())).findFirst().orElse(null)
                    : productUnitService.getAvailableByUnitName(product.getId(), unitName);
        }
        if (unit == null) {
            throw new InputErrorException("商品单位不存在或已停用！");
        }
        return unit;
    }

    /**
     * 新增库存
     * 
     * @param sheet
     * @param details
     */
    private void addStock(ReceiveSheet sheet, List<ReceiveSheetDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        for (ReceiveSheetDetail detail : details) {
            if (detail.getOrderNum() == null || detail.getOrderNum().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            ProductStockPendingCostResolveDto resolveDto = productStockPendingCostService.settle(
                    sheet, detail, ProductStockBizType.PURCHASE);

            AddProductStockVo addProductStockVo = new AddProductStockVo();
            addProductStockVo.setProductId(detail.getProductId());
            addProductStockVo.setScId(sheet.getScId());
            addProductStockVo.setStockNum(detail.getOrderNum());
            addProductStockVo.setTaxAmount(resolveDto.getRemainTaxAmount());
            addProductStockVo.setLogTaxAmount(detail.getTaxAmount());
            addProductStockVo.setTaxPrice(detail.getTaxPrice());
            addProductStockVo.setBizId(sheet.getId());
            addProductStockVo.setBizDetailId(detail.getId());
            addProductStockVo.setBizCode(sheet.getCode());
            addProductStockVo.setBizType(ProductStockBizType.PURCHASE.getCode());

            productStockService.addStock(addProductStockVo);
        }
    }

    /**
     * 修改时，先回滚库存，然后再新增库存；
     * 
     * @param sheet
     * @param details
     */
    private void rollbackStock(ReceiveSheet sheet, List<ReceiveSheetDetail> details) {
        if (CollectionUtil.isEmpty(details)) {
            return;
        }

        for (ReceiveSheetDetail detail : details) {
            if (detail.getOrderNum() == null || detail.getOrderNum().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal settledTaxAmount = productStockPendingCostService.rollback(detail.getId(),
                    ProductStockBizType.PURCHASE);
            SubProductStockVo subProductStockVo = new SubProductStockVo();
            subProductStockVo.setProductId(detail.getProductId());
            subProductStockVo.setScId(sheet.getScId());
            subProductStockVo.setStockNum(detail.getOrderNum());
            subProductStockVo.setTaxAmount(NumberUtil.getNumber(
                    NumberUtil.sub(detail.getTaxAmount(), settledTaxAmount), 2));
            subProductStockVo.setBizId(sheet.getId());
            subProductStockVo.setBizDetailId(detail.getId());
            subProductStockVo.setBizCode(sheet.getCode());
            subProductStockVo.setBizType(ProductStockBizType.PURCHASE.getCode());

            productStockService.subStock(subProductStockVo);
        }
    }

    /**
     * 是否有库存变动记录。
     * 
     * @param sheetId
     * @return
     */
    private boolean hasStockSynced(String sheetId) {
        Wrapper<ProductStockLog> queryWrapper = Wrappers.lambdaQuery(ProductStockLog.class)
                .eq(ProductStockLog::getBizId, sheetId)
                .eq(ProductStockLog::getBizType, ProductStockBizType.PURCHASE);
        return productStockLogService.count(queryWrapper) > 0;
    }

    /**
     * 更新商品价格
     * 
     * @param product
     * @param detail
     */
    private void updateProductPrice(Product product, ReceiveSheetDetail detail) {
        QuerySysParameterVo sysParameterVo = new QuerySysParameterVo();
        sysParameterVo.setPmKey("latest_price_override_product_price");
        List<SysParameter> list = sysParameterService.query(sysParameterVo);
        if (CollectionUtil.isEmpty(list)) {
            return;
        }

        boolean override = BooleanUtil.toBoolean(list.get(0).getPmValue());
        if (override) {
            productService.updatePrice(product.getId(), null,
                    toBasePrice(detail.getTaxPrice(), detail.getConversionRate()));
        }
    }

    private BigDecimal toBasePrice(BigDecimal price, BigDecimal conversionRate) {
        BigDecimal rate = conversionRate == null ? BigDecimal.ONE : conversionRate;
        return price.divide(rate, 6, RoundingMode.HALF_UP);
    }

    /**
     * 根据供应商获取初始结算状态
     *
     * @param supplier
     * @return
     */
    private SettleStatus getInitSettleStatus(Supplier supplier) {

        if (supplier.getManageType() == ManageType.DISTRIBUTION) {
            return SettleStatus.UN_CHECK_BILL;
        } else {
            return SettleStatus.UN_REQUIRE;
        }
    }

    private BigDecimal normalizeTotalAmount(BigDecimal totalAmount, BigDecimal detailTotalAmount) {
        BigDecimal actualTotalAmount = totalAmount == null ? detailTotalAmount : totalAmount;
        if (NumberUtil.lt(actualTotalAmount, BigDecimal.ZERO)) {
            throw new InputErrorException("折后金额不允许小于0！");
        }

        if (!NumberUtil.isNumberPrecision(actualTotalAmount, 2)) {
            throw new InputErrorException("折后金额最多允许2位小数！");
        }

        if (NumberUtil.gt(actualTotalAmount, detailTotalAmount)) {
            throw new InputErrorException("折后金额不允许大于明细金额！");
        }

        return actualTotalAmount;
    }

    private BigDecimal normalizePaidAmount(BigDecimal paidAmount, BigDecimal totalAmount) {
        // 未付款则=0
        BigDecimal actualPaidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        if (NumberUtil.lt(actualPaidAmount, BigDecimal.ZERO)) {
            throw new InputErrorException("付款金额不允许小于0！");
        }

        if (!NumberUtil.isNumberPrecision(actualPaidAmount, 6)) {
            throw new InputErrorException("付款金额最多允许6位小数！");
        }

        if (NumberUtil.gt(actualPaidAmount, totalAmount)) {
            throw new InputErrorException("付款金额不允许大于单据总金额！");
        }

        return actualPaidAmount;
    }

    private void adjustSupplierAmount(String supplierId) {

        if (StringUtil.isBlank(supplierId)) {
            return;
        }

        List<ReceiveSheet> sheets = this.list(Wrappers.lambdaQuery(ReceiveSheet.class)
                .eq(ReceiveSheet::getSupplierId, supplierId));

        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        for (ReceiveSheet item : sheets) {
            BigDecimal itemPaidAmount = item.getPaidAmount() == null ? BigDecimal.ZERO : item.getPaidAmount();
            BigDecimal itemTotalAmount = item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount();
            paidAmount = NumberUtil.add(paidAmount, itemPaidAmount);
            unpaidAmount = NumberUtil.add(unpaidAmount, NumberUtil.sub(itemTotalAmount, itemPaidAmount));
        }

        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setPaidAmount(paidAmount);
        supplier.setUnpaidAmount(unpaidAmount);
        if (!supplierService.updateById(supplier)) {
            throw new DefaultClientException("供应商金额更新失败，请重试！");
        }
    }

    @Override
    public List<ReceiveProductVo> checkImport(List<ReceiveSheetImportModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }
        handleSeq(list);
        // 匹配编号
        List<String> errors = checkImportData(list);
        Assert.isTrue(CollectionUtils.isEmpty(errors), StringUtils.join(errors, "；\r\n"));

        return list.stream()
                .map(item -> BeanUtil.copyProperties(item, ReceiveProductVo.class))
                .collect(Collectors.toList());
    }

    private void handleSeq(List<ReceiveSheetImportModel> list) {
        for (int i = 0; i < list.size(); i++) {
            ReceiveSheetImportModel model = list.get(i);
            if (model.getSeq() == null) {
                model.setSeq(i + 2);
            }
        }
    }

    private List<String> checkImportData(List<ReceiveSheetImportModel> list) {
        List<String> productNames = list.stream().map(ReceiveSheetImportModel::getProductName)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productService.selectByProductName(productNames);
        Map<String, List<Product>> nameUnitMap = new HashMap<>();
        for (Product product : products) {
            productUnitService.getAvailableByProductId(product.getId()).stream()
                    .forEach(unit -> nameUnitMap.computeIfAbsent(
                            buildProductImportKey(product.getName(), unit.getUnitName()), key -> new ArrayList<>())
                            .add(product));
        }

        List<String> errors = Lists.newArrayList();
        for (int i = 0; i < list.size(); i++) {
            ReceiveSheetImportModel data = list.get(i);
            int rowIndex = data.getSeq();

            if (StringUtils.isBlank(data.getProductName())) {
                errors.add("第" + rowIndex + "行“商品名称”不能为空");
            }
            if (StringUtils.isBlank(data.getUnit())) {
                errors.add("第" + rowIndex + "行“单位”不能为空");
            }
            errors.addAll(validateImportNumbers(data));

            Product product = matchImportProduct(data, nameUnitMap);
            if (product != null) {
                ProductUnit unit = productUnitService.getAvailableByUnitName(product.getId(),
                        StringUtils.trim(data.getUnit()));
                if (unit == null) {
                    continue;
                }
                data.setProductCode(product.getCode());
                data.setProductId(product.getId());
                data.setUnitId(unit.getId());
                data.setSpec(product.getSpec());
                BigDecimal defaultPurchasePrice = productLatestPriceCacheService
                        .getLatestPurchasePrice(product.getId());
                if (data.getPurchasePrice() == null) {
                    data.setPurchasePrice(defaultPurchasePrice == null ? BigDecimal.ZERO
                            : NumberUtil.mul(defaultPurchasePrice, unit.getConversionRate()));
                }
            }

        }
        return errors;
    }

    static List<String> validateImportNumbers(ReceiveSheetImportModel data) {
        List<String> errors = Lists.newArrayList();
        int rowIndex = data.getSeq();
        if (data.getReceiveNum() != null && NumberUtil.lt(data.getReceiveNum(), BigDecimal.ZERO)) {
            errors.add("第" + rowIndex + "行“数量”不允许小于0");
        }
        if (data.getReceiveNum() != null && !NumberUtil.isNumberPrecision(data.getReceiveNum(), 8)) {
            errors.add("第" + rowIndex + "行“数量”最多允许8位小数");
        }
        if (data.getPurchasePrice() != null && NumberUtil.lt(data.getPurchasePrice(), BigDecimal.ZERO)) {
            errors.add("第" + rowIndex + "行“单价”不允许小于0");
        }
        if (data.getPurchasePrice() != null && !NumberUtil.isNumberPrecision(data.getPurchasePrice(), 6)) {
            errors.add("第" + rowIndex + "行“单价”最多允许6位小数");
        }
        return errors;
    }

    private String buildProductImportKey(String productName, String unit) {
        return StringUtils.trimToEmpty(productName) + StringPool.STR_SPLIT
                + StringUtils.trimToEmpty(unit);
    }

    private Product matchImportProduct(ReceiveSheetImportModel data,
                                       Map<String, List<Product>> nameUnitMap) {
        if (StringUtils.isBlank(data.getProductName()) || StringUtils.isBlank(data.getUnit())) {
            return null;
        }

        List<Product> candidates = nameUnitMap.get(buildProductImportKey(data.getProductName(), data.getUnit()));
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        String spec = StringUtils.trimToEmpty(data.getSpec());
        List<Product> specMatchedProducts = candidates.stream()
                .filter(item -> StringUtils.equals(StringUtils.trimToEmpty(item.getSpec()), spec))
                .collect(Collectors.toList());

        return specMatchedProducts.size() == 1 ? specMatchedProducts.get(0) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<String> importByQuery(List<ReceiveSheetQueryImportModel> list) {
        if (CollectionUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        for (int i = 0; i < list.size(); i++) {
            ReceiveSheetQueryImportModel model = list.get(i);
            model.setSeq(i + 2);
            normalizeQueryImportNumbers(model);
        }
        ReceiveSheetService thisService = getThis(this.getClass());
        Map<String, List<ReceiveSheetQueryImportModel>> map = list.stream().collect(
                Collectors.groupingBy(item -> item.getOrderDate() + item.getSupplierName()));

        return map.keySet().stream().map(item -> thisService.create(buildCreateVo(map.get(item))))
                .collect(Collectors.toList());
    }

    static void normalizeQueryImportNumbers(ReceiveSheetQueryImportModel model) {
        if (model.getReceiveNum() == null) {
            model.setReceiveNum(BigDecimal.ZERO);
        }
    }

    private CreateReceiveSheetVo buildCreateVo(List<ReceiveSheetQueryImportModel> list) {
        ReceiveSheetQueryImportModel model = list.get(0);
        List<Supplier> suppliers = supplierService.queryByNames(Lists.newArrayList(model.getSupplierName()));
        Assert.notEmpty(suppliers, "供应商不存在：" + model.getSupplierName());

        CreateReceiveSheetVo res = new CreateReceiveSheetVo();
        res.setSupplierId(suppliers.get(0).getId());
        res.setOrderDate(DateUtil.parseDate(model.getOrderDate(), "yyyyMMdd"));
        res.setProducts(buildProducts(list));
        res.setScId(storeCenterService.getDefaultStoreId());

        return res;
    }

    private List<ReceiveProductVo> buildProducts(List<ReceiveSheetQueryImportModel> list) {
        if (CollectionUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        List<ReceiveSheetImportModel> collect = list.stream()
                .map(item -> BeanUtil.copyProperties(item, ReceiveSheetImportModel.class)).collect(Collectors.toList());
        List<ReceiveProductVo> checked = checkImport(collect);
        List<ReceiveProductVo> products = checked.stream()
                .map(item -> BeanUtil.copyProperties(item, ReceiveProductVo.class))
                .collect(Collectors.toList());

        for (int i = 0; i < products.size(); i++) {
            products.get(i).setActualDate(parseActualDate(list.get(i).getActualDate(), list.get(i).getSeq()));
        }

        return products;
    }

    private LocalDate parseActualDate(String value, Integer rowIndex) {
        if (StringUtil.isBlank(value)) {
            return null;
        }

        String text = value.trim();
        int blankIndex = text.indexOf(' ');
        if (blankIndex > 0) {
            text = text.substring(0, blankIndex);
        }
        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            text = text.substring(0, tIndex);
        }

        try {
            return LocalDate.parse(text, QUERY_IMPORT_ACTUAL_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DefaultClientException("第" + rowIndex + "行“配送日期”格式错误，正确格式为yyyy-MM-dd");
        }
    }

    private List<ReceiveSheetDetailExportModel> buildDailySummaryExportModels(
            List<QueryReceiveSheetDetailDto> details) {
        if (CollectionUtils.isEmpty(details)) {
            return new ArrayList<>();
        }

        Map<String, ReceiveSheetDetailExportModel> summaryMap = new LinkedHashMap<>();
        for (QueryReceiveSheetDetailDto detail : details) {
            String productKey = StringUtil.isBlank(detail.getProductId()) ? detail.getProductCode()
                    : detail.getProductId();
            ReceiveSheetDetailExportModel current = buildDetailExportModel(detail);
            ReceiveSheetDetailExportModel summary = summaryMap.get(productKey);
            if (summary == null) {
                summaryMap.put(productKey, current);
                continue;
            }

            summary.setOrderNum(NumberUtil.add(defaultValue(summary.getOrderNum()),
                    defaultValue(current.getOrderNum())));
            summary.setTaxAmount(NumberUtil.add(defaultValue(summary.getTaxAmount()),
                    defaultValue(current.getTaxAmount())));
        }

        return new ArrayList<>(summaryMap.values());
    }

    private ReceiveSheetDetailExportModel buildDetailExportModel(QueryReceiveSheetDetailDto detail) {
        ReceiveSheetDetailExportModel model = new ReceiveSheetDetailExportModel();
        model.setOrderDate(detail.getOrderDate());
        model.setSupplierName(detail.getSupplierName());
        model.setProductCode(detail.getProductCode());
        model.setProductName(detail.getProductName());
        // model.setShortName();
        model.setSpec(detail.getSpec());
        model.setUnit(detail.getUnit());
        model.setCategoryName(detail.getCategoryName());
        model.setTaxPrice(detail.getTaxPrice());
        model.setOrderNum(detail.getOrderNum());
        model.setTaxAmount(detail.getTaxAmount());
        model.setDescription(detail.getDescription());
        return model;
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public List<ReceiveSheet> selectByIds(List<String> ids) {
        return getBaseMapper().selectBatchIds(ids);
    }
}
