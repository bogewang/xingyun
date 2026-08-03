package com.lframework.xingyun.basedata.service.print;

import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.bo.print.PrintTemplateColumnDescription;
import com.lframework.xingyun.basedata.entity.PrintTemplate;
import com.lframework.xingyun.basedata.vo.print.CopyPrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.CreatePrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.QueryPrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateDemoDataVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateSettingVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateVo;

import java.util.List;

public interface PrintTemplateService extends BaseMpService<PrintTemplate> {

    /**
     * 查询列表
     *
     * @return
     */
    PageResult<PrintTemplate> query(Integer pageIndex, Integer pageSize, QueryPrintTemplateVo vo);

    /**
     * 查询列表
     *
     * @param vo
     * @return
     */
    List<PrintTemplate> query(QueryPrintTemplateVo vo);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    PrintTemplate findById(Integer id);

    /**
     * 删除
     *
     * @param id
     */
    void deleteById(Integer id);

    /**
     * 创建
     *
     * @param vo
     * @return
     */
    Integer create(CreatePrintTemplateVo vo);

    /**
     * 复制
     *
     * @param vo
     * @return
     */
    Integer copy(CopyPrintTemplateVo vo);

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdatePrintTemplateVo vo);

    /**
     * 保存设置
     *
     * @param vo
     */
    void updateSetting(UpdatePrintTemplateSettingVo vo);

    /**
     * 保存示例数据
     *
     * @param vo
     */
    void updateDemoData(UpdatePrintTemplateDemoDataVo vo);

    /**
     * 查询模板配置字段说明
     *
     * @return
     */
    List<PrintTemplateColumnDescription> getFieldDesc();

    /**
     * 按业务类型查询模板配置字段说明。
     *
     * @param bizType 业务类型
     * @return 模板字段说明
     */
    List<PrintTemplateColumnDescription> getFieldDesc(String bizType);

    /**
     * 设为默认模板（同一业务类型下仅允许一个默认）
     *
     * @param id 模板ID
     */
    void setDefault(Integer id);
}
