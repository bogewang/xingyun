<template>
  <div class="print-designer-shell">
    <FullDesigner
      ref="designerRef"
      :initial-template="normalizedTemplate"
      :initial-print-data="normalizedPrintData"
      default-lang="cn"
      @save="handleSave"
    >
      <template #headerLeft>
        <div class="print-designer-header-left">
          <a-button
            class="print-template-field-doc-btn"
            size="small"
            @click="openFieldDoc"
          >
            模板字段说明
          </a-button>
        </div>
      </template>
    </FullDesigner>
    <a-modal
      v-model:open="fieldDocVisible"
      title="模板字段说明"
      width="960px"
      :footer="null"
      destroy-on-close
    >
      <div class="print-field-docs">
        <div class="print-field-docs__summary">
          当前共加载 {{ fieldDocRows.length }} 个可用字段。优先使用“字段路径”进行模板绑定；
          如果说明列为空，仍然可以直接按路径取值。
        </div>
        <a-table
          :loading="fieldDocLoading"
          :data-source="fieldDocRows"
          :columns="fieldDocColumns"
          :pagination="false"
          :scroll="{ y: 520 }"
          row-key="path"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'path'" class="print-field-docs__path">
              {{ record.path }}
            </span>
          </template>
        </a-table>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, nextTick, onMounted, ref } from 'vue';
  import { FullDesigner, hiprint } from 'vg-print';
  import * as printTemplateApi from '@/api/base-data/print-template';
  import type { PrintTemplateColumnDescription } from '@/api/base-data/print-template/model/printTemplateColumnDescription';
  import {
    createEmptyTemplate,
    normalizeDemoData,
    normalizePrintData,
    normalizeTemplate,
    type PrintDemoData,
    type PrintTemplateJson,
  } from './printUtils';

  hiprint.register({ authKey: 'eyJrIjoiZ21jNTc2MDMzNyJ9' });

  const IMPORTED_TEMPLATE_STORAGE_KEY = 'xingyun-print-imported-templates';

  type SavePayload = {
    template?: PrintTemplateJson;
    data?: PrintDemoData;
  };

  type DesignerTemplateItem = {
    tempId: string;
    name: string;
    desc: string;
    template: PrintTemplateJson;
    testData?: PrintDemoData;
  };

  type FieldDocRow = {
    path: string;
    description: string;
    sample: string;
  };

  const fieldDocColumns = [
    {
      title: '字段路径',
      dataIndex: 'path',
      key: 'path',
      width: 320,
    },
    {
      title: '字段说明',
      dataIndex: 'description',
      key: 'description',
      width: 320,
    },
    {
      title: '示例值',
      dataIndex: 'sample',
      key: 'sample',
    },
  ];

  function mapFieldDocRows(data: PrintTemplateColumnDescription[] = []): FieldDocRow[] {
    return data.map((item) => ({
      path: item.columnName,
      description: item.description,
      sample: item.demo,
    }));
  }

  /**
   * 根据导入文件名生成模板列表中的显示名称。
   */
  function buildImportedTemplateName(fileName: string) {
    return fileName.replace(/\.json$/i, '') || '导入模板';
  }

  /**
   * 从浏览器本地缓存读取用户导入过的模板。
   */
  function loadImportedTemplates(): DesignerTemplateItem[] {
    try {
      const raw = window.localStorage.getItem(IMPORTED_TEMPLATE_STORAGE_KEY);
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  /**
   * 将导入模板保存到浏览器本地缓存。
   */
  function saveImportedTemplates(templates: DesignerTemplateItem[]) {
    try {
      window.localStorage.setItem(IMPORTED_TEMPLATE_STORAGE_KEY, JSON.stringify(templates));
    } catch {
      // 忽略本地缓存异常，避免影响导入主流程。
    }
  }

  /**
   * 合并设计器内置模板与本地导入模板。
   *
   * 以 `tempId` 去重，保证用户导入模板在刷新后仍能出现在模板列表中。
   */
  function mergeTemplateList(
    currentList: DesignerTemplateItem[] = [],
    importedList: DesignerTemplateItem[] = [],
  ) {
    const merged = new Map<string, DesignerTemplateItem>();

    currentList.forEach((item) => {
      if (item?.tempId) {
        merged.set(item.tempId, item);
      }
    });

    importedList.forEach((item) => {
      if (item?.tempId) {
        merged.set(item.tempId, item);
      }
    });

    return Array.from(merged.values());
  }

  export default defineComponent({
    name: 'PrintDesigner',
    components: { FullDesigner },
    props: {
      tempValue: {
        type: Object,
        default: () => createEmptyTemplate(),
      },
      demoData: {
        type: [Array, Object, String],
        default: () => ({}),
      },
      bizType: {
        type: [String, Number],
        default: undefined,
      },
    },
    emits: ['save'],
    setup(props, { emit, expose }) {
      const designerRef = ref<any>(null);
      const fieldDocVisible = ref(false);
      const fieldDocLoading = ref(false);
      const fieldDocRows = ref<FieldDocRow[]>([]);
      let importPatched = false;

      const normalizedTemplate = computed(() => normalizeTemplate(props.tempValue));
      const normalizedPrintData = computed(() => normalizePrintData(props.demoData));

      /**
       * 接收设计器保存事件，并向业务页面输出规范化模板。
       */
      function handleSave(payload?: SavePayload) {
        emit(
          'save',
          payload?.template || createEmptyTemplate(),
          normalizeDemoData(payload?.data ?? props.demoData),
        );
      }

      /**
       * 触发设计器保存动作，供父组件通过组件实例调用。
       */
      function saveTemp() {
        designerRef.value?.save();
      }

      /**
       * 触发设计器内置预览动作，使用当前示例数据查看模板效果。
       */
      function previewTemp() {
        designerRef.value?.preView();
      }

      /**
       * 返回底层设计器实例，便于业务侧按需扩展调用。
       */
      function getDesigner() {
        return designerRef.value;
      }

      async function openFieldDoc() {
        fieldDocVisible.value = true;

        if (fieldDocRows.value.length > 0 || fieldDocLoading.value) {
          return;
        }

        fieldDocLoading.value = true;
        try {
          const data = await printTemplateApi.getFieldDesc(props.bizType);
          fieldDocRows.value = mapFieldDocRows(data);
        } finally {
          fieldDocLoading.value = false;
        }
      }

      /**
       * 增强设计器的本地模板导入能力。
       *
       * vg-print 默认导入流程不会持久化到项目业务列表，这里拦截上传选择、
       * 解析 JSON 模板、写入本地缓存，并把导入模板合并回设计器模板列表。
       */
      function patchTemplateImport() {
        const designer = designerRef.value;

        if (!designer || importPatched) {
          return;
        }

        const originalOnLocalTemplates =
          typeof designer.onLocalTemplates === 'function'
            ? designer.onLocalTemplates.bind(designer)
            : null;
        const originalOnTemplateFileSelected =
          typeof designer.onTemplateFileSelected === 'function'
            ? designer.onTemplateFileSelected.bind(designer)
            : null;

        designer.onLocalTemplates = (templates: DesignerTemplateItem[]) => {
          if (originalOnLocalTemplates) {
            originalOnLocalTemplates(templates);
          }

          designer.templateList = mergeTemplateList(designer.templateList, loadImportedTemplates());
        };

        designer.handleUploadTemplate = () => {
          const input = designer.$refs?.uploadTplInput as HTMLInputElement | undefined;
          if (input?.click) {
            input.value = '';
            input.click();
          }
        };

        designer.onTemplateFileSelected = async (event: Event) => {
          const input = event?.target as HTMLInputElement | null;
          const file = input?.files?.[0];

          if (!file) {
            return;
          }

          try {
            const content = await file.text();
            const parsedContent = JSON.parse(content);

            if (!Array.isArray((parsedContent as { panels?: unknown[] }).panels)) {
              designer.$message?.warning?.('选中的文件不是有效的模板文件');
              return;
            }

            const parsedTemplate = normalizeTemplate(parsedContent);
            const templateItem: DesignerTemplateItem = {
              tempId: `imported:${file.name}`,
              name: buildImportedTemplateName(file.name),
              desc: '导入模板',
              template: parsedTemplate,
              testData: designer.printDataVar,
            };

            const importedTemplates = mergeTemplateList(loadImportedTemplates(), [templateItem]);
            saveImportedTemplates(importedTemplates);
            designer.templateList = mergeTemplateList(designer.templateList, importedTemplates);

            if (typeof designer.applyTemplateItem === 'function') {
              await designer.applyTemplateItem(templateItem);
            } else if (originalOnTemplateFileSelected) {
              await originalOnTemplateFileSelected(event);
            }

            designer.$message?.success?.('模板导入成功');
          } catch (error) {
            designer.$message?.error?.(`模板导入失败: ${String(error)}`);
          } finally {
            if (input) {
              input.value = '';
            }
          }
        };

        designer.templateList = mergeTemplateList(designer.templateList, loadImportedTemplates());
        importPatched = true;
      }

      onMounted(() => {
        nextTick(() => {
          patchTemplateImport();
        });
      });

      expose({ getDesigner, previewTemp, saveTemp });

      return {
        designerRef,
        fieldDocColumns,
        fieldDocLoading,
        fieldDocRows,
        fieldDocVisible,
        handleSave,
        normalizedPrintData,
        normalizedTemplate,
        openFieldDoc,
      };
    },
  });
</script>

<style lang="scss">
  .print-designer-shell {
    width: 100%;
    height: 100%;
  }

  .print-designer-header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    color: #fff;
  }

  .print-field-docs__summary {
    margin-bottom: 12px;
    color: rgb(0 0 0 / 65%);
  }

  .print-field-docs :deep(.ant-table-cell) {
    vertical-align: top;
    word-break: break-all;
  }

  .print-field-docs__path {
    color: #1677ff;
    font-weight: 500;
  }

  .print-template-field-doc-btn.ant-btn {
    height: 28px;
    padding: 0 10px;
    color: #fff !important;
    font-size: 12px;
    line-height: 26px;
    background: transparent !important;
    border: 1px solid rgb(255 255 255 / 75%) !important;
    border-radius: 3px;
    box-shadow: none;
  }

  .print-template-field-doc-btn.ant-btn:hover,
  .print-template-field-doc-btn.ant-btn:focus {
    color: #fff !important;
    background: rgb(255 255 255 / 14%) !important;
    border-color: #fff !important;
  }

  .print-designer-shell :deep(.designer-page) {
    height: 100%;
  }
</style>
