# 商品询价标识 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为商品新增以 `1/0` 保存的询价标识，并支持录入、查看、查询筛选、Excel 导入导出。

**Architecture:** 后端业务契约采用 `Boolean inquiryProduct`，MyBatis-Plus 映射至 `base_data_product.inquiry_product`。Excel 模型采用独立的“是/否”文本列，在 Service 内转换，兼容旧模板空值。前端录入使用复选框，查询使用“全部/是/否”下拉。

**Tech Stack:** Java 8、Spring Boot 2.2.2、MyBatis-Plus 3.4.2、EasyExcel、TestNG、Vue 3、TypeScript、ant-design-vue。

## Global Constraints

- 租户库迁移列必须是 `TINYINT(1) NOT NULL DEFAULT 0`。
- 业务异常使用 `DefaultClientException`；`@Transactional` 仅保留在 Service。
- Excel 空值：新增商品保存 `false`，更新商品保留原值；非空值只接受“是/否”。
- 新增、修改使用复选框；查询页使用默认“全部”的三态下拉框。
- 新增 Java 方法必须添加中文注释。

---

## File Structure

- Create: `xingyun-api/src/main/resources/db/migration/tenant/V2.3-product-inquiry-product.sql`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/Product.java`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/{vo/product/info/{CreateProductVo,UpdateProductVo,QueryProductVo}.java,bo/product/info/{GetProductBo,QueryProductBo}.java,excel/product/ProductImportModel.java,impl/product/ProductServiceImpl.java}`
- Modify: `xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml`
- Modify: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`
- Modify: `frontend/src/api/base-data/product/info/model/{createProductVo,updateProductVo,queryProductVo,getProductBo,queryProductBo}.ts`
- Modify: `frontend/src/views/base-data/product/info/{add,modify,detail,index}.vue`

### Task 1: 添加数据库和 API 契约

**Files:**
- Create: `xingyun-api/src/main/resources/db/migration/tenant/V2.3-product-inquiry-product.sql`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/Product.java`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info/{CreateProductVo,UpdateProductVo,QueryProductVo}.java`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info/{GetProductBo,QueryProductBo}.java`
- Test: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Produces: `Boolean Product#getInquiryProduct()` 和所有商品 API 中的同名字段。
- Produces: `base_data_product.inquiry_product TINYINT(1) NOT NULL DEFAULT 0`。

- [ ] **Step 1: 写失败测试**

在 `ProductServiceImplTest` 新增：

```java
@Test
void shouldKeepInquiryProductOnProductEntity() {
  Product product = new Product();
  product.setInquiryProduct(Boolean.TRUE);
  Assert.assertTrue(product.getInquiryProduct());
}
```

- [ ] **Step 2: 验证测试失败**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest#shouldKeepInquiryProductOnProductEntity test`

Expected: 编译失败，提示 `Product` 没有 `get/setInquiryProduct`。

- [ ] **Step 3: 写最小实现**

创建迁移：

```sql
ALTER TABLE `base_data_product`
    ADD COLUMN `inquiry_product` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否询价商品：0否，1是' AFTER `retail_price`;
```

在 `Product` 添加：

```java
/** 是否询价商品 */
private Boolean inquiryProduct;
```

在创建、修改、查询 VO 及列表、详情 BO 各添加同名字段；VO/BO 上使用 `@ApiModelProperty("是否询价商品")`。查询 VO 中字段可空，以表达“全部”。

- [ ] **Step 4: 验证测试通过**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest#shouldKeepInquiryProductOnProductEntity test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add xingyun-api/src/main/resources/db/migration/tenant/V2.3-product-inquiry-product.sql xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/Product.java xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: add product inquiry field contract"
```

### Task 2: 持久化字段并支持查询筛选

**Files:**
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- Modify: `xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml`
- Test: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: `CreateProductVo/UpdateProductVo/QueryProductVo.inquiryProduct`。
- Produces: 创建空值为 false、修改覆盖值、查询按可空字段筛选。

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldDefaultNullInquiryProductToFalse() {
  Assert.assertFalse(ProductServiceImpl.resolveInquiryProduct(null, null, true));
}

@Test
void shouldKeepExistingInquiryProductWhenImportValueIsBlank() {
  Assert.assertTrue(ProductServiceImpl.resolveInquiryProduct(null, Boolean.TRUE, false));
}
```

- [ ] **Step 2: 验证测试失败**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest#shouldDefaultNullInquiryProductToFalse+shouldKeepExistingInquiryProductWhenImportValueIsBlank test`

Expected: 编译失败，提示 `resolveInquiryProduct` 未定义。

- [ ] **Step 3: 写最小实现**

在 `ProductServiceImpl` 新增带中文注释的包可见辅助方法：

```java
static Boolean resolveInquiryProduct(Boolean inquiryProduct, Boolean existingInquiryProduct,
        boolean isNew) {
    if (inquiryProduct != null) {
        return inquiryProduct;
    }
    return isNew ? Boolean.FALSE : existingInquiryProduct;
}
```

在 `create` 设置：

```java
data.setInquiryProduct(Boolean.TRUE.equals(vo.getInquiryProduct()));
```

在 `update` 的 `LambdaUpdateWrapper` 设置：

```java
.set(Product::getInquiryProduct, Boolean.TRUE.equals(vo.getInquiryProduct()))
```

在 `ProductMapper.xml` 查询条件中增加：

```xml
<if test="vo.inquiryProduct != null">
    AND g.inquiry_product = #{vo.inquiryProduct}
</if>
```

- [ ] **Step 4: 验证通过并提交**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: `BUILD SUCCESS`。

```bash
git add xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: persist and filter product inquiry flag"
```

### Task 3: 支持 Excel 导入、导出和旧模板

**Files:**
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/excel/product/ProductImportModel.java`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- Test: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: Excel “询价商品”列的 `是`、`否`或空值。
- Produces: 导出“是/否”；非法非空值抛出 `DefaultClientException("第N行“询价商品”只能填写“是、否”")`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldParseInquiryProductYesAndNo() {
  Assert.assertTrue(ProductServiceImpl.parseInquiryProduct("是", 2));
  Assert.assertFalse(ProductServiceImpl.parseInquiryProduct("否", 2));
  Assert.assertNull(ProductServiceImpl.parseInquiryProduct(null, 2));
}

@Test(expectedExceptions = DefaultClientException.class,
    expectedExceptionsMessageRegExp = "第2行“询价商品”只能填写“是、否”")
void shouldRejectInvalidInquiryProductText() {
  ProductServiceImpl.parseInquiryProduct("1", 2);
}
```

- [ ] **Step 2: 验证测试失败**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest#shouldParseInquiryProductYesAndNo+shouldRejectInvalidInquiryProductText test`

Expected: 编译失败，提示 `parseInquiryProduct` 未定义。

- [ ] **Step 3: 写最小实现**

在 `ProductImportModel` 添加独立文本字段，避免 `BaseBo<Product>` 同名自动复制覆盖值：

```java
/** 是否询价商品 */
@ExcelProperty("询价商品")
private String inquiryProductText;

@ExcelIgnore
private Boolean inquiryProductValue;

@ExcelIgnore
private Boolean existingInquiryProduct;
```

在 `afterInit(Product dto)` 设置：

```java
this.inquiryProductText = Boolean.TRUE.equals(dto.getInquiryProduct()) ? "是" : "否";
```

在 Service 添加带中文注释的方法：

```java
static Boolean parseInquiryProduct(String inquiryProductText, int rowIndex) {
    if (StringUtil.isBlank(inquiryProductText)) {
        return null;
    }
    if ("是".equals(inquiryProductText)) {
        return Boolean.TRUE;
    }
    if ("否".equals(inquiryProductText)) {
        return Boolean.FALSE;
    }
    throw new DefaultClientException("第" + rowIndex + "行“询价商品”只能填写“是、否”");
}
```

在 `checkRules` 中解析文本并把结果保存到 `inquiryProductValue`。在现有按编码、SKU、名称规格单位批量查询命中商品的过程中，同时填充 `existingInquiryProduct`，不得逐行查库。在 `buildProducts` 中设置：

```java
record.setInquiryProduct(resolveInquiryProduct(data.getInquiryProductValue(),
        data.getExistingInquiryProduct(), isNew));
```

- [ ] **Step 4: 验证通过、手工往返并提交**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: `BUILD SUCCESS`。

手工验证模板：新增空值保存 0；已有询价商品空值更新后仍为 1；“是/否”分别为 1/0；导出显示“是/否”且可再次导入。

```bash
git add xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/excel/product/ProductImportModel.java xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: support product inquiry flag in excel"
```

### Task 4: 更新前端类型与商品页面

**Files:**
- Modify: `frontend/src/api/base-data/product/info/model/{createProductVo,updateProductVo,queryProductVo,getProductBo,queryProductBo}.ts`
- Modify: `frontend/src/views/base-data/product/info/{add,modify,detail,index}.vue`

**Interfaces:**
- Consumes: 后端 `inquiryProduct?: boolean` 查询条件与响应。
- Produces: 录入复选框、三态筛选、列表和详情“是/否”。

- [ ] **Step 1: 先更新 API 类型并运行检查**

在创建、修改、详情、列表类型中增加：

```ts
/** 是否询价商品 */
inquiryProduct: boolean;
```

在 `QueryProductVo` 增加：

```ts
/** 是否询价商品；空值表示全部 */
inquiryProduct: boolean | '';
```

Run: `pnpm --dir frontend run type:check`

Expected: 现有页面尚未处理新增字段时，若类型检查能检测到则失败；否则进入下一步并以最终检查验证。

- [ ] **Step 2: 实现录入复选框、查询下拉和展示**

新增、修改页加入：

```vue
<a-col :md="6" :sm="24">
  <a-form-item label="询价商品" name="inquiryProduct">
    <a-checkbox v-model:checked="formData.inquiryProduct">询价商品</a-checkbox>
  </a-form-item>
</a-col>
```

两个页面的 `initFormData` 均使用：

```js
this.formData = { inquiryProduct: false, multiUnitEnabled: false, auxiliaryUnits: [] };
```

修改页加载数据时使用 `inquiryProduct: Boolean(data.inquiryProduct)`，保证历史空值显示为未勾选。

详情添加：

```vue
<a-descriptions-item label="询价商品" :span="2">
  {{ formData.inquiryProduct ? '是' : '否' }}
</a-descriptions-item>
```

列表查询表单添加：

```vue
<j-form-item label="询价商品">
  <a-select v-model:value="searchFormData.inquiryProduct" allow-clear>
    <a-select-option value="">全部</a-select-option>
    <a-select-option :value="true">是</a-select-option>
    <a-select-option :value="false">否</a-select-option>
  </a-select>
</j-form-item>
```

将 `searchFormData.inquiryProduct` 初始化为 `''`，并增加列表列：

```js
{
  field: 'inquiryProduct',
  title: '询价商品',
  width: 100,
  formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
}
```

- [ ] **Step 3: 校验并提交**

Run: `pnpm --dir frontend run type:check`

Expected: 退出码为 0。

Run: `pnpm --dir frontend run lint`

Expected: 退出码为 0；若有既存无关失败，只记录并不修改无关文件。

```bash
git add frontend/src/api/base-data/product/info/model frontend/src/views/base-data/product/info
git commit -m "feat: manage product inquiry flag in frontend"
```

### Task 5: 全量验证

**Files:**
- Modify: 仅在验证发现本功能缺陷时修改对应文件。

- [ ] **Step 1: 执行后端验证**

Run: `mvn -pl xingyun-basedata test`

Expected: `BUILD SUCCESS`。

Run: `mvn clean compile -DskipTests`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 执行前端验证**

Run: `pnpm --dir frontend run type:check && pnpm --dir frontend run lint`

Expected: 两个命令均退出码 0。

- [ ] **Step 3: 检查提交范围**

Run: `git diff --check && git status --short`

Expected: 无空白错误；不暂存用户已有的 `.codex/` 未跟踪目录。

- [ ] **Step 4: 提交验证修复（如有）**

```bash
git add xingyun-api/src/main/resources/db/migration/tenant/V2.3-product-inquiry-product.sql xingyun-basedata frontend/src/api/base-data/product/info frontend/src/views/base-data/product/info
git commit -m "test: verify product inquiry feature"
```

仅当验证修复产生本功能的未提交改动时执行。
