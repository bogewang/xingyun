<template>
  <div v-permission="['report:sale-profit:query']">
    <page-wrapper content-full-height fixed-height>
      <div class="summary-panel">
        <div v-for="item in summaryItems" :key="item.key" class="summary-item">
          <div class="summary-title">{{ item.title }}</div>
          <div class="summary-value">{{ item.value }}</div>
        </div>
      </div>

      <vxe-grid
        id="SaleProfitSheetReport"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        row-id="id"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :pager-config="{}"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="$refs.grid.refreshColumn()" @keyup.enter="search">
              <j-form-item label="单据日期">
                <a-range-picker
                  v-model:value="orderDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                />
              </j-form-item>

              <j-form-item label="客户">
                <a-select
                  v-model:value="searchFormData.customerId"
                  allow-clear
                  show-search
                  :filter-option="filterSelectOption"
                  :options="customerOptions"
                  placeholder="请选择客户"
                  @focus="loadCustomerOptions()"
                  @search="loadCustomerOptions"
                  @change="onCustomerChange"
                />
              </j-form-item>

              <j-form-item label="单据类型">
                <a-select v-model:value="sheetType" disabled>
                  <a-select-option value="saleOut">销售出库单</a-select-option>
                </a-select>
              </j-form-item>

              <template #more>
                <j-form-item label="单据编号">
                  <a-input v-model:value="searchFormData.code" allow-clear/>
                </j-form-item>
                <j-form-item label="商品名称">
                  <a-input v-model:value="searchFormData.productName" allow-clear/>
                </j-form-item>
              </template>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button @click="resetSearchForm">清空</a-button>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">搜索</a-button>
            <a-button
              v-permission="['report:sale-profit:export']"
              :icon="h(DownloadOutlined)"
              @click="exportList"
            >导出
            </a-button
            >
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a @click="viewDetail(row.id)">{{ row.code }}</a>
        </template>

        <template #salesCost_default="{ row }">
          {{ formatAmount(calcSalesCost(row)) }}
        </template>

        <template #profitRate_default="{ row }">
          {{ calcProfitRate(row.totalProfit, row.totalAmount) }}
        </template>
      </vxe-grid>

      <detail :id="id" ref="viewDialog"/>
    </page-wrapper>
  </div>
</template>

<script>
import {h, defineComponent} from 'vue';
import moment from 'moment';
import {SearchOutlined, DownloadOutlined} from '@ant-design/icons-vue';
import * as api from '@/api/sc/sale/out';
import Detail from '@/views/sc/sale/out/detail.vue';
import {buildSortPageVo} from '@/utils/utils';
import {
  buildVisibleSelectOptions,
  filterSelectOption,
  mergeSelectOptionMap,
  normalizeSelectValue,
} from '@/utils/searchSelect';
import {requestCustomerSelectOptions} from '@/utils/labelSelect';
import {createSuccess} from '@/hooks/web/msg';

export default defineComponent({
  name: 'SaleProfitSheetReport',
  components: {
    Detail,
  },
  setup() {
    return {
      h,
      SearchOutlined,
      DownloadOutlined,
    };
  },
  data() {
    return {
      loading: false,
      id: '',
      sheetType: 'saleOut',
      searchFormData: {
        code: '',
        productName: '',
        customerId: undefined,
      },
      orderDateRange: this.getDefaultOrderDateRange(),
      customerOptions: [],
      customerOptionMap: {},
      summary: {
        saleCount: 0,
        salesAmount: 0,
        salesCost: 0,
        salesProfit: 0,
        otherIncome: 0,
        otherExpense: 0,
        netProfit: 0,
        otherFee: 0,
      },
      toolbarConfig: {
        slots: {
          buttons: 'toolbar_buttons',
        },
      },
      tableColumn: [
        {field: 'orderDate', title: '单据日期', width: 140},
        {
          field: 'code',
          title: '单据编号',
          width: 180,
          sortable: true,
          slots: {default: 'code_default'},
        },
        {field: 'customerName', title: '客户', minWidth: 160},
        {field: 'totalAmount', title: '销售收入', align: 'right', width: 120},
        {
          field: 'salesCost',
          title: '销售成本',
          align: 'right',
          width: 120,
          slots: {default: 'salesCost_default'},
        },
        {
          field: 'totalProfit', title: '销售毛利', align: 'right', width: 120,
          formatter: ({ cellValue }) => this.formatAmount(cellValue)
        },
        {
          field: 'profitRate',
          title: '毛利率',
          align: 'right',
          width: 100,
          slots: {default: 'profitRate_default'},
        },
        {field: 'paidAmount', title: '实收金额', align: 'right', width: 120},
        {
          field: 'otherFee',
          title: '其他费用',
          align: 'right',
          width: 120,
          formatter: () => '0.00',
        },
      ],
      proxyConfig: {
        props: {
          result: 'datas',
          total: 'totalCount',
        },
        ajax: {
          query: async ({page, sorts}) => {
            const params = this.buildQueryParams(page, sorts);
            const [summary, result] = await Promise.all([
              api.queryProfitSummary(params),
              api.queryProfit(params),
            ]);
            this.summary = Object.assign({}, this.summary, summary || {});
            return result;
          },
        },
      },
    };
  },
  computed: {
    summaryItems() {
      return [
        {
          key: 'saleCount',
          title: '销售笔数',
          value: this.formatQuantity(this.summary.saleCount),
        },
        {
          key: 'salesAmount',
          title: '销售额',
          value: this.formatCurrency(this.summary.salesAmount),
        },
        {key: 'salesCost', title: '成本', value: this.formatCurrency(this.summary.salesCost)},
        {
          key: 'salesProfit',
          title: '销售毛利',
          value: this.formatCurrency(this.summary.salesProfit),
        },
        {
          key: 'profitRate',
          title: '毛利率',
          value: this.calcProfitRate(this.summary.salesProfit, this.summary.salesAmount),
        },
        {
          key: 'otherIncome',
          title: '其他收入',
          value: this.formatCurrency(this.summary.otherIncome),
        },
        {
          key: 'otherExpense',
          title: '其他支出',
          value: this.formatCurrency(this.summary.otherExpense),
        },
        {key: 'netProfit', title: '净利润', value: this.formatCurrency(this.summary.netProfit)},
        {key: 'otherFee', title: '其他费用', value: this.formatCurrency(this.summary.otherFee)},
      ];
    },
  },
  methods: {
    search() {
      this.$refs.grid.commitProxy('reload');
    },
    resetSearchForm() {
      this.searchFormData = {
        code: '',
        productName: '',
        customerId: undefined,
      };
      this.orderDateRange = this.getDefaultOrderDateRange();
      this.search();
    },
    getDefaultOrderDateRange() {
      return [moment().startOf('month').format('YYYY-MM-DD'), moment().format('YYYY-MM-DD')];
    },
    buildQueryParams(page, sorts) {
      return {
        ...buildSortPageVo(page, sorts),
        ...this.buildSearchFormData(),
      };
    },
    buildSearchFormData() {
      return {
        ...this.searchFormData,
        orderDateStart: this.orderDateRange?.[0] || '',
        orderDateEnd: this.orderDateRange?.[1] || '',
      };
    },
    footerMethod({columns, data}) {
      return [
        columns.map((column) => {
          if (column.field === 'totalAmount') {
            return this.formatAmount(this.sumBy(data, 'totalAmount'));
          }
          if (column.field === 'salesCost') {
            return this.formatAmount(data.reduce((sum, row) => sum + this.calcSalesCost(row), 0));
          }
          if (column.field === 'totalProfit') {
            return this.formatAmount(this.sumBy(data, 'totalProfit'));
          }
          if (column.field === 'paidAmount') {
            return this.formatAmount(this.sumBy(data, 'paidAmount'));
          }
          if (column.field === 'otherFee') {
            return '0.00';
          }
          if (column.field === 'orderDate') {
            return '合计';
          }
          return '';
        }),
      ];
    },
    sumBy(data, field) {
      return (data || []).reduce((sum, item) => {
        const value = Number(item?.[field] || 0);
        return sum + (Number.isNaN(value) ? 0 : value);
      }, 0);
    },
    calcSalesCost(row) {
      return Number(row?.totalAmount || 0) - Number(row?.totalProfit || 0);
    },
    calcProfitRate(profit, amount) {
      const amountNumber = Number(amount || 0);
      if (!amountNumber) {
        return '0.00%';
      }
      return ((Number(profit || 0) / amountNumber) * 100).toFixed(2) + '%';
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2);
    },
    formatCurrency(value) {
      return '￥' + this.formatAmount(value);
    },
    formatQuantity(value) {
      return Number(value || 0).toFixed(0);
    },
    filterSelectOption(input, option) {
      return filterSelectOption(input, option);
    },
    async loadCustomerOptions(keyword = '') {
      const options = await requestCustomerSelectOptions(keyword);
      const optionMap = mergeSelectOptionMap(this.customerOptionMap, options);

      this.customerOptionMap = optionMap;
      this.customerOptions = buildVisibleSelectOptions(
        this.searchFormData.customerId,
        optionMap,
        options,
      );
    },
    onCustomerChange(value) {
      this.searchFormData.customerId = normalizeSelectValue(value, this.customerOptionMap);
    },
    exportList() {
      this.loading = true;
      api
        .exportProfit(this.buildSearchFormData())
        .then(() => {
          createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    viewDetail(id) {
      this.id = id;
      this.$nextTick(() => this.$refs.viewDialog.openDialog());
    },
  },
});
</script>

<style lang="less" scoped>
.summary-panel {
  display: grid;
  grid-template-columns: repeat(9, minmax(110px, 1fr));
  gap: 0;
  margin-bottom: 12px;
  padding: 20px 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgb(0 0 0 / 8%);
}

.summary-item {
  min-width: 0;
  text-align: center;
}

.summary-title {
  color: #52616f;
  font-size: 14px;
  line-height: 22px;
}

.summary-value {
  margin-top: 8px;
  color: #2f3f48;
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
  white-space: nowrap;
}

@media (max-width: 1400px) {
  .summary-panel {
    grid-template-columns: repeat(3, minmax(140px, 1fr));
    row-gap: 16px;
  }
}
</style>
