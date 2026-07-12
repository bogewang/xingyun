package com.lframework.xingyun.basedata.service;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.excel.unit.UnitImportModel;
import com.lframework.xingyun.basedata.vo.unit.CreateUnitVo;
import com.lframework.xingyun.basedata.vo.unit.QueryUnitVo;
import com.lframework.xingyun.basedata.vo.unit.UpdateUnitVo;

import java.util.List;

public interface UnitService extends BaseMpService<Unit> {

    /**
     * 查询列表
     */
    PageResult<Unit> query(Integer pageIndex, Integer pageSize, QueryUnitVo vo);

    /**
     * 根据ID查询
     */
    Unit findById(String id);

    /**
     * 创建
     */
    String create(CreateUnitVo vo);

    /**
     * 修改
     */
    void update(UpdateUnitVo vo);

    /**
     * 导入
     */
    void importExcel(List<UnitImportModel> items);

    /**
     * 生成编码
     */
    String generateCode();
}
