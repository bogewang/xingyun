package com.lframework.xingyun.settle.service;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.settle.entity.SettleCheckSheetDetail;

import java.util.List;

public interface SettleCheckSheetDetailService extends BaseMpService<SettleCheckSheetDetail> {

    List<SettleCheckSheetDetail> listByBizIds(List<String> bizIds);
}
