<template>
  <div>
    <excel-importer
      ref="importer"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="{ orderDate }"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/sc/sale/order';

  export default defineComponent({
    name: 'SaleOrderImporter',
    components: { ExcelImporter },
    props: {
      orderDate: {
        type: String,
        required: true,
      },
    },
    data() {
      return {};
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
