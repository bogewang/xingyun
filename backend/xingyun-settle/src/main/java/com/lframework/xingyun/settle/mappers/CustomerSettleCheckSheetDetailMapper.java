package com.lframework.xingyun.settle.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.xingyun.settle.entity.CustomerSettleCheckSheetDetail;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 客户对账单明细数据访问接口。
 */
public interface CustomerSettleCheckSheetDetailMapper extends BaseMapper<CustomerSettleCheckSheetDetail> {

  /**
   * 批量新增客户对账单明细。
   *
   * @param details 对账单明细
   * @return 新增记录数
   */
  @Insert({"<script>", "INSERT INTO customer_settle_check_sheet_detail",
      "(id, sheet_id, biz_id, biz_type, pay_amount, description, order_no) VALUES",
      "<foreach collection='details' item='detail' separator=','>",
      "(#{detail.id}, #{detail.sheetId}, #{detail.bizId}, #{detail.bizType},",
      "#{detail.payAmount}, #{detail.description}, #{detail.orderNo})", "</foreach>",
      "</script>"})
  int insertBatch(@Param("details") List<CustomerSettleCheckSheetDetail> details);
}
