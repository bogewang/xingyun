<template>
  <div class="print-designer-shell">
    <FullDesigner
      ref="designerRef"
      :initial-template="normalizedTemplate"
      :initial-print-data="normalizedPrintData"
      default-lang="cn"
      @save="handleSave"
    />
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, ref } from 'vue';
  import { FullDesigner, hiprint } from 'vg-print';
  import {
    createEmptyTemplate,
    normalizePrintData,
    normalizeTemplate,
    type PrintTemplateJson,
  } from './printUtils';

  hiprint.register({ authKey: 'eyJrIjoiZ21jNTc2MDMzNyJ9' });

  type SavePayload = {
    template?: PrintTemplateJson;
  };

  export default defineComponent({
    name: 'PrintDesigner',
    components: { FullDesigner },
    props: {
      tempValue: {
        type: Object,
        default: () => createEmptyTemplate(),
      },
      demoData: {
        type: [Array, Object],
        default: () => ({}),
      },
    },
    emits: ['save'],
    setup(props, { emit, expose }) {
      const designerRef = ref<any>(null);

      const normalizedTemplate = computed(() => normalizeTemplate(props.tempValue));
      const normalizedPrintData = computed(() => normalizePrintData(props.demoData));

      function handleSave(payload?: SavePayload) {
        emit('save', payload?.template || createEmptyTemplate());
      }

      function saveTemp() {
        designerRef.value?.save();
      }

      function previewTemp() {
        designerRef.value?.preView();
      }

      function getDesigner() {
        return designerRef.value;
      }

      expose({ getDesigner, previewTemp, saveTemp });

      return {
        designerRef,
        handleSave,
        normalizedPrintData,
        normalizedTemplate,
      };
    },
  });
</script>

<style lang="scss">
  .print-designer-shell {
    width: 100%;
    height: 100%;
  }

  .print-designer-shell :deep(.designer-page) {
    height: 100%;
  }
</style>
