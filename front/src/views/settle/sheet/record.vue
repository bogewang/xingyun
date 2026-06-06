<template>
  <div v-permission="['settle:sheet:query']" class="sheet-record-page">
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        id="SupplierSettleSheetRecord"
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
            <j-form bordered @collapse="$refs.grid.refreshColumn()">
              <j-form-item label="供应商">
                <supplier-selector v-model:value="searchFormData.supplierId" />
              </j-form-item>
              <j-form-item label="单据日期" :content-nest="false">
                <div class="date-range-container">
                  <a-date-picker
                    v-model:value="dateRange[0]"
                    placeholder=""
                    value-format="YYYY-MM-DD"
                  />
                  <span class="date-split">至</span>
                  <a-date-picker
                    v-model:value="dateRange[1]"
                    placeholder=""
                    value-format="YYYY-MM-DD"
                  />
                </div>
              </j-form-item>
              <j-form-item label="货流单号">
                <a-input
                  v-model:value.trim="searchFormData.keyword"
                  allow-clear
                  @pressEnter="search"
                />
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button @click="closeCurrentPage">返回</a-button>
            <a-button type="primary" @click="search">查询</a-button>
          </a-space>
        </template>

        <template #bizCode_default="{ row }">
          <span v-if="row.detailCount > 1" class="sheet-record-page__link-text"
            @click="openDetailDialog(row)"
            >共{{ row.detailCount }}单</span
          >
          <a v-else @click="openReceiveSheet(row.bizCode)">{{ row.bizCode || '-' }}</a>
        </template>
      </vxe-grid>

      <a-modal
        v-model:open="detailDialog.visible"
        title="货流单明细"
        width="900px"
        :footer="null"
      >
        <vxe-grid
          size="mini"
          resizable
          show-overflow
          row-id="bizId"
          :data="detailDialog.tableData"
          :columns="detailDialog.columns"
          max-height="480"
        >
          <template #bizCode_detail="{ row }">
            <a @click="openReceiveSheet(row.bizCode)">{{ row.bizCode }}</a>
          </template>
        </vxe-grid>
      </a-modal>
    </page-wrapper>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import moment from 'moment';
  import * as settleApi from '@/api/settle/sheet';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { createError } from '@/hooks/web/msg';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';
  import { buildSortPageVo, getDateTimeWithMaxTime, getDateTimeWithMinTime } from '@/utils/utils';

  export default defineComponent({
    name: 'SupplierSettleSheetRecord',
    components: {
      SupplierSelector,
    },
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
          supplierId: routeQuery.supplierId ? String(routeQuery.supplierId) : '',
          keyword: '',
        },
        dateRange: [startTime, endTime],
        toolbarConfig: {
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
        detailDialog: {
          visible: false,
          tableData: [],
          columns: [
            { type: 'seq', title: '序号', width: 70 },
            { field: 'bizCode', title: '货流单号', minWidth: 180, slots: { default: 'bizCode_detail' } },
            { field: 'totalPayAmount', title: '货流总价', width: 120, align: 'right' },
            { field: 'payAmount', title: '实付金额', width: 120, align: 'right' },
            { field: 'totalUnPayAmount', title: '未付金额', width: 120, align: 'right' },
            { field: 'description', title: '备注', minWidth: 220 },
          ],
        },
        tableData: [],
        tableColumn: [
          { type: 'seq', title: '序号', width: 70, fixed: 'left' },
          { field: 'recordTime', title: '结算时间', width: 180, fixed: 'left' },
          { field: 'supplierName', title: '供应商名称', minWidth: 220, fixed: 'left' },
          { field: 'bizCodeText', title: '货单号', width: 140, slots: { default: 'bizCode_default' } },
          { field: 'bizTotalAmount', title: '货流总价', width: 140, align: 'right' },
          { field: 'actualSettleAmount', title: '实付金额', width: 120, align: 'right' },
          { field: 'description', title: '备注', minWidth: 240 },
        ],
      };
    },
    created() {
      this.search();
    },
    methods: {
      formatAmount(value) {
        return Number(value || 0).toFixed(2);
      },
      footerMethod({ columns, data }) {
        const fieldMap = {
          bizTotalAmount: this.formatAmount(this.sumByField(data, 'bizTotalAmount')),
          actualSettleAmount: this.formatAmount(this.sumByField(data, 'actualSettleAmount')),
        };

        return [
          columns.map((column, index) => {
            if (index === 0) {
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
      buildSettleQueryParams(pageIndex = this.pagerConfig.currentPage, pageSize = this.pagerConfig.pageSize) {
        return {
          ...buildSortPageVo(
            {
              currentPage: pageIndex,
              pageSize,
            },
            [],
          ),
          supplierId: this.searchFormData.supplierId || undefined,
          createStartTime: this.dateRange?.[0] ? `${this.dateRange[0]} 00:00:00` : undefined,
          createEndTime: this.dateRange?.[1] ? `${this.dateRange[1]} 23:59:59` : undefined,
        };
      },
      async hydrateRows(records) {
        const detailRows = await Promise.all(
          (records || []).map(async (item) => {
            try {
              const detail = await settleApi.get(item.id);
              return {
                ...item,
                detailInfo: detail,
              };
            } catch (e) {
              return {
                ...item,
                detailInfo: null,
              };
            }
          }),
        );

        return detailRows.map((item) => {
          const details = item.detailInfo?.details || [];
          const bizTotalAmount = details.reduce(
            (total, detail) => total + Number(detail.totalPayAmount || 0),
            0,
          );
          const actualSettleAmount =
            item.totalAmount === null || item.totalAmount === undefined
              ? details.reduce((total, detail) => total + Number(detail.payAmount || 0), 0)
              : Number(item.totalAmount || 0);

          return {
            ...item,
            recordTime: item.createTime || '',
            detailCount: details.length,
            bizCode: details[0]?.bizCode || '',
            bizCodeText: details.length > 1 ? `共${details.length}单` : details[0]?.bizCode || '-',
            bizTotalAmount,
            actualSettleAmount,
            description: item.description || '',
          };
        });
      },
      async loadList() {
        this.loading = true;
        try {
          const keyword = this.searchFormData.keyword || '';
          if (!keyword) {
            const res = await settleApi.query(this.buildSettleQueryParams());
            const rows = await this.hydrateRows(res?.datas || []);
            this.tableData = rows;
            this.pagerConfig.total = res?.totalCount || 0;
            return;
          }

          const res = await settleApi.query(this.buildSettleQueryParams(1, 200));
          const rows = await this.hydrateRows(res?.datas || []);
          const filteredRows = rows.filter((item) => {
            const details = item.detailInfo?.details || [];
            return details.some(
              (detail) => String(detail.bizCode || '').includes(keyword),
            );
          });

          this.pagerConfig.total = filteredRows.length;
          const start = (this.pagerConfig.currentPage - 1) * this.pagerConfig.pageSize;
          const end = start + this.pagerConfig.pageSize;
          this.tableData = filteredRows.slice(start, end);
        } catch (err) {
          this.tableData = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询结算记录失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      search() {
        this.pagerConfig.currentPage = 1;
        this.loadList();
      },
      openDetailDialog(row) {
        const details = row?.detailInfo?.details || [];
        this.detailDialog.tableData = details.map((item) => ({
          ...item,
          totalPayAmount: this.formatAmount(item.totalPayAmount),
          payAmount: this.formatAmount(item.payAmount),
          totalUnPayAmount: this.formatAmount(item.totalUnPayAmount),
          description: item.description || '',
        }));
        this.detailDialog.visible = true;
      },
      openReceiveSheet(code) {
        if (!code) {
          return;
        }

        this.openChildPage({
          path: '/purchase/receive',
          query: {
            code,
          },
        });
      },
      handlePageChange({ currentPage, pageSize }) {
        this.pagerConfig.currentPage = currentPage;
        this.pagerConfig.pageSize = pageSize;
        this.loadList();
      },
      onRefreshPage() {
        this.loadList();
      },
    },
  });
</script>

<style scoped lang="less">
  .sheet-record-page__link-text {
    color: #1677ff;
  }
</style>
