<template>
  <div v-permission="['customer-settle:sheet:query']" class="customer-settle-detail">
    <page-wrapper v-if="!routeError" content-full-height fixed-height dense>
      <vxe-grid
        id="CustomerSettleDetail"
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
        :pager-config="pagerConfig"
        :checkbox-config="checkboxConfig"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
        @checkbox-change="syncSelection"
        @checkbox-all="syncSelection"
        @page-change="handlePageChange"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="$refs.grid.refreshColumn()" @keyup.enter="search">
              <j-form-item label="业务类型">
                <a-select
                  v-model:value="searchFormData.bizType"
                  allow-clear
                  placeholder="全部业务类型"
                >
                  <a-select-option :value="1">销售出库单</a-select-option>
                  <a-select-option :value="2">销售退货单</a-select-option>
                </a-select>
              </j-form-item>
              <j-form-item label="单据号">
                <a-input
                  v-model:value.trim="searchFormData.code"
                  allow-clear
                  placeholder="请输入单据号"
                />
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="search">查询</a-button>
            <a-button @click="openRecordPage">结算记录</a-button>
            <a-button v-permission="['customer-settle:sheet:export']" @click="exportList"
              >导出</a-button
            >
            <a-button
              v-permission="['customer-settle:sheet:approve']"
              type="primary"
              :disabled="!canConfirmCheck"
              @click="openCheckDialog"
            >
              确认对账
            </a-button>
            <a-button
              v-permission="['customer-settle:sheet:approve']"
              type="primary"
              :disabled="!canConfirmSettle"
              @click="openSettleDialog"
            >
              确认结算
            </a-button>
          </a-space>
        </template>

        <template #code_default="{ row }">
          <a-button type="link" size="small" @click="openBizList(row)">{{
            row.code || '-'
          }}</a-button>
        </template>

        <template #settleStatus_default="{ row }">
          <span :class="['status-text', getStatusClass(row.settleStatus)]">
            {{ SETTLE_STATUS.getDesc(row.settleStatus) || '-' }}
          </span>
        </template>
      </vxe-grid>
    </page-wrapper>
    <a-result v-else status="error" :title="routeError" />

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
          <div class="amount-tip">选中单据应收总额：{{ formatAmount(selectedTotalAmount) }}</div>
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
            :precision="2"
            style="width: 100%"
            placeholder="请输入结算金额"
          />
          <div class="amount-tip"
            >选中单据未结算总额：{{ formatAmount(selectedTotalUnSettleAmount) }}</div
          >
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
  </div>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';
  import * as checkApi from '@/api/customer-settle/check';
  import * as api from '@/api/customer-settle/sheet';
  import { CUSTOMER_SALE_SETTLE_BIZ_TYPE } from '@/enums/biz/customerSaleSettleBizType';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { buildSortPageVo } from '@/utils/utils';
  import {
    buildCustomerDetailQuery,
    buildDirectSettlePayload,
    canDirectSettle,
    getCustomerSettleBizListPath,
    isDirectSettleAmountValid,
    queryCustomerSettleWorkbenchPages,
    validateCustomerDetailRoute,
  } from './customerSettleWorkbench';

  /** 单客户结算明细页面。 */
  export default defineComponent({
    name: 'CustomerSettleDetail',
    mixins: [multiplePageMix],
    data() {
      const routeQuery = this.$route.query || {};
      return {
        loading: false,
        routeError: validateCustomerDetailRoute(routeQuery),
        SETTLE_STATUS,
        CUSTOMER_SALE_SETTLE_BIZ_TYPE,
        searchFormData: {
          bizType: routeQuery.bizType
            ? Number(routeQuery.bizType)
            : (undefined as number | undefined),
          code: routeQuery.code ? String(routeQuery.code) : '',
        },
        toolbarConfig: {
          refresh: { queryMethod: () => this.loadList() },
          slots: { buttons: 'toolbar_buttons' },
        },
        pagerConfig: {
          currentPage: 1,
          pageSize: 20,
          total: 0,
          layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'Sizes', 'Total'],
        },
        checkboxConfig: {
          checkMethod: ({ row }: { row: any }) => this.canCheckRow(row),
        },
        loadRequestSequence: 0,
        tableData: [] as any[],
        selectedRows: [] as any[],
        tableColumn: [
          { type: 'checkbox', width: 45, fixed: 'left' },
          { type: 'seq', title: '序号', width: 60, fixed: 'left' },
          {
            field: 'code',
            title: '销售单/销售退货单',
            width: 180,
            slots: { default: 'code_default' },
          },
          {
            field: 'bizType',
            title: '单据类型',
            width: 120,
            formatter: ({ cellValue }: { cellValue: number }) =>
              CUSTOMER_SALE_SETTLE_BIZ_TYPE.getDesc(cellValue) || '-',
          },
          { field: 'totalAmount', title: '应收', width: 110, align: 'right' },
          { field: 'receivedAmount', title: '已收', width: 110, align: 'right' },
          { field: 'checkAmount', title: '对账金额', width: 110, align: 'right' },
          { field: 'settleAmount', title: '已结算', width: 110, align: 'right' },
          { field: 'unSettleAmount', title: '未结算', width: 110, align: 'right' },
          {
            field: 'settleStatus',
            title: '状态',
            width: 100,
            slots: { default: 'settleStatus_default' },
          },
        ],
        checkDialog: {
          visible: false,
          loading: false,
          amount: undefined as number | undefined,
          description: '',
        },
        settleDialog: {
          visible: false,
          loading: false,
          amount: undefined as number | undefined,
          description: '',
        },
      };
    },
    computed: {
      /** 获取由路由固定的客户 ID。 */
      customerId(): string {
        return String(this.$route.query.customerId || '').trim();
      },
      /** 计算选中单据的应收总额。 */
      selectedTotalAmount(): number {
        return this.selectedRows.reduce((total, item) => total + Number(item.totalAmount || 0), 0);
      },
      /** 计算选中单据的未结算总额。 */
      selectedTotalUnSettleAmount(): number {
        return this.selectedRows.reduce(
          (total, item) => total + Number(item.unSettleAmount || 0),
          0,
        );
      },
      /** 判断当前勾选是否均为待对账单据。 */
      canConfirmCheck(): boolean {
        return (
          this.selectedRows.length > 0 &&
          this.selectedRows.every(
            (item) =>
              item.customerId === this.customerId &&
              [7, '7', 'UN_CHECK_BILL'].includes(item.settleStatus),
          )
        );
      },
      /** 判断当前勾选是否可直接结算。 */
      canConfirmSettle(): boolean {
        return (
          this.selectedRows.every((item) => item.customerId === this.customerId) &&
          canDirectSettle(this.selectedRows)
        );
      },
    },
    watch: {
      /** 路由查询参数变化时同步固定客户状态。 */
      '$route.query': {
        handler(routeQuery, previousRouteQuery) {
          this.handleRouteQueryChange(routeQuery || {}, previousRouteQuery || {});
        },
      },
    },
    created() {
      this.handleRouteQueryChange(this.$route.query || {}, {});
    },
    methods: {
      /** 清空无效路由或客户切换前遗留的表格和勾选状态。 */
      clearRouteData() {
        this.tableData = [];
        this.selectedRows = [];
        this.pagerConfig.total = 0;
        this.checkDialog.visible = false;
        this.settleDialog.visible = false;
        (this.$refs.grid as any)?.clearCheckboxRow?.();
      },
      /** 每个请求入口重新校验路由客户参数。 */
      ensureValidRoute(showError = true): boolean {
        this.routeError = validateCustomerDetailRoute(this.$route.query || {});
        if (!this.routeError) {
          return true;
        }
        this.clearRouteData();
        if (showError) {
          createError(this.routeError);
        }
        return false;
      },
      /** 处理组件复用时的路由客户切换。 */
      handleRouteQueryChange(
        routeQuery: Record<string, unknown>,
        previousRouteQuery: Record<string, unknown>,
      ) {
        this.routeError = validateCustomerDetailRoute(routeQuery);
        if (this.routeError) {
          this.clearRouteData();
          createError(this.routeError);
          return;
        }
        const customerId = String(routeQuery.customerId || '').trim();
        const previousCustomerId = String(previousRouteQuery.customerId || '').trim();
        if (customerId !== previousCustomerId) {
          this.clearRouteData();
          this.search();
        }
      },
      /** 格式化金额。 */
      formatAmount(value: number): string {
        return Number(value || 0).toFixed(2);
      },
      /** 汇总指定金额列。 */
      sumByField(data: any[], field: string): number {
        return (data || []).reduce((total, item) => total + Number(item[field] || 0), 0);
      },
      /** 生成表格合计行。 */
      footerMethod({ columns, data }: { columns: any[]; data: any[] }) {
        const amountFields = [
          'totalAmount',
          'receivedAmount',
          'checkAmount',
          'settleAmount',
          'unSettleAmount',
        ];
        return [
          columns.map((column) => {
            if (column.type === 'seq') return '合计';
            return amountFields.includes(column.field)
              ? this.formatAmount(this.sumByField(data, column.field))
              : '';
          }),
        ];
      },
      /** 生成由路由客户限定的工作台查询条件。 */
      buildQueryParams() {
        return buildCustomerDetailQuery(this.$route.query || {}, {
          ...buildSortPageVo(this.pagerConfig, []),
          code: this.searchFormData.code || undefined,
          bizType: this.searchFormData.bizType || undefined,
        });
      },
      /** 查询当前固定客户的工作台数据。 */
      async loadList() {
        const requestSequence = ++this.loadRequestSequence;
        if (!this.ensureValidRoute()) return;
        this.loading = true;
        try {
          const res = await queryCustomerSettleWorkbenchPages(
            this.buildQueryParams(),
            api.querySaleSettleInfos as any,
          );
          if (requestSequence === this.loadRequestSequence && !this.routeError) {
            this.tableData = res?.datas || [];
            this.pagerConfig.total = res?.totalCount || 0;
            this.$nextTick(() => {
              if (requestSequence === this.loadRequestSequence) {
                this.syncSelection();
              }
            });
          }
        } catch (err: any) {
          if (requestSequence !== this.loadRequestSequence) return;
          this.tableData = [];
          this.selectedRows = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询客户结算单据失败，请稍后重试！');
        } finally {
          if (requestSequence === this.loadRequestSequence) {
            this.loading = false;
          }
        }
      },
      /** 重置到首页并查询。 */
      search() {
        this.pagerConfig.currentPage = 1;
        this.loadList();
      },
      /** 处理分页切换。 */
      handlePageChange({ currentPage, pageSize }: { currentPage: number; pageSize: number }) {
        this.pagerConfig.currentPage = currentPage;
        this.pagerConfig.pageSize = pageSize;
        this.loadList();
      },
      /** 同步表格勾选数据。 */
      syncSelection() {
        this.selectedRows = (this.$refs.grid as any)?.getCheckboxRecords?.() || [];
      },
      /** 限制只能选择当前客户待对账、待结算或部分结算的单据。 */
      canCheckRow(row: any): boolean {
        return (
          row.customerId === this.customerId && [7, 0, 1, '7', '0', '1'].includes(row.settleStatus)
        );
      },
      /** 返回状态对应的展示样式。 */
      getStatusClass(status: number): string {
        if (SETTLE_STATUS.UN_CHECK_BILL.equalsCode(status)) return 'status-text--warning';
        if (SETTLE_STATUS.UN_SETTLE.equalsCode(status)) return 'status-text--primary';
        if (SETTLE_STATUS.PART_SETTLE.equalsCode(status)) return 'status-text--processing';
        if (SETTLE_STATUS.SETTLED.equalsCode(status)) return 'status-text--success';
        return 'status-text--muted';
      },
      /** 跳转到当前客户的结算记录。 */
      openRecordPage() {
        this.openChildPage({
          path: '/settle/customer/sheet-record',
          query: { customerId: this.customerId },
        });
      },
      /** 跳转到关联的销售单据列表。 */
      openBizList(row: any) {
        const path = getCustomerSettleBizListPath(row.bizType);
        if (!path) {
          createError('业务类型不正确，无法跳转关联单据！');
          return;
        }
        this.openChildPage({ path, query: { code: row.code || '' } });
      },
      /** 提交当前固定客户的工作台导出任务。 */
      async exportList() {
        if (!this.ensureValidRoute()) return;
        this.loading = true;
        try {
          const params = this.buildQueryParams();
          if (params.bizType) {
            await api.exportSaleSettleInfos(params as any);
          } else {
            await Promise.all([
              api.exportSaleSettleInfos({ ...params, bizType: 1 } as any),
              api.exportSaleSettleInfos({ ...params, bizType: 2 } as any),
            ]);
          }
          createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
        } catch (err: any) {
          createError(err?.message || '创建导出任务失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      /** 打开确认对账弹窗。 */
      openCheckDialog() {
        if (!this.ensureValidRoute()) return;
        if (!this.canConfirmCheck) {
          createError('请勾选状态为“待对账”的单据！');
          return;
        }
        this.checkDialog.amount = Number(this.selectedTotalAmount.toFixed(2));
        this.checkDialog.description = '';
        this.checkDialog.visible = true;
      },
      /** 提交当前固定客户的对账单。 */
      async submitCheck() {
        if (!this.ensureValidRoute()) return;
        const amount = Number(this.checkDialog.amount);
        if (!this.canConfirmCheck || !isDirectSettleAmountValid(amount, this.selectedTotalAmount)) {
          createError('对账金额必须与所选单据应收净额方向一致，且不能超出其范围！');
          return;
        }
        this.checkDialog.loading = true;
        try {
          await checkApi.directApprovePass({
            customerId: this.customerId,
            checkAmount: amount,
            description: this.checkDialog.description || undefined,
            items: this.selectedRows.map(({ id, bizType }) => ({ bizId: id, bizType })),
          });
          createSuccess('确认对账成功！');
          this.checkDialog.visible = false;
          this.search();
        } catch (err: any) {
          createError(err?.message || '确认对账失败，请稍后重试！');
        } finally {
          this.checkDialog.loading = false;
        }
      },
      /** 打开直接结算弹窗。 */
      openSettleDialog() {
        if (!this.ensureValidRoute()) return;
        if (!this.canConfirmSettle) {
          createError('请勾选状态为“待结算”或“部分结算”的单据！');
          return;
        }
        this.settleDialog.amount = Number(this.selectedTotalUnSettleAmount.toFixed(2));
        this.settleDialog.description = '';
        this.settleDialog.visible = true;
      },
      /** 提交当前固定客户的直接结算。 */
      async submitSettle() {
        if (!this.ensureValidRoute()) return;
        const amount = Number(this.settleDialog.amount);
        if (
          !this.canConfirmSettle ||
          !isDirectSettleAmountValid(amount, this.selectedTotalUnSettleAmount)
        ) {
          createError('结算金额必须与所选单据未结算净额方向一致，且不能超出其范围！');
          return;
        }
        this.settleDialog.loading = true;
        try {
          await api.directApprovePass({
            ...buildDirectSettlePayload(this.selectedRows, amount, this.settleDialog.description),
            customerId: this.customerId,
          });
          createSuccess('结算成功！');
          this.settleDialog.visible = false;
          this.search();
        } catch (err: any) {
          createError(err?.message || '结算失败，请稍后重试！');
        } finally {
          this.settleDialog.loading = false;
        }
      },
    },
  });
</script>

<style scoped lang="less">
  .amount-tip {
    margin-top: 8px;
    color: #8c8c8c;
  }
  .status-text--warning {
    color: #fa8c16;
  }
  .status-text--primary {
    color: #1677ff;
  }
  .status-text--processing {
    color: #fa8c16;
  }
  .status-text--success {
    color: #52c41a;
  }
  .status-text--muted {
    color: #8c8c8c;
  }
</style>
