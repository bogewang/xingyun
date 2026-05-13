<template>
  <div>
    <excel-importer
      ref="importer"
      :tip-msg="
        '按导入文件中的“单据日期 + 供应商”自动分组生成采购订单。\n注：\n1、仓库为本次导入统一使用的仓库，必填。\n2、采购员为本次导入统一使用的采购员，选填。\n3、Excel 中其余字段都会作为订单明细导入。'
      "
      :download-template-url="downloadTemplate"
      :upload-url="upload"
      :form-data="formData"
      @confirm="(e) => $emit('confirm', e)"
    >
      <template #form>
        <j-border>
          <j-form bordered>
            <j-form-item label="仓库" required>
              <store-center-selector v-model:value="formData.scId" />
            </j-form-item>
            <j-form-item label="采购员">
              <user-selector v-model:value="formData.purchaserId" />
            </j-form-item>
          </j-form>
        </j-border>
      </template>
    </excel-importer>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import ExcelImporter from '@/components/ExcelImporterNew';
  import StoreCenterSelector from '@/components/Selector/StoreCenterSelector.vue';
  import UserSelector from '@/components/Selector/UserSelector.vue';
  import * as api from '@/api/sc/purchase/receive';
  import { createError } from '@/hooks/web/msg';

  export default defineComponent({
    name: 'PurchaseOrderQueryImporter',
    components: {
      ExcelImporter,
      StoreCenterSelector,
      UserSelector,
    },
    data() {
      return {
        formData: {
          scId: '',
          purchaserId: '',
        },
      };
    },
    methods: {
      openDialog() {
        this.formData = {
          scId: '',
          purchaserId: '',
        };
        this.$refs.importer.openDialog();
      },
      downloadTemplate() {
        return api.downloadQueryImportTemplate();
      },
      upload(params) {
        if (!params.scId) {
          createError('请先选择仓库！');
          return Promise.reject(new Error('请先选择仓库！'));
        }
        return api.importByQuery(params);
      },
    },
  });
</script>

<style lang="less"></style>
