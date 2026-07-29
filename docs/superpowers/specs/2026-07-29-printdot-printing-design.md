# PrintDot 打印组件替换设计

## 目标

将前端现有基于 `vg-print` 的打印设计与业务预览能力替换为 `vue-print-designer@1.7.33`（PrintDot），同时保持业务页面现有的打印调用入口稳定。

## 已确认的范围

- 已保存的 `vg-print` 模板由人工迁移为 PrintDot 模板；本次不提供自动转换。
- `PrintDialog` 的预览、打印和模板切换能力整体切换到 PrintDot。
- 默认使用浏览器打印；本地客户端与云打印只保留后续接入所需的扩展边界。
- 模板列表、模板加载、保存和字段说明继续使用项目现有后端接口。
- 设计器保留“模板字段说明”入口；移除 `vg-print` 专用的本地模板导入缓存。
- 新增依赖必须精确固定为 `vue-print-designer@1.7.33`。
- 验收仅执行自动化检查：单元测试、类型检查和生产构建；不以人工操作或真实打印机验证为阻塞条件。

## 方案选择

采用“PrintDot Web Component 适配层”方案：保留 `PrintDialog`、`PrintDesigner` 与 `$printRuntimeApi` 的对外接口，内部统一由 PrintDot 驱动。

未采用 PrintDot 原生远程 CRUD 方案，因为它要求改造现有后端模板接口；未采用双引擎过渡方案，因为它会保留额外维护成本且不符合整体替换目标。

## 架构

在 `src/components/PrintDialog` 和 `src/components/PrintDesigner` 中建立面向 PrintDot 的 Vue 封装与类型适配层。

- `PrintDesigner` 负责挂载 `<print-designer>`，加载项目接口返回的模板与示例数据，并在保存时将 PrintDot 模板 JSON 和示例数据回写到现有接口。
- `PrintDialog` 负责承接 `$printRuntimeApi.preview()` 的调用，展示新的 PrintDot 打印容器，保留项目模板下拉选择；选中模板后通过现有接口加载并注入 PrintDot。
- 业务页面继续调用 `vgPrintPreview` 与 `$printRuntimeApi.preview()`；名称可以暂时保留，内部行为改为 PrintDot，以避免修改所有采购、销售和零售页面。
- 适配层为浏览器打印组装模板、变量和打印参数，并提供未来设置本地/云打印模式的单一配置点。

## 数据与兼容性

- PrintDot 模板和测试数据继续存入现有 `templateJson`、`demoData` 字段。
- 仅接受人工迁移后的 PrintDot 模板。检测到 `vg-print` 的 `panels` 模板或其他不符合 PrintDot 结构的数据时，显示“请先迁移模板”的明确错误，并禁止打印或编辑加载。
- 不自动转换、不静默清空、不修改旧模板数据。
- 保留模板切换、后端模板加载与字段说明接口；删除 `vg-print` 的 `panels` 规范化、授权注册、PDF 导出、本地打印机连接以及浏览器本地模板缓存。

## 错误处理

- 模板不存在、模板加载失败、模板格式不支持或 PrintDot 初始化失败时，显示可操作的错误提示。
- 出现上述错误时不执行浏览器打印，避免输出空白或错误单据。
- 模板切换失败时保留当前已成功加载的模板，避免将预览状态置空。

## 测试与验证

- 为 PrintDot 模板识别、旧模板拦截、业务数据规范化和浏览器打印参数组装添加单元测试。
- 执行 `pnpm run test:unit`、`pnpm run type:check` 和 `pnpm run build`。
- 测试不要求真实打印机、浏览器交互或人工验收。

## 非目标

- 不改造后端模板 API。
- 不提供旧 `vg-print` 模板自动转换。
- 不接入 PrintDot 本地客户端打印或云打印。
