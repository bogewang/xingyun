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
 * 创建按单据重置页码的模板副本。
 *
 * vg-print 会根据面板的 `paperNumberContinue` 配置决定多条数据是否连续计页。
 * 批量单据打印应让每张单据独立计页，因此仅在副本中关闭该配置，避免影响保存的模板。
 */
export function resetPageNumberForEachPrintData(templateJson: unknown): PrintTemplateJson {
  const template = normalizeTemplate(templateJson);
  const panels = template.panels as unknown[];

  return {
    ...template,
    panels: panels.map((panel) => {
      if (!panel || typeof panel !== 'object') {
        return panel;
      }

      return {
        ...(panel as Record<string, unknown>),
        paperNumberContinue: false,
      };
    }),
  };
}

/**
 * 判断打印数据是否应按每份单据独立计页。
 *
 * 批量单据或同一单据打印多份时，均不能把各份的页数累计到同一个总页码中。
 */
export function shouldResetPageNumberForPrintData(
  resetPageNumberPerData: boolean,
  copies: number,
): boolean {
  return resetPageNumberPerData || copies > 1;
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
