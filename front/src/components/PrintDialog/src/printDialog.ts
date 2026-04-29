import { reactive, readonly } from 'vue';
import type { PrintTemplateJson } from '@/components/PrintDesigner/src/printUtils';

interface PrintDialogPayload {
  templateJson: PrintTemplateJson;
  printData: unknown;
  title?: string;
  bizType?: string;
}

interface PrintDialogState {
  open: boolean;
  title: string;
  templateJson: PrintTemplateJson;
  printData: unknown;
  bizType: string;
  frameKey: number;
}

const DEFAULT_TITLE = '打印预览';

const state = reactive<PrintDialogState>({
  open: false,
  title: DEFAULT_TITLE,
  templateJson: {},
  printData: [],
  bizType: '',
  frameKey: 0,
});

export function openPrintDialog(payload: PrintDialogPayload) {
  state.title = payload.title || DEFAULT_TITLE;
  state.templateJson = payload.templateJson || {};
  state.printData = payload.printData ?? [];
  state.bizType = payload.bizType || '';
  state.frameKey += 1;
  state.open = true;
}

export function closePrintDialog() {
  state.open = false;
}

export function usePrintDialogState() {
  return readonly(state);
}
