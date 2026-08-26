import { reactive, readonly } from 'vue';
import type { PrintTemplateJson } from '@/components/PrintDesigner/src/printUtils';

export interface PrintTemplateOption {
  id: string;
  name: string;
  bizType?: string;
}

export interface PrintDialogPayload {
  templateJson: PrintTemplateJson;
  printData: unknown;
  resetPageNumberPerData?: boolean;
  title?: string;
  bizType?: string;
  templateId?: string;
  templateList?: PrintTemplateOption[];
  enableTemplateSwitch?: boolean;
}

export interface PrintDialogState {
  open: boolean;
  templateJson: PrintTemplateJson;
  printData: unknown;
  resetPageNumberPerData: boolean;
  title: string;
  bizType: string;
  templateId: string;
  templateList: PrintTemplateOption[];
  enableTemplateSwitch: boolean;
  frameKey: number;
}

const state = reactive<PrintDialogState>({
  open: false,
  templateJson: {},
  printData: [],
  resetPageNumberPerData: false,
  title: '',
  bizType: '',
  templateId: '',
  templateList: [],
  enableTemplateSwitch: false,
  frameKey: 0,
});

let previewOwnerFrameKey = -1;
let previewOwnerId = '';

/**
 * 为当前打印请求分配唯一的预览组件。
 *
 * 页面中的列表、详情等区域可能同时挂载多个打印预览组件；
 * 它们共用同一份打印状态时，只允许第一个领取当前请求的组件展示弹窗。
 */
export function acquirePrintDialogOwner(ownerId: string, frameKey: number) {
  if (previewOwnerFrameKey !== frameKey) {
    previewOwnerFrameKey = frameKey;
    previewOwnerId = ownerId;
  }

  return previewOwnerId === ownerId;
}

/**
 * 打开全局打印预览弹窗。
 *
 * 写入模板、打印数据和模板切换选项；每次打开都会递增 `frameKey`，
 * 让弹窗组件能识别同一个弹窗内的新一轮预览请求。
 */
export function openPrintDialog(payload: PrintDialogPayload) {
  state.templateJson = payload.templateJson || {};
  state.printData = payload.printData ?? [];
  state.resetPageNumberPerData = payload.resetPageNumberPerData === true;
  state.title = payload.title || '';
  state.bizType = payload.bizType || '';
  state.templateId = payload.templateId || '';
  state.templateList = payload.templateList || [];
  state.enableTemplateSwitch = payload.enableTemplateSwitch === true;
  state.frameKey += 1;
  state.open = true;
}

/**
 * 关闭全局打印预览弹窗。
 */
export function closePrintDialog() {
  state.open = false;
}

/**
 * 暴露只读弹窗状态，供预览组件消费。
 */
export function usePrintDialogState() {
  return readonly(state);
}
