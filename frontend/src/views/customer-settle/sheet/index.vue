<template>
  <div v-permission="['customer-settle:sheet:query']" class="customer-settle-overview">
    <page-wrapper content-full-height fixed-height dense>
      <vxe-grid
        id="CustomerSettleOverview"
        ref="grid"
        auto-resize
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        row-id="customerId"
        :data="tableData"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :pager-config="pagerConfig"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
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
              <j-form-item label="单据日期" :content-nest="false">
                <div class="date-range-container">
                  <a-date-picker
                    v-model:value="searchFormData.orderStartTime"
                    placeholder=""
                    value-format="YYYY-MM-DD 00:00:00"
                  />
                  <span class="date-split">至</span>
                  <a-date-picker
                    v-model:value="searchFormData.orderEndTime"
                    placeholder=""
                    value-format="YYYY-MM-DD 23:59:59"
                  />
                </div>
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="search">查询</a-button>
            <a-button @click="openRecordPage">结算记录</a-button>
            <a-button v-permission="['customer-settle:sheet:export']" @click="exportList">
              导出
            </a-button>
          </a-space>
        </template>

        <template #action_default="{ row }">
          <a-button type="link" size="small" @click="openDetailPage(row)">结算</a-button>
        </template>
      </vxe-grid>
    </page-wrapper>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';
  import moment from 'moment';
  import * as api from '@/api/customer-settle/sheet';
  import CustomerSelector from '@/components/Selector/CustomerSelector.vue';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import {
    buildSortPageVo,
    formatDateTime,
    getDateTimeWithMaxTime,
    getDateTimeWithMinTime,
  } from '@/utils/utils';

  /** 客户结算总览页面。 */
  export default defineComponent({
    name: 'CustomerSettleOverview',
    components: { CustomerSelector },
    mixins: [multiplePageMix],
    data() {
      return {
        loading: false,
        searchFormData: {
          customerId: this.$route.query.customerId ? String(this.$route.query.customerId) : '',
          orderStartTime: formatDateTime(getDateTimeWithMinTime(moment().subtract(3, 'M'))),
          orderEndTime: formatDateTime(getDateTimeWithMaxTime(moment())),
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
        tableColumn: [
          { type: 'seq', title: '序号', width: 60, fixed: 'left' },
          {
            title: '操作',
            width: 100,
            fixed: 'left',
            slots: { default: 'action_default' },
          },
          { field: 'customerCode', title: '客户编号', width: 130, fixed: 'left' },
          { field: 'customerName', title: '客户名称', width: 160, fixed: 'left' },
          {
            title: '待对账',
            children: [
              { field: 'unCheckCount', title: '单据数', width: 90, align: 'right' },
              { field: 'unCheckAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '待结算',
            children: [
              { field: 'unSettleCount', title: '单据数', width: 90, align: 'right' },
              { field: 'unSettleAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '部分结算',
            children: [
              { field: 'partSettleCount', title: '单据数', width: 90, align: 'right' },
              { field: 'partSettleAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '已结算',
            children: [
              { field: 'settledCount', title: '单据数', width: 90, align: 'right' },
              { field: 'settledAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
        ],
      };
    },
    created() {
      this.search();
    },
    methods: {
      /** 格式化金额。 */
      formatAmount(value: number): string {
        return Number(value || 0).toFixed(2);
      },
      /** 汇总指定列。 */
      sumByField(data: any[], field: string): number {
        return (data || []).reduce((total, item) => total + Number(item[field] || 0), 0);
      },
      /** 生成总览表格合计行。 */
      footerMethod({ columns, data }: { columns: any[]; data: any[] }) {
        const countFields = ['unCheckCount', 'unSettleCount', 'partSettleCount', 'settledCount'];
        const amountFields = [
          'unCheckAmount',
          'unSettleAmount',
          'partSettleAmount',
          'settledAmount',
        ];
        return [
          columns.map((column) => {
            if (column.type === 'seq') return '合计';
            if (countFields.includes(column.field)) return this.sumByField(data, column.field);
            return amountFields.includes(column.field)
              ? this.formatAmount(this.sumByField(data, column.field))
              : '';
          }),
        ];
      },
      /** 生成总览查询条件。 */
      buildQueryParams() {
        return {
          ...buildSortPageVo(this.pagerConfig, []),
          customerId: this.searchFormData.customerId || undefined,
          orderStartTime: this.searchFormData.orderStartTime || undefined,
          orderEndTime: this.searchFormData.orderEndTime || undefined,
        };
      },
      /** 查询客户结算总览。 */
      async loadList() {
        this.loading = true;
        try {
          const res = await api.querySettleOverviews(this.buildQueryParams());
          this.tableData = res?.datas || [];
          this.pagerConfig.total = res?.totalCount || 0;
        } catch (err: any) {
          this.tableData = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询客户结算总览失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      /** 重置分页并查询。 */
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
      /** 打开客户结算记录。 */
      openRecordPage() {
        this.openChildPage({
          path: '/settle/customer/sheet-record',
          query: { customerId: this.searchFormData.customerId || '' },
        });
      },
      /** 打开指定客户的结算明细。 */
      openDetailPage(row: { customerId: string }) {
        this.openChildPage({
          path: '/settle/customer/settle',
          query: {
            customerId: row.customerId,
            startTime: this.searchFormData.orderStartTime || '',
            endTime: this.searchFormData.orderEndTime || '',
          },
        });
      },
      /** 创建客户结算总览导出任务。 */
      async exportList() {
        this.loading = true;
        try {
          await api.exportSettleOverviews(this.buildQueryParams());
          createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
        } catch (err: any) {
          createError(err?.message || '创建导出任务失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
    },
  });
</script>

<style scoped lang="less">
  .date-range-container {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .date-split {
    color: #8c8c8c;
  }
</style>
