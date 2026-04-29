import { reactive, readonly } from 'vue';

interface PrintDialogPayload {
  html: string;
  title?: string;
  bizType?: string;
}

const state = reactive({
  open: false,
  title: '订单打印预览',
  html: '',
  bizType: '',
  frameKey: 0,
});

export function openPrintDialog(payload: PrintDialogPayload) {
  state.title = payload.title || '订单打印预览';
  state.html = payload.html || '';
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
