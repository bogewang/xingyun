package com.lframework.xingyun.api.migration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.testng.Assert;
import org.testng.annotations.Test;

/** 采购收货单乐观锁字段迁移契约测试。 */
public class ReceiveSheetVersionMigrationContractTest {

  /** 验证迁移为采购收货单添加非空且默认值为零的版本号。 */
  @Test
  public void shouldAddReceiveSheetVersionColumn() throws Exception {
    String sql = new String(Files.readAllBytes(Paths.get(
        "src/main/resources/db/migration/tenant/V3.4-receive-sheet-version.sql")),
        StandardCharsets.UTF_8);

    Assert.assertTrue(sql.contains("ALTER TABLE `tbl_receive_sheet`"));
    Assert.assertTrue(sql.contains("`version` int NOT NULL DEFAULT 0"));
  }
}
