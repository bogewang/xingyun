import { openPrintDialog } from '/@/components/PrintDialog';
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

const printRuntime: PrintRuntimeApi = {
  preview,
};

export default printRuntime;
