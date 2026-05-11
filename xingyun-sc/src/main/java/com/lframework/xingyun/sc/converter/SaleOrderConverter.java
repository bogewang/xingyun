package com.lframework.xingyun.sc.converter;

import cn.hutool.core.bean.BeanUtil;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.bo.sale.PrintSaleOrderBo;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
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
        ProductService productService = ApplicationUtil.getBean(ProductService.class);
        List<Product> products = productService.lambdaQuery().in(Product::getId, productIds).list();
        Map<String, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, item -> item));

        return details.stream().map(item -> {
            PrintSaleOrderBo.OrderDetailBo orderDetailBo = new PrintSaleOrderBo.OrderDetailBo();
            // orderDetailBo.setOrderNo(item.getOrderNo());
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                return null;
            }
            orderDetailBo.setProductCode(product.getCode());
            orderDetailBo.setProductName(product.getName());
            orderDetailBo.setSpec(product.getSpec());
            orderDetailBo.setUnit(product.getUnit());
            orderDetailBo.setOrderNum(item.getOrderNum());
            orderDetailBo.setTaxPrice(item.getTaxPrice());
            orderDetailBo.setOrderAmount(item.getTaxAmount());

            return orderDetailBo;
        }).collect(Collectors.toList());
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
        res.setDeliveryDate(DateUtil.formatDate(data.getOrderDate(), "yyyy-MM-dd"));
        res.setDetails(orderDetailDTO2PrintDetailBOS(data.getDetails()));

        return res;
    }
}
