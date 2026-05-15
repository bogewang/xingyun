package com.lframework.xingyun.basedata.controller;

import com.lframework.starter.mq.core.utils.ExportTaskUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.product.info.GetProductBo;
import com.lframework.xingyun.basedata.bo.product.info.QueryProductBo;
import com.lframework.xingyun.basedata.converter.ProductConverter;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.excel.product.ProductExportTaskWorker;
import com.lframework.xingyun.basedata.excel.product.ProductImportModel;
import com.lframework.xingyun.basedata.service.product.ProductBundleService;
import com.lframework.xingyun.basedata.service.product.ProductPropertyRelationService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.vo.product.info.CreateProductVo;
import com.lframework.xingyun.basedata.vo.product.info.QueryProductVo;
import com.lframework.xingyun.basedata.vo.product.info.UpdateProductVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 商品管理
 *
 * @author zmj
 */
@Api(tags = "商品管理")
@Validated
@RestController
@RequestMapping("/basedata/product")
@Slf4j
public class ProductController extends DefaultBaseController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductBundleService productBundleService;

    @Autowired
    private ProductPropertyRelationService productPropertyRelationService;

    /**
     * 商品列表
     */
    @ApiOperation("商品列表")
    @HasPermission({"base-data:product:info:query", "base-data:product:info:add", "base-data:product:info:modify"})
    @GetMapping("/query")
    public InvokeResult<PageResult<QueryProductBo>> query(@Valid QueryProductVo vo) {
        PageResult<Product> pageResult = productService.query(getPageIndex(vo), getPageSize(vo), vo);

        List<QueryProductBo> results = ProductConverter.DO2BOList(pageResult.getDatas(), vo.getScId());

        return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
    }

    /**
     * 商品详情
     */
    @ApiOperation("商品详情")
    @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
    @HasPermission({"base-data:product:info:query", "base-data:product:info:add",
            "base-data:product:info:modify"})
    @GetMapping
    public InvokeResult<GetProductBo> get(@NotBlank(message = "ID不能为空！") String id) {

        Product data = productService.findById(id);

        GetProductBo result = new GetProductBo(data);

        return InvokeResultBuilder.success(result);
    }

    /**
     * 新增商品
     */
    @ApiOperation("新增商品")
    @HasPermission({"base-data:product:info:add"})
    @PostMapping
    public InvokeResult<Void> create(@Valid @RequestBody CreateProductVo vo) {

        productService.create(vo);

        return InvokeResultBuilder.success();
    }

    /**
     * 修改商品
     */
    @ApiOperation("修改商品")
    @HasPermission({"base-data:product:info:modify"})
    @PutMapping
    public InvokeResult<Void> update(@Valid @RequestBody UpdateProductVo vo) {

        productService.update(vo);

        productService.cleanCacheByKey(vo.getId());

        productPropertyRelationService.cleanCacheByKey(vo.getId());

        productBundleService.cleanCacheByKey(vo.getId());

        return InvokeResultBuilder.success();
    }

    /**
     * 根据ID删除
     */
    @ApiOperation("根据ID删除")
    @HasPermission({"base-data:product:info:delete"})
    @DeleteMapping
    public InvokeResult<Void> deleteById(
            @ApiParam(value = "ID", required = true) @NotEmpty(message = "ID不能为空！") String id) {

        productService.deleteById(id);

        productService.cleanCacheByKey(id);

        productPropertyRelationService.cleanCacheByKey(id);

        productBundleService.cleanCacheByKey(id);

        return InvokeResultBuilder.success();
    }

    @ApiOperation("导出")
    @HasPermission({"base-data:product:info:import"})
    @PostMapping("/export")
    public InvokeResult<Void> export(@Valid QueryProductVo vo) {
        vo.setOrderBy("g.category_id, g.name");
        ExportTaskUtil.exportTask("商品信息", ProductExportTaskWorker.class, vo);

        return InvokeResultBuilder.success();
    }

    @ApiOperation("下载导入模板")
    @HasPermission({"base-data:product:info:import"})
    @GetMapping("/import/template")
    public void downloadImportTemplate() {
        ExcelUtil.export("商品导入模板", ProductImportModel.class);
    }

    @ApiOperation("导入")
    @HasPermission({"base-data:product:info:import"})
    @PostMapping("/import")
    public InvokeResult<Void> importExcel(@NotNull(message = "请上传文件") MultipartFile file) {
        try {
            List<ProductImportModel> list = EasyExcelUtils.syncReadModel(file.getInputStream(), ProductImportModel.class);
            productService.importExcel(list);

            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }
}
