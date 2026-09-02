<template>
  <page-wrapper content-full-height fixed-height>
    <vxe-grid
      ref="grid"
      row-id="detailId"
      auto-resize
      resizable
      show-overflow
      height="auto"
      :proxy-config="proxyConfig"
      :columns="columns"
      :toolbar-config="toolbarConfig"
      :pager-config="pagerConfig"
    >
      <template #form>
        <j-border>
          <j-form bordered @keyup.enter="search">
            <j-form-item label="报价单名称">
              <a-input v-model:value="searchForm.quoteSheetName" allow-clear />
            </j-form-item>
            <j-form-item label="商品名称/编号">
              <a-input v-model:value="searchForm.productKeyword" allow-clear />
            </j-form-item>
            <j-form-item label="状态">
              <a-select v-model:value="searchForm.status" allow-clear>
                <a-select-option value="ENABLED">启用</a-select-option>
                <a-select-option value="DISABLED">停用</a-select-option>
              </a-select>
            </j-form-item>
            <j-form-item label="是否询价">
              <a-select v-model:value="searchForm.inquiryProduct" allow-clear>
                <a-select-option :value="true">是</a-select-option>
                <a-select-option :value="false">否</a-select-option>
              </a-select>
            </j-form-item>
            <j-form-item label="生效日期">
              <a-range-picker
                v-model:value="effectiveDateRange"
                value-format="YYYY-MM-DD"
                :placeholder="['开始日期', '结束日期']"
              />
            </j-form-item>
          </j-form>
        </j-border>
      </template>
      <template #toolbar_buttons>
        <a-space>
          <a-button @click="resetSearchForm">清空</a-button>
          <a-button type="primary" @click="search">查询</a-button>
        </a-space>
      </template>
      <template #status_default="{ row }">
        <a-tag :color="row.status === 'ENABLED' ? 'green' : 'default'">
          {{ row.status === 'ENABLED' ? '启用' : '停用' }}
        </a-tag>
      </template>
      <template #inquiry_default="{ row }">
        <a-tag :color="row.inquiryProduct ? 'green' : 'red'">
          {{ row.inquiryProduct ? '是' : '否' }}
        </a-tag>
      </template>
      <template #quote_sheet_name_default="{ row }">
        <a @click="openModifyPage(row.quoteSheetId)">{{ row.quoteSheetName }}</a>
      </template>
    </vxe-grid>
  </page-wrapper>
</template>

<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';
  import { buildSortPageVo } from '@/utils/utils';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { multiplePageMix } from '@/mixins/multiplePageMix';

  const createDefaultSearchForm = () => ({
    quoteSheetName: '',
    productKeyword: '',
    status: 'ENABLED',
    inquiryProduct: undefined,
  });

  export default defineComponent({
    name: 'QuoteSheetDetailList',
    mixins: [gridCollapseHeightMix, multiplePageMix],
    data() {
      return {
        searchForm: createDefaultSearchForm(),
        effectiveDateRange: [],
        toolbarConfig: { slots: { buttons: 'toolbar_buttons' } },
        pagerConfig: {
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        },
        columns: [
          { type: 'seq', title: '序号', width: 60 },
          {
            field: 'quoteSheetName',
            title: '报价单名称',
            width: 180,
            slots: { default: 'quote_sheet_name_default' },
          },
          { field: 'startDate', title: '生效开始日期', width: 130 },
          { field: 'endDate', title: '生效结束日期', width: 130 },
          { field: 'status', title: '状态', width: 90, slots: { default: 'status_default' } },
          { field: 'productCode', title: '商品编号', width: 140 },
          { field: 'productName', title: '商品名称', width: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
          { field: 'salePrice', title: '销售单价（元）', width: 140, align: 'right' },
          {
            field: 'inquiryProduct',
            title: '是否询价',
            width: 100,
            slots: { default: 'inquiry_default' },
          },
        ],
        proxyConfig: {
          props: { result: 'datas', total: 'totalCount' },
          ajax: {
            query: ({ page, sorts }) => api.queryDetail(this.buildQueryParams(page, sorts)),
          },
        },
      };
    },
    methods: {
      /** 刷新明细表格。 */
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      /** 还原明细查询条件。 */
      resetSearchForm() {
        this.searchForm = createDefaultSearchForm();
        this.effectiveDateRange = [];
        this.search();
      },
      /** 打开报价单修改页面。 */
      openModifyPage(quoteSheetId) {
        this.openChildPage(`/base-data/quote/modify/${quoteSheetId}`);
      },
      /** 组装明细查询请求。 */
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.searchForm,
          startDate: this.effectiveDateRange?.[0] || undefined,
          endDate: this.effectiveDateRange?.[1] || undefined,
        };
      },
    },
  });
</script>
