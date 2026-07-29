export type PrintDemoData = unknown[] | Record<string, unknown>;

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
