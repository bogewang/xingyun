import { openPrintDialog } from '/@/components/PrintDialog';
import { createTemplate, printBrowser as printTemplateInBrowser } from 'vg-print';
import { normalizePrintData, normalizeTemplate } from './printUtils';
import type { PrintDialogPayload } from '/@/components/PrintDialog';

export interface PrintRuntimePreviewOptions
  extends Partial<
    Pick<
      PrintDialogPayload,
      'title' | 'bizType' | 'templateId' | 'templateList' | 'enableTemplateSwitch'
    >
  > {}

export interface PrintRuntimeApi {
  preview: (templateJson: unknown, data: unknown, options?: PrintRuntimePreviewOptions) => void;
  browserPrint: (templateJson: unknown, data: unknown) => void;
}

/**
 * 运行时打印预览入口。
 *
 * 将外部传入的模板和业务数据规范化后写入全局预览弹窗状态，
 * 由 `PrintDialog` 负责实际创建 vg-print 模板实例并展示。
 */
function preview(templateJson: unknown, data: unknown, options: PrintRuntimePreviewOptions = {}) {
  openPrintDialog({
    title: options.title,
    bizType: options.bizType,
    templateId: options.templateId,
    templateList: options.templateList || [],
    enableTemplateSwitch: options.enableTemplateSwitch === true,
    templateJson: normalizeTemplate(templateJson),
    printData: normalizePrintData(data),
  });
}

/**
 * 使用浏览器打印当前模板和业务数据。
 *
 * 与预览弹窗共用相同的数据和模板标准化逻辑，直接调起系统打印对话框，
 * 不依赖本地打印客户端。
 */
function browserPrint(templateJson: unknown, data: unknown) {
  const template = createTemplate(normalizeTemplate(templateJson));
  printTemplateInBrowser(template, normalizePrintData(data));
}

const printRuntime: PrintRuntimeApi = {
  preview,
  browserPrint,
};

export default printRuntime;
