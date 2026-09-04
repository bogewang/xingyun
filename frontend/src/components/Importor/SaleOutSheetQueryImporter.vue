<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="'按导入文件中的“销售日期 + 客户”自动分组生成销售出库单。\n注：\n1、Excel 中其余字段都会作为出库明细导入；未填写“单价”时，系统按“销售日期”匹配生效报价。\n2、“配送日期”格式为 yyyy-MM-dd。\n3、Excel 可填写“验收数量”；“验收金额”由系统按验收数量×单价自动计算，不能手工覆盖。'"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="formData"
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
    name: 'SaleOutSheetQueryImporter',
    components: {
      ExcelImporter,
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
