import { createTemplate, exportImage, exportPdf, getHtml, printBrowser } from 'vg-print';
import { openOrderPrintDialog } from '@/components/OrderPrintDialog';

interface PreviewOptions {
  title?: string;
  bizType?: string;
}

const PREVIEW_DOCUMENT_STYLE = 'html,body{margin:0;padding:0;background:#fff;}';

function createEmptyTemplate(): Record<string, unknown> {
  return { panels: [] };
}

function normalizeTemplate(templateJson: unknown): Record<string, unknown> {
  const isObjectValue = !!templateJson && typeof templateJson === 'object';
  const hasPanels = isObjectValue && Array.isArray((templateJson as { panels?: unknown[] }).panels);
  return hasPanels ? (templateJson as Record<string, unknown>) : createEmptyTemplate();
}

function normalizeData(data: unknown): unknown[] {
  if (Array.isArray(data)) {
    return data;
  }

  if (data && typeof data === 'object') {
    return [data];
  }

  return [];
}

function buildTemplate(templateJson: unknown) {
  return createTemplate(normalizeTemplate(templateJson));
}

function preparePrintContext(templateJson: unknown, data: unknown) {
  return {
    template: buildTemplate(templateJson),
    printData: normalizeData(data),
  };
}

function createHtmlDocument(bodyHtml: string, style = PREVIEW_DOCUMENT_STYLE): string {
  return [
    '<!DOCTYPE html>',
    '<html lang="zh-CN">',
    '<head>',
    '<meta charset="UTF-8" />',
    '<meta name="viewport" content="width=device-width, initial-scale=1.0" />',
    `<style>${style}</style>`,
    '</head>',
    `<body>${bodyHtml}</body>`,
    '</html>',
  ].join('');
}

function wrapPreviewHtml(html: string): string {
  if (/<html[\s>]/i.test(html)) {
    return html;
  }

  return createHtmlDocument(html);
}

function preview(templateJson: unknown, data: unknown, options: PreviewOptions = {}) {
  const { template, printData } = preparePrintContext(templateJson, data);
  const html = getHtml(template, printData);

  openOrderPrintDialog({
    title: options.title,
    bizType: options.bizType,
    html: wrapPreviewHtml(typeof html === 'string' ? html : ''),
  });
}

function print(templateJson: unknown, data: unknown) {
  const { template, printData } = preparePrintContext(templateJson, data);
  return printBrowser(template, printData);
}

function toPdf(
  templateJson: unknown,
  data: unknown,
  filename = 'print-document',
  options: Record<string, unknown> = {},
) {
  const { template, printData } = preparePrintContext(templateJson, data);
  return exportPdf(template, printData, filename, options);
}

function toImage(
  templateJson: unknown,
  data: unknown,
  options: Record<string, unknown> = {},
) {
  const { template, printData } = preparePrintContext(templateJson, data);
  return exportImage(template, printData, options);
}

export default {
  preview,
  print,
  toImage,
  toPdf,
};
