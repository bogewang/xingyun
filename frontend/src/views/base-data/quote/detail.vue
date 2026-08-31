<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    wrap-class-name="quote-sheet-detail-modal-wrap"
    width="75%"
    title="查看"
    :style="{ top: '2px', paddingBottom: 0 }"
    :body-style="{ flex: 1, minHeight: 0, overflow: 'hidden' }"
  >
    <div
      v-if="visible"
      v-permission="['base-data:quote:query']"
      v-loading="loading"
      class="quote-sheet-detail-modal-content"
    >
      <div class="quote-sheet-detail-pane">
        <j-border title="报价单详情">
          <j-form bordered>
            <j-form-item label="名称" :span="6">{{ form.name }}</j-form-item>
            <j-form-item label="生效日期" :span="6"
              >{{ form.startDate }} 至 {{ form.endDate }}
            </j-form-item>
            <j-form-item label="状态" :span="6"
              >{{ form.status === 'ENABLED' ? '启用' : '停用' }}
            </j-form-item>
            <j-form-item label="备注" :span="12">{{ form.description }}</j-form-item>
          </j-form>
        </j-border>
        <div class="quote-sheet-detail-grid-wrap">
          <j-border title="报价商品">
            <vxe-grid :data="form.products" :columns="columns" height="100%" />
          </j-border>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="form-modal-footer">
        <a-space>
          <a-button
            v-permission="['base-data:quote:export']"
            :loading="loading"
            @click="exportDetails"
            >导出明细</a-button
          >
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </template>
  </a-modal>
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';
  import * as unitApi from '@/api/base-data/unit';
  import { resolveQuoteProductUnitName } from './quoteSheet';
  import { createSuccess } from '@/hooks/web/msg';

  export default defineComponent({
    name: 'QuoteSheetDetail',
    props: {
      id: { type: String, required: true },
    },
    data() {
      return {
        visible: false,
        loading: false,
        form: { products: [] },
        columns: [
          { type: 'seq', title: '序号', width: 60 },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
          { field: 'salePrice', title: '销售单价（元）', width: 140, align: 'right' },
        ],
      };
    },
    methods: {
      /** 打开报价单查看弹窗并加载详情数据。 */
      openDialog() {
        this.visible = true;
        this.$nextTick(() => this.loadData());
      },
      /** 关闭报价单查看弹窗。 */
      closeDialog() {
        this.visible = false;
      },
      /** 创建当前报价单的商品明细导出任务。 */
      exportDetails() {
        this.loading = true;
        api
          .exportDetail({ idList: [this.id] })
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      /** 加载报价单详情及单位名称映射。 */
      loadData() {
        this.loading = true;
        Promise.all([api.get(this.id), unitApi.query({ pageIndex: 1, pageSize: 1000 })])
          .then(([data, unitResult]) => {
            const unitNameMap = (unitResult.datas || []).reduce((result, item) => {
              result[item.id] = item.name;
              return result;
            }, {});
            this.form = {
              ...data,
              products: (data.products || []).map((item) => ({
                ...item,
                unit: resolveQuoteProductUnitName(item.unit, unitNameMap),
              })),
            };
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  });
</script>
<style scoped>
  :global(.quote-sheet-detail-modal-wrap .ant-modal) {
    top: 2px !important;
    padding-bottom: 0;
  }

  :global(.quote-sheet-detail-modal-wrap .ant-modal-content) {
    height: calc(100vh - 4px);
    display: flex;
    flex-direction: column;
  }

  :global(.quote-sheet-detail-modal-wrap .ant-modal-body) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .quote-sheet-detail-modal-content {
    height: 100%;
    overflow: hidden;
  }

  .quote-sheet-detail-pane {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .quote-sheet-detail-grid-wrap {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }
</style>
