package com.lframework.xingyun.basedata.excel.customer;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.dto.dic.city.DicCityDto;
import com.lframework.starter.web.inner.service.DicCityService;
import com.lframework.xingyun.basedata.entity.Customer;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * 客户信息导出 Excel 行数据。
 */
@Data
public class CustomerExportModel implements ExcelModel {

  @ExcelProperty("编号")
  private String code;

  @ExcelProperty("名称")
  private String name;

  @ExcelProperty("昵称")
  private String nickName;

  @ExcelProperty("简码")
  private String mnemonicCode;

  @ExcelProperty("联系人")
  private String contact;

  @ExcelProperty("联系电话")
  private String telephone;

  @ExcelProperty("电子邮箱")
  private String email;

  @ExcelProperty("邮编")
  private String zipCode;

  @ExcelProperty("传真")
  private String fax;

  @ExcelProperty("地区")
  private String city;

  @ExcelProperty("地址")
  private String address;

  @ExcelProperty("结算方式")
  private String settleType;

  @ExcelProperty("统一社会信用代码")
  private String creditCode;

  @ExcelProperty("纳税人识别号")
  private String taxIdentifyNo;

  @ExcelProperty("开户银行")
  private String bankName;

  @ExcelProperty("户名")
  private String accountName;

  @ExcelProperty("银行账号")
  private String accountNo;

  @ExcelProperty("累计已付金额")
  private BigDecimal paidAmount;

  @ExcelProperty("累计未付金额")
  private BigDecimal unpaidAmount;

  @ExcelProperty("备注")
  private String description;

  /**
   * 创建空的 Excel 行数据。
   */
  public CustomerExportModel() {
  }

  /**
   * 根据客户实体创建 Excel 行数据。
   */
  public CustomerExportModel(Customer data) {
    this.code = data.getCode();
    this.name = data.getName();
    this.nickName = data.getNickName();
    this.mnemonicCode = data.getMnemonicCode();
    this.contact = data.getContact();
    this.telephone = data.getTelephone();
    this.email = data.getEmail();
    this.zipCode = data.getZipCode();
    this.fax = data.getFax();
    this.address = data.getAddress();
    this.settleType = data.getSettleType() == null ? null : data.getSettleType().getDesc();
    this.creditCode = data.getCreditCode();
    this.taxIdentifyNo = data.getTaxIdentifyNo();
    this.bankName = data.getBankName();
    this.accountName = data.getAccountName();
    this.accountNo = data.getAccountNo();
    this.paidAmount = data.getPaidAmount();
    this.unpaidAmount = data.getUnpaidAmount();
    this.description = data.getDescription();

    if (!StringUtil.isBlank(data.getCityId())) {
      DicCityService dicCityService = ApplicationUtil.getBean(DicCityService.class);
      List<DicCityDto> cityList = dicCityService.getChainById(data.getCityId());
      this.city = cityList.stream().map(DicCityDto::getName)
          .collect(Collectors.joining(StringPool.CITY_SPLIT));
    }
  }
}
