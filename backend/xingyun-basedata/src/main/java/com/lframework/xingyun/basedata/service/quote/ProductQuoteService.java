package com.lframework.xingyun.basedata.service.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.bo.quote.QueryQuoteSheetBo;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.mappers.ProductMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetMapper;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
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

    /** 返回商品维护页面可选报价单。 */
    public List<QueryQuoteSheetBo> options() {
        return sheetMapper.selectList(Wrappers.lambdaQuery(QuoteSheet.class)
                .orderByDesc(QuoteSheet::getCreateTime)).stream()
                .map(converter::toQueryBo).collect(Collectors.toList());
    }

    /** 同一事务追加多个报价单，重复提交保留既有报价。 */
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(String productId, List<String> sheetIds) {
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
        Set<String> present = new HashSet<>();
        Map<String, Integer> maxOrder = new HashMap<>();
        for (QuoteSheetDetail detail : details) {
            if (Objects.equals(productId, detail.getProductId())) present.add(detail.getQuoteSheetId());
            maxOrder.merge(detail.getQuoteSheetId(), detail.getOrderNo() == null ? 0 : detail.getOrderNo(), Math::max);
        }
        List<QuoteSheetDetail> additions = new ArrayList<>();
        for (String id : ids) {
            if (present.contains(id)) continue;
            QuoteSheetDetail detail = new QuoteSheetDetail();
            detail.setId(IdUtil.getId());
            detail.setQuoteSheetId(id);
            detail.setProductId(productId);
            detail.setProductSnapshot(JsonUtil.toJsonString(product));
            detail.setOrderNo(maxOrder.getOrDefault(id, 0) + 1);
            detail.setSalePrice(BigDecimal.ZERO);
            detail.setInquiryProduct(true);
            additions.add(detail);
        }
        if (!additions.isEmpty()) detailMapper.batchInsert(additions);
    }
}
