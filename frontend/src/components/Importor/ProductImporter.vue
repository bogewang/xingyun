<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="'导入只支持新增商品信息。\n注：\n1、只支持导入普通商品。\n2、名称+规格+单位与已停用商品重复时，该行将被忽略。'"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/base-data/product/info';

  export default defineComponent({
    name: 'ProductImporter',
    components: { ExcelImporter },
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
