package com.lframework.xingyun.basedata.service.quote;

import java.util.List;

/** 报价单业务引用检查扩展点。 */
public interface QuoteSheetReferenceChecker {
  /**
   * 判断报价单是否已被业务数据引用。
   *
   * @param quoteSheetId 报价单 ID
   * @return 已被引用时返回 true
   */
  boolean hasReference(String quoteSheetId);

  /**
   * 判断指定报价单明细是否已被业务单据通过来源 ID 引用。
   *
   * @param quoteSheetDetailIds 报价单明细 ID
   * @return 已被引用时返回 true
   */
  default boolean hasDetailReference(List<String> quoteSheetDetailIds) {
    return false;
  }
}
