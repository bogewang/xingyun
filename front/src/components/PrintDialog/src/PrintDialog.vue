<template>
  <a-modal
    :open="state.open"
    :title="null"
    :footer="null"
    :closable="false"
    :mask-closable="true"
    width="96vw"
    wrap-class-name="order-print-dialog-wrap"
    @cancel="handleClose"
  >
    <div class="order-print-dialog">
      <div class="order-print-dialog__toolbar">
        <div class="order-print-dialog__toolbar-left">
          <div class="order-print-dialog__title">{{ state.title }}</div>
          <div v-if="state.bizType" class="order-print-dialog__meta">{{ state.bizType }}</div>
        </div>
        <div class="order-print-dialog__toolbar-right">
          <a-input-number
            v-model:value="printCount"
            class="order-print-dialog__copies"
            :min="1"
            :precision="0"
            addon-after="x"
          />
          <a-select
            v-model:value="printerName"
            class="order-print-dialog__printer"
            :options="printerList"
            :loading="printerLoading"
            :allow-clear="true"
            placeholder="导出为 WPS PDF"
          />
          <a-space :size="8" wrap>
            <a-button type="primary" ghost @click="handlePreview">预览</a-button>
            <a-button type="primary" ghost @click="handleBrowserPrint">浏览器打印</a-button>
            <a-button type="primary" ghost @click="handleExportPdf">导出 PDF</a-button>
            <a-button type="primary" ghost @click="handleExportImage">导出图片</a-button>
            <a-button type="primary" ghost @click="handleClientGenerate">客户端生成</a-button>
            <a-button type="primary" @click="handleDirectPrint">直接打印</a-button>
            <a-button @click="handleClose">关闭</a-button>
          </a-space>
        </div>
      </div>
      <div class="order-print-dialog__body">
        <div ref="previewRef" class="order-print-dialog__preview"></div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref, watch } from 'vue';
  import {
    clientGenerate,
    connect,
    createTemplate,
    exportImage,
    exportPdf,
    getHtml,
    isClientConnected,
    printBrowser,
    refreshPrinterList,
  } from 'vg-print';
  import { createError, createSuccessTip, createWarning } from '@/hooks/web/msg';
  import {
    buildPrintPayload,
    normalizeTemplate,
    sanitizePrintFileName,
  } from '@/components/PrintDesigner/src/printUtils';
  import { closePrintDialog, usePrintDialogState } from './printDialog';

  type PrinterOption = {
    label: string;
    value: string;
  };

  const state = usePrintDialogState();
  const previewRef = ref<HTMLDivElement | null>(null);
  const printerList = ref<PrinterOption[]>([]);
  const printerLoading = ref(false);
  const printerName = ref<string>();
  const printCount = ref(1);

  const templateInstance = computed<any>(() =>
    createTemplate(normalizeTemplate(state.templateJson)),
  );
  const currentPrintData = computed(() => buildPrintPayload(state.printData, printCount.value));
  const baseFileName = computed(() =>
    sanitizePrintFileName(state.bizType || state.title || 'print-document'),
  );

  function createPreviewHtml() {
    const html = getHtml(templateInstance.value, currentPrintData.value);
    return typeof html === 'string' ? html : '';
  }

  async function renderPreview() {
    await nextTick();

    if (!previewRef.value) {
      return;
    }

    previewRef.value.innerHTML = createPreviewHtml();
  }

  async function loadPrinters() {
    printerLoading.value = true;

    try {
      await connect();

      const printers = await refreshPrinterList();
      printerList.value = (printers || []).map((item: { name?: string; label?: string }) => ({
        label: item.label || item.name || '',
        value: item.name || '',
      }));

      if (!printerName.value && printerList.value.length > 0) {
        printerName.value = printerList.value[0].value;
      }
    } catch {
      printerList.value = [];
    } finally {
      printerLoading.value = false;
    }
  }

  function ensureClientConnected(message: string) {
    if (isClientConnected()) {
      return true;
    }

    createWarning(message);
    return false;
  }

  async function refreshPreviewIfOpen() {
    if (!state.open) {
      return;
    }

    await renderPreview();
  }

  function handleClose() {
    closePrintDialog();
  }

  async function handlePreview() {
    await renderPreview();
    createSuccessTip('预览已刷新');
  }

  function handleBrowserPrint() {
    printBrowser(templateInstance.value, currentPrintData.value);
  }

  async function handleExportPdf() {
    try {
      await exportPdf(templateInstance.value, currentPrintData.value, baseFileName.value);
      createSuccessTip('PDF 导出已开始');
    } catch {
      createError('PDF 导出失败');
    }
  }

  async function handleExportImage() {
    try {
      await exportImage(templateInstance.value, currentPrintData.value, {
        isDownload: true,
        splitPages: true,
        name: baseFileName.value,
        pixelRatio: 2,
        type: 'image/png',
      });
      createSuccessTip('图片导出已开始');
    } catch {
      createError('图片导出失败');
    }
  }

  async function handleClientGenerate() {
    if (!ensureClientConnected('客户端未连接')) {
      return;
    }

    try {
      await clientGenerate(templateInstance.value, currentPrintData.value, {
        pdfName: `${baseFileName.value}-pdf`,
        imgName: `${baseFileName.value}-img`,
      });
      createSuccessTip('客户端文件已生成');
    } catch {
      createError('客户端文件生成失败');
    }
  }

  async function handleDirectPrint() {
    if (!printerName.value) {
      createWarning('请先选择打印机');
      return;
    }

    if (!ensureClientConnected('打印客户端未连接')) {
      return;
    }

    try {
      await templateInstance.value.print2(currentPrintData.value, { printer: printerName.value });
      createSuccessTip('打印任务已发送');
    } catch {
      createError('直接打印失败');
    }
  }

  watch(
    () => state.open,
    async (open) => {
      if (!open) {
        return;
      }

      printCount.value = 1;
      await renderPreview();
      await loadPrinters();
    },
    { immediate: true },
  );

  watch(
    () => state.frameKey,
    refreshPreviewIfOpen,
  );

  watch(printCount, refreshPreviewIfOpen);
</script>

<style scoped>
  .order-print-dialog {
    --print-dialog-height: calc(100vh - 48px);
    display: flex;
    flex-direction: column;
    gap: 12px;
    height: var(--print-dialog-height);
    max-height: var(--print-dialog-height);
  }

  .order-print-dialog__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 12px 16px;
    color: #fff;
    background: linear-gradient(90deg, #5b21b6 0%, #6d28d9 55%, #7c3aed 100%);
    border-radius: 12px;
  }

  .order-print-dialog__toolbar-left {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  .order-print-dialog__title {
    font-size: 18px;
    font-weight: 600;
    line-height: 1.2;
  }

  .order-print-dialog__meta {
    font-size: 13px;
    color: rgb(255 255 255 / 78%);
  }

  .order-print-dialog__toolbar-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex: 1;
    flex-wrap: wrap;
    gap: 8px;
  }

  .order-print-dialog__copies {
    width: 110px;
  }

  .order-print-dialog__printer {
    width: 220px;
  }

  .order-print-dialog__body {
    flex: 1;
    min-height: 0;
    padding: 12px;
    overflow: auto;
    background: #e5e7eb;
    border-radius: 12px;
  }

  .order-print-dialog__preview {
    min-height: 100%;
    padding: 16px;
    overflow: auto;
    background: #f3f4f6;
    border-radius: 8px;
  }

  .order-print-dialog__preview :deep(.hiprint-printPaper) {
    margin: 0 auto 16px;
    box-sizing: border-box;
    border: 1px solid #d1d5db;
    box-shadow: 0 10px 24px rgb(15 23 42 / 10%);
  }

  :global(.order-print-dialog-wrap .ant-modal) {
    max-width: 92vw;
    top: 12px;
    height: calc(100vh - 24px);
    padding-bottom: 0;
  }

  :global(.order-print-dialog-wrap .ant-modal-content) {
    height: 100%;
    max-height: calc(100vh - 24px);
    padding: 12px;
    overflow: hidden;
  }

  :global(.order-print-dialog-wrap .ant-modal-body) {
    height: 100%;
    max-height: 100%;
    overflow: hidden;
  }

  :global(.order-print-dialog-wrap .ant-modal-close) {
    display: none;
  }

  :global(.order-print-dialog-wrap .ant-btn-background-ghost) {
    border-color: rgb(255 255 255 / 42%);
    color: #fff;
  }

  @media (max-width: 1200px) {
    .order-print-dialog {
      --print-dialog-height: calc(100vh - 32px);
    }

    .order-print-dialog__toolbar {
      align-items: flex-start;
      flex-direction: column;
    }

    .order-print-dialog__toolbar-right {
      width: 100%;
      justify-content: flex-start;
    }
  }
</style>
