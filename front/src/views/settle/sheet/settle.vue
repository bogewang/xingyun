<template>
  <div v-permission="['settle:sheet:query']" class="settle-workbench">
    <page-wrapper content-full-height fixed-height dense>
      <vxe-grid
        id="SupplierSettleWorkbench"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        row-id="id"
        :data="tableData"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="pagerConfig"
        :footer-method="footerMethod"
        :loading="loading"
        :height="'auto'"
        @checkbox-change="syncSelection"
        @checkbox-all="syncSelection"
        @page-change="handlePageChange"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="$refs.grid.refreshColumn()" @keyup.enter="search">
              <j-form-item label="供应商">
                <supplier-selector
                  v-model:value="searchFormData.supplierId"
                  allow-clear
                  placeholder="请选择供应商"
                />
              </j-form-item>
              <j-form-item label="状态">
                <a-select
                  v-model:value="searchFormData.settleStatus"
                  allow-clear
                  placeholder="全部状态"
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
              <j-form-item label="单据日期">
                <a-range-picker
                  v-model:value="orderDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                />
              </j-form-item>
              <j-form-item label="关键字">
                <a-input
                  v-model:value.trim="searchFormData.keyword"
                  allow-clear
                  placeholder="货流单号/采购单号"
                />
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button type="primary" @click="openRecordPage">结算记录</a-button>
            <a-button :icon="h(DownloadOutlined)" @click="exportList">导出</a-button>
            <a-button
              v-permission="['settle:sheet:query']"
              :disabled="!canConfirmCheck"
              @click="openCheckDialog"
            >
              确认对账
            </a-button>
            <a-button
              v-permission="['settle:sheet:query']"
              type="primary"
              :disabled="!canConfirmSettle"
              @click="openSettleDialog"
            >
              确认结算
            </a-button>
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a-button type="link" size="small" @click="openDetail(row.id)">
            {{ row.code || '-' }}
          </a-button>
        </template>

        <template #settleStatus_default="{ row }">
          <span :class="['status-text', getStatusClass(row.settleStatus)]">
            {{ SETTLE_STATUS.getDesc(row.settleStatus) || '-' }}
          </span>
        </template>
      </vxe-grid>
    </page-wrapper>

    <a-modal
      v-model:open="checkDialog.visible"
      title="确认对账"
      :confirm-loading="checkDialog.loading"
      @ok="submitCheck"
    >
      <a-form layout="vertical">
        <a-form-item label="对账金额">
          <a-input-number
            v-model:value="checkDialog.amount"
            :precision="2"
            style="width: 100%"
            placeholder="请输入对账金额"
          />
          <div class="amount-tip">
            选中货流单总金额：{{ formatAmount(selectedTotalAmount) }}
          </div>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value.trim="checkDialog.description"
            :rows="4"
            maxlength="200"
            placeholder="请输入备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="settleDialog.visible"
      title="确认结算"
      :confirm-loading="settleDialog.loading"
      @ok="submitSettle"
    >
      <a-form layout="vertical">
        <a-form-item label="结算金额">
          <a-input-number
            v-model:value="settleDialog.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入结算金额"
          />
          <div class="amount-tip">选中单据未结算总额：{{ formatAmount(selectedTotalUnSettleAmount) }}</div>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value.trim="settleDialog.description"
            :rows="4"
            maxlength="200"
            placeholder="请输入备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <detail :id="currentId" ref="detailDialog" />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import moment from 'moment';
  import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue';
  import * as settleCheckApi from '@/api/settle/check';
  import * as settleApi from '@/api/settle/sheet';
  import Detail from '@/views/sc/purchase/receive/detail.vue';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';
  import { SETTLE_CHECK_SHEET_BIZ_TYPE } from '@/enums/biz/settleCheckSheetBizType';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import {
    buildSortPageVo,
    dateTimeToDate,
    getDateTimeWithMaxTime,
    getDateTimeWithMinTime,
    isEmpty,
  } from '@/utils/utils';

  export default defineComponent({
    name: 'AddSupplierSettleSheet',
    components: {
      Detail,
      SupplierSelector,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        SearchOutlined,
        DownloadOutlined,
        SETTLE_STATUS,
        SETTLE_CHECK_SHEET_BIZ_TYPE,
      };
    },
    data() {
      const routeQuery = this.$route.query || {};
      const keyword = routeQuery.code
        ? String(routeQuery.code).trim()
        : routeQuery.keyword
          ? String(routeQuery.keyword).trim()
          : '';
      const startTime = routeQuery.startTime
        ? moment(String(routeQuery.startTime)).format('YYYY-MM-DD')
        : moment(getDateTimeWithMinTime(moment().subtract(3, 'M'))).format('YYYY-MM-DD');
      const endTime = routeQuery.endTime
        ? moment(String(routeQuery.endTime)).format('YYYY-MM-DD')
        : moment(getDateTimeWithMaxTime(moment())).format('YYYY-MM-DD');

      return {
        loading: false,
        currentId: '',
        keywordField: 'code',
        searchFormData: {
          supplierId: routeQuery.supplierId ? String(routeQuery.supplierId) : '',
          settleStatus: undefined,
          keyword,
        },
        orderDateRange: keyword ? [] : [startTime, endTime],
        toolbarConfig: {
          refresh: {
            queryMethod: () => this.loadList(),
          },
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        pagerConfig: {
          currentPage: 1,
          pageSize: 20,
          total: 0,
          layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'Sizes', 'Total'],
        },
        tableData: [],
        selectedRows: [],
        tableColumn: [
          { type: 'checkbox', width: 45, fixed: 'left' },
          { type: 'seq', title: '序号', width: 60, fixed: 'left' },
          { field: 'supplierName', title: '供应商', minWidth: 120 },
          { field: 'code', title: '货流单号', width: 170, slots: { default: 'code_default' } },
          { field: 'orderDate', title: '下单时间', width: 120, sortable: true },
          {
            field: 'bizType',
            title: '货单类型',
            width: 100,
            formatter: () => '进货单',
          },
          { field: 'totalNum', title: '商品数量', width: 90, align: 'right' },
          { field: 'totalAmount', title: '货流单金额', width: 100, align: 'right' },
          { field: 'unSettleAmount', title: '本单未结算', width: 100, align: 'right' },
          {
            field: 'checkAmount',
            title: '对账金额',
            width: 110,
            align: 'right',
            formatter: ({ cellValue }) =>
              cellValue === null || cellValue === undefined || cellValue === ''
                ? '-'
                : Number(cellValue || 0).toFixed(2),
          },
          {
            field: 'settleAmount',
            title: '结算金额',
            width: 110,
            align: 'right',
            formatter: ({ cellValue }) =>
              cellValue === null || cellValue === undefined || cellValue === ''
                ? '-'
                : Number(cellValue || 0).toFixed(2),
          },
          {
            field: 'settleStatus',
            title: '状态',
            width: 100,
            slots: { default: 'settleStatus_default' },
          },
          {
            field: 'checkTime',
            title: '对账时间',
            width: 170,
            formatter: ({ cellValue }) =>
              cellValue ? moment(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-',
          },
          {
            field: 'settleTime',
            title: '结算时间',
            width: 170,
            formatter: ({ cellValue }) =>
              cellValue ? moment(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-',
          },
          {
            field: 'description',
            title: '货流备注',
            minWidth: 180,
          },
          {
            field: 'checkDescription',
            title: '对账备注',
            minWidth: 180,
          },
          {
            field: 'settleDescription',
            title: '结算备注',
            minWidth: 180,
          },
        ],
        checkDialog: {
          visible: false,
          loading: false,
          amount: undefined,
          description: '',
        },
        settleDialog: {
          visible: false,
          loading: false,
          amount: undefined,
          description: '',
        },
      };
    },
    computed: {
      selectedCount() {
        return this.selectedRows.length;
      },
      selectedStatusList() {
        return [...new Set(this.selectedRows.map((item) => item.settleStatus))];
      },
      selectedTotalUnSettleAmount() {
        return this.selectedRows.reduce((total, item) => total + Number(item.unSettleAmount || 0), 0);
      },
      selectedTotalAmount() {
        return this.selectedRows.reduce((total, item) => total + Number(item.totalAmount || 0), 0);
      },
      canConfirmCheck() {
        return (
          this.selectedCount > 0 &&
          this.selectedStatusList.length === 1 &&
          SETTLE_STATUS.UN_CHECK_BILL.equalsCode(this.selectedStatusList[0])
        );
      },
      canConfirmSettle() {
        if (this.selectedCount === 0) {
          return false;
        }

        return this.selectedStatusList.every(
          (status) =>
            SETTLE_STATUS.UN_SETTLE.equalsCode(status) ||
            SETTLE_STATUS.PART_SETTLE.equalsCode(status),
        );
      },
    },
    created() {
      this.applyRouteQuery();
      this.search();
    },
    methods: {
      applyRouteQuery() {
        const routeQuery = this.$route.query || {};
        const keyword = routeQuery.code
          ? String(routeQuery.code).trim()
          : routeQuery.keyword
            ? String(routeQuery.keyword).trim()
            : '';

        this.searchFormData.supplierId = routeQuery.supplierId ? String(routeQuery.supplierId) : '';
        this.searchFormData.keyword = keyword;
        this.orderDateRange = keyword
          ? []
          : [
              routeQuery.startTime
                ? moment(String(routeQuery.startTime)).format('YYYY-MM-DD')
                : moment(getDateTimeWithMinTime(moment().subtract(3, 'M'))).format('YYYY-MM-DD'),
              routeQuery.endTime
                ? moment(String(routeQuery.endTime)).format('YYYY-MM-DD')
                : moment(getDateTimeWithMaxTime(moment())).format('YYYY-MM-DD'),
            ];
      },
      formatAmount(value) {
        return Number(value || 0).toFixed(2);
      },
      formatQuantity(value) {
        const text = Number(value || 0).toFixed(2);
        return text.replace(/\.?0+$/, '');
      },
      getStatusClass(status) {
        if (SETTLE_STATUS.UN_CHECK_BILL.equalsCode(status)) {
          return 'status-text--warning';
        }
        if (SETTLE_STATUS.UN_SETTLE.equalsCode(status)) {
          return 'status-text--primary';
        }
        if (SETTLE_STATUS.PART_SETTLE.equalsCode(status)) {
          return 'status-text--processing';
        }
        if (SETTLE_STATUS.SETTLED.equalsCode(status)) {
          return 'status-text--success';
        }
        return 'status-text--muted';
      },
      footerMethod({ columns, data }) {
        const fieldMap = {
          totalNum: this.formatQuantity(this.sumByField(data, 'totalNum')),
          totalGiftNum: this.formatQuantity(this.sumByField(data, 'totalGiftNum')),
          totalAmount: this.formatAmount(this.sumByField(data, 'totalAmount')),
          checkAmount: this.formatAmount(this.sumByField(data, 'checkAmount')),
          settleAmount: this.formatAmount(this.sumByField(data, 'settleAmount')),
        };

        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }
            return fieldMap[column.field] || '';
          }),
        ];
      },
      sumByField(data, field) {
        return (data || []).reduce((total, item) => {
          const value = Number(item?.[field] ?? 0);
          return total + (Number.isNaN(value) ? 0 : value);
        }, 0);
      },
      buildQueryParams() {
        const keyword = this.searchFormData.keyword || '';
        const params = {
          ...buildSortPageVo(
            {
              currentPage: this.pagerConfig.currentPage,
              pageSize: this.pagerConfig.pageSize,
            },
            [],
          ),
          supplierId: this.searchFormData.supplierId || undefined,
          settleStatus: this.searchFormData.settleStatus,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
        };

        if (keyword) {
          params[this.keywordField] = keyword;
        }

        return params;
      },
      async resolveQueryResult() {
        const keyword = this.searchFormData.keyword || '';
        this.keywordField = 'code';
        let queryParams = this.buildQueryParams();
        let res = await settleApi.queryReceiveSheetSettleInfos(queryParams);

        if (keyword && (!res || res.length === 0)) {
          this.keywordField = 'purchaseOrderCode';
          queryParams = this.buildQueryParams();
          res = await settleApi.queryReceiveSheetSettleInfos(queryParams);
        }

        return {
          queryParams,
          res,
        };
      },
      async loadList() {
        this.loading = true;
        try {
          const { res } = await this.resolveQueryResult();

          this.tableData = (res || []).map((item) => ({
            ...item,
            checkDescription: item.checkDescription || '',
            settleDescription: item.settleDescription || '',
          }));
          this.pagerConfig.total = this.tableData.length;
          this.$nextTick(() => {
            this.syncSelection();
          });
        } catch (err) {
          this.tableData = [];
          this.pagerConfig.total = 0;
          this.selectedRows = [];
          createError(err?.message || '查询结算单据失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      search() {
        this.pagerConfig.currentPage = 1;
        this.loadList();
      },
      handlePageChange({ currentPage, pageSize }) {
        this.pagerConfig.currentPage = currentPage;
        this.pagerConfig.pageSize = pageSize;
        this.loadList();
      },
      syncSelection() {
        this.selectedRows = this.$refs.grid?.getCheckboxRecords?.() || [];
      },
      openDetail(id) {
        this.currentId = id;
        this.$nextTick(() => this.$refs.detailDialog.openDialog());
      },
      openRecordPage() {
        this.openChildPage({
          path: '/settle/supplier/sheet-record',
          query: {
            supplierId: this.searchFormData.supplierId || '',
            startTime: this.orderDateRange?.[0] ? `${this.orderDateRange[0]} 00:00:00` : '',
            endTime: this.orderDateRange?.[1] ? `${this.orderDateRange[1]} 23:59:59` : '',
          },
        });
      },
      exportList() {
        this.loading = true;
        this.resolveQueryResult()
          .then(({ queryParams }) =>
            settleApi.exportReceiveSheetSettleInfos(queryParams),
          )
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .catch((err) => {
            createError(err?.message || '创建导出任务失败，请稍后重试！');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      validateCheckedRows() {
        if (isEmpty(this.selectedRows)) {
          createError('请选择单据！');
          return false;
        }

        const supplierIds = [...new Set(this.selectedRows.map((item) => item.supplierId))];
        if (supplierIds.length > 1) {
          createError('一次只能处理同一个供应商的单据！');
          return false;
        }

        return true;
      },
      openCheckDialog() {
        if (!this.validateCheckedRows()) {
          return;
        }

        if (!this.canConfirmCheck) {
          createError('勾选的单据必须全部为“待对账”，才能执行确认对账！');
          return;
        }

        this.checkDialog.amount = Number((this.selectedTotalAmount).toFixed(2));
        this.checkDialog.description = '';
        this.checkDialog.visible = true;
      },
      buildCheckItems(records) {
        const orderedRecords = this.tableData.filter((item) =>
          records.some((row) => row.id === item.id),
        );
        const items = [];

        orderedRecords.forEach((item) => {
          if (Number(item.totalAmount || 0) <= 0) {
            return;
          }

          items.push({
            id: item.id,
            bizType: SETTLE_CHECK_SHEET_BIZ_TYPE.RECEIVE_SHEET.code,
            description: item.description || '',
            bizAmount: item.totalAmount || 0,
          });
        });

        return items;
      },
      async submitCheck() {
        const amount = Number(this.checkDialog.amount || 0);
        const items = this.buildCheckItems(this.selectedRows);
        if (isEmpty(items)) {
          createError('未生成有效的对账明细，请检查选择的单据！');
          return;
        }

        this.checkDialog.loading = true;
        const supplierId = this.selectedRows[0].supplierId;
        try {
          await settleCheckApi.directApprovePass({
            supplierId,
            checkAmt: amount,
            description: this.checkDialog.description || '',
            items,
          });
          createSuccess('确认对账成功！');
          this.checkDialog.visible = false;
          this.loadList();
        } catch (err) {
          createError(err?.message || '确认对账失败，请稍后重试！');
          throw err;
        } finally {
          this.checkDialog.loading = false;
        }
      },
      openSettleDialog() {
        if (!this.validateCheckedRows()) {
          return;
        }

        if (!this.canConfirmSettle) {
          createError('勾选的单据必须全部为“已对账待结算/部分结算”，才能执行确认结算！');
          return;
        }

        this.settleDialog.amount = Number(this.selectedTotalUnSettleAmount.toFixed(2));
        this.settleDialog.description = '';
        this.settleDialog.visible = true;
      },
      buildSettleItems(records) {
        const orderedRecords = this.tableData.filter((item) =>
          records.some((row) => row.id === item.id),
        );
        const items = [];

        orderedRecords.forEach((item) => {
          items.push({
            id: item.id,
            bizType: SETTLE_CHECK_SHEET_BIZ_TYPE.RECEIVE_SHEET.code,
            unSettleAmount: item.unSettleAmount || 0,
            checkAmount: item.checkAmount || 0,
          });
        });

        return items;
      },
      async submitSettle() {
        const amount = Number(this.settleDialog.amount || 0);
        const items = this.buildSettleItems(this.selectedRows);
        if (isEmpty(items)) {
          createError('未生成有效的结算明细，请检查选择的单据！');
          return;
        }

        const supplierId = this.selectedRows[0].supplierId;
        this.settleDialog.loading = true;
        try {
          await settleApi.directApprovePass({
            supplierId,
            description: this.settleDialog.description || '',
            items,
            settleAmount: amount,
          });
          createSuccess('结算完成！');
          this.settleDialog.visible = false;
          this.loadList();
        } catch (err) {
          createError(err?.message || '确认结算失败，请稍后重试！');
          throw err;
        } finally {
          this.settleDialog.loading = false;
        }
      },
      onRefreshPage() {
        this.applyRouteQuery();
        this.loadList();
      },
    },
  });
</script>

<style scoped lang="less">
  .status-text {
    font-weight: 500;
  }

  .status-text--warning {
    color: #fa8c16;
  }

  .status-text--primary {
    color: #1677ff;
  }

  .status-text--processing {
    color: #13a8a8;
  }

  .status-text--success {
    color: #52c41a;
  }

  .status-text--muted {
    color: #8c8c8c;
  }

  .amount-tip {
    margin-top: 8px;
    color: #8c8c8c;
    font-size: 12px;
    line-height: 1.5;
  }
</style>
