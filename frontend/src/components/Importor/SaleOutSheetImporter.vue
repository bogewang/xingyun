<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="'Excel 可填写“验收数量”；“验收金额”由系统按验收数量×单价自动计算，不能手工覆盖。'"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="{ orderDate }"
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
    props: {
      orderDate: {
        type: String,
        required: true,
      },
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
