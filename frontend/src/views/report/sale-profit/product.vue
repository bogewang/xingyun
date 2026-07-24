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
              <j-form-item label="商品名称">
                <a-input v-model:value="searchFormData.productName" allow-clear />
              </j-form-item>
              <j-form-item label="规格型号">
                <a-input v-model:value="searchFormData.productSpec" allow-clear />
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
            <a-button
              v-permission="['report:sale-profit:product:query']"
              :icon="h(SyncOutlined)"
              @click="openCostRefresh"
              >成本重算
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
        <div v-if="trendEmpty" class="chart-empty">
          <a-empty description="暂无趋势数据" />
        </div>
        <div v-if="!trendEmpty" class="trend-chart-wrapper">
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </a-modal>

      <a-modal
        v-model:open="costRefreshVisible"
        title="成本重算"
        :confirm-loading="costRefreshLoading"
        @ok="executeCostRefresh"
      >
        <a-form layout="vertical">
          <a-form-item label="日期范围">
            <a-range-picker
              v-model:value="costRefreshDateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </page-wrapper>
  </div>
</template>

<script>
  import { h, defineComponent, markRaw } from 'vue';
  import moment from 'moment';
  import { SearchOutlined, DownloadOutlined, SyncOutlined } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import { monthEndRecalculate } from '@/api/sc/sale/out';
  import { buildSortPageVo } from '@/utils/utils';
  import * as echarts from 'echarts';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions } from '@/utils/labelSelect';
  import { createSuccess } from '@/hooks/web/msg';

  const TREND_SERIES_NAMES = ['销售金额', '毛利', '毛利率'];

  export default defineComponent({
    name: 'SaleProfitProductReport',
    setup() {
      return {
        h,
        SearchOutlined,
        DownloadOutlined,
        SyncOutlined,
      };
    },
    data() {
      return {
        loading: false,
        trendVisible: false,
        trendTitle: '商品销售趋势',
        trendDatas: [],
        trendEmpty: false,
        costRefreshVisible: false,
        costRefreshLoading: false,
        costRefreshDateRange: this.getDefaultOrderDateRange(),
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
          { type: 'seq', title: '序号', width: 50 },
          {
            field: 'productName',
            title: '商品名称',
            minWidth: 180,
            sortable: true,
            slots: { default: 'productName_default' },
          },
          { field: 'productCategory', title: '商品分类', width: 100 },
          { field: 'spec', title: '规格型号', minWidth: 120 },
          { field: 'unit', title: '单位', width: 90 },
          {
            field: 'saleNum',
            title: '销售数量',
            align: 'right',
            width: 100,
            sortable: true,
            formatter: ({ cellValue }) => this.formatQuantity(cellValue),
          },
          {
            field: 'salePrice',
            title: '销售均价',
            align: 'right',
            width: 120,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          {
            field: 'purchasePrice',
            title: '采购均价',
            align: 'right',
            width: 120,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          {
            field: 'salesAmount',
            title: '销售金额',
            align: 'right',
            width: 130,
            sortable: true,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          {
            field: 'salesCost',
            title: '销售成本',
            align: 'right',
            width: 130,
            sortable: true,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          {
            field: 'salesProfit',
            title: '销售毛利',
            align: 'right',
            width: 130,
            sortable: true,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          {
            field: 'profitRate',
            title: '销售毛利率',
            align: 'right',
            width: 130,
            slots: { default: 'profitRate_default' },
          },
        ],
        proxyConfig: {
          props: {
            result: 'datas',
            total: 'totalCount',
          },
          ajax: {
            query: async ({ page, sorts }) => {
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
          { key: 'saleNum', title: '销售数量', value: this.formatQuantity(this.summary.saleNum) },
          {
            key: 'salesAmount',
            title: '销售额',
            value: this.formatCurrency(this.summary.salesAmount),
          },
          { key: 'salesCost', title: '成本', value: this.formatCurrency(this.summary.salesCost) },
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
          { key: 'netProfit', title: '净利润', value: this.formatCurrency(this.summary.netProfit) },
          { key: 'otherFee', title: '其他费用', value: this.formatCurrency(this.summary.otherFee) },
        ];
      },
    },
    created() {
      this._trendChart = null;
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
        return { ...buildSortPageVo(page, sorts), ...this.buildSearchFormData() };
      },
      buildSearchFormData() {
        return {
          ...this.searchFormData,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
        };
      },
      footerMethod({ columns, data }) {
        const saleNum = this.sumBy(data, 'saleNum');
        const salesAmount = this.sumBy(data, 'salesAmount');
        const salesCost = this.sumBy(data, 'salesCost');
        const salesProfit = salesAmount - salesCost;
        return [
          columns.map((column) => {
            if (column.field === 'productName') return '合计';
            if (column.field === 'saleNum') return this.formatQuantity(saleNum);
            if (column.field === 'salePrice') return this.calcAverage(salesAmount, saleNum);
            if (column.field === 'purchasePrice') return this.calcAverage(salesCost, saleNum);
            if (column.field === 'salesAmount') return this.formatAmount(salesAmount);
            if (column.field === 'salesCost') return this.formatAmount(salesCost);
            if (column.field === 'salesProfit') return this.formatAmount(salesProfit);
            if (column.field === 'profitRate') return this.calcProfitRate(salesProfit, salesAmount);
            return '';
          }),
        ];
      },
      sumBy(data, field) {
        return (data || []).reduce((sum, item) => {
          return sum + this.toNumber(item?.[field]);
        }, 0);
      },
      toNumber(value) {
        const valueNumber = Number(value || 0);
        return Number.isNaN(valueNumber) ? 0 : valueNumber;
      },
      calcProfitRate(profit, amount) {
        const amountNumber = this.toNumber(amount);
        if (!amountNumber) return this.formatPercent(0);
        return this.formatPercent((this.toNumber(profit) / amountNumber) * 100);
      },
      calcAverage(amount, num) {
        const numNumber = this.toNumber(num);
        if (!numNumber) return this.formatAmount(0);
        return this.formatAmount(this.toNumber(amount) / numNumber);
      },
      formatAmount(value) {
        return this.toNumber(value).toFixed(2);
      },
      formatCurrency(value) {
        return '￥' + this.formatAmount(value);
      },
      formatPercent(value) {
        return this.formatAmount(value) + '%';
      },
      formatQuantity(value) {
        return this.toNumber(value)
          .toFixed(2)
          .replace(/\.?0+$/, '');
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
          .then(() => createSuccess('创建导出任务成功，请前往“导出中心”进行下载。'))
          .finally(() => (this.loading = false));
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
          .finally(() => (this.loading = false));
      },
      renderTrendChart() {
        this.trendEmpty = this.trendDatas.length === 0;
        if (this.trendEmpty) {
          this.disposeTrendChart();
          return;
        }

        const chartDom = this.$refs.trendChartRef;
        if (!chartDom) return;

        this.disposeTrendChart();
        this._trendChart = markRaw(echarts.init(chartDom));

        const dates = this.trendDatas.map((i) => this.formatChartDate(i.orderDate));
        const salesAmount = this.trendDatas.map((i) => this.toNumber(i.salesAmount));
        const salesProfit = this.trendDatas.map((i) => this.toNumber(i.salesProfit));
        const profitRate = this.trendDatas.map((i) => this.toNumber(i.profitRate));

        const option = {
          tooltip: {
            trigger: 'axis',
            confine: true,
            axisPointer: { type: 'cross', snap: true, z: 9999 },
            formatter: this.formatTrendTooltip,
          },
          legend: { data: TREND_SERIES_NAMES, top: 5 },
          grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
          xAxis: { type: 'category', boundaryGap: false, data: dates },
          yAxis: [
            { type: 'value', name: '金额', axisLabel: { formatter: (v) => this.formatAmount(v) } },
            {
              type: 'value',
              name: '毛利率',
              axisLabel: { formatter: (v) => this.formatPercent(v) },
            },
          ],
          series: [
            { name: TREND_SERIES_NAMES[0], type: 'line', smooth: true, data: salesAmount },
            { name: TREND_SERIES_NAMES[1], type: 'line', smooth: true, data: salesProfit },
            {
              name: TREND_SERIES_NAMES[2],
              type: 'line',
              smooth: true,
              yAxisIndex: 1,
              data: profitRate,
            },
          ],
        };

        this._trendChart.setOption(option);

        setTimeout(() => {
          this._trendChart?.resize();
        }, 200);
      },
      formatChartDate(value) {
        return value ? moment(value).format('YYYY-MM-DD') : '';
      },
      formatTrendTooltip(params) {
        const items = Array.isArray(params) ? params : [params];
        const title = items[0]?.axisValue || '';
        const lines = items.map((item) => {
          const value =
            item.seriesName === TREND_SERIES_NAMES[2]
              ? this.formatPercent(item.value)
              : this.formatCurrency(item.value);
          return `${item.marker}${item.seriesName}：${value}`;
        });
        return [`日期：${title}`, ...lines].join('<br/>');
      },
      openCostRefresh() {
        this.costRefreshDateRange = this.getDefaultOrderDateRange();
        this.costRefreshVisible = true;
      },
      executeCostRefresh() {
        const [beginDate, endDate] = this.costRefreshDateRange || [];
        if (!beginDate || !endDate) {
          return;
        }
        this.costRefreshLoading = true;
        monthEndRecalculate({
          beginDate,
          endDate,
        })
          .then((res) => {
            createSuccess(
              `重算完成：更新单据 ${res.updatedSheetCount} 条，明细 ${res.updatedDetailCount} 条` +
                (res.notFilledCount > 0 ? `，${res.notFilledCount} 条未填充` : ''),
            );
            this.costRefreshVisible = false;
            this.search();
          })
          .finally(() => {
            this.costRefreshLoading = false;
          });
      },
      disposeTrendChart() {
        if (this._trendChart) {
          this._trendChart.dispose();
          this._trendChart = null;
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

  .trend-chart-wrapper {
    position: relative;
  }

  .trend-chart {
    width: 100%;
    height: 500px !important;
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
