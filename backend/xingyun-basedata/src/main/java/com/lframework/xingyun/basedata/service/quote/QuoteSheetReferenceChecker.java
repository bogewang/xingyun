package com.lframework.xingyun.basedata.service.quote;

/** 报价单业务引用检查扩展点。 */
public interface QuoteSheetReferenceChecker {
  /**
   * 判断报价单是否已被业务数据引用。
   *
   * @param quoteSheetId 报价单 ID
   * @return 已被引用时返回 true
   */
  boolean hasReference(String quoteSheetId);
}
