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
            <a-button type="primary" @click="search">查询</a-button>
          </a-space>
        </template>

        <template #seq_default="{ row }">
          <span v-if="!row.isDetailRow">{{ row.seqNo }}</span>
        </template>

        <template #bizCode_default="{ row }">
          <span
            v-if="!row.isDetailRow && row.detailCount > 1"
            class="sheet-record-page__link-text"
            @click="toggleDetailRow(row)"
            >共{{ row.detailCount }}单</span
          >
          <a v-else-if="!row.isDetailRow" @click="openReceiveSheet(row.bizCode)">{{
            row.bizCode || '-'
          }}</a>
        </template>

        <template #description_default="{ row }">
          <template v-if="row.isDetailRow">
            <div class="sheet-record-page__detail-inline">
              <span class="sheet-record-page__detail-label">货单号：</span>
              <template v-for="(item, index) in row.detailLinks" :key="item.bizId">
                <a @click="openReceiveSheet(item.bizCode)">{{ item.bizCode }}</a>
                <span v-if="index < row.detailLinks.length - 1">，</span>
              </template>
            </div>
          </template>
          <span v-else>{{ row.description || '' }}</span>
        </template>
      </vxe-grid>
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
        expandedRowIds: [],
        pagerConfig: {
          currentPage: 1,
          pageSize: 20,
          total: 0,
          layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'Sizes', 'Total'],
        },
        rawTableData: [],
        tableData: [],
        tableColumn: [
          { field: 'seqNo', title: '序号', width: 70, fixed: 'left', slots: { default: 'seq_default' } },
          { field: 'recordTime', title: '结算时间', width: 180, fixed: 'left' },
          { field: 'supplierName', title: '供应商名称', minWidth: 220, fixed: 'left' },
          { field: 'bizCodeText', title: '货单号', width: 140, slots: { default: 'bizCode_default' } },
          { field: 'bizTotalAmount', title: '货流总价', width: 140, align: 'right' },
          { field: 'actualSettleAmount', title: '实付金额', width: 120, align: 'right' },
          { field: 'description', title: '备注', minWidth: 240, slots: { default: 'description_default' } },
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
      buildDisplayRows(rows) {
        const displayRows = [];
        (rows || []).forEach((row) => {
          displayRows.push(row);
          if (this.expandedRowIds.includes(row.id) && row.detailCount > 1) {
            displayRows.push({
              id: `${row.id}__detail`,
              parentId: row.id,
              isDetailRow: true,
              seqNo: '',
              recordTime: '',
              supplierName: '',
              bizCodeText: '',
              bizTotalAmount: '',
              actualSettleAmount: '',
              description: '',
              detailLinks: (row.detailInfo?.details || []).map((item) => ({
                bizId: item.bizId,
                bizCode: item.bizCode,
              })),
            });
          }
        });

        return displayRows;
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
            isDetailRow: false,
            seqNo: 0,
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
            this.rawTableData = rows.map((item, index) => ({ ...item, seqNo: index + 1 }));
            this.tableData = this.buildDisplayRows(this.rawTableData);
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
          this.rawTableData = filteredRows
            .slice(start, end)
            .map((item, index) => ({ ...item, seqNo: start + index + 1 }));
          this.tableData = this.buildDisplayRows(this.rawTableData);
        } catch (err) {
          this.rawTableData = [];
          this.tableData = [];
          this.pagerConfig.total = 0;
          createError(err?.message || '查询结算记录失败，请稍后重试！');
        } finally {
          this.loading = false;
        }
      },
      search() {
        this.pagerConfig.currentPage = 1;
        this.expandedRowIds = [];
        this.loadList();
      },
      toggleDetailRow(row) {
        if (!row || row.isDetailRow || row.detailCount <= 1) {
          return;
        }

        if (this.expandedRowIds.includes(row.id)) {
          this.expandedRowIds = this.expandedRowIds.filter((item) => item !== row.id);
        } else {
          this.expandedRowIds = [...this.expandedRowIds, row.id];
        }

        this.tableData = this.buildDisplayRows(this.rawTableData);
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
    cursor: pointer;
  }

  .sheet-record-page__detail-inline {
    white-space: normal;
    line-height: 1.8;
  }

  .sheet-record-page__detail-label {
    color: #595959;
  }
</style>
