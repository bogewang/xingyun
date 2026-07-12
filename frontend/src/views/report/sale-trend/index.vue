<template>
  <div v-permission="['report:sale-trend:query']">
    <page-wrapper content-full-height fixed-height :content-style="{ overflowY: 'auto' }">
      <div v-loading="loading" class="sale-trend-page">
        <div class="summary-panel">
          <div v-for="item in summaryItems" :key="item.key" class="summary-item">
            <div class="summary-title">{{ item.title }}</div>
            <div class="summary-value">{{ item.value }}</div>
          </div>
        </div>

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

        <div class="pie-chart-grid">
          <div class="product-pie-section">
            <div class="chart-toolbar">
              <a-button
                v-if="salesPieLevel === 'product'"
                size="small"
                @click="renderSalesCategoryPie"
              >
                返回分类
              </a-button>
              <span class="chart-section-title">{{ salesPieTitle }}</span>
              <span class="chart-tip">{{ salesPieTip }}</span>
            </div>
            <div v-if="salesPieEmpty" class="chart-empty">
              <a-empty description="暂无商品销售数据" />
            </div>
            <div v-show="!salesPieEmpty" class="product-pie-wrapper">
              <div ref="salesPieChartRef" class="product-pie-chart"></div>
            </div>
          </div>

          <div class="product-pie-section">
            <div class="chart-toolbar">
              <a-button
                v-if="profitPieLevel === 'product'"
                size="small"
                @click="renderProfitCategoryPie"
              >
                返回分类
              </a-button>
              <span class="chart-section-title">{{ profitPieTitle }}</span>
              <span class="chart-tip">{{ profitPieTip }}</span>
            </div>
            <div v-if="profitPieEmpty" class="chart-empty">
              <a-empty description="暂无商品利润数据" />
            </div>
            <div v-show="!profitPieEmpty" class="product-pie-wrapper">
              <div ref="profitPieChartRef" class="product-pie-chart"></div>
            </div>
          </div>
        </div>

        <div class="bar-chart-grid">
          <div class="product-bar-section">
            <div class="chart-toolbar">
              <span class="chart-section-title">商品正毛利率 TOP10</span>
              <span class="chart-tip">按商品汇总毛利率后取前 10</span>
            </div>
            <div v-if="positiveProfitBarEmpty" class="chart-empty">
              <a-empty description="暂无正毛利率商品数据" />
            </div>
            <div v-show="!positiveProfitBarEmpty" class="product-bar-wrapper">
              <div ref="positiveProfitBarChartRef" class="product-bar-chart"></div>
            </div>
          </div>

          <div class="product-bar-section">
            <div class="chart-toolbar">
              <span class="chart-section-title">商品负毛利率 TOP10</span>
              <span class="chart-tip">按商品汇总毛利率后取最低 10 条</span>
            </div>
            <div v-if="negativeProfitBarEmpty" class="chart-empty">
              <a-empty description="暂无负毛利率商品数据" />
            </div>
            <div v-show="!negativeProfitBarEmpty" class="product-bar-wrapper">
              <div ref="negativeProfitBarChartRef" class="product-bar-chart"></div>
            </div>
          </div>
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
  import { debounce } from '@/utils';
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
        detailDatas: [],
        salesPieDatas: [],
        salesPieEmpty: false,
        salesPieTitle: '商品分类销售额占比',
        salesPieTip: '点击分类查看商品占比',
        salesPieLevel: 'category',
        profitPieDatas: [],
        profitPieEmpty: false,
        profitPieTitle: '商品分类利润占比',
        profitPieTip: '点击分类查看商品占比',
        profitPieLevel: 'category',
        positiveProfitBarDatas: [],
        positiveProfitBarEmpty: false,
        negativeProfitBarDatas: [],
        negativeProfitBarEmpty: false,
        orderDateRange: this.getDefaultOrderDateRange(),
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
        searchFormData: {
          customerId: undefined,
          productName: '',
        },
        customerOptions: [],
        customerOptionMap: {},
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
      this._salesPieChart = null;
      this._profitPieChart = null;
      this._positiveProfitBarChart = null;
      this._negativeProfitBarChart = null;
      this._chartResizeObserver = null;
      this._chartResizeHandler = debounce(() => {
        this.resizeAllCharts();
      }, 100);
    },
    mounted() {
      window.addEventListener('resize', this._chartResizeHandler);
      this.initChartResizeObserver();
      this.loadTrendDatas();
    },
    beforeUnmount() {
      window.removeEventListener('resize', this._chartResizeHandler);
      this.destroyChartResizeObserver();
      this.disposeTrendChart();
      this.disposeSalesPieChart();
      this.disposeProfitPieChart();
      this.disposePositiveProfitBarChart();
      this.disposeNegativeProfitBarChart();
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
        const params = this.buildSearchFormData();
        Promise.all([
          api.queryProfitSummary(params),
          api.queryProfitTrend(params),
          api.queryDetail({
            ...params,
            pageIndex: 1,
            pageSize: 10000,
          }),
        ])
          .then(([summary, trendDatas, detailResult]) => {
            this.summary = Object.assign({}, this.summary, summary || {});
            this.trendDatas = trendDatas || [];
            this.detailDatas = detailResult?.datas || [];
            this.$nextTick(() => {
              this.renderTrendChart();
              this.renderSalesCategoryPie();
              this.renderProfitCategoryPie();
              this.renderProfitBarCharts();
            });
          })
          .finally(() => {
            this.loading = false;
          });
      },
      initChartResizeObserver() {
        if (typeof ResizeObserver === 'undefined' || !this.$el) {
          return;
        }

        this.destroyChartResizeObserver();
        this._chartResizeObserver = new ResizeObserver(() => {
          this._chartResizeHandler?.();
        });
        this._chartResizeObserver.observe(this.$el);
      },
      destroyChartResizeObserver() {
        if (this._chartResizeObserver) {
          this._chartResizeObserver.disconnect();
          this._chartResizeObserver = null;
        }
      },
      resizeAllCharts() {
        this._trendChart?.resize();
        this._salesPieChart?.resize();
        this._profitPieChart?.resize();
        this._positiveProfitBarChart?.resize();
        this._negativeProfitBarChart?.resize();
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
      renderSalesCategoryPie() {
        this.salesPieLevel = 'category';
        this.salesPieTitle = '商品分类销售额占比';
        this.salesPieTip = '点击分类查看商品占比';
        this.salesPieDatas = this.buildPieData(
          (detail) => detail.categoryName || '未分类',
          null,
          'taxAmount',
        );
        this.renderSalesPieChart((params) => {
          this.renderSalesProductPie(params.name);
        });
      },
      renderSalesProductPie(categoryName) {
        this.salesPieLevel = 'product';
        this.salesPieTitle = `${categoryName} - 商品销售额占比`;
        this.salesPieTip = `${categoryName} 下各商品销售额占比`;
        this.salesPieDatas = this.buildPieData(
          (detail) => this.formatDetailProductLabel(detail),
          (detail) => (detail.categoryName || '未分类') === categoryName,
          'taxAmount',
        );
        this.renderSalesPieChart();
      },
      renderProfitCategoryPie() {
        this.profitPieLevel = 'category';
        this.profitPieTitle = '商品分类利润占比';
        this.profitPieTip = '点击分类查看商品占比';
        this.profitPieDatas = this.buildPieData(
          (detail) => detail.categoryName || '未分类',
          null,
          'totalProfit',
        );
        this.renderProfitPieChart((params) => {
          this.renderProfitProductPie(params.name);
        });
      },
      renderProfitProductPie(categoryName) {
        this.profitPieLevel = 'product';
        this.profitPieTitle = `${categoryName} - 商品利润占比`;
        this.profitPieTip = `${categoryName} 下各商品利润占比`;
        this.profitPieDatas = this.buildPieData(
          (detail) => this.formatDetailProductLabel(detail),
          (detail) => (detail.categoryName || '未分类') === categoryName,
          'totalProfit',
        );
        this.renderProfitPieChart();
      },
      buildPieData(getName, filterDetail, valueField) {
        const dataMap = {};
        (this.detailDatas || [])
          .filter((detail) => !filterDetail || filterDetail(detail))
          .forEach((detail) => {
            const name = getName(detail);
            dataMap[name] = (dataMap[name] || 0) + this.toNumber(detail[valueField]);
          });

        return Object.keys(dataMap)
          .map((name) => ({
            name,
            value: Number(dataMap[name].toFixed(2)),
          }))
          .filter((item) => item.value !== 0)
          .sort((prev, next) => next.value - prev.value);
      },
      renderProfitBarCharts() {
        const productProfitDatas = this.buildProductProfitRateDatas();
        this.positiveProfitBarDatas = productProfitDatas
          .filter((item) => item.value > 0)
          .sort((prev, next) => next.value - prev.value)
          .slice(0, 10)
          .reverse();
        this.negativeProfitBarDatas = productProfitDatas
          .filter((item) => item.value < 0)
          .sort((prev, next) => prev.value - next.value)
          .slice(0, 10);

        this.renderPositiveProfitBarChart();
        this.renderNegativeProfitBarChart();
      },
      buildProductProfitRateDatas() {
        const dataMap = {};
        (this.detailDatas || []).forEach((detail) => {
          const name = this.formatDetailProductLabel(detail);
          const current = dataMap[name] || { salesAmount: 0, salesProfit: 0 };
          current.salesAmount += this.toNumber(detail.taxAmount);
          current.salesProfit += this.toNumber(detail.totalProfit);
          dataMap[name] = current;
        });

        return Object.keys(dataMap)
          .map((name) => {
            const item = dataMap[name];
            const value = item.salesAmount
              ? Number(((item.salesProfit / item.salesAmount) * 100).toFixed(2))
              : 0;
            return {
              name,
              value,
            };
          })
          .filter((item) => item.value !== 0);
      },
      renderSalesPieChart(clickHandler) {
        this.salesPieEmpty = this.salesPieDatas.length === 0;
        if (this.salesPieEmpty) {
          this.disposeSalesPieChart();
          return;
        }

        const chartDom = this.$refs.salesPieChartRef;
        if (!chartDom) {
          return;
        }

        this.disposeSalesPieChart();
        this._salesPieChart = markRaw(echarts.init(chartDom));
        this._salesPieChart.off('click');
        if (clickHandler) {
          this._salesPieChart.on('click', clickHandler);
        }
        this._salesPieChart.setOption({
          color: ['#1677ff', '#13c2c2', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96'],
          tooltip: {
            trigger: 'item',
            formatter: (params) =>
              `${params.name}<br/>销售额：${this.formatCurrency(params.value)}<br/>占比：${
                params.percent
              }%`,
          },
          legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 0,
            top: 24,
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
              data: this.salesPieDatas,
            },
          ],
        });

        setTimeout(() => {
          this._salesPieChart?.resize();
        }, 200);
      },
      renderProfitPieChart(clickHandler) {
        this.profitPieEmpty = this.profitPieDatas.length === 0;
        if (this.profitPieEmpty) {
          this.disposeProfitPieChart();
          return;
        }

        const chartDom = this.$refs.profitPieChartRef;
        if (!chartDom) {
          return;
        }

        this.disposeProfitPieChart();
        this._profitPieChart = markRaw(echarts.init(chartDom));
        this._profitPieChart.off('click');
        if (clickHandler) {
          this._profitPieChart.on('click', clickHandler);
        }
        this._profitPieChart.setOption({
          color: ['#52c41a', '#1677ff', '#13c2c2', '#faad14', '#f5222d', '#722ed1', '#eb2f96'],
          tooltip: {
            trigger: 'item',
            formatter: (params) =>
              `${params.name}<br/>利润：${this.formatCurrency(params.value)}<br/>占比：${
                params.percent
              }%`,
          },
          legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 0,
            top: 24,
            bottom: 12,
          },
          series: [
            {
              name: '利润',
              type: 'pie',
              radius: ['42%', '68%'],
              center: ['40%', '56%'],
              avoidLabelOverlap: true,
              label: {
                formatter: '{b}\n{d}%',
              },
              data: this.profitPieDatas,
            },
          ],
        });

        setTimeout(() => {
          this._profitPieChart?.resize();
        }, 200);
      },
      renderPositiveProfitBarChart() {
        this.positiveProfitBarEmpty = this.positiveProfitBarDatas.length === 0;
        if (this.positiveProfitBarEmpty) {
          this.disposePositiveProfitBarChart();
          return;
        }

        const chartDom = this.$refs.positiveProfitBarChartRef;
        if (!chartDom) {
          return;
        }

        this.disposePositiveProfitBarChart();
        this._positiveProfitBarChart = markRaw(echarts.init(chartDom));
        this._positiveProfitBarChart.setOption(
          this.buildProfitBarOption(this.positiveProfitBarDatas, '#52c41a', '正毛利率'),
        );

        setTimeout(() => {
          this._positiveProfitBarChart?.resize();
        }, 200);
      },
      renderNegativeProfitBarChart() {
        this.negativeProfitBarEmpty = this.negativeProfitBarDatas.length === 0;
        if (this.negativeProfitBarEmpty) {
          this.disposeNegativeProfitBarChart();
          return;
        }

        const chartDom = this.$refs.negativeProfitBarChartRef;
        if (!chartDom) {
          return;
        }

        this.disposeNegativeProfitBarChart();
        this._negativeProfitBarChart = markRaw(echarts.init(chartDom));
        this._negativeProfitBarChart.setOption(
          this.buildProfitBarOption(this.negativeProfitBarDatas, '#f5222d', '负毛利率'),
        );

        setTimeout(() => {
          this._negativeProfitBarChart?.resize();
        }, 200);
      },
      buildProfitBarOption(datas, color, seriesName) {
        return {
          color: [color],
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            confine: true,
            formatter: (params) => {
              const item = Array.isArray(params) ? params[0] : params;
              return `${item.name}<br/>${seriesName}：${this.formatPercent(item.value)}`;
            },
          },
          grid: {
            left: 12,
            right: 24,
            top: 12,
            bottom: 12,
            containLabel: true,
          },
          xAxis: {
            type: 'value',
            axisLabel: {
              formatter: (value) => this.formatPercent(value),
            },
          },
          yAxis: {
            type: 'category',
            data: datas.map((item) => item.name),
            axisLabel: {
              width: 180,
              overflow: 'truncate',
            },
          },
          series: [
            {
              name: seriesName,
              type: 'bar',
              barMaxWidth: 28,
              label: {
                show: true,
                position: 'right',
                formatter: ({ value }) => this.formatPercent(value),
              },
              data: datas.map((item) => item.value),
            },
          ],
        };
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
      formatDetailProductLabel(item) {
        const productName = item?.productName || '未命名商品';
        const spec = item?.spec || '';
        return spec ? `${productName} / ${spec}` : productName;
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
      formatQuantity(value) {
        return this.toNumber(value).toFixed(0);
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
      disposeSalesPieChart() {
        if (this._salesPieChart) {
          this._salesPieChart.dispose();
          this._salesPieChart = null;
        }
      },
      disposeProfitPieChart() {
        if (this._profitPieChart) {
          this._profitPieChart.dispose();
          this._profitPieChart = null;
        }
      },
      disposePositiveProfitBarChart() {
        if (this._positiveProfitBarChart) {
          this._positiveProfitBarChart.dispose();
          this._positiveProfitBarChart = null;
        }
      },
      disposeNegativeProfitBarChart() {
        if (this._negativeProfitBarChart) {
          this._negativeProfitBarChart.dispose();
          this._negativeProfitBarChart = null;
        }
      },
    },
  });
</script>

<style lang="less" scoped>
  .sale-trend-page {
    min-height: 100%;
  }

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

  .pie-chart-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: 12px;
  }

  .bar-chart-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: 12px;
  }

  .product-pie-section {
    padding: 16px;
    background: #fff;
  }

  .product-bar-section {
    padding: 16px;
    background: #fff;
  }

  .chart-section-title {
    color: #2f3f48;
    font-size: 16px;
    font-weight: 500;
  }

  .chart-toolbar {
    display: flex;
    align-items: center;
    min-height: 32px;
    color: #2f3f48;
    margin-bottom: 8px;
  }

  .chart-toolbar .ant-btn + .chart-section-title {
    margin-left: 12px;
  }

  .chart-tip {
    margin-left: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }

  .product-pie-wrapper {
    width: 100%;
  }

  .product-pie-chart {
    width: 100%;
    height: 420px !important;
  }

  .product-bar-wrapper {
    width: 100%;
  }

  .product-bar-chart {
    width: 100%;
    height: 420px !important;
  }

  .product-pie-section .chart-empty {
    margin-top: 0;
    padding: 64px 0;
  }

  .product-bar-section .chart-empty {
    margin-top: 0;
    padding: 64px 0;
  }

  .chart-empty {
    margin-top: 12px;
    padding: 96px 0;
    background: #fff;
  }

  @media (max-width: 1400px) {
    .summary-panel {
      grid-template-columns: repeat(3, minmax(140px, 1fr));
      row-gap: 16px;
    }

    .pie-chart-grid {
      grid-template-columns: 1fr;
    }

    .bar-chart-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 1200px) {
    .sale-trend-page :deep(.j-form-item) {
      width: 100% !important;
    }

    .sale-trend-page :deep(.j-form-item-content),
    .sale-trend-page :deep(.j-form-item-content-wrapper) {
      min-width: 0;
    }
  }

  @media (max-width: 768px) {
    .summary-panel {
      grid-template-columns: 1fr;
      row-gap: 16px;
    }

    .summary-value {
      font-size: 20px;
    }

    .chart-toolbar {
      align-items: flex-start;
      flex-direction: column;
      gap: 8px;
    }

    .chart-tip {
      margin-left: 0;
    }
  }
</style>
