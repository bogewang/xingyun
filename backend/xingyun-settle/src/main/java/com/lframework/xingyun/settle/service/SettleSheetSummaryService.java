package com.lframework.xingyun.settle.service;

import com.lframework.xingyun.settle.bo.sheet.SettleSheetSummaryBo;
import com.lframework.xingyun.settle.vo.sheet.QuerySettleSheetSummaryVo;
import java.util.List;

/**
 * 供应商结算汇总服务
 */
public interface SettleSheetSummaryService {

  List<SettleSheetSummaryBo> query(QuerySettleSheetSummaryVo vo);
}
