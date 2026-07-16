package com.lframework.xingyun.basedata.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.excel.unit.UnitImportModel;
import com.lframework.xingyun.basedata.mappers.UnitMapper;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.vo.unit.CreateUnitVo;
import com.lframework.xingyun.basedata.vo.unit.QueryUnitVo;
import com.lframework.xingyun.basedata.vo.unit.UpdateUnitVo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitServiceImpl extends BaseMpServiceImpl<UnitMapper, Unit> implements UnitService {

    @Override
    public PageResult<Unit> query(Integer pageIndex, Integer pageSize, QueryUnitVo vo) {

        Page<Unit> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<Unit> wrapper = Wrappers.lambdaQuery(Unit.class)
                .like(vo.getCode() != null && !vo.getCode().trim().isEmpty(), Unit::getCode, vo.getCode())
                .like(vo.getName() != null && !vo.getName().trim().isEmpty(), Unit::getName, vo.getName())
                .eq(Unit::getAvailable, true)
                .orderByAsc(Unit::getCode);
        getBaseMapper().selectPage(page, wrapper);
        return PageResultUtil.convert(page);
    }

    @Override
    public Unit findById(String id) {

        return getBaseMapper().selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateUnitVo vo) {

        checkUnique(vo.getCode(), vo.getName(), null);

        Unit unit = new Unit();
        unit.setId(IdUtil.getId());
        unit.setCode(vo.getCode());
        unit.setName(vo.getName());
        unit.setDescription(vo.getDescription());
        unit.setAvailable(true);
        getBaseMapper().insert(unit);

        return unit.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdateUnitVo vo) {

        if (getBaseMapper().selectById(vo.getId()) == null) {
            throw new DefaultClientException("单位不存在！");
        }

        checkUnique(vo.getCode(), vo.getName(), vo.getId());

        Unit unit = new Unit();
        unit.setId(vo.getId());
        unit.setCode(vo.getCode());
        unit.setName(vo.getName());
        unit.setDescription(vo.getDescription());
        getBaseMapper().updateById(unit);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importExcel(List<UnitImportModel> items) {

        List<Unit> units = new ArrayList<>();
        for (UnitImportModel item : items) {
            checkUnique(null, item.getName(), null);
            Unit unit = new Unit();
            unit.setId(IdUtil.getId());
            if (StringUtils.isBlank(item.getCode())) {
                unit.setCode(generateCode());
            }
            unit.setName(item.getName());
            unit.setDescription(item.getDescription());
            unit.setAvailable(true);
            units.add(unit);
        }
        saveBatch(units);
    }

    @Override
    public String generateCode() {

        int maxRetry = 100;
        while (maxRetry-- > 0) {
            String code = "U" + IdUtil.getId();
            if (getBaseMapper().selectCount(
                    Wrappers.lambdaQuery(Unit.class).eq(Unit::getCode, code)) == 0) {
                return code;
            }
        }
        throw new DefaultClientException("生成单位编码失败，请重试！");
    }

    private void checkUnique(String code, String name, String id) {

        LambdaQueryWrapper<Unit> wrapper = Wrappers.lambdaQuery(Unit.class).eq(Unit::getAvailable, true);
        if (code == null) {
            wrapper.eq(Unit::getName, name);
        } else {
            wrapper.and(w -> w.eq(Unit::getCode, code).or().eq(Unit::getName, name));
        }
        if (id != null) {
            wrapper.ne(Unit::getId, id);
        }
        if (getBaseMapper().selectCount(wrapper) > 0) {
            throw new DefaultClientException("单位编码或名称重复！");
        }
    }
}
