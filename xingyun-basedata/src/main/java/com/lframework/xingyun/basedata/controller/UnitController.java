package com.lframework.xingyun.basedata.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.excel.unit.UnitImportModel;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.vo.unit.QueryUnitVo;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/basedata/unit")
public class UnitController extends DefaultBaseController {
    @Autowired
    private UnitService unitService;

    @GetMapping("/query")
    @HasPermission("base-data:unit:query")
    public InvokeResult<PageResult<Unit>> query(@Valid QueryUnitVo vo) {
        Page<Unit> page = new Page<>(getPageIndex(vo), getPageSize(vo));
        page = unitService.page(page, Wrappers.lambdaQuery(Unit.class)
                .like(vo.getCode() != null && !vo.getCode().trim().isEmpty(), Unit::getCode, vo.getCode())
                .like(vo.getName() != null && !vo.getName().trim().isEmpty(), Unit::getName, vo.getName())
                .eq(Unit::getAvailable, true)
                .orderByAsc(Unit::getCode));
        return InvokeResultBuilder.success(PageResultUtil.convert(page));
    }

    @GetMapping("/generate/code")
    @HasPermission("base-data:unit:add")
    public InvokeResult<String> generateCode() {
        return InvokeResultBuilder.success(generateCodeValue());
    }

    @PostMapping
    @HasPermission("base-data:unit:add")
    public InvokeResult<Void> create(@RequestParam @NotBlank String code, @RequestParam @NotBlank String name,
                                     String description) {
        checkUnique(code, name, null);
        Unit unit = new Unit();
        unit.setId(IdUtil.getId());
        unit.setCode(code);
        unit.setName(name);
        unit.setDescription(description);
        unit.setAvailable(true);
        unitService.save(unit);
        return InvokeResultBuilder.success();
    }

    @PutMapping
    @HasPermission("base-data:unit:modify")
    public InvokeResult<Void> update(@RequestParam @NotBlank String id, @RequestParam @NotBlank String code,
                                     @RequestParam @NotBlank String name, String description) {
        if (unitService.getById(id) == null) throw new DefaultClientException("单位不存在！");
        checkUnique(code, name, id);
        Unit unit = new Unit();
        unit.setId(id);
        unit.setCode(code);
        unit.setName(name);
        unit.setDescription(description);
        unitService.updateById(unit);
        return InvokeResultBuilder.success();
    }

    @DeleteMapping
    @HasPermission("base-data:unit:delete")
    public InvokeResult<Void> delete(@RequestParam @NotBlank String id) {
        unitService.removeById(id);
        return InvokeResultBuilder.success();
    }

    @GetMapping("/import/template")
    @HasPermission("base-data:unit:import")
    public void downloadImportTemplate() {
        ExcelUtil.export("单位导入模板", UnitImportModel.class);
    }

    @PostMapping("/import")
    @HasPermission("base-data:unit:import")
    @Transactional(rollbackFor = Exception.class)
    public InvokeResult<Void> importExcel(@RequestParam MultipartFile file) throws Exception {
        List<UnitImportModel> items = EasyExcelUtils.syncReadModel(file.getInputStream(), UnitImportModel.class);
        for (UnitImportModel item : items) {
            checkUnique(null, item.getName(), null);
            Unit unit = new Unit();
            unit.setId(IdUtil.getId());
            unit.setCode(generateCodeValue());
            unit.setName(item.getName());
            unit.setDescription(item.getDescription());
            unit.setAvailable(true);
            unitService.save(unit);
        }
        return InvokeResultBuilder.success();
    }

    private void checkUnique(String code, String name, String id) {
        LambdaQueryWrapper<Unit> wrapper = Wrappers.lambdaQuery(Unit.class).eq(Unit::getAvailable, true);
        if (code == null) wrapper.eq(Unit::getName, name);
        else wrapper.and(w -> w.eq(Unit::getCode, code).or().eq(Unit::getName, name));
        if (id != null) wrapper.ne(Unit::getId, id);
        if (unitService.count(wrapper) > 0)
            throw new DefaultClientException("单位编码或名称重复！");
    }

    private String generateCodeValue() {
        int maxRetry = 100;
        while (maxRetry-- > 0) {
            String code = "U" + IdUtil.getId();
            if (unitService.count(Wrappers.lambdaQuery(Unit.class).eq(Unit::getCode, code)) == 0) return code;
        }
        throw new DefaultClientException("生成单位编码失败，请重试！");
    }
}
