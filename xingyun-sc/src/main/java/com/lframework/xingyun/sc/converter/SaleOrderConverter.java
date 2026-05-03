package com.lframework.xingyun.sc.converter;

import cn.hutool.core.bean.BeanUtil;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.sc.bo.sale.PrintSaleOrderBo;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class SaleOrderConverter {

    public static List<PrintSaleOrderBo.OrderDetailBo> orderDetailDTO2PrintDetailBOS(List<SaleOrderFullDto.OrderDetailDto> details) {
        if (CollectionUtil.isEmpty(details)) {
            return Collections.emptyList();
        }

        List<String> productIds = details.stream().map(SaleOrderFullDto.OrderDetailDto::getProductId).collect(Collectors.toList());
        SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
        List<SaleOrder> saleOrders = saleOrderService.lambdaQuery().in(SaleOrder::getId, productIds).list();
        Map<String, SaleOrder> orderMap = saleOrders.stream().collect(Collectors.toMap(SaleOrder::getId, item -> item));


        return details.stream().map(item -> {
            PrintSaleOrderBo.OrderDetailBo orderDetailBo = new PrintSaleOrderBo.OrderDetailBo();
            // orderDetailBo.setOrderNo();
            orderDetailBo.setProductName();
            orderDetailBo.setSpec();
            orderDetailBo.setUnit();
            orderDetailBo.setOrderNum();
            orderDetailBo.setTaxPrice();
            orderDetailBo.setOrderAmount();




        })
        this.orderNum = dto.getOrderNum();
        this.taxPrice = dto.getTaxPrice();
        this.orderAmount = dto.getTaxAmount();


        SaleProductDto product = saleOrderService.getSaleById(dto.getProductId());

        this.productCode = product.getCode();
        this.productName = product.getName();
        this.skuCode = product.getSkuCode();
        this.externalCode = product.getExternalCode();
    }

    public static PrintSaleOrderBo fullDTO2PrintBO(SaleOrderFullDto data) {
        if (data == null) {
            return null;
        }
        // 客户名称
        CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
        Customer customer = customerService.findById(data.getCustomerId());

        PrintSaleOrderBo res = BeanUtil.copyProperties(data, PrintSaleOrderBo.class);
        res.setCustomerName(customer.getName());
        res.setCreateTime(DateUtil.formatDateTime(data.getCreateTime()));
        res.setDetails(orderDetailDTO2PrintDetailBOS(data.getDetails()));

        return res;
    }
}
