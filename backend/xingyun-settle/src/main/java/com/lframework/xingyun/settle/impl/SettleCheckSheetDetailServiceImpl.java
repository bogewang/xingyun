package com.lframework.xingyun.settle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.xingyun.settle.entity.SettleCheckSheetDetail;
import com.lframework.xingyun.settle.mappers.SettleCheckSheetDetailMapper;
import com.lframework.xingyun.settle.service.SettleCheckSheetDetailService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettleCheckSheetDetailServiceImpl
    extends BaseMpServiceImpl<SettleCheckSheetDetailMapper, SettleCheckSheetDetail>
    implements SettleCheckSheetDetailService {

    @Override
    public List<SettleCheckSheetDetail> listByBizIds(List<String> bizIds) {
        LambdaQueryWrapper<SettleCheckSheetDetail> wrapper = Wrappers.lambdaQuery(SettleCheckSheetDetail.class)
                        .in(SettleCheckSheetDetail::getBizId, bizIds);

        return getBaseMapper().selectList(wrapper);
    }
}
