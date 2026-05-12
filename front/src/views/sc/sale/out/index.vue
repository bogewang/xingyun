<template>
  <div>
    <div v-permission="['sale:out:query']">
      <page-wrapper content-full-height fixed-height>
        <!-- 数据列表 -->
        <vxe-grid
          id="SaleOutSheet"
          ref="grid"
          resizable
          show-overflow
          highlight-hover-row
          keep-source
          row-id="id"
          :proxy-config="proxyConfig"
          :columns="tableColumn"
          :toolbar-config="toolbarConfig"
          :custom-config="{}"
          :pager-config="{}"
          :loading="loading"
          height="auto"
        >
          <template #form>
            <j-border>
              <j-form bordered @collapse="$refs.grid.refreshColumn()">
                <j-form-item label="单据号">
                  <a-input v-model:value="searchFormData.code" allow-clear />
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

                <j-form-item label="订单日期">
                  <a-range-picker
                    v-model:value="orderDateRange"
                    value-format="YYYY-MM-DD"
                    :placeholder="['开始日期', '结束日期']"
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
                      >{{ item.desc }}</a-select-option
                    >
                  </a-select>
                </j-form-item>

                <template #more>
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
                        >{{ item.desc }}</a-select-option
                      >
                    </a-select>
                  </j-form-item>
                </template>
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
                v-permission="['sale:out:export']"
                :icon="h(DownloadOutlined)"
                @click="exportList"
                >导出</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(PrinterOutlined)"
                @click="tagPrint"
                >标签打印</a-button
              >
            </a-space>
          </template>

          <!-- 销售订单号 列自定义内容 -->
          <template #saleOrderCode_default="{ row }">
            <span v-if="isEmpty(row.saleOrderCode)">-</span>
            <span v-else>
              <a
                v-permission="['sale:order:query']"
                @click="viewSaleOrderDetail(row.saleOrderId)"
                >{{ row.saleOrderCode }}</a
              >
              <span v-no-permission="['sale:order:query']">{{ row.saleOrderCode }}</span>
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

      <!-- 销售订单查看窗口 -->
      <sale-order-detail :id="saleOrderId" ref="viewSaleOrderDetailDialog" />

      <!-- 批量操作 -->
      <batch-handler
        ref="batchApprovePassHandlerDialog"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          // { field: 'scCode', title: '仓库编号', width: 100 },
          // { field: 'scName', title: '仓库名称', width: 120 },
          // { field: 'customerCode', title: '客户编号', width: 100 },
          { field: 'customerName', title: '客户名称', width: 120 },
          // { field: 'salerName', title: '销售员', width: 100 },
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
          // { field: 'scCode', title: '仓库编号', width: 100 },
          // { field: 'scName', title: '仓库名称', width: 120 },
          // { field: 'customerCode', title: '客户编号', width: 100 },
          { field: 'customerName', title: '客户名称', width: 120 },
          // { field: 'salerName', title: '销售员', width: 100 },
        ]"
        title="审核拒绝"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchApproveRefuse"
        @confirm="search"
      />
      <batch-handler
        ref="batchDeleteHandlerDialog"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          // { field: 'scCode', title: '仓库编号', width: 100 },
          // { field: 'scName', title: '仓库名称', width: 120 },
          // { field: 'customerCode', title: '客户编号', width: 100 },
          { field: 'customerName', title: '客户名称', width: 120 },
          // { field: 'salerName', title: '销售员', width: 100 },
        ]"
        title="批量删除"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchDelete"
        @confirm="search"
      />
      <order-print-dialog />
    </div>
  </div>
</template>

<script>
  import { h, defineComponent } from 'vue';
  import Detail from './detail.vue';
  import ApproveRefuse from '@/components/ApproveRefuse';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import moment from 'moment';
  import {
    SearchOutlined,
    PlusOutlined,
    CheckOutlined,
    CloseOutlined,
    DeleteOutlined,
    DownloadOutlined,
    PrinterOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import * as configApi from '@/api/sc/sale/config';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { printMix } from '@/mixins/print.ts';
  import { isEmpty, buildSortPageVo } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import BatchHandler from '@/components/BatchHandler';
  import PrintDialog from '/@/components/PrintDialog';

  export default defineComponent({
    name: 'SaleOutSheet',
    components: {
      Detail,
      ApproveRefuse,
      SaleOrderDetail,
      BatchHandler,
      OrderPrintDialog: PrintDialog,
    },
    mixins: [multiplePageMix, printMix],
    setup() {
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        CheckOutlined,
        CloseOutlined,
        DeleteOutlined,
        DownloadOutlined,
        PrinterOutlined,
        isEmpty,
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
          scId: '',
          customerId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
        },
        orderDateRange: [
          moment().subtract(1, 'M').format('YYYY-MM-DD'),
          moment().format('YYYY-MM-DD'),
        ],
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
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'code', title: '单据号', width: 180, sortable: true },
          { field: 'customerName', title: '客户名称', width: 120 },
          { field: 'orderDate', title: '订单日期', width: 120 },
          { field: 'totalAmount', title: '单据总金额', align: 'right', width: 100 },
          { field: 'totalProfit', title: '总利润', align: 'right', width: 100 },
          { field: 'totalNum', title: '商品数量', align: 'right', width: 120 },
          { field: 'createTime', title: '操作时间', width: 170, sortable: true },
          { field: 'createBy', title: '操作人', width: 100 },
          {
            field: 'status',
            title: '状态',
            width: 100,
            formatter: ({ cellValue }) => {
              return SALE_OUT_SHEET_STATUS.getDesc(cellValue);
            },
          },
          { field: 'approveTime', title: '审核时间', width: 170, sortable: true },
          { field: 'approveBy', title: '审核人', width: 100 },
          { field: 'description', title: '备注', width: 200 },
          { title: '操作', width: 200, fixed: 'right', slots: { default: 'action_default' } },
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
      };
    },
    created() {},
    methods: {
      // 列表发生查询时的事件
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      resetSearchForm() {
        this.searchFormData = {
          code: '',
          scId: '',
          customerId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
        };
        this.orderDateRange = [
          moment().subtract(1, 'M').format('YYYY-MM-DD'),
          moment().format('YYYY-MM-DD'),
        ];
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
        if (!isEmpty(row.saleOrderId)) {
          this.openChildPage('/sale/out/modify/require/' + row.id);
        } else {
          this.openChildPage('/sale/out/modify/un-require/' + row.id);
        }
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
      async tagPrint() {
        this.loading = true;
        try {
          const res = await api.tagPrint(this.buildQueryParams({}, {}));
          await this.vgPrintPreview(PRINT_TYPE.SALE_TAG.code, res);
        } finally {
          this.loading = false;
        }
      },
      viewSaleOrderDetail(id) {
        this.saleOrderId = id;
        this.$nextTick(() => this.$refs.viewSaleOrderDetailDialog.openDialog());
      },
      createActions(row) {
        return [
          {
            label: '查看',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.viewDialog.openDialog());
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
                SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.openModifyDialog(row);
            },
          },
          {
            permission: ['sale:out:delete'],
            label: '删除',
            danger: true,
            ifShow: () => {
              return (
                SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.deleteOrder(row);
            },
          },
        ];
      },
      onRefreshPage() {
        this.search();
      },
    },
  });
</script>
<style scoped></style>
