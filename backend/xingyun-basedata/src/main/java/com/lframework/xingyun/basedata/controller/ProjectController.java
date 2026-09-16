package com.lframework.xingyun.basedata.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.xingyun.basedata.entity.Project;
import com.lframework.xingyun.basedata.mappers.ProjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 项目选择接口。 */
@RestController
@RequestMapping("/basedata/project")
public class ProjectController extends DefaultBaseController {
  @Autowired
  private ProjectMapper projectMapper;

  /** 查询当前租户可用项目。 */
  @GetMapping("/selector")
  @HasPermission({"base-data:quote:query", "base-data:customer:query"})
  public InvokeResult<List<Project>> selector() {
    return InvokeResultBuilder.success(projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
        .eq(Project::getAvailable, Boolean.TRUE).orderByAsc(Project::getCode)));
  }
}
