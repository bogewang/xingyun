package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 月底成本重算（逐天执行）请求参数
 *
 * @author bogewang
 */
@Data
public class MonthEndRecalculateStepVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * start 接口返回的任务ID
     */
    @ApiModelProperty(value = "任务ID", required = true)
    @NotBlank(message = "任务ID不能为空！")
    private String taskId;

    /**
     * 本次要处理的日期（单天）
     */
    @ApiModelProperty(value = "处理日期", required = true)
    @NotNull(message = "处理日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate processDate;
}
