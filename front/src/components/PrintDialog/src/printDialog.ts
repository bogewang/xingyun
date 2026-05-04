import { reactive, readonly } from 'vue';
import type { PrintTemplateJson } from '@/components/PrintDesigner/src/printUtils';

interface PrintTemplateOption {
  id: string;
  name: string;
  bizType?: string;
}

interface PrintDialogPayload {
  templateJson: PrintTemplateJson;
  printData: unknown;
  title?: string;
  bizType?: string;
  templateId?: string;
  templateList?: PrintTemplateOption[];
  enableTemplateSwitch?: boolean;
}

interface PrintDialogState {
  open: boolean;
  templateJson: PrintTemplateJson;
  printData: unknown;
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
  bizType: '',
  templateId: '',
  templateList: [],
  enableTemplateSwitch: false,
  frameKey: 0,
});

export function openPrintDialog(payload: PrintDialogPayload) {
  state.templateJson = payload.templateJson || {};
  state.printData = payload.printData ?? [];
  state.bizType = payload.bizType || '';
  state.templateId = payload.templateId || '';
  state.templateList = payload.templateList || [];
  state.enableTemplateSwitch = payload.enableTemplateSwitch === true;
  state.frameKey += 1;
  state.open = true;
}

export function closePrintDialog() {
  state.open = false;
}

export function usePrintDialogState() {
  return readonly(state);
}
