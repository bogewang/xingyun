export type PrintTemplateJson = Record<string, unknown>;

export function createEmptyTemplate(): PrintTemplateJson {
  return { panels: [] };
}

export function normalizeTemplate(templateJson: unknown): PrintTemplateJson {
  const isObjectValue = !!templateJson && typeof templateJson === 'object';
  const hasPanels = isObjectValue && Array.isArray((templateJson as { panels?: unknown[] }).panels);

  return hasPanels ? (templateJson as PrintTemplateJson) : createEmptyTemplate();
}

export function normalizePrintData(data: unknown): unknown[] {
  if (Array.isArray(data)) {
    return data;
  }

  if (data && typeof data === 'object') {
    return [data];
  }

  return [];
}

export function buildPrintPayload(data: unknown, copies: number): unknown[] {
  const normalizedData = normalizePrintData(data);
  const safeCopies = Math.max(1, copies || 1);

  if (safeCopies === 1) {
    return normalizedData;
  }

  return Array.from({ length: safeCopies }).flatMap(() => normalizedData);
}
