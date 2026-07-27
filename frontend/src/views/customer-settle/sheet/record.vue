<template>
  <div v-permission="['customer-settle:sheet:query']" class="customer-settle-record-page">
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        id="CustomerSettleSheetRecord"
        ref="grid"
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
        @page-change="handlePageChange"
      >
        <template #form>
          <j-border>
            <j-form bordered @collapse="$refs.grid.refreshColumn()" @keyup.enter="search">
              <j-form-item label="客户">
                <a-select
                  v-model:value="searchFormData.customerId"
                  allow-clear
                  show-search
                  :filter-option="false"
                  :options="customerOptions"
                  placeholder="请选择客户"
                  @focus="loadCustomerOptions()"
                  @search="loadCustomerOptions"
                  @change="search"
                />
              </j-form-item>
              <j-form-item label="结算日期" :content-nest="false">
                <div class="date-range-container">
                  <a-date-picker v-model:value="dateRange[0]" value-format="YYYY-MM-DD" />
                  <span class="date-split">至</span>
                  <a-date-picker v-model:value="dateRange[1]" value-format="YYYY-MM-DD" />
                </div>
              </j-form-item>
              <j-form-item label="结算单号">
                <a-input v-model:value.trim="searchFormData.code" allow-clear />
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="search">查询</a-button>
            <a-button v-permission="['customer-settle:sheet:export']" @click="exportList">导出</a-button>
          </a-space>
        </template>

        <template #seq_default="{ row }">
          <span v-if="!row.isDetailRow">{{ row.seqNo }}</span>
        </template>

        <template #biz_default="{ row }">
          <a v-if="!row.isDetailRow" @click="toggleDetailRow(row)">
            {{ expandedRowIds.includes(row.id) ? '收起明细' : `共${row.detailCount || 0}单` }}
          </a>
        </template>

        <template #description_default="{ row }">
          <template v-if="row.isDetailRow">
            <div class="customer-settle-record-page__detail-inline">
              <span class="customer-settle-record-page__detail-label">关联单据：</span>
              <template v-for="(item, index) in row.details" :key="item.bizId">
                <a @click="openBizList(item)">{{ item.bizCode || '-' }}</a>
                <span v-if="index < row.details.length - 1">，</span>
              </template>
            </div>
          </template>
          <span v-else>{{ row.description || '' }}</span>
        </template>
      </vxe-grid>
    </page-wrapper>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from 'vue';
  import moment from 'moment';
  import * as api from '@/api/customer-settle/sheet';
  import { requestCustomerSelectOptions } from '@/utils/labelSelect';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { buildSortPageVo, getDateTimeWithMaxTime, getDateTimeWithMinTime } from '@/utils/utils';
  import { getCustomerSettleBizListPath } from './customerSettleWorkbench';

  export default defineComponent({
    name: 'CustomerSettleSheetRecord',
    mixins: [multiplePageMix],
    data() {
      const routeQuery = this.$route.query || {};
      const startTime = routeQuery.startTime
        ? moment(String(routeQuery.startTime)).format('YYYY-MM-DD')
        : moment(getDateTimeWithMinTime(moment().subtract(1, 'M'))).format('YYYY-MM-DD');
      const endTime = routeQuery.endTime
        ? moment(String(routeQuery.endTime)).format('YYYY-MM-DD')
        : moment(getDateTimeWithMaxTime(moment())).format('YYYY-MM-DD');
      return {
        loading: false,
        searchFormData: {
          customerId: routeQuery.customerId ? String(routeQuery.customerId) : '',
          code: routeQuery.code ? String(routeQuery.code) : '',
        },
        dateRange: [startTime, endTime],
        customerOptions: [] as Array<{ label: string; value: string }>,
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
        expandedRowIds: [] as string[],
        detailMap: {} as Record<string, any[]>,
        rawTableData: [] as any[],
        tableData: [] as any[],
        tableColumn: [
          { field: 'seqNo', title: '序号', width: 70, fixed: 'left', slots: { default: 'seq_default' } },
          { field: 'code', title: '结算单号', width: 180, fixed: 'left' },
          { field: 'createTime', title: '结算时间', width: 180 },
          { field: 'createBy', title: '操作人', width: 120 },
          { field: 'customerName', title: '客户名称', width: 140 },
          { field: 'biz', title: '关联单据', width: 100, slots: { default: 'biz_default' } },
          { field: 'totalAmount', title: '结算金额', width: 120, align: 'right' },
          { field: 'description', title: '备注', minWidth: 240, slots: { default: 'description_default' } },
        ],
      };
    },
    created() {
      this.loadCustomerOptions();
      this.search();
    },
    methods: {
      /** 加载客户下拉选项。 */
      async loadCustomerOptions(keyword = '') {
        this.customerOptions = await requestCustomerSelectOptions(keyword);
      },
      /** 格式化金额。 */
      formatAmount(value: number): string {
        return Number(value || 0).toFixed(2);
      },
      /** 生成表格合计行。 */
      footerMethod({ columns, data }: { columns: any[]; data: any[] }) {
        const totalAmount = (data || []).reduce((total, item) => total + Number(item.totalAmount || 0), 0);
        return [
          columns.map((column, index) => {
            if (index === 0) return '合计';
            return column.field === 'totalAmount' ? this.formatAmount(totalAmount) : '';
          }),
        ];
      },
      /** 生成结算记录查询条件。 */
      buildQueryParams() {
        return {
          ...buildSortPageVo(this.pagerConfig, []),
          customerId: this.searchFormData.customerId || undefined,
          code: this.searchFormData.code || undefined,
          createStartTime: this.dateRange[0] ? `${this.dateRange[0]} 00:00:00` : undefined,
          createEndTime: this.dateRange[1] ? `${this.dateRange[1]} 23:59:59` : undefined,
        };
      },
      /** 生成主行与已展开的明细行。 */
      buildDisplayRows(rows: any[]) {
        return (rows || []).flatMap((row) => {
          const details = this.detailMap[row.id] || [];
          if (!this.expandedRowIds.includes(row.id)) return [row];
          return [
            row,
            {
              id: `${row.id}__detail`,
              isDetailRow: true,
              seqNo: '',
              code: '',
              createTime: '',
              createBy: '',
              customerName: '',
              totalAmount: '',
              description: '',
              details,
            },
          ];
        });
      },
      /** 查询客户结算记录。 */
      async loadList() {
        this.loading = true;
        try {
          const res = await api.query(this.buildQueryParams());
          const start = (this.pagerConfig.currentPage - 1) * this.pagerConfig.pageSize;
          this.rawTableData = (res?.datas || []).map((item, index) => ({
            ...item,
            seqNo: start + index + 1,
            totalAmount: Number(item.totalAmount || 0),
          }));
          this.tableData = this.buildDisplayRows(this.rawTableData);
          this.pagerConfig.total = res?.totalCount || 0;
        } catch (err: any) {
          this.rawTableData = [];
          this.tableData = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询客户结算记录失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      /** 重置到首页并查询。 */
      search() {
        this.pagerConfig.currentPage = 1;
        this.expandedRowIds = [];
        this.loadList();
      },
      /** 查询并展开或收起记录明细。 */
      async toggleDetailRow(row: any) {
        if (this.expandedRowIds.includes(row.id)) {
          this.expandedRowIds = this.expandedRowIds.filter((id) => id !== row.id);
          this.tableData = this.buildDisplayRows(this.rawTableData);
          return;
        }
        this.loading = true;
        try {
          if (!this.detailMap[row.id]) {
            const detail = await api.get(row.id);
            this.detailMap[row.id] = detail?.details || [];
          }
          this.expandedRowIds = [...this.expandedRowIds, row.id];
          this.tableData = this.buildDisplayRows(this.rawTableData);
        } catch (err: any) {
          createError(err?.message || '查询结算明细失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      /** 按业务类型跳转到销售出库或销售退货列表。 */
      openBizList(item: any) {
        const path = getCustomerSettleBizListPath(item.bizType);
        if (!path) {
          createError('结算明细缺少业务类型，无法跳转关联单据！');
          return;
        }
        this.openChildPage({
          path,
          query: { code: item.bizCode || '' },
        });
      },
      /** 处理分页切换。 */
      handlePageChange({ currentPage, pageSize }: { currentPage: number; pageSize: number }) {
        this.pagerConfig.currentPage = currentPage;
        this.pagerConfig.pageSize = pageSize;
        this.loadList();
      },
      /** 提交结算记录导出任务。 */
      async exportList() {
        this.loading = true;
        try {
          await api.exportRecord(this.buildQueryParams());
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
  .customer-settle-record-page__detail-inline { line-height: 1.8; white-space: normal; }
  .customer-settle-record-page__detail-label { color: #595959; }
  .date-range-container { display: flex; align-items: center; gap: 8px; }
  .date-split { color: #8c8c8c; }
</style>
