package com.lframework.xingyun.api.config;

import com.lframework.starter.web.core.components.security.CheckPermissionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile({"dev", "@profiles-active@"})
@Configuration
public class DevPermissionBypassConfiguration {

  @Bean
  @Primary
  public CheckPermissionHandler checkPermissionHandler() {

    return (calcType, requirePlatform, permissions) -> true;
  }
}
