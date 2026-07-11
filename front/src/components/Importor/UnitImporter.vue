<template>
  <a-modal v-model:open="visible" title="导入单位" :footer="null" @cancel="visible = false">
    <a-upload-dragger name="file" accept=".xls,.xlsx" :custom-request="upload" :show-upload-list="false">
      <p class="ant-upload-text">点击或拖拽 Excel 文件导入</p>
      <p class="ant-upload-hint">仅支持 xls、xlsx 格式；编码由系统自动生成。</p>
    </a-upload-dragger>
    <div style="margin-top: 12px"><a-button type="link" @click="downloadTemplate">下载模板文件</a-button></div>
  </a-modal>
</template>
<script>
  import { defineComponent } from 'vue';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import * as api from '@/api/base-data/unit';

  export default defineComponent({
    name: 'UnitImporter',
    data() { return { visible: false }; },
    methods: {
      openDialog() { this.visible = true; },
      downloadTemplate() {
        return api.downloadImportTemplate();
      },
      upload({ file, onSuccess, onError }) {
        api.importExcel({ file }).then(() => {
          createSuccess('导入成功'); onSuccess(); this.visible = false; this.$emit('confirm');
        }).catch((error) => { createError(error?.message || '导入失败'); onError(error); });
      },
    },
  });
</script>
