package com.lframework.xingyun.sc.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.xingyun.sc.dto.purchase.receive.QueryReceiveSheetDetailDto;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author zmj
 * @since 2021-10-09
 */
public interface ReceiveSheetDetailMapper extends BaseMapper<ReceiveSheetDetail> {

  /**
   * 增加退货数量
   *
   * @param id
   * @param num
   * @return
   */
  int addReturnNum(@Param("id") String id, @Param("num") BigDecimal num);

  /**
   * 减少退货数量
   *
   * @param id
   * @param num
   * @return
   */
  int subReturnNum(@Param("id") String id, @Param("num") BigDecimal num);

  List<QueryReceiveSheetDetailDto> getLatestCostPriceList(@Param("beginDate") LocalDate beginDate, @Param("orderDate") LocalDate orderDate);

}
