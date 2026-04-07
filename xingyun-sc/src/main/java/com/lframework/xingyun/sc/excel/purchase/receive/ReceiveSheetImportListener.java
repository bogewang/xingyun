package com.lframework.xingyun.sc.excel.purchase.receive;

import com.alibaba.excel.context.AnalysisContext;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.web.core.components.excel.ExcelImportListener;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import com.lframework.xingyun.sc.vo.purchase.receive.CreateReceiveSheetVo;
import com.lframework.xingyun.sc.vo.purchase.receive.ReceiveProductVo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ReceiveSheetImportListener extends ExcelImportListener<ReceiveSheetImportModel> {

  @Override
  protected void doInvoke(ReceiveSheetImportModel data, AnalysisContext context) {

  }

  @Override
  protected void afterAllAnalysed(AnalysisContext context) {

    // 根据仓库、供应商、采购员、预计到货日期 分组
    Map<Object, List<ReceiveSheetImportModel>> groupByMap = this.getDatas().stream().collect(
        Collectors.groupingBy(
            t -> t.getScCode() + "_" + t.getSupplierCode() + "_" + t.getPurchaserCode() + "_"
                + DateUtil.toLocalDate(t.getPaymentDate()) + "_" + DateUtil.toLocalDate(
                t.getReceiveDate())));

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);

    int index = 0;
    for (List<ReceiveSheetImportModel> value : groupByMap.values()) {
      ReceiveSheetImportModel valueObj = value.get(0);
      CreateReceiveSheetVo createReceiveSheetVo = new CreateReceiveSheetVo();
      createReceiveSheetVo.setScId(valueObj.getScId());
      createReceiveSheetVo.setSupplierId(valueObj.getSupplierId());
      createReceiveSheetVo.setPurchaserId(valueObj.getPurchaserId());
      createReceiveSheetVo.setPaymentDate(DateUtil.toLocalDate(valueObj.getPaymentDate()));
      createReceiveSheetVo.setAllowModifyPaymentDate(Boolean.TRUE);
      createReceiveSheetVo.setReceiveDate(DateUtil.toLocalDate(valueObj.getReceiveDate()));
      createReceiveSheetVo.setDescription(valueObj.getDescription());
      createReceiveSheetVo.setRequired(Boolean.FALSE);

      List<ReceiveProductVo> products = new ArrayList<>();
      for (ReceiveSheetImportModel data : value) {
        ReceiveProductVo purchaseProductVo = new ReceiveProductVo();
        purchaseProductVo.setProductId(data.getProductId());
        purchaseProductVo.setPurchasePrice(data.getPurchasePrice());
        purchaseProductVo.setReceiveNum(data.getReceiveNum());
        purchaseProductVo.setDescription(data.getDetailDescription());

        products.add(purchaseProductVo);

        index++;
        this.setSuccessProcessByIndex(index);
      }
      createReceiveSheetVo.setProducts(products);

      receiveSheetService.create(createReceiveSheetVo);
    }
  }

  @Override
  protected void doComplete() {

  }
}