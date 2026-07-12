<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="'导入只支持新增单位信息。\n注：单位编码由系统自动生成。'"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import * as api from '@/api/base-data/unit';

  export default defineComponent({
    name: 'UnitImporter',
    components: { ExcelImporter },
    emits: ['confirm'],
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
