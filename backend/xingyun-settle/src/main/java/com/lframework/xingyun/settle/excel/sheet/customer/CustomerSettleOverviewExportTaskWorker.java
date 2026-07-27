package com.lframework.xingyun.settle.excel.sheet.customer;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSettleOverviewBo;
import com.lframework.xingyun.settle.service.CustomerSettleSheetService;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleOverviewVo;

/**
 * 客户结算总览导出任务。
 */
public class CustomerSettleOverviewExportTaskWorker implements ExportTaskWorker<
    QueryCustomerSettleOverviewVo, CustomerSettleOverviewBo, CustomerSettleOverviewExportModel> {

  /**
   * 解析导出任务参数。
   *
   * @param json 参数 JSON
   * @return 查询条件
   */
  @Override
  public QueryCustomerSettleOverviewVo parseParams(String json) {
    return JsonUtil.parseObject(json, QueryCustomerSettleOverviewVo.class);
  }

  /**
   * 分页查询客户结算总览数据。
   *
   * @param pageIndex 页码
   * @param pageSize 每页数量
   * @param params 查询条件
   * @return 分页总览数据
   */
  @Override
  public PageResult<CustomerSettleOverviewBo> getDataList(int pageIndex, int pageSize,
      QueryCustomerSettleOverviewVo params) {
    params.setPageIndex(pageIndex);
    params.setPageSize(pageSize);
    CustomerSettleSheetService customerSettleSheetService = ApplicationUtil.getBean(
        CustomerSettleSheetService.class);
    return customerSettleSheetService.querySettleOverviews(params);
  }

  /**
   * 将总览数据转换为导出模型。
   *
   * @param data 总览数据
   * @return 导出模型
   */
  @Override
  public CustomerSettleOverviewExportModel exportData(CustomerSettleOverviewBo data) {
    return new CustomerSettleOverviewExportModel(data);
  }

  /**
   * 获取 Excel 导出模型类型。
   *
   * @return 导出模型类型
   */
  @Override
  public Class<CustomerSettleOverviewExportModel> getModelClass() {
    return CustomerSettleOverviewExportModel.class;
  }
}
