package com.lframework.xingyun.basedata.impl.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.mappers.SupplierMapper;
import com.lframework.xingyun.basedata.vo.supplier.UpdateSupplierAvailableVo;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

/**
 * 供应商停用功能测试。
 */
class SupplierAvailabilityTest {

  /**
   * 停用供应商后应拒绝新增业务单据。
   *
   * @throws Exception 反射注入失败时抛出
   */
  @Test
  void shouldRejectDisabledSupplier() throws Exception {
    SupplierMapper mapper = mock(SupplierMapper.class);
    Supplier supplier = new Supplier();
    supplier.setAvailable(Boolean.FALSE);
    when(mapper.selectById("supplier-1")).thenReturn(supplier);

    SupplierServiceImpl service = new SupplierServiceImpl();
    setBaseMapper(service, mapper);

    DefaultClientException exception = assertThrows(DefaultClientException.class,
        () -> service.assertAvailable("supplier-1"));

    assertEquals("供应商已停用，无法新增业务单据！", exception.getMessage());
  }

  /**
   * 批量状态更新应过滤空白和重复 ID。
   *
   * @throws Exception 反射注入失败时抛出
   */
  @Test
  void shouldUpdateSupplierAvailabilityInOneMapperCall() throws Exception {
    SupplierMapper mapper = mock(SupplierMapper.class);
    when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

    SupplierServiceImpl service = new SupplierServiceImpl();
    setBaseMapper(service, mapper);
    UpdateSupplierAvailableVo vo = new UpdateSupplierAvailableVo();
    vo.setIds(Arrays.asList("supplier-1", " ", "supplier-1", "supplier-2"));
    vo.setAvailable(Boolean.FALSE);

    service.updateAvailable(vo);

    verify(mapper).update(isNull(), any(LambdaUpdateWrapper.class));
  }

  /**
   * 通过反射注入 BaseMapper，隔离 Spring 容器。
   *
   * @param service 供应商服务
   * @param mapper 供应商 Mapper
   * @throws Exception 反射注入失败时抛出
   */
  private void setBaseMapper(SupplierServiceImpl service, SupplierMapper mapper) throws Exception {
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
        Supplier.class);
    Class<?> type = service.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    throw new IllegalStateException("未找到 BaseMapper 字段");
  }
}
