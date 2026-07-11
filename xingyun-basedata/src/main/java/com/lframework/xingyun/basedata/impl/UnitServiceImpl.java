package com.lframework.xingyun.basedata.impl;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.mappers.UnitMapper;
import com.lframework.xingyun.basedata.service.UnitService;
import org.springframework.stereotype.Service;
@Service
public class UnitServiceImpl extends BaseMpServiceImpl<UnitMapper, Unit> implements UnitService {}
