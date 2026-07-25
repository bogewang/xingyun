package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 月底成本重算（启动）返回结果
 *
 * @author bogewang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndRecalculateStartResult implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID，前端需要保存并在每次 step 调用时回传
     */
    private String taskId;

    /**
     * 总天数，用于前端展示进度条
     */
    private int totalDays;
}
