package com.lframework.xingyun.basedata.mappers.quote;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.xingyun.basedata.bo.quote.QueryQuoteSheetDetailBo;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteSheetDetailVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 报价单明细数据访问接口。
 */
public interface QuoteSheetDetailMapper extends BaseMapper<QuoteSheetDetail> {
  /** 批量插入报价单明细。 */
  int batchInsert(@Param("details") List<QuoteSheetDetail> details);

  /** 查询报价单商品明细。 */
  List<QueryQuoteSheetDetailBo> queryDetails(@Param("vo") QueryQuoteSheetDetailVo vo);
}
