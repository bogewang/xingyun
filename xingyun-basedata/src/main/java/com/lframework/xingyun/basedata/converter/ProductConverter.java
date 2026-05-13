package com.lframework.xingyun.basedata.converter;

import cn.hutool.core.collection.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.bo.product.info.QueryProductBo;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductBrand;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.service.product.ProductBrandService;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProductConverter {
    /**
     * 商品DO转换为商品BO列表
     * @param datas
     * @param scId
     * @return
     */
    public static List<QueryProductBo> DO2BOList(List<Product> datas, String scId) {
        if (CollectionUtil.isEmpty(datas)) {
            return Collections.emptyList();
        }

        ProductCategoryService productCategoryService = ApplicationUtil.getBean(ProductCategoryService.class);
        ProductBrandService productBrandService = ApplicationUtil.getBean(ProductBrandService.class);
        // 转换分类名称
        List<String> categoryIds = datas.stream().map(Product::getCategoryId).filter(StringUtil::isNotBlank)
                .distinct().collect(Collectors.toList());
        Map<String, ProductCategory> categoryMap = CollectionUtil.isEmpty(categoryIds) ? Collections.emptyMap()
                : productCategoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ProductCategory::getId, Function.identity()));
        // 转换品牌名称
        List<String> brandIds = datas.stream().map(Product::getBrandId).filter(StringUtil::isNotBlank)
                .distinct().collect(Collectors.toList());
        Map<String, ProductBrand> brandMap = CollectionUtil.isEmpty(brandIds) ? Collections.emptyMap()
                : productBrandService.listByIds(brandIds).stream()
                        .collect(Collectors.toMap(ProductBrand::getId, Function.identity()));
        // 转换库存数量字段 todo

        return datas.stream().map(dto -> {
            QueryProductBo bo = new QueryProductBo(dto);
            if (categoryMap.containsKey(dto.getCategoryId())) {
                bo.setCategoryName(categoryMap.get(dto.getCategoryId()).getName());
            }

            if (brandMap.containsKey(dto.getBrandId())) {
                bo.setBrandName(brandMap.get(dto.getBrandId()).getName());
            }

            bo.setPurchasePrice(dto.getPurchasePrice());
            bo.setRetailPrice(dto.getRetailPrice());
            bo.setSalePrice(dto.getSalePrice());
            bo.setAlias(dto.getAlias());
            bo.setRemark(dto.getRemark());
            bo.setRemark2(dto.getRemark2());

            return bo;
        }).collect(Collectors.toList());
    }
}
