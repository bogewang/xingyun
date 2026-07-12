export type PrintTemplateJson = Record<string, unknown>;
export type PrintDemoData = unknown[] | Record<string, unknown>;

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
 * 将后端或编辑器中的示例数据规范化为数组或对象。
 */
export function normalizeDemoData(data: unknown): PrintDemoData {
  if (typeof data === 'string') {
    const trimmedData = data.trim();

    if (!trimmedData) {
      return {};
    }

    try {
      return normalizeDemoData(JSON.parse(trimmedData));
    } catch {
      return {};
    }
  }

  if (Array.isArray(data)) {
    return data;
  }

  if (data && typeof data === 'object') {
    return data as Record<string, unknown>;
  }

  return {};
}

/**
 * 将示例数据格式化为编辑器可读的 JSON 字符串。
 */
export function stringifyDemoData(data: unknown): string {
  return JSON.stringify(normalizeDemoData(data), null, 2);
}

/**
 * 将单条或多条业务数据统一整理为 vg-print 预览需要的数组格式。
 */
export function normalizePrintData(data: unknown): unknown[] {
  const normalizedData = normalizeDemoData(data);

  if (Array.isArray(normalizedData)) {
    return normalizedData;
  }

  if (normalizedData && typeof normalizedData === 'object') {
    return [normalizedData];
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
