package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 月底成本重算（启动）请求参数
 * <p>
 * calcBeginDate/calcEndDate 用于计算全范围月加权均价，
 * 前端按天拆分后逐天调用 step 接口执行。
 *
 * @author bogewang
 */
@Data
public class MonthEndRecalculateStartVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 均价计算范围——起
     */
    @ApiModelProperty(value = "计算均价开始日期", required = true)
    @NotNull(message = "计算均价开始日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate calcBeginDate;

    /**
     * 均价计算范围——止
     */
    @ApiModelProperty(value = "计算均价结束日期", required = true)
    @NotNull(message = "计算均价结束日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate calcEndDate;

    /**
     * 仓库ID，为空则全仓库
     */
    @ApiModelProperty("仓库ID")
    private String scId;

    /**
     * 自定义校验：calcEndDate 不能早于 calcBeginDate
     */
    @AssertTrue(message = "计算均价结束日期不能早于开始日期！")
    public boolean isCalcDateRangeValid() {
        if (calcBeginDate == null || calcEndDate == null) {
            return true;
        }
        return !calcEndDate.isBefore(calcBeginDate);
    }
}
