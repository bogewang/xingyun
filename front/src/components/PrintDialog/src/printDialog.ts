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
  templateJson: PrintTemplateJson;
  printData: unknown;
  bizType: string;
  frameKey: number;
}

const state = reactive<PrintDialogState>({
  open: false,
  templateJson: {},
  printData: [],
  bizType: '',
  frameKey: 0,
});

export function openPrintDialog(payload: PrintDialogPayload) {
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
