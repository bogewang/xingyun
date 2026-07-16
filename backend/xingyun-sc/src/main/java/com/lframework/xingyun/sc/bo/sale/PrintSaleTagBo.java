package com.lframework.xingyun.sc.bo.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Slf4j
public class PrintSaleTagBo {
    /**
     * 客户名称
     *
     */
    private String customerSimpleName;
    /**
     * 品名
     */
    private String productName;
    /**
     * 6公斤
     */
    private String orderNum;
    /**
     * '2026年05月05日'
     */
    private String orderDate;
}
