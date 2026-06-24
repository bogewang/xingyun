<template>
  <div>
    <page-wrapper content-full-height fixed-height dense>
      <vxe-grid
        id="ReceiveSheetDetailList"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        keep-source
        row-id="detailId"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="pagerConfig"
        :footer-method="footerMethod"
        :loading="loading"
        :height="gridHeight || 'auto'"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="handleFormCollapse" @keyup.enter="search">
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
              <j-form-item label="供应商">
                <supplier-selector
                  v-model:value="searchFormData.supplierId"
                  allow-clear
                  placeholder="请选择供应商"
                />
              </j-form-item>

              <template #more>
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
                      v-for="item in RECEIVE_SHEET_STATUS.values()"
                      :key="item.code"
                      :value="item.code"
                    >
                      {{ item.desc }}
                    </a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="采购订单号">
                  <a-input v-model:value="searchFormData.purchaseOrderCode" allow-clear />
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
              v-permission="['purchase:receive:export']"
              :icon="h(DownloadOutlined)"
              @click="exportDetails"
            >
              导出
            </a-button>
            <a-button
              v-permission="['purchase:receive:export']"
              :icon="h(DownloadOutlined)"
              @click="exportDetailDailySummary"
            >
              按天汇总导出
            </a-button>
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a v-permission="['purchase:receive:modify']" @click="openModifyDialog(row)">{{
            row.code
          }}</a>
          <span v-no-permission="['purchase:receive:modify']">{{ row.code }}</span>
        </template>

        <template #isGift_default="{ row }">
          {{ row.isGift ? '是' : '否' }}
        </template>
      </vxe-grid>
    </page-wrapper>

    <detail :id="id" ref="viewDialog" />
    <purchase-order-detail :id="purchaseOrderId" ref="viewPurchaseOrderDetailDialog" />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import moment from 'moment';
  import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import Detail from '../detail.vue';
  import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
  import * as api from '@/api/sc/purchase/receive';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { buildSortPageVo, isEmpty } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestUserSelectOptions } from '@/utils/labelSelect';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { createSuccess } from '@/hooks/web/msg';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';

  export default defineComponent({
    name: 'ReceiveSheetDetailList',
    components: {
      Detail,
      PurchaseOrderDetail,
      SupplierSelector,
    },
    mixins: [multiplePageMix, gridCollapseHeightMix],
    setup() {
      return {
        h,
        isEmpty,
        SearchOutlined,
        DownloadOutlined,
        RECEIVE_SHEET_STATUS,
        SETTLE_STATUS,
      };
    },
    data() {
      return {
        loading: false,
        id: '',
        purchaseOrderId: '',
        searchFormData: {
          code: '',
          productName: '',
          supplierId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          purchaser: '',
          purchaseOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
        },
        orderDateRange: [],
        approveDateRange: [],
        createByOptions: [],
        createByOptionMap: {},
        approveByOptions: [],
        approveByOptionMap: {},
        toolbarConfig: {
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        pagerConfig: {
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
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
          { field: 'supplierName', title: '供应商名称', width: 140 },
          { field: 'orderDate', title: '订单日期', width: 120, sortable: true },
          { field: 'productCode', title: '商品编号', width: 120 },
          { field: 'productName', title: '商品名称', width: 180 },
          { field: 'spec', title: '规格', width: 100 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'orderNum', title: '收货数量', align: 'right', width: 100 },
          { field: 'taxPrice', title: '采购价', align: 'right', width: 100 },
          { field: 'taxAmount', title: '采购金额', align: 'right', width: 100 },
          { field: 'createTime', title: '操作时间', width: 170, sortable: true },
          { field: 'createBy', title: '操作人', width: 100 },
          { field: 'approveTime', title: '审核时间', width: 170, sortable: true },
          { field: 'approveBy', title: '审核人', width: 100 },
          {
            field: 'status',
            title: '状态',
            width: 100,
            formatter: ({ cellValue }) => RECEIVE_SHEET_STATUS.getDesc(cellValue),
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
    created() {
      this.orderDateRange = this.getDefaultOrderDateRange();
    },
    methods: {
      footerMethod({ columns, data }) {
        const orderNum = this.sumByField(data, 'orderNum');
        const taxAmount = this.sumByField(data, 'taxAmount');

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
          supplierId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          purchaser: '',
          purchaseOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
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
          supplierId: this.searchFormData.supplierId,
          createBy: this.searchFormData.createBy,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
          approveBy: this.searchFormData.approveBy,
          approveStartTime: this.approveDateRange?.[0]
            ? `${this.approveDateRange[0]} 00:00:00`
            : '',
          approveEndTime: this.approveDateRange?.[1] ? `${this.approveDateRange[1]} 23:59:59` : '',
          purchaserId: this.searchFormData.purchaser,
        });
      },
      filterSelectOption(input, option) {
        return filterSelectOption(input, option);
      },
      async updateSelectOptions(keyword, optionMapKey, optionsKey, selectedValueKey) {
        const options = await requestUserSelectOptions(keyword);
        const optionMap = mergeSelectOptionMap(this[optionMapKey], options);
        this[optionMapKey] = optionMap;
        this[optionsKey] = buildVisibleSelectOptions(
          this.searchFormData[selectedValueKey],
          optionMap,
          options,
        );
      },
      async loadCreateByOptions(keyword = '') {
        await this.updateSelectOptions(keyword, 'createByOptionMap', 'createByOptions', 'createBy');
      },
      async loadApproveByOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          'approveByOptionMap',
          'approveByOptions',
          'approveBy',
        );
      },
      normalizeSelectValue(value, optionMap) {
        return normalizeSelectValue(value, optionMap);
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
      exportDetailDailySummary() {
        api.exportDetailDailySummary(this.buildSearchFormData());
      },
      viewDetail(id) {
        this.id = id;
        this.$nextTick(() => this.$refs.viewDialog.openDialog());
      },
      viewPurchaseOrderDetail(id) {
        this.purchaseOrderId = id;
        this.$nextTick(() => this.$refs.viewPurchaseOrderDetailDialog.openDialog());
      },
      openModifyDialog(row) {
        if (!isEmpty(row.purchaseOrderId)) {
          this.openChildPage('/purchase/receive/modify/require/' + row.id);
        } else {
          this.openChildPage('/purchase/receive/modify/un-require/' + row.id);
        }
      },
    },
  });
</script>
