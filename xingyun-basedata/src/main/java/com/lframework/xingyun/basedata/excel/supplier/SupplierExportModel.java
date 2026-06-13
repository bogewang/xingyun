package com.lframework.xingyun.basedata.excel.supplier;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.dto.dic.city.DicCityDto;
import com.lframework.starter.web.inner.service.DicCityService;
import com.lframework.xingyun.basedata.entity.Supplier;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class SupplierExportModel implements ExcelModel {

  /**
   * 编号
   */
  @ExcelProperty("编号")
  private String code;

  /**
   * 名称
   */
  @ExcelProperty("名称")
  private String name;

  /**
   * 简码
   */
  @ExcelProperty("简码")
  private String mnemonicCode;

  /**
   * 联系人
   */
  @ExcelProperty("联系人")
  private String contact;

  /**
   * 联系电话
   */
  @ExcelProperty("联系电话")
  private String telephone;

  /**
   * 电子邮箱
   */
  @ExcelProperty("电子邮箱")
  private String email;

  /**
   * 邮编
   */
  @ExcelProperty("邮编")
  private String zipCode;

  /**
   * 传真
   */
  @ExcelProperty("传真")
  private String fax;

  /**
   * 地区
   */
  @ExcelProperty("地区")
  private String city;

  /**
   * 地址
   */
  @ExcelProperty("地址")
  private String address;

  /**
   * 送货周期（天）
   */
  @ExcelProperty("送货周期（天）")
  private Integer deliveryCycle;

  /**
   * 经营方式
   */
  @ExcelProperty("经营方式")
  private String manageType;

  /**
   * 结算方式
   */
  @ExcelProperty("结算方式")
  private String settleType;

  /**
   * 统一社会信用代码
   */
  @ExcelProperty("统一社会信用代码")
  private String creditCode;

  /**
   * 纳税人识别号
   */
  @ExcelProperty("纳税人识别号")
  private String taxIdentifyNo;

  /**
   * 开户银行
   */
  @ExcelProperty("开户银行")
  private String bankName;

  /**
   * 户名
   */
  @ExcelProperty("户名")
  private String accountName;

  /**
   * 银行账号
   */
  @ExcelProperty("银行账号")
  private String accountNo;

  /**
   * 备注
   */
  @ExcelProperty("备注")
  private String description;

  public SupplierExportModel() {

  }

  public SupplierExportModel(Supplier dto) {

    this.code = dto.getCode();
    this.name = dto.getName();
    this.mnemonicCode = dto.getMnemonicCode();
    this.contact = dto.getContact();
    this.telephone = dto.getTelephone();
    this.email = dto.getEmail();
    this.zipCode = dto.getZipCode();
    this.fax = dto.getFax();
    this.address = dto.getAddress();
    this.deliveryCycle = dto.getDeliveryCycle();
    this.manageType = dto.getManageType() == null ? null : dto.getManageType().getDesc();
    this.settleType = dto.getSettleType() == null ? null : dto.getSettleType().getDesc();
    this.creditCode = dto.getCreditCode();
    this.taxIdentifyNo = dto.getTaxIdentifyNo();
    this.bankName = dto.getBankName();
    this.accountName = dto.getAccountName();
    this.accountNo = dto.getAccountNo();
    // this.paidAmount = dto.getPaidAmount();
    // this.unpaidAmount = dto.getUnpaidAmount();
    // this.amountTotal = getAmountOrZero(this.paidAmount).add(getAmountOrZero(this.unpaidAmount));
    this.description = dto.getDescription();

    if (!StringUtil.isBlank(dto.getCityId())) {
      DicCityService dicCityService = ApplicationUtil.getBean(DicCityService.class);
      List<DicCityDto> cityList = dicCityService.getChainById(dto.getCityId());
      this.city = cityList.stream().map(DicCityDto::getName)
          .collect(Collectors.joining(StringPool.CITY_SPLIT));
    }
  }

  private BigDecimal getAmountOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
