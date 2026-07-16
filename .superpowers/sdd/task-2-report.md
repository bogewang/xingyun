# Task 2 实施报告：商品询价标识持久化与查询筛选

## 变更内容

- `ProductServiceImpl#create` 将空的 `inquiryProduct` 持久化为 `false`。
- `ProductServiceImpl#update` 覆盖持久化传入标识，空值按 `false` 保存。
- 新增包可见静态方法 `resolveInquiryProduct`：新增空值返回 `false`，导入更新空值保留原值。
- `ProductMapper.xml` 的 `query` 与 `queryCount` 增加可空 `inquiryProduct` 筛选，保证分页数据和总数一致。
- 新增两项单元测试，覆盖新增默认值和导入更新空值保留逻辑。

## TDD 证据

### RED

命令：

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductServiceImplTest#shouldDefaultNullInquiryProductToFalse+shouldKeepExistingInquiryProductWhenImportValueIsBlank -DfailIfNoTests=false test
```

结果：退出码 `1`。`ProductServiceImplTest` 在测试编译阶段失败，错误为找不到 `ProductServiceImpl.resolveInquiryProduct(...)`，符合先写测试、生产方法尚不存在的预期。

### GREEN（聚焦）

命令：

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductServiceImplTest#shouldDefaultNullInquiryProductToFalse+shouldKeepExistingInquiryProductWhenImportValueIsBlank -DfailIfNoTests=false test
```

结果：退出码 `0`，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`。

### GREEN（全类回归）

命令：

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductServiceImplTest -DfailIfNoTests=false test
```

结果：退出码 `0`，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`。

## 修改文件

- `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- `xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml`
- `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`
- `.superpowers/sdd/task-2-report.md`

## 自检

- 已执行 `git diff --check`，无空白错误。
- 创建、修改和查询筛选均使用简报指定表达式；未新增直接 SQL、未触及 Excel 或前端。
- `queryCount` 同步添加筛选条件，避免分页总数与列表不一致。
- 所有新增 Java 方法均含中文 Javadoc。

## 提交

提交信息：`feat: persist and filter product inquiry flag`

## Concerns

- 本任务按简报只覆盖解析辅助方法的单元测试；Mapper XML 筛选及完整服务持久化链路依赖 MyBatis/Spring 集成环境，未新增数据库集成测试。
