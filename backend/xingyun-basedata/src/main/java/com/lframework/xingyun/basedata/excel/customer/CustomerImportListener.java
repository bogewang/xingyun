package com.lframework.xingyun.basedata.excel.customer;

import com.alibaba.excel.context.AnalysisContext;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.constants.PatternPool;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.RegUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.components.excel.ExcelImportListener;
import com.lframework.starter.web.core.utils.EnumUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.enums.SettleType;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.starter.web.inner.dto.dic.city.DicCityDto;
import com.lframework.starter.web.inner.service.DicCityService;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomerImportListener extends ExcelImportListener<CustomerImportModel> {

  private static final Integer CUSTOMER_CODE_TYPE = 5;

  private List<String> checkList = new ArrayList<>();

  @Override
  protected void doInvoke(CustomerImportModel data, AnalysisContext context) {
    if (StringUtil.isBlank(data.getCode())) {
      data.setCode(generateCode());
    }
    validAndCheckCode(data, context);

    if (StringUtil.isBlank(data.getName())) {
      throw new DefaultClientException(
          "第" + context.readRowHolder().getRowIndex() + "行“名称”不能为空");
    }
    if (StringUtil.isBlank(data.getMnemonicCode())) {
      data.setMnemonicCode(data.getCode());
    }
    if (StringUtil.isBlank(data.getSettleType())) {
      data.setSettleTypeEnum(SettleType.ARBITRARILY);
    } else {
      SettleType settleType = EnumUtil.getByDesc(SettleType.class, data.getSettleType());
      if (settleType == null) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex() + "行“结算方式”只能填写“"
                + CollectionUtil.join(
                EnumUtil.getDescs(SettleType.class), "、") + "”");
      }
      data.setSettleTypeEnum(settleType);
    }

    if (!StringUtil.isBlank(data.getCity())) {
      String[] arr = data.getCity().split("/");
      if (arr.length != 3) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex()
                + "行“地区”格式错误，应为省/市/区（县），例如：北京市/市辖区/东城区");
      }

      DicCityService dicCityService = ApplicationUtil.getBean(DicCityService.class);
      List<DicCityDto> allCitys = dicCityService.getAll();
      DicCityDto province = allCitys.stream()
          .filter(t -> StringUtil.isEmpty(t.getParentId()) && t.getName().equals(arr[0]))
          .findFirst().orElse(null);
      if (province == null) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex() + "行“地区”错误，省份不存在");
      }
      DicCityDto city = allCitys.stream()
          .filter(t -> province.getId().equals(t.getParentId()) && t.getName().equals(arr[1]))
          .findFirst().orElse(null);
      if (city == null) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex() + "行“地区”错误，市不存在");
      }
      DicCityDto area = allCitys.stream()
          .filter(t -> city.getId().equals(t.getParentId()) && t.getName().equals(arr[2]))
          .findFirst().orElse(null);
      if (area == null) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex() + "行“地区”错误，区（县）不存在");
      }

      data.setAreaId(area.getId());
    }

    if (!StringUtil.isEmpty(data.getEmail())) {
      if (!RegUtil.isMatch(PatternPool.EMAIL, data.getEmail())) {
        throw new DefaultClientException(
            "第" + context.readRowHolder().getRowIndex() + "行“电子邮箱”格式有误");
      }
    }
  }

  @Override
  protected void afterAllAnalysed(AnalysisContext context) {

    CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);

    List<CustomerImportModel> datas = this.getDatas();
    for (int i = 0; i < datas.size(); i++) {
      CustomerImportModel data = datas.get(i);

      Customer record = new Customer();
      record.setId(IdUtil.getId());

      record.setCode(data.getCode());
      record.setName(data.getName());
      record.setNickName(data.getNickName());
      record.setMnemonicCode(data.getMnemonicCode());
      record.setContact(data.getContact());
      record.setTelephone(data.getTelephone());
      record.setEmail(data.getEmail());
      record.setZipCode(data.getZipCode());
      record.setFax(data.getFax());
      record.setCityId(data.getAreaId());
      record.setAddress(data.getAddress());
      record.setSettleType(data.getSettleTypeEnum());
      record.setCreditCode(data.getCreditCode());
      record.setTaxIdentifyNo(data.getTaxIdentifyNo());
      record.setBankName(data.getBankName());
      record.setAccountName(data.getAccountName());
      record.setAccountNo(data.getAccountNo());
      record.setDescription(
          StringUtil.isBlank(data.getDescription()) ? StringPool.EMPTY_STR : data.getDescription());

      record.setAvailable(Boolean.TRUE);

      customerService.save(record);
      data.setId(record.getId());

      this.setSuccessProcess(i);
    }
  }

  @Override
  protected void doComplete() {
  }

  private void validAndCheckCode(CustomerImportModel data, AnalysisContext context) {
    if (!RegUtil.isMatch(PatternPool.PATTERN_CODE, data.getCode())) {
      throw new DefaultClientException(
          "第" + context.readRowHolder().getRowIndex()
              + "行“编号”必须由字母、数字、“-_.”组成，长度不能超过20位");
    }

    if (checkList.contains(data.getCode())) {
      throw new DefaultClientException(
          "第" + context.readRowHolder().getRowIndex() + "行“编号”与第"
              + (checkList.indexOf(data.getCode()) + 1) + "行重复");
    }

    Wrapper<Customer> checkWrapper = Wrappers.lambdaQuery(Customer.class)
        .eq(Customer::getCode, data.getCode())
        .eq(Customer::getAvailable, Boolean.TRUE);
    CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
    if (customerService.count(checkWrapper) > 0) {
      throw new DefaultClientException(
          "第" + context.readRowHolder().getRowIndex() + "行“编号”已存在");
    }

    checkList.add(data.getCode());
  }

  private String generateCode() {
    GenerateCodeService generateCodeService = ApplicationUtil.getBean(GenerateCodeService.class);
    CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);

    while (true) {
      String code = generateCodeService.generate(CUSTOMER_CODE_TYPE);
      if (checkList.contains(code)) {
        continue;
      }

      Wrapper<Customer> checkWrapper = Wrappers.lambdaQuery(Customer.class)
          .eq(Customer::getCode, code)
          .eq(Customer::getAvailable, Boolean.TRUE);
      if (customerService.count(checkWrapper) == 0) {
        return code;
      }
    }
  }
}
