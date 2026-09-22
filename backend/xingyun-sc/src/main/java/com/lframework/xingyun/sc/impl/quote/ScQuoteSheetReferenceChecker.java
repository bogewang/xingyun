package com.lframework.xingyun.sc.impl.quote;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheetDetail;
import com.lframework.xingyun.basedata.mappers.quote.QuoteSheetDetailMapper;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetReferenceChecker;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.ReceiveSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 仓储业务报价单引用检查器。 */
@Service
public class ScQuoteSheetReferenceChecker implements QuoteSheetReferenceChecker {
  private final QuoteSheetDetailMapper quoteSheetDetailMapper;
  private final SaleOutSheetMapper saleOutSheetMapper;
  private final SaleOutSheetDetailMapper saleOutSheetDetailMapper;
  private final ReceiveSheetDetailMapper receiveSheetDetailMapper;

  /** 创建报价单引用检查器。 */
  public ScQuoteSheetReferenceChecker(QuoteSheetDetailMapper quoteSheetDetailMapper,
      SaleOutSheetMapper saleOutSheetMapper, SaleOutSheetDetailMapper saleOutSheetDetailMapper,
      ReceiveSheetDetailMapper receiveSheetDetailMapper) {
    this.quoteSheetDetailMapper = quoteSheetDetailMapper;
    this.saleOutSheetMapper = saleOutSheetMapper;
    this.saleOutSheetDetailMapper = saleOutSheetDetailMapper;
    this.receiveSheetDetailMapper = receiveSheetDetailMapper;
  }

  /** 判断报价单或其明细是否已被业务单据引用。 */
  @Override
  public boolean hasReference(String quoteSheetId) {
    if (saleOutSheetMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheet.class)
        .eq(SaleOutSheet::getQuoteSheetId, quoteSheetId)) > 0) {
      return true;
    }
    List<String> detailIds = quoteSheetDetailMapper.selectList(Wrappers
        .lambdaQuery(QuoteSheetDetail.class).select(QuoteSheetDetail::getId)
        .eq(QuoteSheetDetail::getQuoteSheetId, quoteSheetId)).stream()
        .map(QuoteSheetDetail::getId).collect(Collectors.toList());
    return hasDetailReference(detailIds);
  }

  /** 判断报价单明细是否已被销售出库或采购收货明细引用。 */
  @Override
  public boolean hasDetailReference(List<String> quoteSheetDetailIds) {
    return !quoteSheetDetailIds.isEmpty() && (saleOutSheetDetailMapper.selectCount(Wrappers
        .lambdaQuery(SaleOutSheetDetail.class).in(SaleOutSheetDetail::getSourceId,
            quoteSheetDetailIds)) > 0 || receiveSheetDetailMapper.selectCount(Wrappers
        .lambdaQuery(ReceiveSheetDetail.class).in(ReceiveSheetDetail::getSourceId,
            quoteSheetDetailIds)) > 0);
  }
}
