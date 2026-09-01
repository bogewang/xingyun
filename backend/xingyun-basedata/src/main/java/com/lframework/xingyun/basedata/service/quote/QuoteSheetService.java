package com.lframework.xingyun.basedata.service.quote;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.bo.quote.*;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel;
import com.lframework.xingyun.basedata.vo.quote.*;

import java.util.List;

/**
 * 报价单服务。
 */
public interface QuoteSheetService extends BaseMpService<QuoteSheet> {
    String create(CreateQuoteSheetVo vo);

    void update(UpdateQuoteSheetVo vo);

    void deleteById(String id);

    void enable(String id);

    void disable(String id);

    GetQuoteSheetBo get(String id);

    PageResult<QuoteSheet> query(Integer pageIndex, Integer pageSize, QueryQuoteSheetVo vo);

    /** 分页查询报价单商品明细。 */
    PageResult<QueryQuoteSheetDetailBo> queryDetails(Integer pageIndex, Integer pageSize, QueryQuoteSheetDetailVo vo);

    List<QuoteProductBo> getActiveQuoteProducts(QueryQuoteProductVo vo);

    List<QuoteSheetImportModel> checkImport(List<QuoteSheetImportModel> items);
}
