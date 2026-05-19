<template>
  <div>
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        id="SaleOutSheetDetailList"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        keep-source
        row-id="detailId"
        :proxy-config="proxyConfig"
        :columns="visibleTableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{}"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="$refs.grid.refreshColumn()" @keyup.enter="search">
              <j-form-item label="订单日期">
                <a-range-picker
                  v-model:value="orderDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                />
              </j-form-item>


              <j-form-item label="商品名称">
                <a-input v-model:value="searchFormData.productName" allow-clear />
              </j-form-item>

              <j-form-item label="是否已录采购">
                <a-select
                  v-model:value="searchFormData.hasCostPrice"
                  placeholder="全部"
                  allow-clear
                >
                  <a-select-option :value="true">已录</a-select-option>
                  <a-select-option :value="false">未录</a-select-option>
                </a-select>
              </j-form-item>

              <template #more>
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
                <j-form-item label="单据号">
                  <a-input v-model:value="searchFormData.code" allow-clear />
                </j-form-item>
                <j-form-item label="操作人">
                  <a-select
                    v-model:value="searchFormData.createBy"
                    allow-clear
                    show-search
                    :filter-option="filterSelectOption"
                    :options="createByOptions"
                    placeholder="请选择操作人"
                    @focus="loadCreateByOptions()"
                    @search="loadCreateByOptions"
                    @change="onCreateByChange"
                  />
                </j-form-item>
                <j-form-item label="审核人">
                  <a-select
                    v-model:value="searchFormData.approveBy"
                    allow-clear
                    show-search
                    :filter-option="filterSelectOption"
                    :options="approveByOptions"
                    placeholder="请选择审核人"
                    @focus="loadApproveByOptions()"
                    @search="loadApproveByOptions"
                    @change="onApproveByChange"
                  />
                </j-form-item>
                <j-form-item label="审核日期">
                  <a-range-picker
                    v-model:value="approveDateRange"
                    value-format="YYYY-MM-DD"
                    :placeholder="['开始日期', '结束日期']"
                  />
                </j-form-item>
                <j-form-item label="状态">
                  <a-select v-model:value="searchFormData.status" placeholder="全部" allow-clear>
                    <a-select-option
                      v-for="item in SALE_OUT_SHEET_STATUS.values()"
                      :key="item.code"
                      :value="item.code"
                    >
                      {{ item.desc }}
                    </a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="销售订单号">
                  <a-input v-model:value="searchFormData.saleOrderCode" allow-clear />
                </j-form-item>
                <j-form-item label="结算状态">
                  <a-select
                    v-model:value="searchFormData.settleStatus"
                    placeholder="全部"
                    allow-clear
                  >
                    <a-select-option
                      v-for="item in SETTLE_STATUS.values()"
                      :key="item.code"
                      :value="item.code"
                    >
                      {{ item.desc }}
                    </a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="是否已付完">
                  <a-select v-model:value="searchFormData.fullyPaid" placeholder="全部" allow-clear>
                    <a-select-option :value="true">已付完</a-select-option>
                    <a-select-option :value="false">未付完</a-select-option>
                  </a-select>
                </j-form-item>

              </template>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button @click="resetSearchForm">清空</a-button>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['sale:out:export']"
              :icon="h(DownloadOutlined)"
              @click="exportDetails"
            >
              导出
            </a-button>
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a @click="viewDetail(row.id)">{{ row.code }}</a>
        </template>

        <template #saleOrderCode_default="{ row }">
          <span v-if="isEmpty(row.saleOrderCode)">-</span>
          <span v-else>
            <a v-permission="['sale:order:query']" @click="viewSaleOrderDetail(row.saleOrderId)">
              {{ row.saleOrderCode }}
            </a>
            <span v-no-permission="['sale:order:query']">{{ row.saleOrderCode }}</span>
          </span>
        </template>

        <template #isGift_default="{ row }">
          {{ row.isGift ? '是' : '否' }}
        </template>

        <template #totalProfit_default="{ row }">
          <span v-if="isEmpty(row.totalProfit)">-</span>
          <span v-else>{{ Number(row.totalProfit || 0).toFixed(2) }}</span>
        </template>
      </vxe-grid>
    </page-wrapper>

    <detail :id="id" ref="viewDialog" />
    <sale-order-detail :id="saleOrderId" ref="viewSaleOrderDetailDialog" />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import moment from 'moment';
  import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import Detail from '../detail.vue';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import * as api from '@/api/sc/sale/out';
  import { buildSortPageVo, isEmpty } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { createSuccess } from '@/hooks/web/msg';
  import { usePermission } from '/@/hooks/web/usePermission';

  export default defineComponent({
    name: 'SaleOutSheetDetailList',
    components: {
      Detail,
      SaleOrderDetail,
    },
    setup() {
      const { hasPermission } = usePermission();
      return {
        h,
        isEmpty,
        hasPermission,
        SearchOutlined,
        DownloadOutlined,
        SETTLE_STATUS,
        SALE_OUT_SHEET_STATUS,
      };
    },
    data() {
      return {
        loading: false,
        id: '',
        saleOrderId: '',
        searchFormData: {
          code: '',
          productName: '',
          scId: '',
          customerId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
          hasCostPrice: undefined,
        },
        orderDateRange: [],
        approveDateRange: [],
        customerOptions: [],
        customerOptionMap: {},
        createByOptions: [],
        createByOptionMap: {},
        approveByOptions: [],
        approveByOptionMap: {},
        toolbarConfig: {
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        tableColumn: [
          { type: 'seq', width: 50, title: '序号' },
          {
            field: 'code',
            title: '单据号',
            width: 180,
            sortable: true,
            slots: { default: 'code_default' },
          },
          { field: 'customerName', title: '客户名称', width: 140 },
          { field: 'orderDate', title: '订单日期', width: 120, sortable: true },
          {
            field: 'saleOrderCode',
            title: '销售订单号',
            width: 180,
            slots: { default: 'saleOrderCode_default' },
          },
          { field: 'productCode', title: '商品编号', width: 120 },
          { field: 'productName', title: '商品名称', width: 180 },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 100 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          { field: 'orderNum', title: '出库数量', align: 'right', width: 100 },
          { field: 'taxPrice', title: '销售价', align: 'right', width: 100 },
          { field: 'taxAmount', title: '销售金额', align: 'right', width: 100 },
          { field: 'costPrice', title: '成本价', align: 'right', width: 100 },
          {
            field: 'totalProfit',
            title: '利润',
            align: 'right',
            width: 100,
            slots: { default: 'totalProfit_default' },
          },
          { field: 'isGift', title: '赠品', width: 80, slots: { default: 'isGift_default' } },
          { field: 'createTime', title: '操作时间', width: 170, sortable: true },
          { field: 'createBy', title: '操作人', width: 100 },
          { field: 'approveTime', title: '审核时间', width: 170, sortable: true },
          { field: 'approveBy', title: '审核人', width: 100 },
          {
            field: 'status',
            title: '状态',
            width: 100,
            formatter: ({ cellValue }) => SALE_OUT_SHEET_STATUS.getDesc(cellValue),
          },
          {
            field: 'settleStatus',
            title: '结算状态',
            width: 100,
            formatter: ({ cellValue }) => SETTLE_STATUS.getDesc(cellValue),
          },
          { field: 'description', title: '备注', width: 200 },
        ],
        proxyConfig: {
          props: {
            result: 'datas',
            total: 'totalCount',
          },
          ajax: {
            query: ({ page, sorts }) => {
              return api.queryDetail(this.buildQueryParams(page, sorts));
            },
          },
        },
      };
    },
    computed: {
      visibleTableColumn() {
        return this.tableColumn.filter((column) => {
          if (column.field === 'totalProfit') {
            return this.hasPermission('sale:out:profit', false);
          }
          return true;
        });
      },
      canViewProfit() {
        return this.hasPermission('sale:out:profit', false);
      },
    },
    created() {
      this.orderDateRange = this.getDefaultOrderDateRange();
    },
    methods: {
      footerMethod({ columns, data }) {
        const orderNum = this.sumByField(data, 'orderNum');
        const taxAmount = this.sumByField(data, 'taxAmount');
        const totalProfit = this.sumByField(data, 'totalProfit');

        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }
            if (column.field === 'orderNum') {
              return this.formatQuantity(orderNum);
            }
            if (column.field === 'taxAmount') {
              return this.formatAmount(taxAmount);
            }
            if (column.field === 'totalProfit') {
              return this.canViewProfit ? this.formatAmount(totalProfit) : '';
            }
            return '';
          }),
        ];
      },
      sumByField(data, field) {
        return (data || []).reduce((total, item) => {
          const value = Number(item?.[field] ?? 0);
          return total + (Number.isNaN(value) ? 0 : value);
        }, 0);
      },
      formatAmount(value) {
        return Number(value || 0).toFixed(2);
      },
      formatQuantity(value) {
        return Number(value || 0)
          .toFixed(2)
          .replace(/\.?0+$/, '');
      },
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      getDefaultOrderDateRange() {
        return [moment().startOf('month').format('YYYY-MM-DD'), moment().format('YYYY-MM-DD')];
      },
      resetSearchForm() {
        this.searchFormData = {
          code: '',
          productName: '',
          scId: '',
          customerId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
          hasCostPrice: undefined,
        };
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.approveDateRange = [];
        this.search();
      },
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.buildSearchFormData(),
        };
      },
      buildSearchFormData() {
        return Object.assign({}, this.searchFormData, {
          customerId: this.searchFormData.customerId,
          scId: this.searchFormData.scId,
          createBy: this.searchFormData.createBy,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
          approveBy: this.searchFormData.approveBy,
          approveStartTime: this.approveDateRange?.[0]
            ? `${this.approveDateRange[0]} 00:00:00`
            : '',
          approveEndTime: this.approveDateRange?.[1] ? `${this.approveDateRange[1]} 23:59:59` : '',
          salerId: this.searchFormData.saler,
        });
      },
      filterSelectOption(input, option) {
        return filterSelectOption(input, option);
      },
      async updateSelectOptions(keyword, requestFn, optionMapKey, optionsKey, selectedValueKey) {
        const options = await requestFn(keyword);
        const optionMap = mergeSelectOptionMap(this[optionMapKey], options);
        this[optionMapKey] = optionMap;
        this[optionsKey] = buildVisibleSelectOptions(
          this.searchFormData[selectedValueKey],
          optionMap,
          options,
        );
      },
      async requestCustomerOptions(keyword = '') {
        return requestCustomerSelectOptions(keyword);
      },
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
      },
      async loadCustomerOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestCustomerOptions,
          'customerOptionMap',
          'customerOptions',
          'customerId',
        );
      },
      async loadCreateByOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestUserOptions,
          'createByOptionMap',
          'createByOptions',
          'createBy',
        );
      },
      async loadApproveByOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestUserOptions,
          'approveByOptionMap',
          'approveByOptions',
          'approveBy',
        );
      },
      normalizeSelectValue(value, optionMap) {
        return normalizeSelectValue(value, optionMap);
      },
      onCustomerChange(value) {
        this.searchFormData.customerId = this.normalizeSelectValue(value, this.customerOptionMap);
      },
      onCreateByChange(value) {
        this.searchFormData.createBy = this.normalizeSelectValue(value, this.createByOptionMap);
      },
      onApproveByChange(value) {
        this.searchFormData.approveBy = this.normalizeSelectValue(value, this.approveByOptionMap);
      },
      exportDetails() {
        api.exportDetail(this.buildSearchFormData()).then(() => {
          createSuccess('已加入导出任务，请到导出中心查看！');
        });
      },
      viewDetail(id) {
        this.id = id;
        this.$nextTick(() => this.$refs.viewDialog.openDialog());
      },
      viewSaleOrderDetail(id) {
        this.saleOrderId = id;
        this.$nextTick(() => this.$refs.viewSaleOrderDetailDialog.openDialog());
      },
    },
  });
</script>
