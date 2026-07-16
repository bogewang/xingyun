package com.lframework.xingyun.sc.converter;

import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.bo.purchase.PurchaseProductBo;
import com.lframework.xingyun.sc.dto.purchase.PurchaseProductDto;
import com.lframework.xingyun.sc.service.ProductHotnessService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PurchaseOrderConverter {

    public static List<PurchaseProductBo> purchaseProductDto2Bos(String scId, List<PurchaseProductDto> datas) {
        List<PurchaseProductBo> results = datas.stream()
                .map(t -> new PurchaseProductBo(scId, t))
                .collect(Collectors.toList());
        ProductHotnessService productHotnessService = ApplicationUtil.getBean(ProductHotnessService.class);
        Map<String, Integer> hotLevels = productHotnessService.getHotLevels(
                results.stream().map(PurchaseProductBo::getProductId).collect(Collectors.toList()));
        // 设置热度，并排序
        return results.stream()
                .peek(t -> t.setHotLevel(hotLevels.getOrDefault(t.getProductId(), 0)))
                .sorted(Comparator.comparing(PurchaseProductBo::getHotLevel, Comparator.reverseOrder())
                        .thenComparing(PurchaseProductBo::getProductName))
                .collect(Collectors.toList());
    }
}
