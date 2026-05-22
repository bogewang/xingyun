<template>
  <div v-permission="['report:sale-profit:query']">
    <page-wrapper content-full-height fixed-height>
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
        :pager-config="{
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        }"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
      >
        <template #form>
          <div class="summary-panel">
            <div v-for="item in summaryItems" :key="item.key" class="summary-item">
              <div class="summary-title">{{ item.title }}</div>
              <div class="summary-value">{{ item.value }}</div>
            </div>
          </div>

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
          <a-space size="small">
            <a @click="viewDetail(row.id)">{{ row.code }}</a>
            <a @click="openSheetChart(row)">饼图</a>
          </a-space>
        </template>

        <template #salesCost_default="{ row }">
          {{ formatAmount(calcSalesCost(row)) }}
        </template>

        <template #profitRate_default="{ row }">
          {{ calcProfitRate(row.totalProfit, row.totalAmount) }}
        </template>
      </vxe-grid>

      <a-modal
        v-model:open="sheetChartVisible"
        :title="sheetChartTitle"
        width="760px"
        :footer="null"
        @cancel="disposeSheetChart"
      >
        <div class="chart-toolbar">
          <a-button v-if="sheetChartLevel === 'product'" size="small" @click="renderCategoryPie">
            返回分类
          </a-button>
          <span class="chart-tip">{{ sheetChartTip }}</span>
        </div>
        <div v-if="sheetChartEmpty" class="chart-empty">
          <a-empty description="暂无商品销售数据"/>
        </div>
        <div v-show="!sheetChartEmpty" ref="sheetPieChartRef" class="chart-container"></div>
      </a-modal>

      <detail :id="id" ref="viewDialog"/>
    </page-wrapper>
  </div>
</template>

<script>
import {h, defineComponent} from 'vue';
import moment from 'moment';
import {SearchOutlined, DownloadOutlined} from '@ant-design/icons-vue';
import * as api from '@/api/sc/sale/out';
import {buildSortPageVo} from '@/utils/utils';
import echarts from '/@/utils/lib/echarts';
import Detail from '@/views/sc/sale/out/detail.vue';
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
      sheetChartVisible: false,
      sheetChartTitle: '商品分类销售额占比',
      sheetChartTip: '点击分类查看商品占比',
      sheetChartLevel: 'category',
      sheetChartDetails: [],
      sheetPieChart: null,
      sheetChartEmpty: false,
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
        {type: 'seq', title: '序号', width: 50},
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
  beforeUnmount() {
    this.disposeSheetChart();
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
      const totalAmount = this.sumBy(data, 'totalAmount');
      const totalProfit = this.sumBy(data, 'totalProfit');
      return [
        columns.map((column) => {
          if (column.field === 'totalAmount') {
            return this.formatAmount(totalAmount);
          }
          if (column.field === 'salesCost') {
            return this.formatAmount(data.reduce((sum, row) => sum + this.calcSalesCost(row), 0));
          }
          if (column.field === 'totalProfit') {
            return this.formatAmount(totalProfit);
          }
          if (column.field === 'profitRate') {
            return this.calcProfitRate(totalProfit, totalAmount);
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
    openSheetChart(row) {
      this.loading = true;
      api
        .get(row.id)
        .then((res) => {
          this.sheetChartDetails = res?.details || [];
          this.sheetChartTitle = `商品分类销售额占比 - ${row.code} - ${row.orderDate || ''} - ${
            row.customerName || ''
          }`;
          this.sheetChartVisible = true;
          this.$nextTick(() => this.renderCategoryPie());
        })
        .finally(() => {
          this.loading = false;
        });
    },
    renderCategoryPie() {
      this.sheetChartLevel = 'category';
      this.sheetChartTip = '点击分类查看商品占比';
      const data = this.buildPieData((detail) => detail.categoryName || '未分类');
      this.renderPie(data, '商品分类销售额占比', (params) => {
        this.renderProductPie(params.name);
      });
    },
    renderProductPie(categoryName) {
      this.sheetChartLevel = 'product';
      this.sheetChartTip = `${categoryName} 下各商品销售额占比`;
      const data = this.buildPieData(
        (detail) => detail.productName || '未命名商品',
        (detail) => (detail.categoryName || '未分类') === categoryName,
      );
      this.renderPie(data, `${categoryName} - 商品销售额占比`);
    },
    buildPieData(getName, filterDetail) {
      const dataMap = {};
      (this.sheetChartDetails || [])
        .filter((detail) => !filterDetail || filterDetail(detail))
        .forEach((detail) => {
          const name = getName(detail);
          dataMap[name] = (dataMap[name] || 0) + this.calcDetailSalesAmount(detail);
        });

      return Object.keys(dataMap)
        .map((name) => ({
          name,
          value: Number(dataMap[name].toFixed(2)),
        }))
        .filter((item) => item.value !== 0)
        .sort((prev, next) => next.value - prev.value);
    },
    calcDetailSalesAmount(detail) {
      const outNum = Number(detail?.outNum ?? detail?.orderNum ?? 0);
      return Number(detail?.taxPrice || 0) * (Number.isNaN(outNum) ? 0 : outNum);
    },
    renderPie(data, title, clickHandler) {
      this.sheetChartEmpty = data.length === 0;
      if (this.sheetChartEmpty) {
        this.disposeSheetChart();
        return;
      }

      const chartDom = this.$refs.sheetPieChartRef;
      if (!chartDom) {
        return;
      }

      if (!this.sheetPieChart) {
        this.sheetPieChart = echarts.init(chartDom);
      }
      this.sheetPieChart.off('click');
      if (clickHandler) {
        this.sheetPieChart.on('click', clickHandler);
      }

      this.sheetPieChart.setOption({
        color: ['#1677ff', '#13c2c2', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96'],
        title: {
          text: title,
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 16,
            fontWeight: 500,
          },
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) =>
            `${params.name}<br/>销售额：${this.formatCurrency(params.value)}<br/>占比：${params.percent}%`,
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 0,
          top: 36,
          bottom: 12,
        },
        series: [
          {
            name: '销售额',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['40%', '56%'],
            avoidLabelOverlap: true,
            label: {
              formatter: '{b}\n{d}%',
            },
            data,
          },
        ],
      });
      this.$nextTick(() => this.sheetPieChart?.resize());
    },
    disposeSheetChart() {
      if (this.sheetPieChart) {
        this.sheetPieChart.dispose();
        this.sheetPieChart = null;
      }
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

.chart-toolbar {
  display: flex;
  align-items: center;
  min-height: 32px;
  margin-bottom: 8px;
}

.chart-tip {
  color: #8c8c8c;
  font-size: 13px;
}

.chart-toolbar .ant-btn + .chart-tip {
  margin-left: 12px;
}

.chart-container {
  width: 100%;
  height: 420px;
}

.chart-empty {
  padding: 64px 0;
}

@media (max-width: 1400px) {
  .summary-panel {
    grid-template-columns: repeat(3, minmax(140px, 1fr));
    row-gap: 16px;
  }
}
</style>
