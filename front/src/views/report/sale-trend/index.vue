<template>
  <div v-permission="['report:sale-trend:query']">
    <page-wrapper content-full-height fixed-height>
      <div v-loading="loading" class="sale-trend-page">
        <j-border>
          <j-form bordered @keyup.enter="search">
            <j-form-item label="销售日期">
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
            <j-form-item label="商品名称">
              <a-input v-model:value="searchFormData.productName" allow-clear />
            </j-form-item>
          </j-form>
        </j-border>

        <div class="toolbar">
          <a-space>
            <a-button @click="resetSearchForm">清空</a-button>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">搜索</a-button>
          </a-space>
        </div>

        <div v-if="trendEmpty" class="chart-empty">
          <a-empty description="暂无趋势数据" />
        </div>
        <div v-show="!trendEmpty" class="trend-chart-wrapper">
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </div>
    </page-wrapper>
  </div>
</template>

<script>
  import { defineComponent, h, markRaw } from 'vue';
  import moment from 'moment';
  import { SearchOutlined } from '@ant-design/icons-vue';
  import * as echarts from 'echarts';
  import * as api from '@/api/sc/sale/out';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions } from '@/utils/labelSelect';

  const TREND_SERIES_NAMES = ['销售金额', '利润', '毛利率'];

  export default defineComponent({
    name: 'SaleTrendReport',
    setup() {
      return {
        h,
        SearchOutlined,
      };
    },
    data() {
      return {
        loading: false,
        trendDatas: [],
        trendEmpty: false,
        orderDateRange: this.getDefaultOrderDateRange(),
        searchFormData: {
          customerId: undefined,
          productName: '',
        },
        customerOptions: [],
        customerOptionMap: {},
      };
    },
    created() {
      this._trendChart = null;
    },
    mounted() {
      this.loadTrendDatas();
    },
    beforeUnmount() {
      this.disposeTrendChart();
    },
    methods: {
      search() {
        this.loadTrendDatas();
      },
      resetSearchForm() {
        this.searchFormData = {
          customerId: undefined,
          productName: '',
        };
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.search();
      },
      getDefaultOrderDateRange() {
        return [moment().startOf('month').format('YYYY-MM-DD'), moment().format('YYYY-MM-DD')];
      },
      buildSearchFormData() {
        return {
          ...this.searchFormData,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
        };
      },
      loadTrendDatas() {
        this.loading = true;
        api
          .queryProfitTrend(this.buildSearchFormData())
          .then((res) => {
            this.trendDatas = res || [];
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
        if (!chartDom) return;

        this.disposeTrendChart();
        this._trendChart = markRaw(echarts.init(chartDom));

        const dates = this.trendDatas.map((i) => this.formatChartDate(i.orderDate));
        const salesAmount = this.trendDatas.map((i) => this.toNumber(i.salesAmount));
        const salesProfit = this.trendDatas.map((i) => this.toNumber(i.salesProfit));
        const profitRate = this.trendDatas.map((i) => this.toNumber(i.profitRate));

        this._trendChart.setOption({
          tooltip: {
            trigger: 'axis',
            confine: true,
            axisPointer: { type: 'cross', snap: true, z: 9999 },
            formatter: this.formatTrendTooltip,
          },
          legend: { data: TREND_SERIES_NAMES, top: 8 },
          color: ['#1677ff', '#52c41a', '#faad14'],
          grid: { left: 24, right: 36, bottom: 24, top: 56, containLabel: true },
          xAxis: { type: 'category', boundaryGap: false, data: dates },
          yAxis: [
            {
              type: 'value',
              name: '金额',
              min: 0,
              axisLabel: { formatter: (value) => this.formatAmount(value) },
            },
            {
              type: 'value',
              name: '毛利率',
              scale: true,
              position: 'right',
              axisLabel: { formatter: (value) => this.formatPercent(value) },
            },
          ],
          series: [
            {
              name: TREND_SERIES_NAMES[0],
              type: 'line',
              smooth: true,
              data: salesAmount,
            },
            {
              name: TREND_SERIES_NAMES[1],
              type: 'line',
              smooth: true,
              symbol: 'circle',
              symbolSize: 6,
              lineStyle: { width: 3 },
              z: 3,
              data: salesProfit,
            },
            {
              name: TREND_SERIES_NAMES[2],
              type: 'line',
              smooth: true,
              yAxisIndex: 1,
              data: profitRate,
            },
          ],
        });
        this._trendChart.getZr().off('click');
        this._trendChart.getZr().on('click', this.handleTrendChartZrClick);

        setTimeout(() => {
          this._trendChart?.resize();
        }, 200);
      },
      handleTrendChartZrClick(event) {
        if (!this._trendChart) {
          return;
        }

        const nativeEvent = event?.event || {};
        const point = [
          event?.offsetX ?? nativeEvent.offsetX ?? nativeEvent.zrX,
          event?.offsetY ?? nativeEvent.offsetY ?? nativeEvent.zrY,
        ];
        if (point.some((item) => item === undefined || item === null)) {
          return;
        }

        if (!this._trendChart.containPixel({ gridIndex: 0 }, point)) {
          return;
        }

        const [xIndex] = this._trendChart.convertFromPixel({ gridIndex: 0 }, point);
        const dataIndex = Math.round(xIndex);
        const item = this.trendDatas[dataIndex];
        this.goSaleOutSheet(item?.orderDate);
      },
      goSaleOutSheet(orderDate) {
        const date = this.formatChartDate(orderDate);
        if (!date) {
          return;
        }

        this.$router.push({
          path: '/sale/out',
          query: {
            orderDateStart: date,
            orderDateEnd: date,
          },
        });
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
        return [`销售日期：${title}`, ...lines].join('<br/>');
      },
      formatChartDate(value) {
        return value ? moment(value).format('YYYY-MM-DD') : '';
      },
      sumBy(data, field) {
        return (data || []).reduce((sum, item) => sum + this.toNumber(item?.[field]), 0);
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
      formatAmount(value) {
        return this.toNumber(value).toFixed(2);
      },
      formatCurrency(value) {
        return '￥' + this.formatAmount(value);
      },
      formatPercent(value) {
        return this.formatAmount(value) + '%';
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
      disposeTrendChart() {
        if (this._trendChart) {
          this._trendChart.getZr().off('click');
          this._trendChart.dispose();
          this._trendChart = null;
        }
      },
    },
  });
</script>

<style lang="less" scoped>
  .sale-trend-page {
    min-height: 100%;
  }

  .toolbar {
    margin: 12px 0;
  }

  .trend-chart-wrapper {
    background: #fff;
  }

  .trend-chart {
    width: 100%;
    height: 520px !important;
    cursor: pointer;
  }

  .chart-empty {
    margin-top: 12px;
    padding: 96px 0;
    background: #fff;
  }
</style>
