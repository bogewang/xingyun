package com.lframework.xingyun.basedata.impl.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import com.lframework.xingyun.basedata.mappers.ProductUnitMapper;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductUnitServiceImpl extends BaseMpServiceImpl<ProductUnitMapper, ProductUnit>
    implements ProductUnitService {
  @Override
  public List<ProductUnit> getByProductId(String productId) {
    return list(Wrappers.lambdaQuery(ProductUnit.class).eq(ProductUnit::getProductId, productId)
        .orderByAsc(ProductUnit::getSortNo));
  }

  @Override
  public List<ProductUnit> getAvailableByProductId(String productId) {
    return list(Wrappers.lambdaQuery(ProductUnit.class).eq(ProductUnit::getProductId, productId)
        .eq(ProductUnit::getAvailable, Boolean.TRUE).orderByAsc(ProductUnit::getSortNo));
  }

  @Override
  public ProductUnit getAvailableById(String productId, String unitId) {
    return getOne(Wrappers.lambdaQuery(ProductUnit.class).eq(ProductUnit::getProductId, productId)
        .eq(ProductUnit::getId, unitId).eq(ProductUnit::getAvailable, Boolean.TRUE));
  }

  @Override
  public ProductUnit getAvailableByUnitName(String productId, String unitName) {
    return getOne(Wrappers.lambdaQuery(ProductUnit.class).eq(ProductUnit::getProductId, productId)
        .eq(ProductUnit::getUnitName, unitName).eq(ProductUnit::getAvailable, Boolean.TRUE));
  }
}
