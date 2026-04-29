import { createTemplate, exportImage, exportPdf, printBrowser } from 'vg-print';
import { openPrintDialog } from '/@/components/PrintDialog';
import {
  normalizePrintData,
  normalizeTemplate,
} from './printUtils';

interface PreviewOptions {
  title?: string;
  bizType?: string;
}

function buildTemplate(templateJson: unknown) {
  return createTemplate(normalizeTemplate(templateJson));
}

function preparePrintContext(templateJson: unknown, data: unknown) {
  return {
    template: buildTemplate(templateJson),
    printData: normalizePrintData(data),
  };
}

function preview(templateJson: unknown, data: unknown, options: PreviewOptions = {}) {
  openPrintDialog({
    title: options.title,
    bizType: options.bizType,
    templateJson: normalizeTemplate(templateJson),
    printData: normalizePrintData(data),
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

function toImage(templateJson: unknown, data: unknown, options: Record<string, unknown> = {}) {
  const { template, printData } = preparePrintContext(templateJson, data);
  return exportImage(template, printData, options);
}

export default {
  preview,
  print,
  toImage,
  toPdf,
};
