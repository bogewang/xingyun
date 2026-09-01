<template>
  <excel-importer
    ref="importer"
    tip-msg="Excel 可填写商品名称、规格、单位和销售单价；未匹配商品会在报价单页面标记，需手动选择后才能保存。"
    :download-template-url="downloadTemplate"
    :upload-url="upload"
    :get-container="getContainer"
    :local-container="localContainer"
    @confirm="(event) => $emit('confirm', event)"
  />
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/base-data/quote';

  export default defineComponent({
    name: 'QuoteSheetImporter',
    components: { ExcelImporter },
    props: {
      getContainer: { type: [Function, Boolean], default: undefined },
      localContainer: { type: Boolean, default: false },
    },
    emits: ['confirm'],
    methods: {
      /** 打开报价单导入弹窗。 */
      openDialog() { this.$refs.importer.openDialog(); },
      /** 下载报价单导入模板。 */
      downloadTemplate() { return api.downloadImportTemplate(); },
      /** 上传并预检报价单导入文件。 */
      upload(params) { return api.importExcel(params); },
    },
  });
</script>
