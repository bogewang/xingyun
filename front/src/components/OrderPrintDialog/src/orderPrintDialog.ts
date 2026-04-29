import { reactive, readonly } from 'vue';

interface OrderPrintDialogPayload {
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

export function openOrderPrintDialog(payload: OrderPrintDialogPayload) {
  state.title = payload.title || '订单打印预览';
  state.html = payload.html || '';
  state.bizType = payload.bizType || '';
  state.frameKey += 1;
  state.open = true;
}

export function closeOrderPrintDialog() {
  state.open = false;
}

export function useOrderPrintDialogState() {
  return readonly(state);
}
