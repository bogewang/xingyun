package com.lframework.xingyun.basedata.impl.print;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.print.PrintTemplateColumnDescription;
import com.lframework.xingyun.basedata.entity.PrintTemplate;
import com.lframework.xingyun.basedata.entity.PrintTemplateComp;
import com.lframework.xingyun.basedata.enums.BaseDataOpLogType;
import com.lframework.xingyun.basedata.mappers.PrintTemplateMapper;
import com.lframework.xingyun.basedata.service.print.PrintTemplateCompService;
import com.lframework.xingyun.basedata.service.print.PrintTemplateService;
import com.lframework.xingyun.basedata.vo.print.CopyPrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.CreatePrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.QueryPrintTemplateVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateDemoDataVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateSettingVo;
import com.lframework.xingyun.basedata.vo.print.UpdatePrintTemplateVo;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrintTemplateServiceImpl extends
        BaseMpServiceImpl<PrintTemplateMapper, PrintTemplate> implements
        PrintTemplateService {

    private static final String PRINT_SALE_ORDER_BO_CLASS_NAME =
            "com.lframework.xingyun.sc.bo.sale.PrintSaleOrderBo";
    private static final String PRINT_RECEIVE_SHEET_BO_CLASS_NAME =
            "com.lframework.xingyun.sc.bo.purchase.receive.PrintReceiveSheetBo";
    private static final String RECEIVE_SHEET_BIZ_TYPE = "2";
    private static final String SALE_OUT_BIZ_TYPE = "7";

    @Autowired
    private PrintTemplateCompService printTemplateCompService;

    @Override
    public PageResult<PrintTemplate> query(Integer pageIndex, Integer pageSize,
                                           QueryPrintTemplateVo vo) {
        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<PrintTemplate> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<PrintTemplate> query(QueryPrintTemplateVo vo) {
        return getBaseMapper().query(vo);
    }

    @Override
    public PrintTemplate findById(Integer id) {
        return getById(id);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "删除打印模板，ID：{}", params = {"#id"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Integer id) {
        PrintTemplate data = getById(id);
        if (data == null) {
            throw new DefaultClientException("打印模板不存在！");
        }

        printTemplateCompService.remove(Wrappers.lambdaQuery(PrintTemplateComp.class)
                .eq(PrintTemplateComp::getTemplateId, id));
        getBaseMapper().deleteById(id);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "新增打印模板，名称：{}", params = {"#vo.name"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer create(CreatePrintTemplateVo vo) {
        Wrapper<PrintTemplate> checkNameWrapper = Wrappers.lambdaQuery(PrintTemplate.class)
                .eq(PrintTemplate::getName, vo.getName());
        if (getBaseMapper().selectCount(checkNameWrapper) > 0) {
            throw new DefaultClientException("名称重复，请重新输入！");
        }

        PrintTemplate data = new PrintTemplate();
        data.setName(vo.getName());
        data.setLang(vo.getLang());
        data.setBizType(vo.getBizType());
        data.setVersion(vo.getVersion());

        getBaseMapper().insert(data);

        return data.getId();
    }

    @OpLog(type = BaseDataOpLogType.class, name = "复制打印模板，来源ID：{}，新名称：{}", params = {
            "#vo.sourceId", "#vo.name"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer copy(CopyPrintTemplateVo vo) {
        PrintTemplate source = getById(vo.getSourceId());
        if (source == null) {
            throw new DefaultClientException("来源打印模板不存在！");
        }

        Wrapper<PrintTemplate> checkNameWrapper = Wrappers.lambdaQuery(PrintTemplate.class)
                .eq(PrintTemplate::getName, vo.getName());
        if (getBaseMapper().selectCount(checkNameWrapper) > 0) {
            throw new DefaultClientException("名称重复，请重新输入！");
        }

        PrintTemplate data = new PrintTemplate();
        data.setName(vo.getName());
        data.setLang(source.getLang());
        data.setBizType(vo.getBizType());
        data.setVersion(source.getVersion());
        data.setTemplateJson(source.getTemplateJson());
        data.setDemoData(source.getDemoData());

        getBaseMapper().insert(data);

        printTemplateCompService.copy(source.getId(), data.getId());

        return data.getId();
    }

    @OpLog(type = BaseDataOpLogType.class, name = "修改打印模板，ID：{}，名称：{}", params = {
            "#vo.id", "#vo.name"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(UpdatePrintTemplateVo vo) {
        Wrapper<PrintTemplate> checkNameWrapper = Wrappers.lambdaQuery(PrintTemplate.class)
                .eq(PrintTemplate::getName, vo.getName()).ne(PrintTemplate::getId, vo.getId());
        if (getBaseMapper().selectCount(checkNameWrapper) > 0) {
            throw new DefaultClientException("名称重复，请重新输入！");
        }

        PrintTemplate data = getById(vo.getId());
        if (data == null) {
            throw new DefaultClientException("打印模板不存在！");
        }

        Wrapper<PrintTemplate> updateWrapper = Wrappers.lambdaUpdate(PrintTemplate.class)
                .eq(PrintTemplate::getId, vo.getId())
                .set(PrintTemplate::getName, vo.getName())
                .set(PrintTemplate::getLang, vo.getLang())
                .set(PrintTemplate::getBizType, vo.getBizType())
                .set(PrintTemplate::getVersion, vo.getVersion());

        getBaseMapper().update(updateWrapper);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "修改打印模板设置，ID：{}", params = {
            "#vo.id"}, autoSaveParams = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSetting(UpdatePrintTemplateSettingVo vo) {
        PrintTemplate data = getById(vo.getId());
        if (data == null) {
            throw new DefaultClientException("打印模板不存在！");
        }

        Wrapper<PrintTemplate> updateWrapper = Wrappers.lambdaUpdate(PrintTemplate.class)
                .eq(PrintTemplate::getId, vo.getId())
                .set(PrintTemplate::getTemplateJson, vo.getTemplateJson())
                .set(PrintTemplate::getDemoData, vo.getDemoData());

        getBaseMapper().update(updateWrapper);
    }

    @OpLog(type = BaseDataOpLogType.class, name = "修改打印模板示例数据，ID：{}", params = {
            "#vo.id"}, autoSaveParams = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDemoData(UpdatePrintTemplateDemoDataVo vo) {
        PrintTemplate data = getById(vo.getId());
        if (data == null) {
            throw new DefaultClientException("打印模板不存在！");
        }

        Wrapper<PrintTemplate> updateWrapper = Wrappers.lambdaUpdate(PrintTemplate.class)
                .eq(PrintTemplate::getId, vo.getId())
                .set(PrintTemplate::getDemoData, vo.getDemoData());

        getBaseMapper().update(updateWrapper);
    }

    @Override
    public void cleanCacheByKey(Serializable key) {

    }

    @OpLog(type = BaseDataOpLogType.class, name = "设为默认打印模板，ID：{}", params = {"#id"})
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setDefault(Integer id) {
        PrintTemplate data = getById(id);
        if (data == null) {
            throw new DefaultClientException("打印模板不存在！");
        }

        // 将同一业务类型下的其他模板取消默认
        Wrapper<PrintTemplate> clearWrapper = Wrappers.lambdaUpdate(PrintTemplate.class)
                .eq(PrintTemplate::getBizType, data.getBizType())
                .set(PrintTemplate::getIsDefault, false);
        getBaseMapper().update(clearWrapper);

        // 设置当前模板为默认
        Wrapper<PrintTemplate> setWrapper = Wrappers.lambdaUpdate(PrintTemplate.class)
                .eq(PrintTemplate::getId, id)
                .set(PrintTemplate::getIsDefault, true);
        getBaseMapper().update(setWrapper);
    }

    @Override
    public List<PrintTemplateColumnDescription> getFieldDesc() {
        return getFieldDesc(null);
    }

    /**
     * 按打印业务类型查询模板字段说明。
     *
     * @param bizType 打印业务类型
     * @return 模板字段说明
     */
    @Override
    public List<PrintTemplateColumnDescription> getFieldDesc(String bizType) {
        String normalizedBizType = bizType == null || bizType.trim().isEmpty()
                ? SALE_OUT_BIZ_TYPE : bizType;
        String className;
        Map<String, String> demos;
        String missingMessage;

        if (RECEIVE_SHEET_BIZ_TYPE.equals(normalizedBizType)) {
            className = PRINT_RECEIVE_SHEET_BO_CLASS_NAME;
            demos = buildPrintReceiveSheetDemoMap();
            missingMessage = "采购入库打印字段定义不存在！";
        } else if (SALE_OUT_BIZ_TYPE.equals(normalizedBizType)) {
            className = PRINT_SALE_ORDER_BO_CLASS_NAME;
            demos = buildPrintSaleOrderDemoMap();
            missingMessage = "销售订单打印字段定义不存在！";
        } else {
            throw new DefaultClientException("不支持的打印业务类型！");
        }

        List<PrintTemplateColumnDescription> res = Lists.newArrayList();
        try {
            Class<?> printBoClass = Class.forName(className);

            appendFieldDesc(res, printBoClass, "", demos);
            for (Class<?> innerClass : printBoClass.getDeclaredClasses()) {
                if ("OrderDetailBo".equals(innerClass.getSimpleName())) {
                    appendFieldDesc(res, innerClass, "details[]", demos);
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            throw new DefaultClientException(missingMessage);
        }

        return res;
    }

    private void appendFieldDesc(List<PrintTemplateColumnDescription> res, Class<?> clazz,
                                 String prefix, Map<String, String> demos) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();
            String columnName = "".equals(prefix) ? fieldName : prefix + "." + fieldName;
            ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
            String description = apiModelProperty == null ? fieldName : apiModelProperty.value();

            res.add(new PrintTemplateColumnDescription(columnName, description,
                    demos.getOrDefault(columnName, buildDefaultDemo(field.getType()))));
        }
    }

    private Map<String, String> buildPrintSaleOrderDemoMap() {
        Map<String, String> demos = new LinkedHashMap<>();
        demos.put("code", "202607050000000001");
        demos.put("customerName", "示例客户");
        demos.put("description", "示例备注");
        demos.put("paidAmount", "100.00");
        demos.put("unpaidAmount", "20.00");
        demos.put("createBy", "张三");
        demos.put("orderDate", "2026-07-05");
        demos.put("id", "SO202607050001");
        demos.put("scId", "SC001");
        demos.put("customerId", "CUS001");
        demos.put("salerId", "USER001");
        demos.put("paymentDate", "2026-07-06");
        demos.put("totalNum", "10");
        demos.put("totalGiftNum", "1");
        demos.put("totalAmount", "120.00");
        demos.put("totalCost", "80.00");
        demos.put("totalProfit", "40.00");
        demos.put("fillAllCost", "true");
        demos.put("createTime", "2026-07-05 10:00:00");
        demos.put("updateBy", "李四");
        demos.put("updateTime", "2026-07-05 11:00:00");
        demos.put("approveBy", "王五");
        demos.put("approveTime", "2026-07-05 12:00:00");
        demos.put("status", "APPROVE_PASS");
        demos.put("refuseReason", "资料不完整");
        demos.put("saleOrderId", "SALE202607050001");
        demos.put("settleStatus", "UN_SETTLE");
        demos.put("details", "[{...}]");
        demos.put("details[].seq", "1");
        demos.put("details[].productCode", "P0001");
        demos.put("details[].productName", "示例商品");
        demos.put("details[].spec", "500g");
        demos.put("details[].unit", "袋");
        demos.put("details[].orderNum", "10");
        demos.put("details[].taxPrice", "12.00");
        demos.put("details[].orderAmount", "120.00");
        demos.put("details[].id", "SOD202607050001");
        demos.put("details[].mainProductId", "MP001");
        demos.put("details[].productId", "P001");
        demos.put("details[].oriPrice", "15.00");
        demos.put("details[].discountRate", "80");
        demos.put("details[].isGift", "false");
        demos.put("details[].taxRate", "13");
        demos.put("details[].description", "明细备注");
        demos.put("details[].orderNo", "1");
        demos.put("details[].settleStatus", "UN_SETTLE");
        demos.put("details[].saleOrderDetailId", "SODTL202607050001");
        demos.put("details[].taxAmount", "120.00");
        demos.put("details[].costPrice", "8.00");
        demos.put("details[].manualInputCost", "false");
        demos.put("details[].totalProfit", "40.00");
        demos.put("details[].categoryName", "调味品");

        return demos;
    }

    /**
     * 构建采购入库打印字段示例值。
     *
     * @return 字段示例值
     */
    private Map<String, String> buildPrintReceiveSheetDemoMap() {
        Map<String, String> demos = new LinkedHashMap<>();
        demos.put("id", "RECEIVE202607050001");
        demos.put("code", "RK202607050001");
        demos.put("scId", "SC001");
        demos.put("scCode", "SC001");
        demos.put("scName", "示例仓库");
        demos.put("supplierId", "SUP001");
        demos.put("supplierCode", "SUP001");
        demos.put("supplierName", "示例供应商");
        demos.put("purchaserId", "USER001");
        demos.put("purchaserName", "张三");
        demos.put("orderDate", "2026-07-05");
        demos.put("purchaseOrderId", "PO202607050001");
        demos.put("purchaseOrderCode", "CG202607050001");
        demos.put("paymentDate", "2026-07-06");
        demos.put("receiveDate", "2026-07-07");
        demos.put("totalNum", "10");
        demos.put("totalGiftNum", "1");
        demos.put("totalAmount", "120.00");
        demos.put("paidAmount", "100.00");
        demos.put("unpaidAmount", "20.00");
        demos.put("description", "示例备注");
        demos.put("createBy", "张三");
        demos.put("createTime", "2026-07-05 10:00:00");
        demos.put("updateBy", "李四");
        demos.put("updateTime", "2026-07-05 11:00:00");
        demos.put("approveBy", "王五");
        demos.put("approveTime", "2026-07-05 12:00:00");
        demos.put("status", "APPROVE_PASS");
        demos.put("refuseReason", "资料不完整");
        demos.put("settleStatus", "UN_SETTLE");
        demos.put("details", "[{...}]");
        demos.put("details[].id", "RECEIVE_DETAIL202607050001");
        demos.put("details[].productId", "P001");
        demos.put("details[].productCode", "P0001");
        demos.put("details[].productName", "示例商品");
        demos.put("details[].skuCode", "SKU001");
        demos.put("details[].externalCode", "SP001");
        demos.put("details[].orderNum", "10");
        demos.put("details[].receiveNum", "10");
        demos.put("details[].unitId", "UNIT001");
        demos.put("details[].unitName", "袋");
        demos.put("details[].conversionRate", "1");
        demos.put("details[].businessNum", "10");
        demos.put("details[].taxPrice", "12.00");
        demos.put("details[].purchasePrice", "12.00");
        demos.put("details[].taxAmount", "120.00");
        demos.put("details[].receiveAmount", "120.00");
        demos.put("details[].isGift", "false");
        demos.put("details[].taxRate", "13");
        demos.put("details[].description", "明细备注");
        demos.put("details[].orderNo", "1");
        demos.put("details[].purchaseOrderDetailId", "POD202607050001");
        demos.put("details[].productionDate", "2026.07.01");

        return demos;
    }

    private String buildDefaultDemo(Class<?> fieldType) {
        if (String.class.equals(fieldType)) {
            return "示例文本";
        }
        if (BigDecimal.class.equals(fieldType)) {
            return "0.00";
        }
        if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
            return "1";
        }
        if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
            return "true";
        }
        if (LocalDate.class.equals(fieldType)) {
            return "2026-07-05";
        }
        if (LocalDateTime.class.equals(fieldType)) {
            return "2026-07-05 10:00:00";
        }
        if (List.class.isAssignableFrom(fieldType)) {
            return "[{...}]";
        }
        if (fieldType.isEnum()) {
            Object[] enumConstants = fieldType.getEnumConstants();
            return enumConstants.length == 0 ? "" : String.valueOf(enumConstants[0]);
        }

        return "";
    }
}
