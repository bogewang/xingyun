<template>
  <Preview
    ref="previewRef"
    :print-template="templateInstance"
    :print-data="currentPrintData"
    :show-title="state.enableTemplateSwitch"
    :show-pdf="false"
    :show-print2="true"
    :dialog-title="dialogTitle"
    :width="PREVIEW_WIDTH"
    v-model:modalShow="previewVisible"
    default-lang="cn"
    :printerList="printers"
    :selectedPrinter="printer"
    :showImg="false"
    @close="closePrintDialog"
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
        <div class="print-count-switcher">
          <span class="print-count-switcher__label">打印份数</span>
          <a-input-number v-model:value="printCount" :min="1" :precision="0" />
        </div>
      </div>
      <span v-else>{{ title }}</span>
      <a-button @click="onToPdf">导出 PDF</a-button>
    </template>
  </Preview>
</template>

<script lang="ts" setup>
  import { computed, onBeforeUnmount, ref, watch } from 'vue';
  import {
    Preview,
    createTemplate,
    connect,
    isClientConnected,
    refreshPrinterList,
  } from 'vg-print';
  import 'vg-print/style.css';
  import * as api from '@/api/base-data/print-template';
  import { createError } from '@/hooks/web/msg';
  import {
    buildPrintPayload,
    normalizeTemplate,
    resetPageNumberForEachPrintData,
    shouldResetPageNumberForPrintData,
    type PrintTemplateJson,
  } from '@/components/PrintDesigner/src/printUtils';
  import { acquirePrintDialogOwner, closePrintDialog, usePrintDialogState } from './printDialog';

  const PREVIEW_WIDTH = 'min(90vw, 1080px)';

  type PrinterOption = {
    label: string;
    name: string;
  };

  type PrinterSource = Partial<PrinterOption>;

  const previewVisible = ref(false);
  const state = usePrintDialogState();
  const templateOptions = computed(() => state.templateList || []);
  const dialogTitle = computed(() => state.title || '打印预览');

  const previewOwnerId = `print-dialog-${Math.random().toString(36).slice(2)}`;
  const printers = ref<PrinterOption[]>([]);
  const printer = ref('');
  const selectedTemplateId = ref('');
  const currentTemplateJson = ref<PrintTemplateJson>(normalizeTemplate(state.templateJson));
  const templateLoading = ref(false);
  const templateCache = new Map<string, PrintTemplateJson>();
  const printCount = ref(1);
  const previewRef = ref<any>(null);

  /** 打印客户端连接状态轮询间隔（毫秒）。 */
  const CLIENT_POLL_INTERVAL = 2000;
  /** 最近一次观察到的打印客户端连接状态，用于检测状态变化。 */
  const clientConnected = ref(false);
  let clientPollTimer: number | undefined;

  // show(template, data, width?)
  // 参数说明：
  // 1) template: createTemplate(...) 返回的模板实例（或 hiprint.PrintTemplate）
  // 2) data: 打印数据（对象或数组）
  // 3) width: 弹窗宽度；支持百分比字符串（如 '80%'）或数字（如 980，单位 px）

  const templateInstance = computed(() => {
    const template = normalizeTemplate(currentTemplateJson.value);
    return createTemplate(
      shouldResetPageNumberForPrintData(state.resetPageNumberPerData, printCount.value)
        ? resetPageNumberForEachPrintData(template)
        : template,
    );
  });
  const currentPrintData = computed(() => buildPrintPayload(state.printData, printCount.value));

  /**
   * 缓存已加载的模板配置，预览弹窗内切换模板时避免重复请求。
   */
  function cacheTemplate(templateId: string, templateJson: unknown) {
    if (!templateId) {
      return;
    }

    templateCache.set(templateId, normalizeTemplate(templateJson));
  }

  /**
   * 重置当前预览弹窗的模板状态。
   *
   * 每次打开或刷新预览时，以入口传入的模板作为初始模板，
   * 并预先写入缓存，保证下拉选中的模板与预览内容一致。
   */
  function resetTemplateState() {
    templateCache.clear();
    printCount.value = 1;
    currentTemplateJson.value = normalizeTemplate(state.templateJson);
    cacheTemplate(state.templateId, state.templateJson);
    selectedTemplateId.value = state.templateId || state.templateList?.[0]?.id || '';
  }

  /**
   * 等待打印客户端建立连接，最长等待 8 秒。
   */
  async function waitForPrinterClient() {
    if (isClientConnected()) {
      return true;
    }

    return new Promise<boolean>((resolve) => {
      let completed = false;
      let timeoutId: number | undefined;
      const finish = (connected: boolean) => {
        if (completed) {
          return;
        }

        completed = true;
        if (timeoutId !== undefined) {
          window.clearTimeout(timeoutId);
        }
        resolve(connected);
      };

      timeoutId = window.setTimeout(() => finish(false), 8000);
      try {
        connect((connected: boolean) => finish(connected));
      } catch {
        finish(false);
      }
    });
  }

  /**
   * 加载打印机列表并设置默认打印机。
   */
  async function loadPrinters() {
    try {
      const connected = await waitForPrinterClient();
      if (!connected) {
        printers.value = [];
        printer.value = '';
        return;
      }

      const printerList = await refreshPrinterList();
      const normalizedPrinterList = Array.isArray(printerList)
        ? (printerList as PrinterSource[])
        : [];

      printers.value = normalizedPrinterList.map((item) => ({
        label: item.label || item.name || '',
        name: item.name || '',
      }));

      if (!printer.value && normalizedPrinterList.length > 0) {
        printer.value = normalizedPrinterList[0].name || '';
      }
    } catch {
      console.log('Failed to load printers');
      printers.value = [];
    }
  }

  /**
   * 打印客户端连接状态变化时，保持弹窗打开并刷新「直接打印」按钮状态。
   *
   * vg-print 的 Preview 只在弹窗打开时检查一次客户端连接；
   * 客户端在弹窗打开后才连接/断开时，按钮的禁用状态不会自动更新。
   * 这里调用其暴露的 show() 方法（弹窗保持打开不关闭），
   * 触发内部重新评估连接状态并刷新打印机列表。
   */
  function handleClientStateChange(connected: boolean) {
    clientConnected.value = connected;
    if (!state.open || !previewVisible.value || !previewRef.value) {
      return;
    }

    previewRef.value.show(templateInstance.value, currentPrintData.value);
    if (connected) {
      void loadPrinters();
    }
  }

  /**
   * 开始轮询打印客户端连接状态。
   *
   * 弹窗打开时 vg-print 会自行检查一次连接状态，这里先记录基线；
   * 之后每次轮询发现连接状态发生变化时，再刷新按钮状态。
   */
  function startClientPolling() {
    stopClientPolling();
    clientConnected.value = isClientConnected();
    clientPollTimer = window.setInterval(() => {
      const connected = isClientConnected();
      if (connected !== clientConnected.value) {
        handleClientStateChange(connected);
      }
    }, CLIENT_POLL_INTERVAL);
  }

  /**
   * 停止轮询打印客户端连接状态。
   */
  function stopClientPolling() {
    if (clientPollTimer !== undefined) {
      window.clearInterval(clientPollTimer);
      clientPollTimer = undefined;
    }
  }

  function onToPdf() {
    templateInstance.value.toPdf(currentPrintData.value, getPdfFilename(), {
      scale: 3,
      imageQuality: 0.92,
      pdfCompress: true,
      imageCompression: 'FAST',
    });
  }

  /**
   * vg-print 的 toPdf 第二个参数用于指定导出文件名。
   * 打印数据会被标准化为数组，因此取第一张单据的 `description` 作为文件名，
   * 移除操作系统不允许的文件名字符。
   */
  function getPdfFilename() {
    const printData = currentPrintData.value[0] as Record<string, unknown> | undefined;
    const filename = String(printData?.description || printData?.orderDate)
      .trim()
      .replace(/[\\/:*?"<>|]/g, '_');

    return filename || '打印预览';
  }

  /**
   * 根据当前模板与打印数据刷新预览内容。
   */
  function showPreview() {
    if (!state.open || !acquirePrintDialogOwner(previewOwnerId, state.frameKey)) {
      return;
    }

    previewVisible.value = true;
    void loadPrinters();
  }

  /**
   * 按模板 ID 加载模板配置。
   *
   * 优先使用弹窗内缓存；缓存未命中时请求后端设置接口，
   * 成功后更新当前模板 JSON，供 `showPreview` 重新渲染。
   */
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

      const templateJson = normalizeTemplate(setting.templateJson);
      templateCache.set(templateId, templateJson);
      currentTemplateJson.value = templateJson;
      return true;
    } catch {
      createError('加载打印模板失败！');
      return false;
    } finally {
      templateLoading.value = false;
    }
  }

  /**
   * 处理预览弹窗内的模板切换。
   *
   * 模板加载成功后立即刷新预览，保持下拉选择和预览画面同步。
   */
  async function handleTemplateChange(templateId: string) {
    const loaded = await loadTemplateById(templateId);
    if (loaded) {
      await showPreview();
    }
  }

  /**
   * 打印份数变更后，重新生成预览数据，使预览、PDF 导出与实际打印保持一致。
   */
  async function handlePrintCountChange() {
    await showPreview();
  }

  watch(
    () => [state.open, state.frameKey] as const,
    async ([open, frameKey], previousValue) => {
      if (!open) {
        stopClientPolling();
        previewVisible.value = false;
        return;
      }

      const [prevOpen, prevFrameKey] = previousValue || [false, state.frameKey];
      const openedNow = !prevOpen;
      const refreshedWhileOpen = prevOpen && frameKey !== prevFrameKey;

      if (openedNow || refreshedWhileOpen) {
        resetTemplateState();
      }

      if (openedNow || refreshedWhileOpen) {
        await showPreview();
        startClientPolling();
      }
    },
    { immediate: true },
  );

  watch(printCount, handlePrintCountChange);

  onBeforeUnmount(stopClientPolling);
</script>

<style scoped>
  .print-dialog-title {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
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

  .print-count-switcher {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    white-space: nowrap;
  }

  .print-count-switcher__label {
    color: #6b7280;
    font-size: 13px;
    font-weight: 500;
  }

  .print-count-switcher :deep(.ant-input-number) {
    width: 72px;
  }

  :deep(.preview-header .print-count) {
    display: none;
  }

  :deep(.preview-container) {
    padding-left: 12px;
    padding-right: 12px;
  }
</style>
