<template>
  <a-modal
    v-model:open="modalShow"
    :title="dialogTitle"
    :width="PREVIEW_WIDTH"
    :footer="null"
    destroy-on-close
  >
    <template #title>
      <div v-if="state.enableTemplateSwitch" class="print-dialog-title">
        <span class="print-dialog-title__text">{{ dialogTitle }}</span>
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
      <span v-else>{{ dialogTitle }}</span>
    </template>
    <a-alert v-if="templateError" type="error" show-icon :message="templateError" />
    <div v-else ref="designerMount" class="print-dialog-preview"></div>
    <div class="print-dialog-actions">
      <a-button @click="closePrintDialog">取消</a-button>
      <a-button type="primary" :disabled="!previewReady" :loading="printing" @click="handlePrint">
        打印
      </a-button>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref, watch } from 'vue';
  import * as api from '@/api/base-data/print-template';
  import { createError } from '@/hooks/web/msg';
  import {
    isPrintDotTemplate,
    toPrintDotVariables,
    type PrintDesignerElement,
    type PrintDotTemplate,
  } from '@/components/PrintDesigner/src/printdot';
  import { closePrintDialog, usePrintDialogState } from './printDialog';

  const PREVIEW_WIDTH = '80%';

  const state = usePrintDialogState();
  const templateOptions = computed(() => state.templateList || []);
  const dialogTitle = computed(() => state.title || '打印预览');

  // 弹窗显示控制
  const modalShow = computed({
    get: () => state.open,
    set: (value: boolean) => {
      if (!value) {
        closePrintDialog();
      }
    },
  });
  const selectedTemplateId = ref('');
  const currentTemplateJson = ref<unknown>(state.templateJson);
  const currentTemplateId = ref('');
  const templateLoading = ref(false);
  const templateCache = new Map<string, PrintDotTemplate>();
  const designerMount = ref<HTMLElement>();
  const designerRef = ref<PrintDesignerElement>();
  const templateError = ref('');
  const previewReady = ref(false);
  const printing = ref(false);

  /**
   * 返回模板不可用于 PrintDot 时的明确提示。
   */
  function getTemplateError(templateJson: unknown) {
    if (templateJson && typeof templateJson === 'object' && 'panels' in templateJson) {
      return '当前打印模板仍是旧版 panels 格式，请先迁移模板！';
    }

    return '打印模板格式无效，无法加载打印预览！';
  }

  /**
   * 在弹窗中创建并返回 PrintDot Web Component 实例。
   */
  async function getDesigner() {
    await nextTick();

    const mount = designerMount.value;
    if (!mount) {
      return undefined;
    }

    let designer = mount.querySelector<PrintDesignerElement>('print-designer');
    if (!designer) {
      designer = document.createElement('print-designer') as PrintDesignerElement;
      designer.lang = 'zh';
      mount.append(designer);
    }

    designerRef.value = designer;
    return designer;
  }

  /**
   * 使用模板和当前业务数据刷新可见 PrintDot 预览。
   */
  async function showPreview(
    templateJson = currentTemplateJson.value,
    preserveCurrentPreview = false,
  ) {
    if (!isPrintDotTemplate(templateJson)) {
      const errorMessage = getTemplateError(templateJson);
      if (!preserveCurrentPreview) {
        templateError.value = errorMessage;
        previewReady.value = false;
      }
      createError(errorMessage);
      return false;
    }

    const previousTemplateError = templateError.value;
    const previousPreviewReady = previewReady.value;
    templateError.value = '';
    try {
      const designer = await getDesigner();
      if (!designer) {
        throw new Error('PrintDot 预览组件未正确初始化');
      }

      const templateLoaded = designer.loadTemplateData(templateJson);
      if (!templateLoaded) {
        throw new Error('PrintDot 模板加载失败');
      }

      await designer.setVariables(toPrintDotVariables(state.printData));
      previewReady.value = true;
      return true;
    } catch {
      const errorMessage = '加载打印模板失败！';
      templateError.value = preserveCurrentPreview ? previousTemplateError : errorMessage;
      previewReady.value = preserveCurrentPreview ? previousPreviewReady : false;
      createError(errorMessage);
      return false;
    }
  }

  /**
   * 按模板 ID 加载模板配置。
   *
   * 优先使用弹窗内缓存；缓存未命中时请求后端设置接口，
   * 成功后更新当前模板 JSON 并刷新预览；失败时不会改动当前有效模板。
   */
  async function loadTemplateById(templateId: string) {
    if (!templateId) {
      createError('请选择打印模板！');
      return false;
    }

    try {
      let templateJson = templateCache.get(templateId);
      if (!templateJson) {
        templateLoading.value = true;
        const setting = await api.getSetting(templateId);
        if (!setting?.templateJson) {
          createError('未找到打印模板配置！');
          return false;
        }

        if (!isPrintDotTemplate(setting.templateJson)) {
          createError(getTemplateError(setting.templateJson));
          return false;
        }

        templateJson = setting.templateJson;
        templateCache.set(templateId, templateJson);
      }

      const loaded = await showPreview(templateJson, true);
      if (!loaded) {
        return false;
      }

      currentTemplateJson.value = templateJson;
      currentTemplateId.value = templateId;
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
   * 模板加载失败时恢复之前的下拉选择，保持已有预览可继续打印。
   */
  async function handleTemplateChange(templateId: string) {
    const loaded = await loadTemplateById(templateId);
    if (!loaded) {
      selectedTemplateId.value = currentTemplateId.value;
    }
  }

  /**
   * 通过 PrintDot 的浏览器通道执行打印。
   */
  async function handlePrint() {
    if (!previewReady.value || !designerRef.value) {
      createError(templateError.value || '打印预览尚未准备完成！');
      return;
    }

    printing.value = true;
    try {
      await designerRef.value.print({ mode: 'browser' });
    } catch {
      createError('浏览器打印失败！');
    } finally {
      printing.value = false;
    }
  }

  /**
   * 重置当前预览状态，并以入口模板作为默认选择。
   */
  function resetTemplateState() {
    templateCache.clear();
    currentTemplateJson.value = state.templateJson;
    currentTemplateId.value = state.templateId || state.templateList?.[0]?.id || '';
    selectedTemplateId.value = currentTemplateId.value;
    templateError.value = '';
    previewReady.value = false;

    if (state.templateId && isPrintDotTemplate(state.templateJson)) {
      templateCache.set(state.templateId, state.templateJson);
    }
  }

  watch(
    () => [state.open, state.frameKey] as const,
    async ([open, frameKey], previousValue) => {
      if (!open) {
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

  .print-dialog-preview {
    min-height: 640px;
  }

  .print-dialog-preview > print-designer {
    display: block;
    width: 100%;
    min-height: 640px;
  }

  .print-dialog-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
  }
</style>
