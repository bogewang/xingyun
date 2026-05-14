<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="
        '按导入文件中的“销售日期 + 客户”自动分组生成销售出库单。\n注：\n1、Excel 中其余字段都会作为出库明细导入。'
      "
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="formData"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/sc/sale/out';

  export default defineComponent({
    name: 'SaleOutSheetQueryImporter',
    components: {
      ExcelImporter,
    },
    data() {
      return {
        formData: {},
      };
    },
    methods: {
      openDialog() {
        this.formData = {};
        this.$refs.importer.openDialog();
      },
      downloadTemplate() {
        return api.downloadQueryImportTemplate();
      },
      upload(params) {
        return api.importByQuery(params);
      },
    },
  });
</script>

<style lang="less"></style>
