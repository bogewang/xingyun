<template>
  <a-modal
    :open="state.open"
    :title="state.title"
    :footer="null"
    :closable="true"
    :mask-closable="true"
    :width="1200"
    wrap-class-name="order-print-dialog-wrap"
    @cancel="handleClose"
  >
    <div class="order-print-dialog">
      <div class="order-print-dialog__toolbar">
        <div class="order-print-dialog__meta" v-if="state.bizType">{{ state.bizType }}</div>
        <a-space>
          <a-button type="primary" @click="handlePrint">打印</a-button>
          <a-button @click="handleClose">关闭</a-button>
        </a-space>
      </div>
      <div class="order-print-dialog__body">
        <iframe
          :key="state.frameKey"
          ref="iframeRef"
          class="order-print-dialog__frame"
          title="订单打印预览"
        />
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
  import { nextTick, ref, watch } from 'vue';
  import { closePrintDialog, usePrintDialogState } from './printDialog';

  const state = usePrintDialogState();
  const iframeRef = ref<HTMLIFrameElement | null>(null);

  function handleClose() {
    closePrintDialog();
  }

  function handlePrint() {
    iframeRef.value?.contentWindow?.focus();
    iframeRef.value?.contentWindow?.print();
  }

  watch(
    () => [state.open, state.html, state.frameKey] as const,
    async ([open, html]) => {
      if (!open) {
        return;
      }

      await nextTick();
      const doc = iframeRef.value?.contentWindow?.document;
      if (!doc) {
        return;
      }

      doc.open();
      doc.write(html);
      doc.close();
    },
    { immediate: true },
  );
</script>

<style scoped>
  .order-print-dialog {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .order-print-dialog__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .order-print-dialog__meta {
    font-size: 13px;
    color: #64748b;
  }

  .order-print-dialog__body {
    height: calc(100vh - 240px);
    min-height: 520px;
    padding: 12px;
    background: #e5e7eb;
    border-radius: 8px;
  }

  .order-print-dialog__frame {
    width: 100%;
    height: 100%;
    border: 0;
    background: #ffffff;
    border-radius: 6px;
  }

  :global(.order-print-dialog-wrap .ant-modal) {
    max-width: calc(100vw - 48px);
  }

  :global(.order-print-dialog-wrap .ant-modal-body) {
    padding-top: 12px;
  }
</style>
