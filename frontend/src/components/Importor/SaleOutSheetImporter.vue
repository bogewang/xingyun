<template>
  <div>
    <excel-importer
      ref="importer"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :get-container="getContainer"
      :local-container="localContainer"
      :hide-on-deactivated="hideOnDeactivated"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/sc/sale/out';

  export default defineComponent({
    name: 'SaleOutSheetImporter',
    components: { ExcelImporter },
    data() {
      return {};
    },
    props: {
      getContainer: {
        type: [Function, Boolean],
        default: undefined,
      },
      localContainer: {
        type: Boolean,
        default: false,
      },
      hideOnDeactivated: {
        type: Boolean,
        default: false,
      },
    },
    computed: {},
    methods: {
      openDialog() {
        this.$refs.importer.openDialog();
      },
      downloadTemplate() {
        return api.downloadImportTemplate();
      },
      upload(params) {
        return api.importExcel(params);
      },
    },
  });
</script>

<style lang="less"></style>
