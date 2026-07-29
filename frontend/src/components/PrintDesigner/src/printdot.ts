/** PrintDot 模板。 */
export interface PrintDotTemplate extends Record<string, unknown> {
  pages: unknown[];
  canvasSize: unknown;
}

/** PrintDot 打印请求。 */
export interface PrintDotPrintRequest {
  mode?: 'browser' | 'local' | 'remote';
  options?: Record<string, unknown>;
}

/** PrintDot Web Component 元素实例。 */
export interface PrintDesignerElement extends HTMLElement {
  /** 加载模板数据。 */
  loadTemplateData(template: PrintDotTemplate): boolean;

  /** 获取当前模板数据。 */
  getTemplateData(): PrintDotTemplate;

  /** 设置设计器测试数据。 */
  setTestData(data: Record<string, unknown>): Promise<void>;

  /** 获取设计器测试数据。 */
  getTestData(): Record<string, unknown>;

  /** 设置业务打印变量。 */
  setVariables(variables: Record<string, unknown>): Promise<void>;

  /** 按指定通道执行打印。 */
  print(request?: PrintDotPrintRequest): Promise<unknown>;
}

/**
 * 判断值是否为 PrintDot 顶层模板结构。
 */
export function isPrintDotTemplate(value: unknown): value is PrintDotTemplate {
  return (
    !!value &&
    typeof value === 'object' &&
    Array.isArray((value as PrintDotTemplate).pages) &&
    'canvasSize' in value
  );
}

/**
 * 将业务数据转换为 PrintDot 所需的变量对象；数组仅使用第一项。
 */
export function toPrintDotVariables(value: unknown): Record<string, unknown> {
  const variables = Array.isArray(value) ? value[0] : value;

  return variables && typeof variables === 'object' && !Array.isArray(variables)
    ? (variables as Record<string, unknown>)
    : {};
}
