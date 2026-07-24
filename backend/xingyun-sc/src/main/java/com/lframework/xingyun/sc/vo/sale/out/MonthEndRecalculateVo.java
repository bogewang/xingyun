package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 月底成本重算请求参数
 */
@Data
public class MonthEndRecalculateVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 采购时间范围起
     */
    @ApiModelProperty(value = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;

    /**
     * 采购时间范围止
     */
    @ApiModelProperty(value = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 仓库ID，为空则全仓库
     */
    @ApiModelProperty("仓库ID")
    private String scId;
}
