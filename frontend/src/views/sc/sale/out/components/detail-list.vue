<template>
  <div ref="priceCheckContainer" class="price-check-local-container">
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

              <j-form-item label="计划日期">
                <a-range-picker
                  v-model:value="planDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                />
              </j-form-item>

              <j-form-item label="商品名称">
                <a-input v-model:value="searchFormData.productName" allow-clear />
              </j-form-item>
              <j-form-item label="备注">
                <a-input v-model:value="searchFormData.description" allow-clear />
              </j-form-item>
              <j-form-item label="客户">
                <customer-selector
                  v-model:value="searchFormData.customerIdList"
                  multiple
                  show-description-filter
                  placeholder="请选择客户"
                />
              </j-form-item>
              <j-form-item label="商品分类">
                <product-category-selector
                  v-model:value="searchFormData.categoryIdList"
                  :multiple="true"
                  :only-final="true"
                />
              </j-form-item>
              <j-form-item label="负毛利商品">
                <a-checkbox v-model:checked="searchFormData.onlyNegativeProfit">
                  仅查询负毛利商品
                </a-checkbox>
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
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button @click="resetSearchForm">清空</a-button>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['sale:out:query']"
              :icon="h(PrinterOutlined)"
              @click="tagPrint"
            >
              标签打印
            </a-button>
            <a-button
              v-if="showPriceUniqueCheck"
              :icon="h(EditOutlined)"
              @click="openPriceCheckDialog"
            >
              检查产品询价是否唯一
            </a-button>
            <a-button
              v-permission="['sale:out:export']"
              :icon="h(DownloadOutlined)"
              @click="exportDetails"
            >
              导出
            </a-button>
            <a-button
              v-permission="['sale:out:export']"
              :icon="h(DownloadOutlined)"
              @click="exportDetailDailySummary"
            >
              按天汇总导出
            </a-button>
            <a-button
              v-permission="['sale:out:export']"
              :icon="h(DownloadOutlined)"
              @click="exportInvoiceDetail"
            >
              导出开票明细
            </a-button>
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a @click="openModifyPage(row)">{{ row.code }}</a>
        </template>
        <template #inquiryProduct_default="{ row }">
          <span :class="formatInquiryProduct(row.inquiryProduct).className">
            {{ formatInquiryProduct(row.inquiryProduct).text }}
          </span>
        </template>
        <template #costPrice_default="{ row }">
          {{ formatPrice(row.costPrice) }}
        </template>
        <template #taxAmount_default="{ row }">
          {{ formatAmount(row.taxAmount) }}
        </template>
        <template #costAmount_default="{ row }">
          {{ formatAmount(calcRowCostAmount(row)) }}
        </template>
        <template #totalProfit_default="{ row }">
          <span :style="{ color: Number(row.totalProfit || 0) < 0 ? '#f5222d' : undefined }">
            {{ formatAmount(row.totalProfit) }}
          </span>
        </template>
        <template #profitRate_default="{ row }">
          <span :style="{ color: Number(row.totalProfit || 0) < 0 ? '#f5222d' : undefined }">
            {{ calcProfitRate(row.totalProfit, row.taxAmount, row.confirmAmt) }}
          </span>
        </template>
      </vxe-grid>
    </page-wrapper>

    <detail :id="id" ref="viewDialog" />
    <sale-order-detail :id="saleOrderId" ref="viewSaleOrderDetailDialog" />

    <a-modal
      v-model:open="priceCheckVisible"
      title="产品询价不唯一明细"
      width="92%"
      :mask-closable="false"
      :get-container="getPriceCheckContainer"
      wrap-class-name="price-check-local-wrap"
      destroy-on-close
      :footer="null"
      :style="{ top: '8px' }"
      :body-style="{ height: priceCheckModalBodyHeight, overflow: 'hidden', padding: '12px 16px' }"
    >
      <div style="margin-bottom: 12px">
        <a-space>
          <a-input
            v-model:value="priceCheckSearchForm.productName"
            allow-clear
            placeholder="请输入商品名称"
            style="width: 220px"
            @keyup.enter="searchPriceCheck"
          />
          <a-button type="primary" :icon="h(SearchOutlined)" @click="searchPriceCheck">
            查询
          </a-button>
          <a-button @click="resetPriceCheckSearch">清空</a-button>
          <a-checkbox v-model:checked="priceCheckSearchForm.onlyMultiPrice">
            仅查询多价格商品
          </a-checkbox>
        </a-space>
      </div>
      <vxe-grid
        v-if="priceCheckVisible"
        ref="priceCheckGrid"
        auto-resize
        resizable
        show-overflow
        highlight-hover-row
        row-id="detailId"
        :proxy-config="priceCheckProxyConfig"
        :columns="visiblePriceCheckTableColumn"
        :checkbox-config="priceCheckCheckboxConfig"
        :toolbar-config="priceCheckToolbarConfig"
        :pager-config="pagerConfig"
        :loading="priceCheckLoading"
        @checkbox-change="onPriceCheckCheckboxChange"
        :height="priceCheckGridHeight"
      >
        <template #toolbar_buttons>
          <a-space>
            <a-button
              v-permission="['sale:out:modify']"
              :icon="h(EditOutlined)"
              @click="batchUpdatePriceInDialog"
            >
              批量调整价格
            </a-button>
          </a-space>
        </template>
        <template #profitRate_default="{ row }">
          {{ calcProfitRate(row.totalProfit, row.taxAmount, row.confirmAmt) }}
        </template>
        <template #costAmount_default="{ row }">
          {{ formatAmount(calcCostAmount(row)) }}
        </template>
      </vxe-grid>
    </a-modal>

    <a-modal
      v-model:open="batchUpdatePriceVisible"
      title="批量调整售价"
      width="760px"
      :mask-closable="false"
      :get-container="getPriceCheckContainer"
      wrap-class-name="price-check-local-wrap"
      destroy-on-close
      :style="{ top: '50px' }"
      :body-style="{ padding: '16px 16px 8px' }"
      :confirm-loading="batchUpdatePriceSubmitting"
      @ok="submitBatchUpdatePrice"
      @cancel="closeBatchUpdatePriceDialog"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="总售价">
              <a-input :value="formatAmount(batchUpdatePriceSummary.totalSaleAmount)" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="总成本">
              <a-input :value="formatAmount(batchUpdatePriceSummary.totalCostAmount)" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="总数量">
              <a-input :value="formatQuantity(batchUpdatePriceForm.totalQty)" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="平均售价">
              <a-input-number
                v-model:value="batchUpdatePriceForm.avgTaxPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                @change="onBatchUpdateAvgTaxPriceChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="平均进价">
              <a-input :value="formatPrice(batchUpdatePriceForm.avgCostPrice)" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="毛利率(%)">
              <a-input-number
                v-model:value="batchUpdatePriceForm.profitRate"
                :precision="2"
                style="width: 100%"
                @change="onBatchUpdateProfitRateChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import moment from 'moment';
  import {
    DownloadOutlined,
    EditOutlined,
    PrinterOutlined,
    SearchOutlined,
  } from '@ant-design/icons-vue';
  import Detail from '../detail.vue';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import * as api from '@/api/sc/sale/out';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { printMix } from '@/mixins/print.ts';
  import { buildSortPageVo, isEmpty } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestUserSelectOptions } from '@/utils/labelSelect';
  import CustomerSelector from '@/components/Selector/CustomerSelector.vue';
  import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { createError, createSuccess, createSuccessAutoClose } from '@/hooks/web/msg';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import { calcSaleOutProfitRateByProfit } from './saleOutProfit';
  import { PRINT_TYPE } from '@/enums/biz/printType';

  const createDefaultSearchFormData = () => ({
    code: '',
    productName: '',
    description: '',
    categoryIdList: [],
    scId: '',
    customerIdList: [],
    createBy: undefined,
    approveBy: undefined,
    status: undefined,
    saler: '',
    saleOrderCode: '',
    settleStatus: undefined,
    fullyPaid: undefined,
    hasCostPrice: undefined,
    onlyNegativeProfit: false,
  });

  export default defineComponent({
    name: 'SaleOutSheetDetailList',
    components: {
      Detail,
      SaleOrderDetail,
      CustomerSelector,
      ProductCategorySelector,
    },
    mixins: [gridCollapseHeightMix, multiplePageMix, printMix],
    setup() {
      const { hasPermission } = usePermission();
      return {
        h,
        isEmpty,
        hasPermission,
        SearchOutlined,
        EditOutlined,
        DownloadOutlined,
        PrinterOutlined,
        formatInquiryProduct,
        SETTLE_STATUS,
        SALE_OUT_SHEET_STATUS,
      };
    },
    data() {
      return {
        viewportHeight: window.innerHeight,
        loading: false,
        priceCheckLoading: false,
        id: '',
        saleOrderId: '',
        showPriceUniqueCheck: false,
        priceCheckVisible: false,
        batchUpdatePriceVisible: false,
        batchUpdatePriceSubmitting: false,
        priceCheckQueryParams: null,
        selectedPriceCheckProductId: '',
        syncingPriceCheckSelection: false,
        priceCheckSearchForm: {
          productName: '',
          onlyMultiPrice: true,
        },
        batchUpdatePriceForm: {
          detailIds: [],
          totalQty: 0,
          totalCostAmount: 0,
          avgCostPrice: 0,
          avgTaxPrice: 0,
          profitRate: 0,
        },
        searchFormData: createDefaultSearchFormData(),
        orderDateRange: [],
        planDateRange: [],
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
        priceCheckToolbarConfig: {
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        pagerConfig: {
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        },
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'orderDate', title: '订单日期', width: 120, sortable: true },
          { field: 'planDate', title: '计划日期', width: 120, sortable: true },
          {
            field: 'code',
            title: '单据号',
            width: 180,
            sortable: true,
            slots: { default: 'code_default' },
          },
          { field: 'customerName', title: '客户名称', width: 140 },
          { field: 'productName', title: '商品名称', width: 180 },
          {
            field: 'inquiryProduct',
            title: '是否询价商品',
            width: 120,
            slots: { default: 'inquiryProduct_default' },
          },
          { field: 'spec', title: '规格', width: 100 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'orderNum', title: '数量', align: 'right', width: 100, sortable: true },
          { field: 'taxPrice', title: '销售价', align: 'right', width: 100, sortable: true },
          {
            field: 'costPrice',
            title: '成本单价',
            align: 'right',
            width: 100,
            sortable: true,
            slots: { default: 'costPrice_default' },
          },
          {
            field: 'taxAmount',
            title: '销售金额',
            align: 'right',
            width: 100,
            sortable: true,
            slots: { default: 'taxAmount_default' },
          },
          { field: 'confirmNum', title: '验收数量', align: 'right', width: 100, sortable: true },
          { field: 'confirmAmt', title: '验收金额', align: 'right', width: 100, sortable: true },
          {
            field: 'costAmount',
            title: '成本',
            align: 'right',
            width: 100,
            sortable: true,
            slots: { default: 'costAmount_default' },
          },
          {
            field: 'totalProfit',
            title: '毛利',
            align: 'right',
            width: 100,
            sortable: true,
            slots: { default: 'totalProfit_default' },
          },
          {
            field: 'profitRate',
            title: '毛利率',
            align: 'right',
            width: 100,
            sortable: true,
            slots: { default: 'profitRate_default' },
          },
          {
            field: 'settleStatus',
            title: '结算状态',
            width: 100,
            formatter: ({ cellValue }) => SETTLE_STATUS.getDesc(cellValue),
          },
          { field: 'productRemark', title: '商品备注', width: 200 },
          { field: 'description', title: '备注', width: 200, sortable: true },
          { field: 'createTime', title: '操作时间', width: 170, sortable: true },
          { field: 'createBy', title: '操作人', width: 100 },
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
        priceCheckTableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'code', title: '单据编号', width: 150 },
          { field: 'productName', title: '商品名称', width: 140 },
          { field: 'orderDate', title: '单据日期', width: 100, sortable: true },
          { field: 'unit', title: '单位', width: 70 },
          { field: 'orderNum', title: '数量', align: 'right', width: 80 },
          { field: 'taxPrice', title: '售价', align: 'right', width: 80 },
          { field: 'costPrice', title: '进价', align: 'right', width: 80 },
          {
            field: 'costAmount',
            title: '成本',
            align: 'right',
            width: 80,
            slots: { default: 'costAmount_default' },
          },
          { field: 'taxAmount', title: '销售金额', align: 'right', width: 80 },
          { field: 'confirmNum', title: '验收数量', align: 'right', width: 80 },
          { field: 'confirmAmt', title: '验收金额', align: 'right', width: 80 },
          { field: 'totalProfit', title: '毛利', align: 'right', width: 80 },
          {
            field: 'profitRate',
            title: '毛利率',
            align: 'right',
            width: 100,
            slots: { default: 'profitRate_default' },
          },
          { field: 'customerName', title: '客户', width: 160 },
        ],
        priceCheckProxyConfig: {
          props: {
            result: 'datas',
            total: 'totalCount',
          },
          ajax: {
            query: ({ page, sorts }) => {
              this.priceCheckLoading = true;
              this.selectedPriceCheckProductId = '';
              return api
                .queryPriceCheckDetail({
                  ...buildSortPageVo(page, sorts),
                  ...this.buildPriceCheckQueryParams(),
                })
                .finally(() => {
                  this.priceCheckLoading = false;
                });
            },
          },
          toolbar: true,
        },
        priceCheckCheckboxConfig: {
          showHeader: false,
          checkMethod: ({ row }) => {
            return (
              !this.selectedPriceCheckProductId ||
              row.productId === this.selectedPriceCheckProductId
            );
          },
        },
      };
    },
    computed: {
      priceCheckModalBodyHeight() {
        return `${Math.max(this.viewportHeight - 230, 420)}px`;
      },
      priceCheckGridHeight() {
        return Math.max(this.viewportHeight - 300, 350);
      },
      visibleTableColumn() {
        return this.tableColumn.filter((column) => {
          if (['totalProfit', 'profitRate'].includes(column.field)) {
            return this.hasPermission('sale:out:profit', false);
          }
          return true;
        });
      },
      canViewProfit() {
        return this.hasPermission('sale:out:profit', false);
      },
      batchUpdatePriceSummary() {
        const totalQty = Number(this.batchUpdatePriceForm.totalQty || 0);
        const totalCostAmount = Number(this.batchUpdatePriceForm.totalCostAmount || 0);
        const avgCostPrice = Number(this.batchUpdatePriceForm.avgCostPrice || 0);
        const avgTaxPrice = Number(this.batchUpdatePriceForm.avgTaxPrice || 0);
        const profitRate = Number(this.batchUpdatePriceForm.profitRate || 0);
        const totalSaleAmount = avgTaxPrice * totalQty;

        return {
          avgTaxPrice,
          avgCostPrice,
          profitRate,
          totalCostAmount,
          totalSaleAmount,
        };
      },
      visiblePriceCheckTableColumn() {
        return this.priceCheckTableColumn.filter((column) => {
          if (['costAmount', 'totalProfit', 'profitRate'].includes(column.field)) {
            return this.canViewProfit;
          }
          return true;
        });
      },
    },
    created() {
      this.orderDateRange = this.getDefaultOrderDateRange();
      this.loadPriceUniqueConfig();
    },
    mounted() {
      window.addEventListener('resize', this.handleViewportResize);
    },
    beforeUnmount() {
      window.removeEventListener('resize', this.handleViewportResize);
    },
    methods: {
      getPriceCheckContainer() {
        return this.$refs.priceCheckContainer;
      },
      handleViewportResize() {
        this.viewportHeight = window.innerHeight;
      },
      async loadPriceUniqueConfig() {
        try {
          this.showPriceUniqueCheck = await api.getPriceUniqueConfig();
        } catch (e) {
          this.showPriceUniqueCheck = false;
        }
      },
      footerMethod({ columns, data }) {
        const orderNum = this.sumByField(data, 'orderNum');
        const taxAmount = this.sumByField(data, 'taxAmount');
        const confirmNum = this.sumByField(data, 'confirmNum');
        const confirmAmt = this.sumByField(data, 'confirmAmt');
        const costAmount = (data || []).reduce(
          (total, item) => total + this.calcRowCostAmount(item),
          0,
        );
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
            if (column.field === 'confirmNum') {
              return this.formatQuantity(confirmNum);
            }
            if (column.field === 'confirmAmt') {
              return this.formatAmount(confirmAmt);
            }
            if (column.field === 'costAmount') {
              return this.formatAmount(costAmount);
            }
            if (column.field === 'totalProfit') {
              return this.canViewProfit ? this.formatAmount(totalProfit) : '';
            }
            if (column.field === 'profitRate') {
              return this.canViewProfit
                ? this.calcProfitRate(totalProfit, taxAmount, confirmAmt)
                : '';
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
      formatPrice(value) {
        return Number(value || 0)
          .toFixed(6)
          .replace(/\.?0+$/, '');
      },
      formatQuantity(value) {
        return Number(value || 0)
          .toFixed(2)
          .replace(/\.?0+$/, '');
      },
      calcCostAmount(row) {
        return Number(row?.taxAmount || 0) - Number(row?.totalProfit || 0);
      },
      calcRowCostAmount(row) {
        const qty = Number(row?.orderNum || 0);
        const costPrice = row?.costPrice;
        if (costPrice !== null && costPrice !== undefined && costPrice !== '') {
          return Number(costPrice) * qty;
        }

        if (
          row?.totalProfit !== null &&
          row?.totalProfit !== undefined &&
          row?.totalProfit !== ''
        ) {
          return Number(row?.taxAmount || 0) - Number(row?.totalProfit || 0);
        }

        return 0;
      },
      calcProfitRate(profit, amount, confirmAmt) {
        return calcSaleOutProfitRateByProfit(profit, amount, confirmAmt);
      },
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      openPriceCheckDialog() {
        this.priceCheckQueryParams = this.buildSearchFormData();
        this.priceCheckSearchForm = {
          productName: '',
          onlyMultiPrice: true,
        };
        this.resetPriceCheckSelection();
        this.priceCheckVisible = true;
        this.reloadPriceCheckGrid();
      },
      onPriceCheckCheckboxChange({ checked, row }) {
        if (this.syncingPriceCheckSelection) {
          return;
        }

        const grid = this.$refs.priceCheckGrid;
        if (!grid) {
          return;
        }

        this.syncingPriceCheckSelection = true;
        try {
          const allRows = grid.getTableData().fullData || [];
          const sameProductRows = allRows.filter((item) => item.productId === row.productId);

          if (checked) {
            this.selectedPriceCheckProductId = row.productId;
            grid.setCheckboxRow(sameProductRows, true);
          } else {
            if (this.selectedPriceCheckProductId === row.productId) {
              this.resetPriceCheckSelection(grid, allRows);
            }
          }
        } finally {
          this.syncingPriceCheckSelection = false;
        }
      },
      resetPriceCheckSelection(grid, rows) {
        this.selectedPriceCheckProductId = '';
        if (grid && rows) {
          grid.setCheckboxRow(rows, false);
        }
      },
      reloadPriceCheckGrid() {
        this.$nextTick(() => {
          this.$refs.priceCheckGrid?.commitProxy('reload');
        });
      },
      buildPriceCheckQueryParams() {
        return Object.assign({}, this.priceCheckQueryParams || this.buildSearchFormData(), {
          productName: this.priceCheckSearchForm.productName,
          onlyMultiPrice: this.priceCheckSearchForm.onlyMultiPrice,
        });
      },
      searchPriceCheck() {
        this.resetPriceCheckSelection();
        this.reloadPriceCheckGrid();
      },
      closeBatchUpdatePriceDialog() {
        this.batchUpdatePriceVisible = false;
        this.batchUpdatePriceSubmitting = false;
      },
      onBatchUpdateAvgTaxPriceChange(value) {
        const avgTaxPrice = Number(value || 0);
        const totalCostAmount = Number(this.batchUpdatePriceForm.totalCostAmount || 0);
        const totalQty = Number(this.batchUpdatePriceForm.totalQty || 0);
        const totalSaleAmount = avgTaxPrice * totalQty;

        this.batchUpdatePriceForm.avgTaxPrice = avgTaxPrice;
        this.batchUpdatePriceForm.profitRate =
          totalSaleAmount > 0
            ? Number((((totalSaleAmount - totalCostAmount) / totalSaleAmount) * 100).toFixed(2))
            : 0;
      },
      onBatchUpdateProfitRateChange(value) {
        const profitRate = Number(value || 0);
        const avgCostPrice = Number(this.batchUpdatePriceForm.avgCostPrice || 0);
        const divisor = 1 - profitRate / 100;

        this.batchUpdatePriceForm.profitRate = profitRate;
        this.batchUpdatePriceForm.avgTaxPrice =
          divisor !== 0 ? Number((avgCostPrice / divisor).toFixed(2)) : 0;
      },
      openBatchUpdatePriceDialog(records) {
        const totalQty = records.reduce((sum, item) => sum + Number(item?.orderNum || 0), 0);
        const totalSaleAmount = records.reduce(
          (sum, item) => sum + Number(item?.taxAmount || 0),
          0,
        );
        const totalCostAmount = records.reduce(
          (sum, item) => sum + this.calcRowCostAmount(item),
          0,
        );
        const avgCostPrice = totalQty > 0 ? totalCostAmount / totalQty : 0;
        const avgTaxPrice = totalQty > 0 ? totalSaleAmount / totalQty : 0;
        const profitRate =
          totalSaleAmount > 0 ? ((totalSaleAmount - totalCostAmount) / totalSaleAmount) * 100 : 0;

        this.batchUpdatePriceForm = {
          detailIds: records.map((item) => item.detailId),
          totalQty,
          totalCostAmount: Number(totalCostAmount.toFixed(2)),
          avgCostPrice: Number(avgCostPrice.toFixed(6)),
          avgTaxPrice: Number(avgTaxPrice.toFixed(2)),
          profitRate: Number(profitRate.toFixed(2)),
        };
        this.batchUpdatePriceVisible = true;
      },
      submitBatchUpdatePrice() {
        const totalQty = Number(this.batchUpdatePriceForm.totalQty || 0);
        const avgTaxPrice = Number(this.batchUpdatePriceForm.avgTaxPrice);
        const profitRate = Number(this.batchUpdatePriceForm.profitRate);

        if (!totalQty) {
          createError('所选商品总数量必须大于0！');
          return;
        }

        if (Number.isNaN(avgTaxPrice) || avgTaxPrice < 0) {
          createError('平均售价必须是数字并且不小于0！');
          return;
        }

        if (Number.isNaN(profitRate)) {
          createError('毛利率必须是数字！');
          return;
        }

        if (profitRate >= 100) {
          createError('毛利率必须小于100！');
          return;
        }

        if (Number.isNaN(avgTaxPrice) || avgTaxPrice < 0 || !Number.isFinite(avgTaxPrice)) {
          createError('根据当前进价和毛利率计算出的平均售价无效，请调整后重试！');
          return;
        }

        this.batchUpdatePriceSubmitting = true;
        this.priceCheckLoading = true;
        api
          .batchUpdatePrice({
            detailIds: this.batchUpdatePriceForm.detailIds,
            taxPrice: Number(avgTaxPrice.toFixed(2)),
          })
          .then(() => {
            createSuccessAutoClose('批量调整售价成功！');
            this.closeBatchUpdatePriceDialog();
            this.$refs.priceCheckGrid.commitProxy('reload');
            this.search();
          })
          .finally(() => {
            this.batchUpdatePriceSubmitting = false;
            this.priceCheckLoading = false;
          });
      },
      resetPriceCheckSearch() {
        this.priceCheckSearchForm = {
          productName: '',
          onlyMultiPrice: true,
        };
        this.searchPriceCheck();
      },
      getDefaultOrderDateRange() {
        const today = moment().format('YYYY-MM-DD');
        return [today, today];
      },
      resetSearchForm() {
        this.searchFormData = createDefaultSearchFormData();
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.planDateRange = [];
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
          customerIdList: this.searchFormData.customerIdList,
          scId: this.searchFormData.scId,
          createBy: this.searchFormData.createBy,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
          planDateStart: this.planDateRange?.[0] || '',
          planDateEnd: this.planDateRange?.[1] || '',
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
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
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
      /** 打印勾选的销售出库明细标签。 */
      async tagPrint() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要打印标签的销售明细！');
          return;
        }

        this.loading = true;
        try {
          const res = await api.tagPrint({
            idList: [...new Set(records.map((item) => item.id))],
            detailIdList: records.map((item) => item.detailId),
          });
          await this.vgPrintPreview(PRINT_TYPE.SALE_TAG.code, res);
        } finally {
          this.loading = false;
        }
      },
      exportDetailDailySummary() {
        api.exportDetailDailySummary(this.buildSearchFormData());
      },
      /** 按当前筛选条件导出按商品和单位汇总的开票明细。 */
      exportInvoiceDetail() {
        api.exportInvoiceDetail(this.buildSearchFormData()).then(() => {
          createSuccess('已加入导出任务，请到导出中心查看！');
        });
      },
      batchUpdatePriceInDialog() {
        const records = this.$refs.priceCheckGrid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择需要调整售价的商品明细！');
          return;
        }

        const productIds = Array.from(new Set(records.map((item) => item.productId)));
        if (productIds.length > 1) {
          createError('一次只能修改同一种产品的售价！');
          return;
        }
        this.openBatchUpdatePriceDialog(records);
      },
      openModifyPage(row) {
        const modifyType = isEmpty(row.saleOrderId) ? 'un-require' : 'require';
        this.openChildPage(`/sale/out/modify/${modifyType}/${row.id}`);
      },
      viewSaleOrderDetail(id) {
        this.saleOrderId = id;
        this.$nextTick(() => this.$refs.viewSaleOrderDetailDialog.openDialog());
      },
    },
  });
</script>

<style lang="less" scoped>
  .price-check-local-container {
    position: relative;
    overflow: hidden;
  }

  :global(.price-check-local-container .ant-modal-mask),
  :global(.price-check-local-container .price-check-local-wrap) {
    position: absolute !important;
    inset: 0;
  }
</style>
