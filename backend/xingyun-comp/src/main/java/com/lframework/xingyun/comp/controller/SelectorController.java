package com.lframework.xingyun.comp.controller;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.inner.bo.system.tenant.QueryTenantBo;
import com.lframework.starter.web.inner.dto.dic.city.DicCityDto;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.DicCityService;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.starter.web.inner.vo.system.tenant.QueryTenantVo;
import com.lframework.xingyun.comp.bo.dic.CitySelectorBo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据选择器
 *
 * @author zmj
 */
@Api(tags = "数据选择器")
@Validated
@RestController
@RequestMapping("/selector")
public class SelectorController extends DefaultBaseController {

    @Autowired
    private DicCityService dicCityService;

    @Autowired
    private TenantService tenantService;

    /**
     * 城市数据
     */
    @ApiOperation("城市数据")
    @GetMapping("/city")
    public InvokeResult<List<CitySelectorBo>> dicCity() {

        List<DicCityDto> datas = dicCityService.getAll();
        List<CitySelectorBo> results = CollectionUtil.emptyList();
        if (!CollectionUtil.isEmpty(datas)) {
            results = datas.stream().map(CitySelectorBo::new).collect(Collectors.toList());
        }

        return InvokeResultBuilder.success(results);
    }

    /**
     * 租户
     */
    @ApiOperation("租户")
    @GetMapping("/list/tenant")
    public InvokeResult<List<QueryTenantBo>> tenant(@Valid QueryTenantVo vo) {

        PageResult<Tenant> pageResult = tenantService.query(getPageIndex(vo), getPageSize(vo), vo);

        List<QueryTenantBo> datas = pageResult.getDatas().stream()
                .map(item -> {
                    Tenant tenant = new Tenant();
                    tenant.setName(item.getName());
                    return tenant;
                })
                .map(QueryTenantBo::new)
                .collect(Collectors.toList());


        return InvokeResultBuilder.success(datas);
    }
}
