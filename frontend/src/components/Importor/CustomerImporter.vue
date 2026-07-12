<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="'导入只支持新增客户信息。\n注：\n1、仅客户名称为必填项，编号留空时系统自动生成，其他字段均可不填。\n2、地区的格式为：省/市/区（县），例如：北京市/市辖区/朝阳区。文字请参考新增或修改功能中的地区选择。\n3、结算方式不填时默认按“任意指定”处理。'"
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      @confirm="(e) => $emit('confirm', e)"
    />
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporter';
  import * as api from '@/api/base-data/customer';

  export default defineComponent({
    name: 'CustomerImporter',
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
