package com.lframework.xingyun.basedata.excel.customer;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.vo.customer.QueryCustomerVo;

/**
 * 客户信息导出任务处理器。
 */
public class CustomerExportTaskWorker implements
    ExportTaskWorker<QueryCustomerVo, Customer, CustomerExportModel> {

  /**
   * 解析导出筛选条件。
   */
  @Override
  public QueryCustomerVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryCustomerVo.class);
  }

  /**
   * 分页查询待导出的客户数据。
   */
  @Override
  public PageResult<Customer> getDataList(int pageIndex, int pageSize, QueryCustomerVo params) {
    CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);

    return customerService.query(pageIndex, pageSize, params);
  }

  /**
   * 将客户实体转换为 Excel 行数据。
   */
  @Override
  public CustomerExportModel exportData(Customer data) {
    return new CustomerExportModel(data);
  }

  /**
   * 获取 Excel 行数据类型。
   */
  @Override
  public Class<CustomerExportModel> getModelClass() {
    return CustomerExportModel.class;
  }
}
