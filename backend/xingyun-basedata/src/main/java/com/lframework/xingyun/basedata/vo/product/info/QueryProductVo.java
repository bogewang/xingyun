package com.lframework.xingyun.basedata.vo.product.info;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.BaseVo;
import com.lframework.starter.web.core.vo.SortPageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QueryProductVo extends SortPageVo implements BaseVo, Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * 仓库ID
	 */
	@ApiModelProperty("仓库ID")
	private String scId;

	/**
	 * 编号
	 */
	@ApiModelProperty("编号")
	private String code;

	/**
	 * 名称
	 */
	@ApiModelProperty("名称")
	private String name;

	/**
	 * SKU
	 */
	@ApiModelProperty("SKU")
	private String skuCode;

	/**
	 * 简称
	 */
	@ApiModelProperty("简称")
	private String shortName;

	/**
	 * 品牌ID
	 */
	@ApiModelProperty("品牌ID")
	private String brandId;

	/**
	 * 分类ID
	 */
	@ApiModelProperty("分类ID")
	private String categoryId;

	/**
	 * 是否询价商品
	 */
	@ApiModelProperty("是否询价商品")
	private Boolean inquiryProduct;

	/**
	 * 创建起始时间
	 */
	@ApiModelProperty("创建起始时间")
	private LocalDateTime startTime;

	/**
	 * 创建截止时间
	 */
	@ApiModelProperty("创建截止时间")
	private LocalDateTime endTime;
	/**
	 * 导出时，需要按分类ID排序
	 */
	private Boolean isExport;
}
