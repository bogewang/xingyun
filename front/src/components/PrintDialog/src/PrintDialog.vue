<template>
  <Preview
    ref="previewRef"
    :show-title="false"
    dialog-title="打印预览"
    v-model:modalShow="modalShow"
    default-lang="cn"
    :printerList="printers"
    :selectedPrinter="printer"
    @update:selected-printer="printer = $event"
  />
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref, watch } from 'vue';
  import { Preview, createTemplate, connect, refreshPrinterList } from 'vg-print';
  import 'vg-print/style.css';
  import { buildPrintPayload, normalizeTemplate } from '@/components/PrintDesigner/src/printUtils';
  import { closePrintDialog, usePrintDialogState } from './printDialog';

  type PrinterOption = {
    label: string;
    name: string;
  };
  const previewRef = ref<any>(null);
  const state = usePrintDialogState();
  // 弹窗显示控制
  const modalShow = computed({
    get: () => state.open,
    set: (value: boolean) => {
      if (!value) {
        closePrintDialog();
      }
    },
  });
  const printers = ref<PrinterOption[]>([]);
  const printer = ref('');
  // 打印份数
  // const printCount = ref(1);

  // show(template, data, width?)
  // 参数说明：
  // 1) template: createTemplate(...) 返回的模板实例（或 hiprint.PrintTemplate）
  // 2) data: 打印数据（对象或数组）
  // 3) width: 弹窗宽度；支持百分比字符串（如 '80%'）或数字（如 980，单位 px）

  const templateInstance = computed<any>(() =>
    createTemplate(normalizeTemplate(state.templateJson)),
  );
  const currentPrintData = computed(() => buildPrintPayload(state.printData, 1));

  /**
   * 加载打印机列表并设置默认打印机
   */
  async function loadPrinters() {
    try {
      await connect();

      const printerList = await refreshPrinterList();
      printers.value = (printerList || []).map((item: { name?: string; label?: string }) => ({
        label: item.label || item.name || '',
        name: item.name || '',
      }));

      if (!printer.value && printerList.length > 0) {
        printer.value = printerList[0].name;
      }
    } catch {
      printers.value = [];
    }
  }

  /**
   * 显示打印预览
   */
  async function showPreview() {
    if (!state.open) {
      return;
    }

    await nextTick();
    previewRef.value?.show(templateInstance.value, currentPrintData.value, '80%');
  }

  watch(
    () => [state.open, state.frameKey] as const,
    async ([open, frameKey], [prevOpen, prevFrameKey]) => {
      if (!open) {
        return;
      }

      const openedNow = !prevOpen;
      const refreshedWhileOpen = prevOpen && frameKey !== prevFrameKey;

      if (openedNow || refreshedWhileOpen) {
        await showPreview();
      }

      if (openedNow) {
        await loadPrinters();
      }
    },
    { immediate: true },
  );
</script>
