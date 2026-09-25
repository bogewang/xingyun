<template>
  <a-modal
    v-model:open="quoteVisible"
    title="添加到报价单"
    :width="850"
    :confirm-loading="quoteSaving"
    :ok-button-props="{ disabled: quoteLoading || quoteLoadFailed }"
    :closable="!quoteSaving && !quoteLoading"
    :mask-closable="!quoteSaving && !quoteLoading"
    :cancel-button-props="{ disabled: quoteSaving || quoteLoading }"
    @ok="saveProductQuotes"
  >
    <a-alert v-if="quoteLoadFailed" type="error" message="报价加载失败，请关闭后重试" show-icon />
    <a-table
      :loading="quoteLoading"
      :data-source="quoteRows"
      :pagination="false"
      row-key="id"
      :scroll="{ y: 360 }"
      :columns="[
        { title: '报价单', dataIndex: 'name' },
        { title: '有效期', dataIndex: 'period', width: 220 },
        { title: '价格', dataIndex: 'salePrice', width: 160 },
        { title: '是否询价', dataIndex: 'inquiryProduct', width: 100 },
      ]"
      :row-selection="{
        selectedRowKeys: quoteSheetIds,
        onChange: selectQuoteRows,
        getCheckboxProps: quoteCheckboxProps,
      }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'salePrice'">
          <a-input-number
            v-model:value="record.salePrice"
            :min="0"
            :precision="2"
            string-mode
            :disabled="quoteSaving || !quoteSheetIds.includes(record.id)"
            style="width: 100%"
          />
        </template>
        <template v-else-if="column.dataIndex === 'inquiryProduct'">
          <a-switch
            v-model:checked="record.inquiryProduct"
            checked-children="是"
            un-checked-children="否"
            :disabled="quoteSaving || !quoteSheetIds.includes(record.id)"
          />
        </template>
      </template>
    </a-table>
    <p style="margin-top: 12px"
      >可修改已勾选报价单的价格和是否询价，{{
        deferred ? '确认后随商品保存。' : '点击确定后统一保存。'
      }}</p
    >
  </a-modal>
</template>
<script>
  import * as api from '@/api/base-data/product/info';
  import { createError, createSuccess } from '@/hooks/web/msg';

  export default {
    props: { deferred: Boolean, value: { type: Array, default: undefined } },
    emits: ['update:value', 'confirm'],
    /** 初始化弹窗编辑状态。 */
    data() {
      return {
        quoteVisible: false,
        quoteSaving: false,
        quoteLoading: false,
        quoteLoadFailed: false,
        quoteProductId: '',
        quoteRows: [],
        quoteSheetIds: [],
      };
    },
    methods: {
      /** 打开商品追加报价单窗口。 */
      async openDialog(productId) {
        this.quoteProductId = productId;
        this.quoteSheetIds = [];
        this.quoteRows = [];
        this.quoteVisible = true;
        this.quoteLoading = true;
        this.quoteLoadFailed = false;
        try {
          const [options, details] = await Promise.all([
            api.quoteOptions(),
            productId ? api.productQuoteDetails(productId) : Promise.resolve([]),
          ]);
          if (this.quoteProductId !== productId) return;
          const existing = new Map(details.map((item) => [item.quoteSheetId, item]));
          const selected = this.deferred && this.value !== undefined ? this.value : details;
          const values = new Map(selected.map((item) => [item.quoteSheetId, item]));
          this.quoteRows = options
            .map((item) => {
              const detail = values.get(item.id) || existing.get(item.id);
              return {
                ...item,
                period: `${item.startDate} ~ ${item.endDate}`,
                salePrice: detail ? detail.salePrice : 0,
                inquiryProduct: detail ? detail.inquiryProduct === true : true,
                existing: existing.has(item.id),
                selected: values.has(item.id) || existing.has(item.id),
              };
            })
            .sort((a, b) => Number(b.existing) - Number(a.existing));
          this.quoteSheetIds = this.quoteRows
            .filter((item) => item.selected)
            .map((item) => item.id);
        } catch {
          this.quoteLoadFailed = true;
        } finally {
          this.quoteLoading = false;
        }
      },
      /** 更新要追加的报价单选择。 */
      selectQuoteRows(keys) {
        this.quoteSheetIds = keys;
      },
      /** 既有报价只回显，防止取消勾选被误认为删除。 */
      quoteCheckboxProps(row) {
        return { disabled: row.existing || this.quoteSaving };
      },
      /** 一次保存所选报价单，失败时保留选择以便重试。 */
      async saveProductQuotes() {
        if (this.quoteSaving || this.quoteLoading || this.quoteLoadFailed) return;
        if (!this.quoteSheetIds.length && !this.deferred) {
          createError('请选择报价单！');
          return;
        }
        this.quoteSaving = true;
        try {
          const quotes = this.quoteRows
            .filter((item) => this.quoteSheetIds.includes(item.id))
            .map((item) => ({
              quoteSheetId: item.id,
              salePrice: item.salePrice,
              inquiryProduct: item.inquiryProduct,
            }));
          if (
            quotes.some(
              (item) =>
                item.salePrice === null || item.salePrice === '' || Number(item.salePrice) < 0,
            )
          ) {
            createError('请填写有效的非负价格！');
            return;
          }
          if (this.deferred) {
            this.$emit('update:value', quotes);
          } else {
            await api.saveProductQuotes(this.quoteProductId, quotes);
            createSuccess('保存成功！');
          }
          this.quoteVisible = false;
          this.$emit('confirm');
        } finally {
          this.quoteSaving = false;
        }
      },
    },
  };
</script>
