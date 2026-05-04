<template>
  <Preview
    ref="previewRef"
    :show-title="state.enableTemplateSwitch"
    dialog-title="打印预览"
    v-model:modalShow="modalShow"
    default-lang="cn"
    :printerList="printers"
    :selectedPrinter="printer"
    :showImg="false"
    @update:selected-printer="printer = $event"
  >
    <template #title="{ title }">
      <div v-if="state.enableTemplateSwitch" class="print-dialog-title">
        <span class="print-dialog-title__text">{{ title }}</span>
        <span class="print-dialog-title__divider"></span>
        <div class="print-template-switcher">
          <span class="print-template-switcher__label">打印模板</span>
          <a-select
            v-model:value="selectedTemplateId"
            class="print-template-switcher__select"
            :loading="templateLoading"
            :disabled="templateOptions.length <= 1"
            placeholder="请选择打印模板"
            @change="handleTemplateChange"
          >
            <a-select-option v-for="item in templateOptions" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <span v-else>{{ title }}</span>
    </template>
  </Preview>
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref, watch } from 'vue';
  import { Preview, createTemplate, connect, refreshPrinterList } from 'vg-print';
  import 'vg-print/style.css';
  import * as api from '@/api/base-data/print-template';
  import { createError } from '@/hooks/web/msg';
  import { buildPrintPayload, normalizeTemplate } from '@/components/PrintDesigner/src/printUtils';
  import { closePrintDialog, usePrintDialogState } from './printDialog';

  type PrinterOption = {
    label: string;
    name: string;
  };
  const previewRef = ref<any>(null);
  const state = usePrintDialogState();
  const templateOptions = computed(() => state.templateList || []);
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
  const selectedTemplateId = ref('');
  const currentTemplateJson = ref(state.templateJson);
  const templateLoading = ref(false);
  const templateCache = new Map<string, unknown>();
  // 打印份数
  // const printCount = ref(1);

  // show(template, data, width?)
  // 参数说明：
  // 1) template: createTemplate(...) 返回的模板实例（或 hiprint.PrintTemplate）
  // 2) data: 打印数据（对象或数组）
  // 3) width: 弹窗宽度；支持百分比字符串（如 '80%'）或数字（如 980，单位 px）

  const templateInstance = computed<any>(() =>
    createTemplate(normalizeTemplate(currentTemplateJson.value)),
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
    previewRef.value?.show(templateInstance.value, currentPrintData.value, {
      width: '80%',
      showTitle: state.enableTemplateSwitch,
    });
  }

  async function loadTemplateById(templateId: string) {
    if (!templateId) {
      createError('请选择打印模板！');
      return false;
    }

    if (templateCache.has(templateId)) {
      currentTemplateJson.value = templateCache.get(templateId) || {};
      return true;
    }

    templateLoading.value = true;
    try {
      const setting = await api.getSetting(templateId);
      if (!setting?.templateJson) {
        createError('未找到打印模板配置！');
        return false;
      }

      templateCache.set(templateId, setting.templateJson);
      currentTemplateJson.value = setting.templateJson;
      return true;
    } finally {
      templateLoading.value = false;
    }
  }

  async function handleTemplateChange(templateId: string) {
    const loaded = await loadTemplateById(templateId);
    if (loaded) {
      await showPreview();
    }
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
        templateCache.clear();
        currentTemplateJson.value = state.templateJson;
        if (state.templateId) {
          templateCache.set(state.templateId, state.templateJson);
        }
        selectedTemplateId.value = state.templateId || state.templateList?.[0]?.id || '';
      }

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

<style scoped>
  .print-dialog-title {
    display: inline-flex;
    align-items: center;
    gap: 14px;
    max-width: 100%;
    padding-right: 16px;
  }

  .print-dialog-title__text {
    color: #1f2937;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.01em;
    white-space: nowrap;
  }

  .print-dialog-title__divider {
    width: 1px;
    height: 18px;
    background: #d9e2f2;
    border-radius: 999px;
  }

  .print-template-switcher {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  .print-template-switcher__label {
    color: #6b7280;
    font-size: 13px;
    font-weight: 500;
    white-space: nowrap;
  }

  .print-template-switcher__select {
    width: 240px;
    min-width: 240px;
  }
</style>
