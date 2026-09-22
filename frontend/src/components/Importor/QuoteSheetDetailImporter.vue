<template>
  <excel-importer
    ref="importer"
    tip-msg="Excel 请填写商品名称、规格、单位、销售单价和是否询价商品；系统按名称、规格和单位匹配商品。未匹配商品需手动选择后才能保存。"
    :download-template-url="downloadTemplate"
    :upload-url="upload"
    :get-container="getContainer"
    :local-container="localContainer"
    :form-data="{ quoteSheetId }"
    @confirm="(event) => $emit('confirm', event)"
  />
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/base-data/quote';

  export default defineComponent({
    name: 'QuoteSheetDetailImporter',
    components: { ExcelImporter },
    props: {
      getContainer: { type: [Function, Boolean], default: undefined },
      localContainer: { type: Boolean, default: false },
      quoteSheetId: { type: String, required: true },
    },
    emits: ['confirm'],
    methods: {
      /** 打开报价单明细导入弹窗。 */
      openDialog() {
        this.$refs.importer.openDialog();
      },
      /** 下载报价单明细导入模板。 */
      downloadTemplate(formData) {
        return api.downloadDetailImportTemplate(formData.quoteSheetId);
      },
      /** 上传并按商品名称、规格和单位校验报价单明细导入文件。 */
      upload(params) {
        return api.importDetailExcel(params);
      },
    },
  });
</script>
