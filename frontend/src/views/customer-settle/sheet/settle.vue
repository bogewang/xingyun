<template>
  <div v-permission="['customer-settle:sheet:query']" class="customer-settle-workbench">
    <page-wrapper content-full-height fixed-height dense>
      <vxe-grid
        id="CustomerSettleWorkbench"
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
              <j-form-item label="客户">
                <customer-selector
                  v-model:value="searchFormData.customerId"
                  allow-clear
                  placeholder="请选择客户"
                />
              </j-form-item>
              <j-form-item label="业务类型">
                <a-select v-model:value="searchFormData.bizType" placeholder="请选择业务类型">
                  <a-select-option :value="1">销售出库单</a-select-option>
                  <a-select-option :value="2">销售退货单</a-select-option>
                </a-select>
              </j-form-item>
              <j-form-item label="单据号">
                <a-input v-model:value.trim="searchFormData.code" allow-clear placeholder="请输入单据号" />
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="search">查询</a-button>
            <a-button @click="openRecordPage">结算记录</a-button>
            <a-button v-permission="['customer-settle:sheet:export']" @click="exportList">导出</a-button>
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
          <a-button type="link" size="small" @click="openBizList(row)">{{ row.code || '-' }}</a-button>
        </template>

        <template #settleStatus_default="{ row }">
          <span :class="['status-text', getStatusClass(row.settleStatus)]">
            {{ SETTLE_STATUS.getDesc(row.settleStatus) || '-' }}
          </span>
        </template>
      </vxe-grid>
    </page-wrapper>

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
          <a-textarea v-model:value.trim="settleDialog.description" :rows="4" maxlength="200" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';
  import * as api from '@/api/customer-settle/sheet';
  import CustomerSelector from '@/components/Selector/CustomerSelector.vue';
  import { CUSTOMER_SETTLE_CHECK_SHEET_BIZ_TYPE } from '@/enums/biz/customerSettleCheckSheetBizType';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { buildSortPageVo } from '@/utils/utils';

  export interface DirectSettleRow {
    customerId?: string;
    settleStatus?: number | string;
  }

  /** 判断勾选的单据是否允许直接结算。 */
  export function canDirectSettle(rows: DirectSettleRow[]): boolean {
    if (!rows.length) {
      return false;
    }

    const customerIds = new Set(rows.map((item) => item.customerId).filter(Boolean));
    const allowedStatuses = new Set([0, 1, '0', '1', 'UN_SETTLE', 'PART_SETTLE']);
    return customerIds.size === 1 && rows.every((item) => allowedStatuses.has(item.settleStatus));
  }

  export default defineComponent({
    name: 'CustomerSettleWorkbench',
    components: { CustomerSelector },
    mixins: [multiplePageMix],
    data() {
      return {
        loading: false,
        SETTLE_STATUS,
        CUSTOMER_SETTLE_CHECK_SHEET_BIZ_TYPE,
        searchFormData: {
          customerId: this.$route.query.customerId ? String(this.$route.query.customerId) : '',
          bizType: Number(this.$route.query.bizType || 1),
          code: this.$route.query.code ? String(this.$route.query.code) : '',
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
        tableData: [] as any[],
        selectedRows: [] as any[],
        tableColumn: [
          { type: 'checkbox', width: 45, fixed: 'left' },
          { type: 'seq', title: '序号', width: 60, fixed: 'left' },
          { field: 'customerName', title: '客户', minWidth: 140 },
          { field: 'code', title: '销售单/销售退货单', width: 180, slots: { default: 'code_default' } },
          {
            field: 'bizType',
            title: '单据类型',
            width: 120,
            formatter: ({ cellValue }: { cellValue: number }) =>
              CUSTOMER_SETTLE_CHECK_SHEET_BIZ_TYPE.getDesc(cellValue) || '-',
          },
          { field: 'totalAmount', title: '应收', width: 110, align: 'right' },
          { field: 'receivedAmount', title: '已收', width: 110, align: 'right' },
          { field: 'settleAmount', title: '已结算', width: 110, align: 'right' },
          { field: 'unSettleAmount', title: '未结算', width: 110, align: 'right' },
          { field: 'settleStatus', title: '状态', width: 100, slots: { default: 'settleStatus_default' } },
        ],
        settleDialog: {
          visible: false,
          loading: false,
          amount: undefined as number | undefined,
          description: '',
        },
      };
    },
    computed: {
      /** 计算选中单据的未结算总额。 */
      selectedTotalUnSettleAmount(): number {
        return this.selectedRows.reduce((total, item) => total + Number(item.unSettleAmount || 0), 0);
      },
      /** 判断当前勾选是否可直接结算。 */
      canConfirmSettle(): boolean {
        return canDirectSettle(this.selectedRows);
      },
    },
    created() {
      this.search();
    },
    methods: {
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
        const amountFields = ['totalAmount', 'receivedAmount', 'settleAmount', 'unSettleAmount'];
        return [
          columns.map((column) => {
            if (column.type === 'seq') return '合计';
            return amountFields.includes(column.field) ? this.formatAmount(this.sumByField(data, column.field)) : '';
          }),
        ];
      },
      /** 生成工作台查询条件。 */
      buildQueryParams() {
        return {
          ...buildSortPageVo(this.pagerConfig, []),
          customerId: this.searchFormData.customerId || undefined,
          code: this.searchFormData.code || undefined,
          bizType: this.searchFormData.bizType,
        };
      },
      /** 查询工作台数据。 */
      async loadList() {
        this.loading = true;
        try {
          const res = await api.querySaleSettleInfos(this.buildQueryParams());
          this.tableData = res?.datas || [];
          this.pagerConfig.total = res?.totalCount || 0;
          this.$nextTick(() => this.syncSelection());
        } catch (err: any) {
          this.tableData = [];
          this.selectedRows = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询客户结算单据失败，请稍后重试！');
        } finally {
          this.loading = false;
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
      /** 返回状态对应的展示样式。 */
      getStatusClass(status: number): string {
        if (SETTLE_STATUS.UN_SETTLE.equalsCode(status)) return 'status-text--primary';
        if (SETTLE_STATUS.PART_SETTLE.equalsCode(status)) return 'status-text--processing';
        if (SETTLE_STATUS.SETTLED.equalsCode(status)) return 'status-text--success';
        return 'status-text--muted';
      },
      /** 跳转到客户结算记录。 */
      openRecordPage() {
        this.openChildPage({
          path: '/settle/customer/sheet-record',
          query: { customerId: this.searchFormData.customerId || '' },
        });
      },
      /** 跳转到关联的销售单据列表。 */
      openBizList(row: any) {
        this.openChildPage({
          path: row.bizType === 2 ? '/sale/return' : '/sale/out',
          query: { code: row.code || '' },
        });
      },
      /** 提交工作台导出任务。 */
      async exportList() {
        this.loading = true;
        try {
          await api.exportSaleSettleInfos(this.buildQueryParams());
          createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
        } catch (err: any) {
          createError(err?.message || '创建导出任务失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      /** 打开直接结算弹窗。 */
      openSettleDialog() {
        if (!this.canConfirmSettle) {
          createError('请勾选同一客户且状态为“待结算”或“部分结算”的单据！');
          return;
        }
        this.settleDialog.amount = Number(this.selectedTotalUnSettleAmount.toFixed(2));
        this.settleDialog.description = '';
        this.settleDialog.visible = true;
      },
      /** 提交直接结算。 */
      async submitSettle() {
        if (!this.canConfirmSettle) {
          createError('勾选的单据不满足结算条件！');
          return;
        }
        if (this.settleDialog.amount === undefined || this.settleDialog.amount < 0) {
          createError('请输入正确的结算金额！');
          return;
        }

        this.settleDialog.loading = true;
        try {
          await api.directApprovePass({
            customerId: this.selectedRows[0].customerId,
            settleAmount: this.settleDialog.amount,
            description: this.settleDialog.description || undefined,
            items: this.selectedRows.map(({ id, bizType }) => ({ bizId: id, bizType })),
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
  .amount-tip { margin-top: 8px; color: #8c8c8c; }
  .status-text--primary { color: #1677ff; }
  .status-text--processing { color: #fa8c16; }
  .status-text--success { color: #52c41a; }
  .status-text--muted { color: #8c8c8c; }
</style>
