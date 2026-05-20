<template>
  <div v-permission="['report:sale-profit:product:query']">
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        id="SaleProfitProductReport"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        row-id="productId"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :pager-config="{}"
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
              <j-form-item label="商品名称">
                <a-input v-model:value="searchFormData.productName" allow-clear/>
              </j-form-item>
              <j-form-item label="规格型号">
                <a-input v-model:value="searchFormData.productSpec" allow-clear/>
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
              <j-form-item label="单据日期">
                <a-range-picker
                  v-model:value="orderDateRange"
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
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">搜索</a-button>
            <a-button
              v-permission="['report:sale-profit:product:export']"
              :icon="h(DownloadOutlined)"
              @click="exportList"
            >导出
            </a-button>
          </a-space>
        </template>

        <template #productName_default="{ row }">
          <a @click="openTrendChart(row)">{{ row.productName }}</a>
        </template>

        <template #profitRate_default="{ row }">
          {{ calcProfitRate(row.salesProfit, row.salesAmount) }}
        </template>
      </vxe-grid>

      <a-modal
        v-model:open="trendVisible"
        :title="trendTitle"
        width="900px"
        :footer="null"
        @cancel="disposeTrendChart"
      >
        <a-card title="趋势图 / 折线图" :bordered="false" class="trend-card">
          <div v-if="trendEmpty" class="chart-empty">
            <a-empty description="暂无趋势数据"/>
          </div>
          <div v-show="!trendEmpty" ref="trendChartRef" class="trend-chart"></div>
        </a-card>
      </a-modal>
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
import {
  buildVisibleSelectOptions,
  filterSelectOption,
  mergeSelectOptionMap,
  normalizeSelectValue,
} from '@/utils/searchSelect';
import {requestCustomerSelectOptions} from '@/utils/labelSelect';
import {createSuccess} from '@/hooks/web/msg';

export default defineComponent({
  name: 'SaleProfitProductReport',
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
      trendVisible: false,
      trendTitle: '商品销售趋势',
      trendDatas: [],
      trendChart: null,
      trendEmpty: false,
      searchFormData: {
        productName: '',
        productSpec: '',
        customerId: undefined,
      },
      orderDateRange: this.getDefaultOrderDateRange(),
      customerOptions: [],
      customerOptionMap: {},
      summary: {
        saleNum: 0,
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
        {
          field: 'productName',
          title: '商品名称',
          minWidth: 180,
          sortable: true,
          slots: {default: 'productName_default'},
        },
        {field: 'spec', title: '规格型号', minWidth: 120},
        {field: 'unit', title: '单位', width: 90},
        {
          field: 'saleNum',
          title: '销售数量',
          align: 'right',
          width: 120,
          sortable: true,
          formatter: ({cellValue}) => this.formatQuantity(cellValue),
        },
        {
          field: 'salePrice',
          title: '销售均价',
          align: 'right',
          width: 120,
          formatter: ({cellValue}) => this.formatAmount(cellValue),
        },
        {
          field: 'salesAmount',
          title: '销售金额',
          align: 'right',
          width: 130,
          sortable: true,
          formatter: ({cellValue}) => this.formatAmount(cellValue),
        },
        {
          field: 'salesCost',
          title: '销售成本',
          align: 'right',
          width: 130,
          sortable: true,
          formatter: ({cellValue}) => this.formatAmount(cellValue),
        },
        {
          field: 'salesProfit',
          title: '销售毛利',
          align: 'right',
          width: 130,
          sortable: true,
          formatter: ({cellValue}) => this.formatAmount(cellValue),
        },
        {
          field: 'profitRate',
          title: '销售毛利率',
          align: 'right',
          width: 130,
          slots: {default: 'profitRate_default'},
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
              api.queryProductProfitSummary(params),
              api.queryProductProfit(params),
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
        {key: 'saleNum', title: '销售数量', value: this.formatQuantity(this.summary.saleNum)},
        {key: 'salesAmount', title: '销售额', value: this.formatCurrency(this.summary.salesAmount)},
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
    this.disposeTrendChart();
  },
  methods: {
    search() {
      this.$refs.grid.commitProxy('reload');
    },
    resetSearchForm() {
      this.searchFormData = {
        productName: '',
        productSpec: '',
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
          if (column.field === 'productName') {
            return '合计';
          }
          if (column.field === 'saleNum') {
            return this.formatQuantity(this.sumBy(data, 'saleNum'));
          }
          if (['salesAmount', 'salesCost', 'salesProfit'].includes(column.field)) {
            return this.formatAmount(this.sumBy(data, column.field));
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
      return Number(value || 0).toFixed(2).replace(/\.?0+$/, '');
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
        .exportProductProfit(this.buildSearchFormData())
        .then(() => {
          createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    openTrendChart(row) {
      this.loading = true;
      api
        .queryProductProfitTrend({
          ...this.buildSearchFormData(),
          productId: row.productId,
        })
        .then((res) => {
          this.trendDatas = res || [];
          this.trendTitle = `商品销售趋势 - ${row.productName}`;
          this.trendVisible = true;
          this.$nextTick(() => this.renderTrendChart());
        })
        .finally(() => {
          this.loading = false;
        });
    },
    renderTrendChart() {
      this.trendEmpty = this.trendDatas.length === 0;
      if (this.trendEmpty) {
        this.disposeTrendChart();
        return;
      }

      const chartDom = this.$refs.trendChartRef;
      if (!chartDom) {
        return;
      }

      if (!this.trendChart) {
        this.trendChart = echarts.init(chartDom);
      }

      const dates = this.trendDatas.map((item) => item.orderDate);
      const salesAmount = this.trendDatas.map((item) => [
        item.orderDate,
        Number(item.salesAmount || 0),
      ]);
      const salesProfit = this.trendDatas.map((item) => [
        item.orderDate,
        Number(item.salesProfit || 0),
      ]);
      const profitRate = this.trendDatas.map((item) => [
        item.orderDate,
        Number(item.profitRate || 0),
      ]);

      this.trendChart.setOption({
        color: ['#1677ff', '#52c41a', '#fa8c16'],
        tooltip: {
          trigger: 'axis',
          triggerOn: 'mousemove|click',
          renderMode: 'html',
          appendToBody: true,
          confine: false,
          enterable: true,
          extraCssText: 'z-index: 3000; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);',
          axisPointer: {
            type: 'line',
            triggerTooltip: true,
          },
          formatter: (params) => {
            const items = Array.isArray(params) ? params : [params];
            const title = this.formatChartDate(items?.[0]?.axisValue || items?.[0]?.name || '');
            const lines = items.map((item) => {
              const itemValue = Array.isArray(item.value) ? item.value[1] : item.value;
              const value =
                item.seriesName === '毛利率'
                  ? `${Number(itemValue || 0).toFixed(2)}%`
                  : this.formatCurrency(itemValue);
              return `${item.marker}${item.seriesName}：${value}`;
            });
            return [`日期：${title}`, ...lines].join('<br/>');
          },
        },
        legend: {
          top: 0,
          data: ['销售金额', '毛利', '毛利率'],
        },
        grid: {
          top: 52,
          left: 56,
          right: 56,
          bottom: 48,
          containLabel: true,
        },
        xAxis: {
          type: 'time',
          boundaryGap: false,
          triggerEvent: true,
          min: dates[0],
          max: dates[dates.length - 1],
          axisLabel: {
            formatter: (value) => this.formatChartDate(value),
          },
          axisPointer: {
            show: true,
            type: 'line',
            triggerTooltip: true,
          },
        },
        yAxis: [
          {
            type: 'value',
            name: '金额',
            axisLabel: {
              formatter: (value) => this.formatAmount(value),
            },
          },
          {
            type: 'value',
            name: '毛利率',
            axisLabel: {
              formatter: '{value}%',
            },
          },
        ],
        series: [
          {
            name: '销售金额',
            type: 'line',
            smooth: true,
            showSymbol: true,
            triggerLineEvent: true,
            symbolSize: 6,
            emphasis: {
              focus: 'series',
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) =>
                `日期：${this.formatChartDate(params.value?.[0])}<br/>销售金额：${this.formatCurrency(
                  params.value?.[1],
                )}`,
            },
            data: salesAmount,
          },
          {
            name: '毛利',
            type: 'line',
            smooth: true,
            showSymbol: true,
            triggerLineEvent: true,
            symbolSize: 6,
            emphasis: {
              focus: 'series',
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) =>
                `日期：${this.formatChartDate(params.value?.[0])}<br/>毛利：${this.formatCurrency(
                  params.value?.[1],
                )}`,
            },
            data: salesProfit,
          },
          {
            name: '毛利率',
            type: 'line',
            smooth: true,
            showSymbol: true,
            triggerLineEvent: true,
            symbolSize: 6,
            yAxisIndex: 1,
            emphasis: {
              focus: 'series',
            },
            tooltip: {
              trigger: 'item',
              formatter: (params) =>
                `日期：${this.formatChartDate(params.value?.[0])}<br/>毛利率：${this.formatAmount(
                  params.value?.[1],
                )}%`,
            },
            data: profitRate,
          },
        ],
      });
      this.$nextTick(() => {
        this.trendChart?.resize();
        this.trendChart?.dispatchAction({
          type: 'showTip',
          seriesIndex: 0,
          dataIndex: 0,
        });
        });
    },
    formatChartDate(value) {
      if (!value) {
        return '';
      }
      return moment(value).format('YYYY-MM-DD');
    },
    disposeTrendChart() {
      if (this.trendChart) {
        this.trendChart.dispose();
        this.trendChart = null;
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

.trend-card {
  margin: -8px -12px -16px;
}

.trend-chart {
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
