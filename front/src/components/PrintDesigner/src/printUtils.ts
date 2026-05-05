export type PrintTemplateJson = Record<string, unknown>;

/**
 * 创建 vg-print 可识别的最小空模板。
 */
export function createEmptyTemplate(): PrintTemplateJson {
  return { panels: [] };
}

/**
 * 校验并规范化模板 JSON。
 *
 * 只有包含 `panels` 数组的对象会作为有效模板返回，
 * 其他输入统一回退为空模板，避免设计器或预览组件初始化失败。
 */
export function normalizeTemplate(templateJson: unknown): PrintTemplateJson {
  const isObjectValue = !!templateJson && typeof templateJson === 'object';
  const hasPanels = isObjectValue && Array.isArray((templateJson as { panels?: unknown[] }).panels);

  return hasPanels ? (templateJson as PrintTemplateJson) : createEmptyTemplate();
}

/**
 * 将单条或多条业务数据统一整理为 vg-print 预览需要的数组格式。
 */
export function normalizePrintData(data: unknown): unknown[] {
  if (Array.isArray(data)) {
    return data;
  }

  if (data && typeof data === 'object') {
    return [data];
  }

  return [];
}

/**
 * 根据打印份数生成预览数据。
 *
 * vg-print 按数组逐条渲染，这里通过复制数据数组实现多份打印。
 */
export function buildPrintPayload(data: unknown, copies: number): unknown[] {
  const normalizedData = normalizePrintData(data);
  const safeCopies = Math.max(1, copies || 1);

  if (safeCopies === 1) {
    return normalizedData;
  }

  return Array.from({ length: safeCopies }).flatMap(() => normalizedData);
}
