package com.lframework.xingyun.settle.service;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.settle.dto.sheet.customer.CustomerSettleSheetFullDto;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSaleSettleInfoBo;
import com.lframework.xingyun.settle.bo.sheet.customer.CustomerSettleOverviewBo;
import com.lframework.xingyun.settle.entity.CustomerSettleSheet;
import com.lframework.xingyun.settle.vo.sheet.customer.ApprovePassCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.ApproveRefuseCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.CreateCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleSheetVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSaleSettleInfoVo;
import com.lframework.xingyun.settle.vo.sheet.customer.QueryCustomerSettleOverviewVo;
import com.lframework.xingyun.settle.vo.sheet.customer.UpdateCustomerSettleSheetVo;
import java.util.List;

public interface CustomerSettleSheetService extends BaseMpService<CustomerSettleSheet> {

    /**
     * 查询客户结算总览。
     *
     * @param vo 查询条件
     * @return 客户结算总览分页数据
     */
    PageResult<CustomerSettleOverviewBo> querySettleOverviews(QueryCustomerSettleOverviewVo vo);

    /**
     * 查询客户销售业务单据结算工作台信息。
     *
     * @param vo 查询条件
     * @return 结算工作台分页数据
     */
    PageResult<CustomerSaleSettleInfoBo> querySaleSettleInfos(QueryCustomerSaleSettleInfoVo vo);

    /**
     * 查询列表
     *
     * @param pageIndex
     * @param pageSize
     * @param vo
     * @return
     */
    PageResult<CustomerSettleSheet> query(Integer pageIndex, Integer pageSize,
        QueryCustomerSettleSheetVo vo);

    /**
     * 查询列表
     *
     * @param vo
     * @return
     */
    List<CustomerSettleSheet> query(QueryCustomerSettleSheetVo vo);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    CustomerSettleSheetFullDto getDetail(String id);

    /**
     * 创建
     *
     * @param vo
     * @return
     */
    String create(CreateCustomerSettleSheetVo vo);

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdateCustomerSettleSheetVo vo);

    /**
     * 审核通过
     *
     * @param vo
     */
    void approvePass(ApprovePassCustomerSettleSheetVo vo);

    /**
     * 直接审核通过
     *
     * @param vo
     */
    String directApprovePass(CreateCustomerSettleSheetVo vo);

    /**
     * 审核拒绝
     *
     * @param vo
     */
    void approveRefuse(ApproveRefuseCustomerSettleSheetVo vo);

    /**
     * 根据ID删除
     *
     * @param id
     */
    void deleteById(String id);

    /**
     * 更新业务单据未结算
     *
     * @param bizId 业务单据ID
     */
    void setBizItemUnSettle(String bizId);

    /**
     * 更新业务单据结算中
     *
     * @param bizId 业务单据ID
     */
    void setBizItemPartSettle(String bizId);

    /**
     * 更新业务单据已结算
     *
     * @param bizId 业务单据ID
     */
    void setBizItemSettled(String bizId);
}
