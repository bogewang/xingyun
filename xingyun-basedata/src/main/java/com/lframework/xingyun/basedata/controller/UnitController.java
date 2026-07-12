package com.lframework.xingyun.basedata.controller;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.unit.QueryUnitBo;
import com.lframework.xingyun.basedata.entity.Unit;
import com.lframework.xingyun.basedata.excel.unit.UnitImportModel;
import com.lframework.xingyun.basedata.service.UnitService;
import com.lframework.xingyun.basedata.vo.unit.CreateUnitVo;
import com.lframework.xingyun.basedata.vo.unit.QueryUnitVo;
import com.lframework.xingyun.basedata.vo.unit.UpdateUnitVo;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/basedata/unit")
public class UnitController extends DefaultBaseController {

    @Autowired
    private UnitService unitService;

    @GetMapping("/query")
    @HasPermission("base-data:unit:query")
    public InvokeResult<PageResult<QueryUnitBo>> query(@Valid QueryUnitVo vo) {

        try {
            PageResult<Unit> pageResult = unitService.query(getPageIndex(vo), getPageSize(vo), vo);

            List<Unit> datas = pageResult.getDatas();
            List<QueryUnitBo> results = null;
            if (!CollectionUtil.isEmpty(datas)) {
                results = datas.stream().map(QueryUnitBo::new).collect(Collectors.toList());
            }

            return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @GetMapping("/generate/code")
    @HasPermission("base-data:unit:add")
    public InvokeResult<String> generateCode() {

        try {
            return InvokeResultBuilder.success(unitService.generateCode());
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage(), null);
        }
    }

    @PostMapping
    @HasPermission("base-data:unit:add")
    public InvokeResult<Void> create(@Valid CreateUnitVo vo) {

        try {
            unitService.create(vo);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    @PutMapping
    @HasPermission("base-data:unit:modify")
    public InvokeResult<Void> update(@Valid UpdateUnitVo vo) {

        try {
            unitService.update(vo);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    @DeleteMapping
    @HasPermission("base-data:unit:delete")
    public InvokeResult<Void> delete(String id) {

        try {
            unitService.removeById(id);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    @GetMapping("/import/template")
    @HasPermission("base-data:unit:import")
    public void downloadImportTemplate() {

        ExcelUtil.export("单位导入模板", UnitImportModel.class);
    }

    @PostMapping("/import")
    @HasPermission("base-data:unit:import")
    public InvokeResult<Void> importExcel(@RequestParam MultipartFile file) {

        try {
            List<UnitImportModel> items = EasyExcelUtils.syncReadModel(file.getInputStream(),
                    UnitImportModel.class);
            unitService.importExcel(items);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }
}
