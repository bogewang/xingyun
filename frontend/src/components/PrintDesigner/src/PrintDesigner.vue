<template>
  <div class="print-designer-shell">
    <a-alert
      v-if="hasLegacyTemplate"
      type="warning"
      show-icon
      message="请先迁移模板"
      description="当前模板仍是旧版 panels 格式，无法在 PrintDot 中加载或编辑。请手动迁移后再保存。"
    />
    <template v-else>
      <div class="print-designer-toolbar">
        <a-button class="print-template-field-doc-btn" size="small" @click="openFieldDoc">
          模板字段说明
        </a-button>
        <a-button type="primary" size="small" @click="saveTemp">保存模板</a-button>
      </div>
      <div ref="designerMount" class="print-designer-mount" />
    </template>
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
  import { computed, defineComponent, nextTick, onMounted, ref, watch } from 'vue';
  import * as printTemplateApi from '@/api/base-data/print-template';
  import type { PrintTemplateColumnDescription } from '@/api/base-data/print-template/model/printTemplateColumnDescription';
  import {
    isPrintDotTemplate,
    toPrintDotVariables,
    type PrintDesignerElement,
  } from './printdot';

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

  /**
   * 将后端字段说明转换为表格展示行。
   */
  function mapFieldDocRows(data: PrintTemplateColumnDescription[] = []): FieldDocRow[] {
    return data.map((item) => ({
      path: item.columnName,
      description: item.description,
      sample: item.demo,
    }));
  }

  /**
   * 判断模板是否仍为旧版 panels 格式。
   */
  function isLegacyTemplate(value: unknown): boolean {
    return !!value && typeof value === 'object' && 'panels' in value;
  }

  export default defineComponent({
    name: 'PrintDesigner',
    props: {
      tempValue: {
        type: Object,
        default: null,
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
      const designerMount = ref<HTMLElement>();
      const designerRef = ref<PrintDesignerElement>();
      const fieldDocVisible = ref(false);
      const fieldDocLoading = ref(false);
      const fieldDocRows = ref<FieldDocRow[]>([]);
      const hasLegacyTemplate = computed(() => isLegacyTemplate(props.tempValue));

      /**
       * 在 PrintDot 元素可用后加载模板与示例数据。
       */
      async function initializeDesigner() {
        await nextTick();

        if (hasLegacyTemplate.value) {
          designerRef.value = undefined;
          return;
        }

        const mount = designerMount.value;
        if (!mount) {
          return;
        }

        let designer = mount.querySelector<PrintDesignerElement>('print-designer');
        if (!designer) {
          designer = document.createElement('print-designer') as PrintDesignerElement;
          designer.lang = 'zh';
          mount.append(designer);
        }

        designerRef.value = designer;

        if (isPrintDotTemplate(props.tempValue)) {
          designer.loadTemplateData(props.tempValue);
        }

        designer.setTestData(toPrintDotVariables(props.demoData));
      }

      /**
       * 从 PrintDot 元素读取模板和测试数据，并保持既有的保存事件契约。
       */
      function saveTemp() {
        const designer = designerRef.value;
        if (!designer || hasLegacyTemplate.value) {
          return;
        }

        emit('save', designer.getTemplateData(), designer.getTestData());
      }

      /**
       * 调用 PrintDot 浏览器打印，供需要即时查看的业务侧使用。
       */
      function previewTemp() {
        return designerRef.value?.print({ mode: 'browser' });
      }

      /**
       * 返回 PrintDot Web Component 实例，便于业务侧按需扩展调用。
       */
      function getDesigner() {
        return designerRef.value;
      }

      /**
       * 查询并显示当前业务类型可绑定的模板字段说明。
       */
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

      onMounted(initializeDesigner);

      watch(
        () => [props.tempValue, props.demoData],
        () => initializeDesigner(),
      );

      expose({ getDesigner, previewTemp, saveTemp });

      return {
        designerRef,
        designerMount,
        fieldDocColumns,
        fieldDocLoading,
        fieldDocRows,
        fieldDocVisible,
        hasLegacyTemplate,
        openFieldDoc,
        saveTemp,
      };
    },
  });
</script>

<style lang="scss">
  .print-designer-shell {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
  }

  .print-designer-toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
    padding: 8px 12px;
    background: #001529;
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
    color: #fff !important;
    background: transparent !important;
    border-color: rgb(255 255 255 / 75%) !important;
  }

  .print-template-field-doc-btn.ant-btn:hover,
  .print-template-field-doc-btn.ant-btn:focus {
    color: #fff !important;
    background: rgb(255 255 255 / 14%) !important;
    border-color: #fff !important;
  }

  .print-designer-mount {
    flex: 1;
    min-height: 0;
  }

  .print-designer-mount > print-designer {
    display: block;
    width: 100%;
    height: 100%;
  }
</style>
