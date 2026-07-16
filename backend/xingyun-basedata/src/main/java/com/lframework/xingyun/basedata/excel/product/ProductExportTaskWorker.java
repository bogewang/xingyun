package com.lframework.xingyun.basedata.excel.product;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.vo.product.info.QueryProductVo;

public class ProductExportTaskWorker implements
    ExportTaskWorker<QueryProductVo, Product, ProductImportModel> {

  @Override
  public QueryProductVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryProductVo.class);
  }

  @Override
  public PageResult<Product> getDataList(int pageIndex, int pageSize, QueryProductVo params) {
    ProductService productService = ApplicationUtil.getBean(ProductService.class);
    return productService.query(pageIndex, pageSize, params);
  }

  @Override
  public ProductImportModel exportData(Product data) {
    return new ProductImportModel(data);
  }

  @Override
  public Class<ProductImportModel> getModelClass() {
    return ProductImportModel.class;
  }
}
