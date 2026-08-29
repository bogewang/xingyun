package com.lframework.xingyun.sc.impl.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetReferenceChecker;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import org.springframework.stereotype.Service;

/** 仓储业务报价单引用检查器。 */
@Service
public class ScQuoteSheetReferenceChecker implements QuoteSheetReferenceChecker {
  private final SaleOutSheetDetailMapper saleOutSheetDetailMapper;
  /** 创建报价单引用检查器。 */
  public ScQuoteSheetReferenceChecker(SaleOutSheetDetailMapper saleOutSheetDetailMapper) { this.saleOutSheetDetailMapper = saleOutSheetDetailMapper; }
  /** 判断报价单是否被销售出库明细引用。 */
  @Override public boolean hasReference(String quoteSheetId) { return saleOutSheetDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheetDetail.class).eq(SaleOutSheetDetail::getQuoteSheetId, quoteSheetId)) > 0; }
}
