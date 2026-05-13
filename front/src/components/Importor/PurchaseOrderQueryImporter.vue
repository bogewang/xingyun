<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="
        '按导入文件中的“单据日期 + 供应商”自动分组生成采购订单。\n注：\n1、Excel 中其余字段都会作为订单明细导入。'
      "
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="formData"
      @confirm="(e) => $emit('confirm', e)"
    >
    </excel-importer>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import StoreCenterSelector from '@/components/Selector/StoreCenterSelector.vue';
  import UserSelector from '@/components/Selector/UserSelector.vue';
  import * as api from '@/api/sc/purchase/receive';

  export default defineComponent({
    name: 'ReceiveSheetQueryImporter',
    components: {
      ExcelImporter,
      StoreCenterSelector,
      UserSelector,
    },
    data() {
      return {
        formData: {},
      };
    },
    methods: {
      openDialog() {
        this.formData = {}
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
