import { openPrintDialog } from '/@/components/PrintDialog';
import { createTemplate, printBrowser as printTemplateInBrowser } from 'vg-print';
import { createError } from '/@/hooks/web/msg';
import {
  normalizePrintData,
  normalizeTemplate,
  resetPageNumberForEachPrintData,
  shouldResetPageNumberForPrintData,
} from './printUtils';
import type { PrintDialogPayload } from '/@/components/PrintDialog';

export interface PrintRuntimePreviewOptions
  extends Partial<
    Pick<
      PrintDialogPayload,
      | 'title'
      | 'bizType'
      | 'templateId'
      | 'templateList'
      | 'enableTemplateSwitch'
      | 'resetPageNumberPerData'
    >
  > {}

/**
 * 浏览器打印选项。
 */
export interface PrintRuntimeBrowserPrintOptions {
  /** 是否让每条打印数据从第一页开始计页。 */
  resetPageNumberPerData?: boolean;
}

export interface PrintRuntimeApi {
  preview: (templateJson: unknown, data: unknown, options?: PrintRuntimePreviewOptions) => void;
  browserPrint: (
    templateJson: unknown,
    data: unknown,
    options?: PrintRuntimeBrowserPrintOptions,
  ) => Promise<boolean>;
}

const BROWSER_PRINT_TIMEOUT = 120000;
let browserPrintInProgress = false;

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
    resetPageNumberPerData: options.resetPageNumberPerData === true,
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
function browserPrint(
  templateJson: unknown,
  data: unknown,
  options: PrintRuntimeBrowserPrintOptions = {},
): Promise<boolean> {
  if (browserPrintInProgress) {
    createError('浏览器打印任务正在进行，请先完成当前打印。');
    return Promise.resolve(false);
  }

  const printData = normalizePrintData(data);
  const normalizedTemplate = normalizeTemplate(templateJson);
  const template = createTemplate(
    shouldResetPageNumberForPrintData(options.resetPageNumberPerData === true, printData.length)
      ? resetPageNumberForEachPrintData(normalizedTemplate)
      : normalizedTemplate,
  );

  browserPrintInProgress = true;
  return new Promise((resolve) => {
    let settled = false;
    const finish = (success: boolean) => {
      if (settled) {
        return;
      }

      settled = true;
      window.clearTimeout(timeoutId);
      browserPrintInProgress = false;
      resolve(success);
    };
    const timeoutId = window.setTimeout(() => {
      createError('浏览器打印等待超时，请关闭系统打印窗口后重试。');
      finish(false);
    }, BROWSER_PRINT_TIMEOUT);

    try {
      const printTask = printTemplateInBrowser(
        template,
        printData,
        {},
        {
          callback: () => finish(true),
        },
      );
      Promise.resolve(printTask).catch(() => {
        createError('浏览器打印启动失败，请重试。');
        finish(false);
      });
    } catch {
      createError('浏览器打印启动失败，请重试。');
      finish(false);
    }
  });
}

const printRuntime: PrintRuntimeApi = {
  preview,
  browserPrint,
};

export default printRuntime;
