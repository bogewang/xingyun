package com.lframework.xingyun.basedata.controller.quote;

import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.*;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.quote.*;
import com.lframework.xingyun.basedata.converter.quote.QuoteSheetConverter;
import com.lframework.xingyun.basedata.entity.quote.QuoteSheet;
import com.lframework.xingyun.basedata.excel.quote.QuoteSheetImportModel;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.EasyExcelUtils;
import com.lframework.xingyun.basedata.service.quote.QuoteSheetService;
import com.lframework.xingyun.basedata.vo.quote.*;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 报价单管理接口。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/basedata/quote")
public class QuoteSheetController extends DefaultBaseController {
    @Autowired
    private QuoteSheetService quoteSheetService;
    @Autowired
    private QuoteSheetConverter quoteSheetConverter;

    /**
     * 下载报价单导入模板。
     */
    @GetMapping("/import/template")
    @HasPermission("base-data:quote:add")
    public void downloadImportTemplate() {
        ExcelUtil.export("报价单导入模板", QuoteSheetImportModel.class);
    }

    /** 解析报价单导入文件，后续由页面提示未匹配商品。 */
    @PostMapping("/import")
    @HasPermission("base-data:quote:add")
    public InvokeResult<List<QuoteSheetImportModel>> importExcel(@RequestParam MultipartFile file) {
        try {
            return InvokeResultBuilder.success(EasyExcelUtils.syncReadModel(file.getInputStream(), QuoteSheetImportModel.class));
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * 分页查询报价单。
     */
    @PostMapping("/query")
    @HasPermission("base-data:quote:query")
    public InvokeResult<PageResult<QueryQuoteSheetBo>> query(@Valid @RequestBody QueryQuoteSheetVo vo) {
        try {
            PageResult<QuoteSheet> page = quoteSheetService.query(getPageIndex(vo), getPageSize(vo), vo);
            List<QueryQuoteSheetBo> data = page.getDatas().stream().map(quoteSheetConverter::toQueryBo).collect(Collectors.toList());
            return InvokeResultBuilder.success(PageResultUtil.rebuild(page, data));
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * 获取报价单详情。
     */
    @PostMapping("/get")
    @HasPermission("base-data:quote:query")
    public InvokeResult<GetQuoteSheetBo> get(@RequestParam @NotBlank(message = "ID不能为空！") String id) {
        try {
            return InvokeResultBuilder.success(quoteSheetService.get(id));
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * 新增报价单。
     */
    @PostMapping("/create")
    @HasPermission("base-data:quote:add")
    public InvokeResult<String> create(@Valid @RequestBody CreateQuoteSheetVo vo) {
        try {
            return InvokeResultBuilder.success(quoteSheetService.create(vo));
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * 修改报价单。
     */
    @PostMapping("/update")
    @HasPermission("base-data:quote:modify")
    public InvokeResult<Void> update(@Valid @RequestBody UpdateQuoteSheetVo vo) {
        try {
            quoteSheetService.update(vo);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 删除报价单。
     */
    @PostMapping("/delete")
    @HasPermission("base-data:quote:delete")
    public InvokeResult<Void> delete(@RequestParam @NotBlank(message = "ID不能为空！") String id) {
        try {
            quoteSheetService.deleteById(id);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 启用报价单。
     */
    @PostMapping("/enable")
    @HasPermission("base-data:quote:modify")
    public InvokeResult<Void> enable(@RequestParam @NotBlank(message = "ID不能为空！") String id) {
        try {
            quoteSheetService.enable(id);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 停用报价单。
     */
    @PostMapping("/disable")
    @HasPermission("base-data:quote:modify")
    public InvokeResult<Void> disable(@RequestParam @NotBlank(message = "ID不能为空！") String id) {
        try {
            quoteSheetService.disable(id);
            return InvokeResultBuilder.success();
        } catch (Exception e) {
            log.error("请求出错", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }

    /**
     * 查询指定日期可用报价商品。
     */
    @PostMapping("/products/active")
    @HasPermission("base-data:quote:query")
    public InvokeResult<List<QuoteProductBo>> activeProducts(@Valid @RequestBody QueryQuoteProductVo vo) {
        try {
            return InvokeResultBuilder.success(quoteSheetService.getActiveQuoteProducts(vo));
        } catch (Exception e) {
            return fail(e);
        }
    }

    /**
     * 将异常转换为统一失败响应。
     */
    @SuppressWarnings("unchecked")
    private <T> InvokeResult<T> fail(Exception e) {
        log.error("请求出错", e);
        return (InvokeResult<T>) (InvokeResult<?>) InvokeResultBuilder.fail(e.getMessage());
    }
}
