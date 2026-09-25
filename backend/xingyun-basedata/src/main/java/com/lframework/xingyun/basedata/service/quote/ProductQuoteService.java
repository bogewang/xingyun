package com.lframework.xingyun.basedata.service.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.bo.quote.QueryQuoteSheetBo;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.bo.product.info.QueryProductBo;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.enums.quote.QuoteSheetStatus;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import com.lframework.xingyun.basedata.vo.product.info.SaveProductQuoteVo;
import com.lframework.xingyun.basedata.vo.product.info.SaveProductQuoteVo.QuoteRow;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商品追加报价单业务，复用报价单锁以防止并发覆盖。 */
@Service
public class ProductQuoteService {
    @Autowired private QuoteSheetMapper sheetMapper;
    @Autowired private QuoteSheetDetailMapper detailMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private QuoteSheetConverter converter;

    /** 批量补全当前页商品所在报价单，包含已停用和已过期的报价单。 */
    public void fillQuoteSheetNames(List<QueryProductBo> products) {
        if (products == null || products.isEmpty()) return;
        products.forEach(product -> {
            product.setQuoteSheetNames("");
            product.setQuoteSheetIds(Collections.emptyList());
        });
        Set<String> productIds = products.stream().map(QueryProductBo::getId).collect(Collectors.toSet());
        List<QuoteSheetDetail> details = detailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class)
                .select(QuoteSheetDetail::getProductId, QuoteSheetDetail::getQuoteSheetId)
                .in(QuoteSheetDetail::getProductId, productIds));
        if (details.isEmpty()) return;
        Set<String> sheetIds = details.stream().map(QuoteSheetDetail::getQuoteSheetId).collect(Collectors.toSet());
        List<QuoteSheet> sheets = sheetMapper.selectList(Wrappers.lambdaQuery(QuoteSheet.class)
                .select(QuoteSheet::getId, QuoteSheet::getName)
                .in(QuoteSheet::getId, sheetIds).orderByDesc(QuoteSheet::getCreateTime)
                .orderByAsc(QuoteSheet::getId));
        Map<String, Set<String>> productSheets = new HashMap<>();
        for (QuoteSheetDetail detail : details) {
            productSheets.computeIfAbsent(detail.getProductId(), key -> new HashSet<>())
                    .add(detail.getQuoteSheetId());
        }
        for (QueryProductBo product : products) {
            Set<String> ids = productSheets.getOrDefault(product.getId(), Collections.emptySet());
            product.setQuoteSheetIds(sheets.stream().filter(sheet -> ids.contains(sheet.getId()))
                    .map(QuoteSheet::getId).collect(Collectors.toList()));
            product.setQuoteSheetNames(sheets.stream().filter(sheet -> ids.contains(sheet.getId()))
                    .map(QuoteSheet::getName).filter(Objects::nonNull).collect(Collectors.joining("、")));
        }
    }

    /** 查询商品在各报价单中的原价和询价标识。 */
    public List<QuoteProductBo> productDetails(String productId) {
        return converter.toProductBos(detailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class)
                .eq(QuoteSheetDetail::getProductId, productId)));
    }

    /** 返回商品维护页面可选的已启用报价单。 */
    public List<QueryQuoteSheetBo> options() {
        return sheetMapper.selectList(Wrappers.lambdaQuery(QuoteSheet.class)
                .eq(QuoteSheet::getStatus, QuoteSheetStatus.ENABLED)
                .orderByDesc(QuoteSheet::getCreateTime)).stream()
                .map(converter::toQueryBo).collect(Collectors.toList());
    }

    /** 同一事务追加多个报价单，重复提交保留既有报价。 */
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(String productId, List<String> sheetIds) {
        persistProductQuotes(productId, sheetIds, Collections.emptyMap());
    }

    /** 原子保存所选报价，保留已有明细ID、快照与顺序。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveProductQuotes(SaveProductQuoteVo vo) {
        if (vo.getQuotes() == null || vo.getQuotes().isEmpty())
            throw new DefaultClientException("请选择报价单！");
        Map<String, QuoteRow> rows = new LinkedHashMap<>();
        for (QuoteRow row : vo.getQuotes()) {
            if (row == null || row.getSalePrice() == null || row.getSalePrice().signum() < 0
                    || row.getSalePrice().stripTrailingZeros().scale() > 2 || row.getInquiryProduct() == null)
                throw new DefaultClientException("请填写有效价格（非负且最多2位小数）和是否询价！");
            if (rows.put(row.getQuoteSheetId(), row) != null)
                throw new DefaultClientException("报价单不能重复！");
        }
        persistProductQuotes(vo.getProductId(), new ArrayList<>(rows.keySet()), rows);
    }

    /** 共用批量写入流程；无编辑数据时仅追加，有编辑数据时更新对应明细。 */
    private void persistProductQuotes(String productId, List<String> sheetIds, Map<String, QuoteRow> rows) {
        if (sheetIds == null || sheetIds.isEmpty()) return;
        if (sheetIds.stream().anyMatch(id -> id == null || id.trim().isEmpty()))
            throw new DefaultClientException("报价单ID不能为空！");
        Set<String> ids = new LinkedHashSet<>(sheetIds);
        Set<String> existing = sheetMapper.selectAllForUpdate().stream()
                .map(QuoteSheet::getId).collect(Collectors.toSet());
        if (!existing.containsAll(ids)) throw new DefaultClientException("报价单不存在，请刷新后重试！");
        Product product = productMapper.selectById(productId);
        if (product == null) throw new DefaultClientException("商品不存在！");
        List<QuoteSheetDetail> details = detailMapper.selectList(Wrappers.lambdaQuery(QuoteSheetDetail.class)
                .in(QuoteSheetDetail::getQuoteSheetId, ids));
        Map<String, QuoteSheetDetail> present = new HashMap<>();
        Map<String, Integer> maxOrder = new HashMap<>();
        for (QuoteSheetDetail detail : details) {
            if (Objects.equals(productId, detail.getProductId())) present.put(detail.getQuoteSheetId(), detail);
            maxOrder.merge(detail.getQuoteSheetId(), detail.getOrderNo() == null ? 0 : detail.getOrderNo(), Math::max);
        }
        List<QuoteSheetDetail> additions = new ArrayList<>();
        for (String id : ids) {
            QuoteSheetDetail detail = present.get(id);
            QuoteRow row = rows.get(id);
            if (detail != null) {
                if (row != null) {
                    detail.setSalePrice(row.getSalePrice());
                    detail.setInquiryProduct(row.getInquiryProduct());
                    additions.add(detail);
                }
                continue;
            }
            detail = new QuoteSheetDetail();
            detail.setId(IdUtil.getId());
            detail.setQuoteSheetId(id);
            detail.setProductId(productId);
            detail.setProductSnapshot(JsonUtil.toJsonString(product));
            detail.setOrderNo(maxOrder.getOrDefault(id, 0) + 1);
            detail.setSalePrice(row == null ? BigDecimal.ZERO : row.getSalePrice());
            detail.setInquiryProduct(row == null ? true : row.getInquiryProduct());
            additions.add(detail);
        }
        if (!additions.isEmpty()) detailMapper.batchInsert(additions);
    }
}
