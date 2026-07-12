package com.lframework.xingyun.basedata.excel.unit;

import com.lframework.starter.mq.core.components.export.ExportTaskWorker;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.vo.unit.QueryUnitVo;

public class UnitExportTaskWorker implements
        ExportTaskWorker<QueryUnitVo, Unit, UnitExportModel> {

    @Override
    public QueryUnitVo parseParams(String json) {
        return JsonUtil.parseObject(json, QueryUnitVo.class);
    }

    @Override
    public PageResult<Unit> getDataList(int pageIndex, int pageSize, QueryUnitVo params) {
        UnitService unitService = ApplicationUtil.getBean(UnitService.class);
        return unitService.query(pageIndex, pageSize, params);
    }

    @Override
    public UnitExportModel exportData(Unit data) {
        return new UnitExportModel(data);
    }

    @Override
    public Class<UnitExportModel> getModelClass() {
        return UnitExportModel.class;
    }
}
