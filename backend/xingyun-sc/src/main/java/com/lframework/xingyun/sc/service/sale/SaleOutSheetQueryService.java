package com.lframework.xingyun.sc.service.sale;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.sc.bo.sale.out.QuerySaleOutSheetBo;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.impl.sale.SaleOutProfitCalculator;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 编排销售出库查询响应及当前页毛利率汇总。 */
@Service
public class SaleOutSheetQueryService {

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    /** 查询单据并由后端计算每行及当前页合计毛利率。 */
    public PageResult<QuerySaleOutSheetBo> query(Integer pageIndex, Integer pageSize,
            QuerySaleOutSheetVo vo) {
        PageResult<SaleOutSheet> page = saleOutSheetService.query(pageIndex, pageSize, vo);
        List<QuerySaleOutSheetBo> results = new ArrayList<>();
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalBase = BigDecimal.ZERO;
        if (page.getDatas() != null) {
            for (SaleOutSheet sheet : page.getDatas()) {
                BigDecimal base = SaleOutProfitCalculator.baseAmount(sheet.getTotalAmount(), sheet.getConfirmAmt());
                QuerySaleOutSheetBo row = new QuerySaleOutSheetBo(sheet);
                row.setProfitRate(SaleOutProfitCalculator.rate(sheet.getTotalProfit(), base));
                results.add(row);
                totalBase = totalBase.add(base);
                totalProfit = totalProfit.add(sheet.getTotalProfit() == null ? BigDecimal.ZERO : sheet.getTotalProfit());
            }
        }
        Map<Object, Object> extra = page.getExtra() == null ? new HashMap<>() : new HashMap<>(page.getExtra());
        extra.put("profitRate", SaleOutProfitCalculator.rate(totalProfit, totalBase));
        page.setExtra(extra);
        return PageResultUtil.rebuild(page, results);
    }
}
