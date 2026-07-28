<template>
  <div ref="importerContainer" class="excel-importer-local-container">
    <div v-permission="['sale:out:query']">
      <page-wrapper content-full-height fixed-height dense>
        <!-- 数据列表 -->
        <vxe-grid
          id="SaleOutSheet"
          ref="grid"
          auto-resize
          resizable
          show-overflow
          show-footer
          highlight-hover-row
          keep-source
          row-id="id"
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
                <j-form-item label="商品名称">
                  <a-input v-model:value="searchFormData.productName" allow-clear />
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
                <j-form-item label="成本状态">
                  <a-select
                    v-model:value="searchFormData.fillAllCost"
                    placeholder="全部"
                    allow-clear
                  >
                    <a-select-option :value="true">已补全</a-select-option>
                    <a-select-option :value="false">未补全</a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="结算状态">
                  <a-select v-model:value="searchFormData.settleStatus" placeholder="全部" allow-clear>
                    <a-select-option
                      v-for="item in SETTLE_STATUS.values()"
                      :key="item.code"
                      :value="item.code"
                    >
                      {{ item.desc }}
                    </a-select-option>
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

                <j-form-item label="已付金额">
                  <a-space class="amount-range-input" :size="4">
                    <a-input-number
                      v-model:value="searchFormData.paidAmountStart"
                      :min="0"
                      :precision="2"
                      placeholder="最小值"
                    />
                    <span>至</span>
                    <a-input-number
                      v-model:value="searchFormData.paidAmountEnd"
                      :min="0"
                      :precision="2"
                      placeholder="最大值"
                    />
                  </a-space>
                </j-form-item>

                <j-form-item label="未付金额">
                  <a-space class="amount-range-input" :size="4">
                    <a-input-number
                      v-model:value="searchFormData.unpaidAmountStart"
                      :min="0"
                      :precision="2"
                      placeholder="最小值"
                    />
                    <span>至</span>
                    <a-input-number
                      v-model:value="searchFormData.unpaidAmountEnd"
                      :min="0"
                      :precision="2"
                      placeholder="最大值"
                    />
                  </a-space>
                </j-form-item>
              </j-form>
            </j-border>
          </template>
          <!-- 工具栏 -->
          <template #toolbar_buttons>
            <a-space>
              <a-button @click="resetSearchForm">清空</a-button>
              <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
              <a-button
                v-permission="['sale:out:add']"
                type="primary"
                :icon="h(PlusOutlined)"
                @click="openAddDialog"
                >新增</a-button
              >
              <a-button
                v-permission="['sale:out:approve']"
                :icon="h(CheckOutlined)"
                @click="batchApprovePass"
                >审核通过</a-button
              >
              <a-button
                v-permission="['sale:out:approve']"
                :icon="h(CloseOutlined)"
                @click="batchApproveRefuse"
                >审核拒绝</a-button
              >
              <a-button
                v-permission="['sale:out:delete']"
                danger
                :icon="h(DeleteOutlined)"
                @click="batchDelete"
                >批量删除</a-button
              >
              <a-button
                v-permission="['sale:out:add']"
                :icon="h(CloudUploadOutlined)"
                @click="$refs.importer.openDialog()"
                >导入Excel</a-button
              >
              <a-button
                v-permission="['sale:out:export']"
                :icon="h(DownloadOutlined)"
                @click="exportList"
                >导出</a-button
              >
              <a-button
                v-permission="['wenshan:sale:out:saleexport']"
                :icon="h(DownloadOutlined)"
                @click="exportSales"
                >销售单导出</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(DownloadOutlined)"
                @click="marketBuySummary"
                >买菜汇总</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(DownloadOutlined)"
                @click="marketBuySummary2"
                >买菜汇总2</a-button
              >
              <a-button
                v-permission="['report:sale-profit:query']"
                :icon="h(SyncOutlined)"
                @click="openCostRecalculate"
                >重算成本</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(PrinterOutlined)"
                @click="tagPrint"
                >标签打印</a-button
              >
            </a-space>
          </template>

          <!-- 单据号 列自定义内容 -->
          <template #code_default="{ row }">
            <a
              v-if="hasPermission('sale:out:modify', false) && !isSettleLocked(row)"
              @click="openModifyDialog(row)"
            >
              {{ row.code }}
            </a>
            <span v-else>{{ row.code }}</span>
          </template>

          <!-- 总利润 列自定义内容 -->
          <template #total_profit="{ row }">
            <span v-if="isEmpty(row.totalProfit)">-</span>
            <span v-else>
              {{ Number(row.totalProfit || 0).toFixed(2) }}
            </span>
          </template>

          <template #profit_rate="{ row }">
            {{ calcProfitRate(row.totalProfit, row.totalAmount) }}
          </template>

          <template #fillAllCost_default="{ row }">
            <span :style="{ color: row.fillAllCost ? '#52c41a' : '#f5222d' }">
              {{ row.fillAllCost ? '已补全' : '未补全' }}
            </span>
          </template>

          <!-- 操作 列自定义内容 -->
          <template #action_default="{ row }">
            <table-action outside :actions="createActions(row)" />
          </template>
        </vxe-grid>
      </page-wrapper>

      <!-- 查看窗口 -->
      <detail :id="id" ref="viewDialog" />

      <approve-refuse ref="approveRefuseDialog" @confirm="doApproveRefuse" />
      <sale-out-sheet-query-importer
        ref="importer"
        :get-container="getImporterContainer"
        local-container
        hide-on-deactivated
        @confirm="handleImportSuccess"
      />

      <!-- 销售订单查看窗口 -->
      <sale-order-detail :id="saleOrderId" ref="viewSaleOrderDetailDialog" />

      <a-modal
        v-model:open="descriptionModal.visible"
        title="修改备注"
        :confirm-loading="descriptionModal.loading"
        @ok="submitDescription"
        @cancel="closeDescriptionDialog"
      >
        <a-textarea
          v-model:value.trim="descriptionModal.description"
          maxlength="200"
          :rows="4"
          allow-clear
        />
      </a-modal>

      <!-- 批量操作 -->
      <batch-handler
        ref="batchApprovePassHandlerDialog"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          { field: 'customerName', title: '客户名称', width: 120 },
        ]"
        title="审核通过"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchApprovePass"
        @confirm="search"
      />
      <batch-handler
        ref="batchApproveRefuseHandlerDialog"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          { field: 'customerName', title: '客户名称', width: 120 },
        ]"
        title="审核拒绝"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchApproveRefuse"
        @confirm="search"
      />
      <batch-handler
        ref="batchDeleteHandlerDialog"
        :concurrency="1"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          { field: 'customerName', title: '客户名称', width: 120 },
        ]"
        title="批量删除"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchDelete"
        @confirm="search"
      />
      <order-print-dialog />

      <!-- 月底成本重算弹窗 -->
      <a-modal
        v-model:open="costRecalculateVisible"
        title="月底成本重算"
        :confirm-loading="costRecalculateLoading"
        @ok="executeCostRecalculate"
      >
        <a-form layout="vertical">
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="costRecalculateDateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import Detail from '../detail.vue';
  import ApproveRefuse from '@/components/ApproveRefuse';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import moment from 'moment';
  import {
    CheckOutlined,
    CloseOutlined,
    CloudUploadOutlined,
    DeleteOutlined,
    DownloadOutlined,
    PlusOutlined,
    PrinterOutlined,
    SearchOutlined,
    SyncOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import * as configApi from '@/api/sc/sale/config';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { printMix } from '@/mixins/print.ts';
  import { buildSortPageVo, isEmpty } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import {
    createConfirm,
    createError,
    createSuccess,
    createSuccessAutoClose,
  } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import BatchHandler from '@/components/BatchHandler';
  import PrintDialog from '/@/components/PrintDialog';
  import SaleOutSheetQueryImporter from '@/components/Importor/SaleOutSheetQueryImporter.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import {
    buildMarketBuySummary2Params,
    buildMarketBuySummaryParams,
  } from './saleOutMarketBuySummary';

  export default defineComponent({
    name: 'SaleOutSheetSheetList',
    components: {
      Detail,
      ApproveRefuse,
      SaleOrderDetail,
      BatchHandler,
      OrderPrintDialog: PrintDialog,
      SaleOutSheetQueryImporter,
    },
    mixins: [multiplePageMix, printMix, gridCollapseHeightMix],
    setup() {
      const { hasPermission } = usePermission();
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        CheckOutlined,
        CloseOutlined,
        CloudUploadOutlined,
        DeleteOutlined,
        DownloadOutlined,
        PrinterOutlined,
        SyncOutlined,
        isEmpty,
        hasPermission,
        RECEIVE_SHEET_STATUS,
        SETTLE_STATUS,
      };
    },
    data() {
      return {
        loading: false,
        // 当前行数据
        id: '',
        saleOrderId: '',
        // 查询列表的查询条件
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
          fillAllCost: undefined,
          paidAmountStart: undefined,
          paidAmountEnd: undefined,
          unpaidAmountStart: undefined,
          unpaidAmountEnd: undefined,
        },
        orderDateRange: this.getDefaultOrderDateRange(),
        approveDateRange: [],
        customerOptions: [],
        customerOptionMap: {},
        createByOptions: [],
        createByOptionMap: {},
        approveByOptions: [],
        approveByOptionMap: {},
        // 工具栏配置
        toolbarConfig: {
          // 自定义左侧工具栏
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        pagerConfig: {
          pageSize: 50,
          pageSizes: [20, 50, 100, 200],
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        },
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'orderDate', title: '订单日期', width: 120, sortable: true },
          {
            field: 'code',
            title: '单据号',
            width: 180,
            sortable: true,
            slots: { default: 'code_default' },
          },
          { field: 'customerName', title: '客户名称', width: 120 },
          { field: 'totalAmount', title: '单据总金额', align: 'right', width: 100 },
          { field: 'confirmAmt', title: '验收金额', align: 'right', width: 100 },
          { field: 'paidAmount', title: '已付金额', align: 'right', width: 80 },
          { field: 'unpaidAmount', title: '未付金额', align: 'right', width: 80 },
          {
            field: 'settleStatus',
            title: '结算状态',
            width: 100,
            formatter: ({ cellValue }) => SETTLE_STATUS.getDesc(cellValue) || '-',
          },
          {
            field: 'totalProfit',
            title: '总利润',
            align: 'right',
            width: 80,
            slots: { default: 'total_profit' },
          },
          {
            field: 'profitRate',
            title: '毛利率',
            align: 'right',
            width: 80,
            slots: { default: 'profit_rate' },
          },
          { field: 'totalNum', title: '商品数量', align: 'right', width: 80 },
          { field: 'confirmNum', title: '验收数量', align: 'right', width: 100 },
          {
            field: 'fillAllCost',
            title: '成本状态',
            width: 80,
            slots: { default: 'fillAllCost_default' },
          },
          { field: 'createTime', title: '操作时间', width: 150, sortable: true },
          { field: 'createBy', title: '操作人', width: 80 },
          { field: 'description', title: '备注', width: 200 },
          { title: '操作', minWidth: 300, fixed: 'right', slots: { default: 'action_default' } },
        ],
        // 请求接口配置
        proxyConfig: {
          props: {
            // 响应结果列表字段
            result: 'datas',
            // 响应结果总条数字段
            total: 'totalCount',
          },
          ajax: {
            // 查询接口
            query: ({ page, sorts }) => {
              return api.query(this.buildQueryParams(page, sorts));
            },
          },
        },
        batchHandleDatas: [],
        batchRefuseReason: '',
        descriptionModal: {
          visible: false,
          loading: false,
          id: '',
          description: '',
        },
        // 月底成本重算
        costRecalculateVisible: false,
        costRecalculateLoading: false,
        costRecalculateDateRange: [],
      };
    },
    computed: {
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
    },
    watch: {
      '$route.query': {
        handler() {
          this.applyRouteQuery();
          this.$nextTick(() => this.search());
        },
      },
    },
    created() {
      this.applyRouteQuery();
    },
    methods: {
      getImporterContainer() {
        return this.$refs.importerContainer;
      },
      applyRouteQuery() {
        const { code, orderDateStart, orderDateEnd } = this.$route.query || {};
        if (code !== undefined) {
          this.searchFormData.code = Array.isArray(code) ? code[0] || '' : code || '';
        }
        if (orderDateStart || orderDateEnd) {
          this.orderDateRange = [orderDateStart || orderDateEnd, orderDateEnd || orderDateStart];
        }
      },
      footerMethod({ columns, data }) {
        const totalAmount = this.sumByField(data, 'totalAmount');
        const paidAmount = this.sumByField(data, 'paidAmount');
        const unpaidAmount = this.sumByField(data, 'unpaidAmount');
        const totalProfit = this.sumByField(data, 'totalProfit');
        const totalNum = this.sumByField(data, 'totalNum');
        const confirmNum = this.sumByField(data, 'confirmNum');
        const confirmAmt = this.sumByField(data, 'confirmAmt');

        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }

            if (column.field === 'totalAmount') {
              return this.formatAmount(totalAmount);
            }

            if (column.field === 'paidAmount') {
              return this.formatAmount(paidAmount);
            }

            if (column.field === 'unpaidAmount') {
              return this.formatAmount(unpaidAmount);
            }

            if (column.field === 'totalProfit') {
              if (!this.canViewProfit) {
                return '';
              }
              return this.formatAmount(totalProfit);
            }

            if (column.field === 'profitRate') {
              return this.canViewProfit ? this.calcProfitRate(totalProfit, totalAmount) : '';
            }

            if (column.field === 'totalNum') {
              return this.formatQuantity(totalNum);
            }
            if (column.field === 'confirmNum') {
              return this.formatQuantity(confirmNum);
            }
            if (column.field === 'confirmAmt') {
              return this.formatAmount(confirmAmt);
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
        return this.toFixedNumber(value, 2);
      },
      formatQuantity(value) {
        return this.toFixedNumber(value, 2, true);
      },
      calcProfitRate(profit, amount) {
        const amountNumber = Number(amount || 0);
        if (!amountNumber) {
          return '0.00%';
        }
        return `${this.toFixedNumber((Number(profit || 0) / amountNumber) * 100)}%`;
      },
      toFixedNumber(value, digits = 2, trimZero = false) {
        const text = Number(value || 0).toFixed(digits);
        return trimZero ? text.replace(/\.?0+$/, '') : text;
      },
      // 列表发生查询时的事件
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
          fillAllCost: undefined,
          paidAmountStart: undefined,
          paidAmountEnd: undefined,
          unpaidAmountStart: undefined,
          unpaidAmountEnd: undefined,
        };
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.approveDateRange = [];
        this.search();
      },
      // 查询前构建查询参数结构
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.buildSearchFormData(),
        };
      },
      // 查询前构建具体的查询参数
      buildSearchFormData() {
        const params = Object.assign({}, this.searchFormData, {
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
          fillAllCost: this.searchFormData.fillAllCost,
        });

        return params;
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
      openAddDialog() {
        configApi.get().then((res) => {
          if (res.outStockRequireSale) {
            this.openChildPage('/sale/out/add/require');
          } else {
            this.openChildPage('/sale/out/add/un-require');
          }
        });
      },
      openModifyDialog(row) {
        if (this.isSettleLocked(row)) {
          createError('销售出库单已对账或已结算，无法修改！');
          return;
        }
        if (!isEmpty(row.saleOrderId)) {
          this.openChildPage('/sale/out/modify/require/' + row.id);
        } else {
          this.openChildPage('/sale/out/modify/un-require/' + row.id);
        }
      },
      openDescriptionDialog(row) {
        this.descriptionModal = {
          visible: true,
          loading: false,
          id: row.id,
          description: row.description || '',
        };
      },
      closeDescriptionDialog() {
        this.descriptionModal.visible = false;
        this.descriptionModal.loading = false;
      },
      submitDescription() {
        this.descriptionModal.loading = true;
        api
          .updateDescription({
            id: this.descriptionModal.id,
            description: this.descriptionModal.description,
          })
          .then(() => {
            createSuccess('保存成功！');
            this.closeDescriptionDialog();
            this.search();
          })
          .finally(() => {
            this.descriptionModal.loading = false;
          });
      },
      // 删除订单
      deleteOrder(row) {
        createConfirm('对选中的销售出库单执行删除操作？').then(() => {
          this.loading = true;
          api
            .deleteById(row.id)
            .then(() => {
              createSuccess('删除成功！');
              this.search();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      doBatchDelete(row) {
        return api.batchDelete(row.id);
      },
      // 批量删除
      batchDelete() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许执行删除操作！');
            return;
          }
          if (this.isSettleLocked(records[i])) {
            createError('第' + (i + 1) + '个销售出库单已对账或已结算，不允许执行删除操作！');
            return;
          }
        }

        this.batchHandleDatas = records;

        this.$refs.batchDeleteHandlerDialog.openDialog();
      },
      doBatchApprovePass(row) {
        return api.batchApprovePass({
          id: row.id,
        });
      },
      // 批量审核通过
      batchApprovePass() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许继续执行审核！');
            return;
          }
        }

        this.batchHandleDatas = records;

        this.$refs.batchApprovePassHandlerDialog.openDialog();
      },
      // 批量审核拒绝
      batchApproveRefuse() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许继续执行审核！');
            return;
          }

          if (SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核拒绝，不允许继续执行审核！');
            return;
          }
        }

        this.$refs.approveRefuseDialog.openDialog();
      },
      doBatchApproveRefuse(row) {
        return api.batchApproveRefuse({
          id: row.id,
          refuseReason: this.batchRefuseReason,
        });
      },
      doApproveRefuse(reason) {
        this.batchHandleDatas = this.$refs.grid.getCheckboxRecords();
        this.batchRefuseReason = reason;

        this.$refs.batchApproveRefuseHandlerDialog.openDialog();
      },
      handleImportSuccess(res) {
        const ids = res?.data || res?.datas || res || [];
        const count = Array.isArray(ids) ? ids.length : 0;
        createSuccessAutoClose('导入成功，已创建' + count + '张销售出库单！');
        this.search();
      },
      exportList() {
        this.loading = true;
        api
          .exportList(this.buildQueryParams({}))
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      exportDetails(row) {
        this.loading = true;
        api
          .exportDetail({
            pageIndex: 1,
            pageSize: 2147483647,
            idList: [row.id],
          })
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      exportSales() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行销售导出的销售出库单！');
          return;
        }

        this.loading = true;
        api
          .exportSales({
            idList: records.map((item) => item.id),
          })
          .finally(() => {
            this.loading = false;
          });
      },
      buildPrintData(printData) {
        // 基础属性保持不变， details字段需要重新赋值，比如 orderNum=>qty
        const res = {
          ...printData,
        };

        const details = Array.isArray(printData?.details) ? printData.details : [];
        res.details = details.map((item, index) => ({
          // 新生成一个对象，避免修改原对象
          ...item,
          seq: index + 1,
        }));

        return res;
      },
      async printOrder(row) {
        this.loading = true;

        try {
          const res = await api.print(row.id);
          // 将res组装成模板定义和打印数据的格式，然后调用打印预览组件进行预览
          const printData = this.buildPrintData(res);
          await this.vgPrintPreview(PRINT_TYPE.SALE_OUT.code, printData);
        } finally {
          this.loading = false;
        }
      },
      async tagPrint() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要打印标签的销售出库单！');
          return;
        }
        this.loading = true;
        try {
          const res = await api.tagPrint({
            ...this.buildQueryParams({}, {}),
            idList: records.map((item) => item.id),
          });
          await this.vgPrintPreview(PRINT_TYPE.SALE_TAG.code, res);
        } finally {
          this.loading = false;
        }
      },
      // 按勾选单据导出买菜汇总
      marketBuySummary() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要汇总的销售出库单！');
          return;
        }

        this.loading = true;
        api.exportMarketBuySummary(buildMarketBuySummaryParams(records)).finally(() => {
          this.loading = false;
        });
      },
      // 按勾选单据导出买菜汇总2
      marketBuySummary2() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要汇总的销售出库单！');
          return;
        }

        this.loading = true;
        api.exportMarketBuySummary2(buildMarketBuySummary2Params(records)).finally(() => {
          this.loading = false;
        });
      },
      viewSaleOrderDetail(id) {
        this.saleOrderId = id;
        this.$nextTick(() => this.$refs.viewSaleOrderDetailDialog.openDialog());
      },
      viewDetail(id) {
        this.id = id;
        this.$nextTick(() => this.$refs.viewDialog.openDialog());
      },
      /** 判断销售出库单是否已进入对账或结算流程。 */
      isSettleLocked(row) {
        return [0, 1, 3].includes(Number(row.settleStatus));
      },
      createActions(row) {
        return [
          {
            label: '查看',
            onClick: () => {
              this.viewDetail(row.id);
            },
          },
          {
            label: '导出明细',
            onClick: () => {
              this.exportDetails(row);
            },
          },
          {
            label: '打印',
            onClick: () => {
              this.printOrder(row);
            },
          },
          {
            permission: ['sale:out:approve'],
            label: '审核',
            ifShow: () => {
              return (
                SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.openChildPage('/sale/out/approve/' + row.id);
            },
          },
          {
            permission: ['sale:out:modify'],
            label: '修改',
            ifShow: () => {
              return (
                (SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                  SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)) &&
                !this.isSettleLocked(row)
              );
            },
            onClick: () => {
              this.openModifyDialog(row);
            },
          },
          {
            permission: ['sale:out:modify'],
            label: '修改备注',
            ifShow: () => !this.isSettleLocked(row),
            onClick: () => {
              this.openDescriptionDialog(row);
            },
          },
          {
            permission: ['sale:out:delete'],
            label: '删除',
            danger: true,
            ifShow: () => {
              return (
                (SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                  SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)) &&
                !this.isSettleLocked(row)
              );
            },
            onClick: () => {
              this.deleteOrder(row);
            },
          },
        ];
      },
      onRefreshPage() {
        this.applyRouteQuery();
        this.search();
      },
      /**
       * 打开成本重算弹窗，默认时间范围为月初到今天
       */
      openCostRecalculate() {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const formatDate = (d) => {
          const year = d.getFullYear();
          const month = String(d.getMonth() + 1).padStart(2, '0');
          const day = String(d.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        };
        this.costRecalculateDateRange = [formatDate(firstDay), formatDate(now)];
        this.costRecalculateVisible = true;
      },
      /**
       * 执行月底成本重算
       */
      executeCostRecalculate() {
        const [beginDate, endDate] = this.costRecalculateDateRange || [];
        if (!beginDate || !endDate) {
          return;
        }
        this.costRecalculateLoading = true;
        api
          .monthEndRecalculate({ beginDate, endDate })
          .then((res) => {
            createSuccess(
              `重算完成：更新单据 ${res.updatedSheetCount} 条，明细 ${res.updatedDetailCount} 条` +
                (res.notFilledCount > 0 ? `，${res.notFilledCount} 条未填充` : ''),
            );
            this.costRecalculateVisible = false;
            this.search();
          })
          .finally(() => {
            this.costRecalculateLoading = false;
          });
      },
    },
  });
</script>

<style scoped></style>
