package com.lframework.xingyun.basedata.impl.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.BeanUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.xingyun.basedata.bo.quote.*;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel;
import com.lframework.xingyun.basedata.entity.quote.*;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.*;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.quote.*;
import com.lframework.xingyun.basedata.vo.quote.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报价单服务实现。
 */
@Service
public class QuoteSheetServiceImpl extends BaseMpServiceImpl<QuoteSheetMapper, QuoteSheet> implements QuoteSheetService {
    @Autowired
    private QuoteSheetDetailMapper quoteSheetDetailMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private QuoteSheetConverter quoteSheetConverter;
    @Autowired(required = false)
    private List<QuoteSheetReferenceChecker> quoteSheetReferenceCheckers = Collections.emptyList();

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductUnitService productUnitService;
    /**
     * 校验导入商品，仅匹配非停用商品。
     */
    @Override
    public List<QuoteSheetImportModel> checkImport(List<QuoteSheetImportModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }

        handleSeq(list);

        // 匹配编号
        List<String> errors = checkImportData(list);
        Assert.isTrue(CollectionUtils.isEmpty(errors), StringUtils.join(errors, ";\r\n"));

        return list;
    }

    private List<String> checkImportData(List<QuoteSheetImportModel> list) {
        List<String> productNames = list.stream().map(QuoteSheetImportModel::getName)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productService.selectByProductName(productNames);
        Map<String, List<Product>> nameUnitMap = new HashMap<>();
        for (Product product : products) {
            productUnitService.getAvailableByProductId(product.getId())
                    .forEach(unit -> nameUnitMap.computeIfAbsent(
                                    buildProductImportKey(product.getName(), unit.getUnitName()), key -> new ArrayList<>())
                            .add(product));
        }

        List<String> errors = Lists.newArrayList();
        for (int i = 0; i < list.size(); i++) {
            QuoteSheetImportModel data = list.get(i);
            int rowIndex = data.getSeq();

            if (StringUtils.isBlank(data.getName())) {
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
                data.setCode(product.getCode());
                data.setProductId(product.getId());
                data.setUnitId(unit.getId());
                data.setSpec(product.getSpec());
                data.setUnit(unit.getUnitName());
            }
        }
        return errors;
    }

    private Product matchImportProduct(QuoteSheetImportModel data, Map<String, List<Product>> nameUnitMap) {
        if (StringUtils.isBlank(data.getName()) || StringUtils.isBlank(data.getUnit())) {
            return null;
        }

        List<Product> candidates = nameUnitMap.get(buildProductImportKey(data.getName(), data.getUnit()));
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

    static List<String> validateImportNumbers(QuoteSheetImportModel data) {
        List<String> errors = Lists.newArrayList();
        int rowIndex = data.getSeq();
        if (data.getSalePrice() != null && NumberUtil.lt(data.getSalePrice(), BigDecimal.ZERO)) {
            errors.add("第" + rowIndex + "行“单价”不允许小于0");
        }
        if (data.getSalePrice() != null && !NumberUtil.isNumberPrecision(data.getSalePrice(), 6)) {
            errors.add("第" + rowIndex + "行“单价”最多允许6位小数");
        }
        return errors;
    }

    public static String buildProductImportKey(String productName, String unit) {
        return StringUtils.trimToEmpty(productName) + StringPool.STR_SPLIT
                + StringUtils.trimToEmpty(unit);
    }

    private void handleSeq(List<QuoteSheetImportModel> list) {
        for (int i = 0; i < list.size(); i++) {
            QuoteSheetImportModel model = list.get(i);
            if (model.getSeq() == null) {
                model.setSeq(i + 2);
            }
        }
    }

    /**
     * 创建报价单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateQuoteSheetVo vo) {
        validateSave(vo, null);
        QuoteSheet sheet = quoteSheetConverter.toEntity(vo);
        sheet.setId(IdUtil.getId());
        sheet.setTenantId(currentTenantId());
        sheet.setStatus(QuoteSheetStatus.DISABLED);
        getBaseMapper().insert(sheet);
        saveDetails(sheet.getId(), sheet.getTenantId(), vo.getProducts());
        return sheet.getId();
    }

    /**
     * 更新报价单及其明细。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateQuoteSheetVo vo) {
        List<QuoteSheet> lockedSheets = lockTenantQuoteSheets();
        QuoteSheet existed = requireLockedSheet(vo.getId(), lockedSheets);
        validateSave(vo, vo.getId(), lockedSheets);
        QuoteSheet sheet = quoteSheetConverter.toEntity(vo);
        sheet.setId(existed.getId());
        sheet.setStatus(existed.getStatus());
        getBaseMapper().updateById(sheet);
        quoteSheetDetailMapper.delete(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, vo.getId()));
        saveDetails(vo.getId(), existed.getTenantId(), vo.getProducts());
    }

    /**
     * 删除未被业务引用的报价单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        requireLockedSheet(id, lockTenantQuoteSheets());
        if (quoteSheetReferenceCheckers.stream().anyMatch(checker -> checker.hasReference(id)))
            throw new DefaultClientException("报价单已被销售单使用，不能删除！");
        quoteSheetDetailMapper.delete(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, id));
        getBaseMapper().deleteById(id);
    }

    /**
     * 启用报价单并复核定价周期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String id) {
        List<QuoteSheet> lockedSheets = lockTenantQuoteSheets();
        QuoteSheet sheet = requireLockedSheet(id, lockedSheets);
        List<QuoteSheetDetail> details = quoteSheetDetailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, id));
        validateSheetData(sheet.getStartDate(), sheet.getEndDate(), details.stream().map(QuoteSheetDetail::getProductId).collect(Collectors.toList()), id, lockedSheets);
        sheet.setStatus(QuoteSheetStatus.ENABLED);
        getBaseMapper().updateById(sheet);
    }

    /**
     * 停用报价单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(String id) {
        QuoteSheet sheet = requireLockedSheet(id, lockTenantQuoteSheets());
        sheet.setStatus(QuoteSheetStatus.DISABLED);
        getBaseMapper().updateById(sheet);
    }

    /**
     * 获取报价单详情及商品基础展示字段。
     */
    @Override
    public GetQuoteSheetBo get(String id) {
        QuoteSheet sheet = requireSheet(id);
        List<QuoteSheetDetail> details = quoteSheetDetailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class).eq(QuoteSheetDetail::getQuoteSheetId, id));
        Map<String, Product> products = details.isEmpty() ? Collections.emptyMap() : productMapper.selectList(Wrappers.lambdaQuery(Product.class).in(Product::getId, details.stream().map(QuoteSheetDetail::getProductId).collect(Collectors.toSet()))).stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<QuoteProductBo> result = details.stream().map(detail -> {
            Product product = StringUtil.isBlank(detail.getProductSnapshot())
                    ? products.get(detail.getProductId())
                    : JsonUtil.parseObject(detail.getProductSnapshot(), Product.class);
            QuoteProductBo productBo = quoteSheetConverter.toProductBo(product,
                    detail.getSalePrice(), detail.getQuoteSheetId());
            productBo.setSourceId(detail.getId());
            return productBo;
        }).collect(Collectors.toList());
        GetQuoteSheetBo bo = quoteSheetConverter.toGetBo(sheet);
        bo.setProducts(result);
        return bo;
    }

    /**
     * 分页查询报价单。
     */
    @Override
    public PageResult<QuoteSheet> query(Integer pageIndex, Integer pageSize, QueryQuoteSheetVo vo) {
        PageHelperUtil.startPage(pageIndex, pageSize);
        return PageResultUtil.convert(new PageInfo<>(getBaseMapper().selectList(Wrappers.lambdaQuery(QuoteSheet.class).eq(vo.getStatus() != null, QuoteSheet::getStatus, vo.getStatus()).like(StringUtil.isNotBlank(vo.getName()), QuoteSheet::getName, vo.getName()).ge(vo.getStartDate() != null, QuoteSheet::getStartDate, vo.getStartDate()).le(vo.getEndDate() != null, QuoteSheet::getEndDate, vo.getEndDate()).orderByDesc(QuoteSheet::getCreateTime))));
    }

    /**
     * 一次关联查询指定日期的已启用报价商品。
     */
    @Override
    public List<QuoteProductBo> getActiveQuoteProducts(QueryQuoteProductVo vo) {
        return getBaseMapper().getActiveQuoteProducts(vo);
    }

    /**
     * 校验保存请求。
     */
    private void validateSave(CreateQuoteSheetVo vo, String excludeId) {
        validateSave(vo, excludeId, lockTenantQuoteSheets());
    }

    /**
     * 使用已锁定的租户范围校验保存请求。
     */
    private void validateSave(CreateQuoteSheetVo vo, String excludeId, List<QuoteSheet> lockedSheets) {
        if (vo == null) throw new DefaultClientException("报价单不能为空！");
        List<QuoteSheetProductVo> products = vo.getProducts();
        validateSheetData(vo.getStartDate(), vo.getEndDate(), products == null ? Collections.emptyList() : products.stream().map(QuoteSheetProductVo::getProductId).collect(Collectors.toList()), excludeId, lockedSheets);
    }

    /**
     * 校验报价单日期、商品明细、重复商品及周期。
     */
    private void validateSheetData(LocalDate startDate, LocalDate endDate, List<String> productIds, String excludeId, List<QuoteSheet> lockedSheets) {
        assertBasicSheetData(startDate, endDate, productIds);
        assertNoDateRangeOverlap(excludeId, startDate, endDate, lockedSheets);
    }

    /**
     * 批量保存报价单明细。
     */
    void saveDetails(String quoteSheetId, String tenantId, List<QuoteSheetProductVo> products) {
        Map<String, Product> productMap = productMapper.selectList(Wrappers.lambdaQuery(Product.class)
                        .in(Product::getId, products.stream().map(QuoteSheetProductVo::getProductId)
                                .collect(Collectors.toSet())))
                .stream().collect(Collectors.toMap(Product::getId, product -> product));
        List<QuoteSheetDetail> details = products.stream().map(p -> {
            Product product = productMap.get(p.getProductId());
            if (product == null) {
                throw new DefaultClientException("商品不存在！");
            }
            QuoteSheetDetail d = quoteSheetConverter.toDetail(p, quoteSheetId);
            d.setId(IdUtil.getId());
            d.setTenantId(tenantId);
            d.setProductSnapshot(JsonUtil.toJsonString(product));
            return d;
        }).collect(Collectors.toList());
        quoteSheetDetailMapper.batchInsert(details);
    }

    /**
     * 获取不存在时报业务异常。
     */
    private QuoteSheet requireSheet(String id) {
        QuoteSheet result = getBaseMapper().selectById(id);
        if (result == null) throw new DefaultClientException("报价单不存在！");
        return result;
    }

    /**
     * 锁定当前租户范围内的报价单，作为所有写流程的统一首个锁。
     */
    List<QuoteSheet> lockTenantQuoteSheets() {
        return getBaseMapper().selectByTenantIdForUpdate(currentTenantId());
    }

    /**
     * 从已锁定的租户报价单范围中获取目标单据。
     */
    static QuoteSheet requireLockedSheet(String id, List<QuoteSheet> lockedSheets) {
        return lockedSheets.stream().filter(sheet -> Objects.equals(sheet.getId(), id)).findFirst().orElseThrow(() -> new DefaultClientException("报价单不存在！"));
    }

    /**
     * 获取当前租户 ID 的字符串形式。
     */
    private String currentTenantId() {
        return String.valueOf(TenantContextHolder.getTenantId());
    }

    /**
     * 判断两个日期闭区间是否重叠。
     */
    public static boolean isDateRangeOverlapped(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return !endA.isBefore(startB) && !startA.isAfter(endB);
    }

    /**
     * 校验报价周期不与其他未删除报价单重叠。
     */
    public static void assertNoDateRangeOverlap(String excludeId, LocalDate startDate, LocalDate endDate, List<QuoteSheet> sheets) {
        if (sheets.stream().filter(s -> !Objects.equals(s.getId(), excludeId)).anyMatch(s -> isDateRangeOverlapped(startDate, endDate, s.getStartDate(), s.getEndDate())))
            throw new DefaultClientException("定价周期与已有报价单冲突！");
    }

    /**
     * 校验同一报价单内商品不重复。
     */
    public static void assertNoDuplicatedProducts(List<QuoteSheetProductVo> products) {
        assertNoDuplicatedProductIds(products.stream().map(QuoteSheetProductVo::getProductId).collect(Collectors.toList()));
    }

    /**
     * 校验商品 ID 列表不重复。
     */
    public static void assertNoDuplicatedProductIds(List<String> productIds) {
        if (productIds.stream().collect(Collectors.toSet()).size() != productIds.size())
            throw new DefaultClientException("同一报价单商品不能重复！");
    }

    /**
     * 校验保存和启用共用的基础字段规则。
     */
    public static void assertBasicSheetData(LocalDate startDate, LocalDate endDate, List<String> productIds) {
        if (startDate == null || endDate == null) throw new DefaultClientException("生效日期不能为空！");
        if (startDate.isAfter(endDate)) throw new DefaultClientException("生效开始日期不能晚于生效结束日期！");
        if (productIds == null || productIds.isEmpty()) throw new DefaultClientException("商品明细不能为空！");
        assertNoDuplicatedProductIds(productIds);
    }

    /**
     * 判断报价单是否在指定日期生效。
     */
    public static boolean isActiveOn(QuoteSheet sheet, LocalDate orderDate) {
        return sheet.getStatus() == QuoteSheetStatus.ENABLED && !orderDate.isBefore(sheet.getStartDate()) && !orderDate.isAfter(sheet.getEndDate());
    }
}
