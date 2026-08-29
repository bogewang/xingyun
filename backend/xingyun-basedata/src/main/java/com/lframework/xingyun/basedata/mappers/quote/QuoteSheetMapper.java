package com.lframework.xingyun.basedata.mappers.quote;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.bo.quote.QuoteProductBo;
import com.lframework.xingyun.basedata.vo.quote.QueryQuoteProductVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 报价单数据访问接口。
 */
public interface QuoteSheetMapper extends BaseMapper<QuoteSheet> {
  /** 一次关联查询指定日期的生效报价商品。 */
  List<QuoteProductBo> getActiveQuoteProducts(@Param("vo") QueryQuoteProductVo vo);
}
