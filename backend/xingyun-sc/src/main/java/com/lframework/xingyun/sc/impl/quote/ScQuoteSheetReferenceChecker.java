package com.lframework.xingyun.sc.impl.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetReferenceChecker;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import org.springframework.stereotype.Service;

/** 仓储业务报价单引用检查器。 */
@Service
public class ScQuoteSheetReferenceChecker implements QuoteSheetReferenceChecker {
  private final SaleOutSheetMapper saleOutSheetMapper;
  /** 创建报价单引用检查器。 */
  public ScQuoteSheetReferenceChecker(SaleOutSheetMapper saleOutSheetMapper) { this.saleOutSheetMapper = saleOutSheetMapper; }
  /** 判断报价单是否被销售出库单引用。 */
  @Override public boolean hasReference(String quoteSheetId) { return saleOutSheetMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheet.class).eq(SaleOutSheet::getQuoteSheetId, quoteSheetId)) > 0; }
}
