<template>
  <div>
    <div v-permission="['settle:sheet:query']">
      <page-wrapper content-full-height fixed-height>
        <vxe-grid
          id="SettleSheetSummary"
          ref="grid"
          resizable
          header-align="center"
          show-overflow
          show-footer
          highlight-hover-row
          row-id="supplierId"
          :data="tableData"
          :columns="tableColumn"
          :toolbar-config="toolbarConfig"
          :custom-config="{}"
          :footer-method="footerMethod"
          :loading="loading"
          height="auto"
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
              <a-button type="primary" @click="handleToolbarSettle">结算</a-button>
              <a-button @click="exportExcel">导出Excel</a-button>
            </a-space>
          </template>

          <template #action_default="{ row }">
            <a-button type="link" size="small" @click="handleSettle(row)">结算</a-button>
          </template>
        </vxe-grid>
      </page-wrapper>
    </div>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import moment from 'moment';
  import * as api from '@/api/settle/sheet';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { formatDateTime, getDateTimeWithMaxTime, getDateTimeWithMinTime } from '@/utils/utils';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';

  export default defineComponent({
    name: 'SettleSheet',
    components: {
      SupplierSelector,
    },
    mixins: [multiplePageMix],
    data() {
      return {
        loading: false,
        searchFormData: {
          supplierId: '',
          orderStartTime: formatDateTime(getDateTimeWithMinTime(moment().subtract(3, 'M'))),
          orderEndTime: formatDateTime(getDateTimeWithMaxTime(moment())),
        },
        toolbarConfig: {
          refresh: {
            queryMethod: () => this.search(),
          },
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        tableData: [],
        tableColumn: [
          { type: 'seq', title: '序号', width: 80, fixed: 'left' },
          { title: '操作', width: 90, fixed: 'left', slots: { default: 'action_default' } },
          { field: 'supplierName', title: '供应商名称', minWidth: 220, fixed: 'left' },
          {
            title: '待对账',
            children: [
              { field: 'unCheckSheetNum', title: '单据数', width: 110, align: 'right' },
              { field: 'unCheckTotalAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '待结算',
            children: [
              { field: 'unSettleSheetNum', title: '单据数', width: 110, align: 'right' },
              { field: 'unSettleTotalAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '部分结算',
            children: [
              { field: 'partSettleSheetNum', title: '单据数', width: 110, align: 'right' },
              { field: 'partSettleTotalAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
          {
            title: '已结算',
            children: [
              { field: 'settledSheetNum', title: '单据数', width: 110, align: 'right' },
              { field: 'settledTotalAmount', title: '金额', width: 120, align: 'right' },
            ],
          },
        ],
      };
    },
    created() {
      this.search();
    },
    methods: {
      footerMethod({ columns, data }) {
        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }

            if (column.field === 'unCheckSheetNum') {
              return this.sumByField(data, 'unCheckSheetNum', 0);
            }

            if (column.field === 'unCheckTotalAmount') {
              return this.sumByField(data, 'unCheckTotalAmount', 2);
            }

            if (column.field === 'unSettleSheetNum') {
              return this.sumByField(data, 'unSettleSheetNum', 0);
            }

            if (column.field === 'unSettleTotalAmount') {
              return this.sumByField(data, 'unSettleTotalAmount', 2);
            }

            if (column.field === 'partSettleSheetNum') {
              return this.sumByField(data, 'partSettleSheetNum', 0);
            }

            if (column.field === 'partSettleTotalAmount') {
              return this.sumByField(data, 'partSettleTotalAmount', 2);
            }

            if (column.field === 'settledSheetNum') {
              return this.sumByField(data, 'settledSheetNum', 0);
            }

            if (column.field === 'settledTotalAmount') {
              return this.sumByField(data, 'settledTotalAmount', 2);
            }

            return '';
          }),
        ];
      },
      sumByField(data, field, digits = 2) {
        const total = (data || []).reduce((sum, item) => {
          const value = Number(item?.[field] ?? 0);
          return sum + (Number.isNaN(value) ? 0 : value);
        }, 0);

        return digits === 0 ? String(total) : total.toFixed(digits);
      },
      buildQueryParams() {
        return {
          supplierId: this.searchFormData.supplierId || undefined,
          orderStartTime: this.searchFormData.orderStartTime || undefined,
          orderEndTime: this.searchFormData.orderEndTime || undefined,
        };
      },
      search() {
        this.loading = true;
        api
          .summary(this.buildQueryParams())
          .then((res) => {
            this.tableData = res || [];
          })
          .catch((err) => {
            this.tableData = [];
            createError(err?.message || '查询供应商结算汇总失败，请稍后重试！');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      handleToolbarSettle() {
        this.openChildPage({
          path: '/settle/supplier/settle',
          query: {
            startTime: this.searchFormData.orderStartTime || '',
            endTime: this.searchFormData.orderEndTime || '',
          },
        });
      },
      handleSettle(row) {
        this.openChildPage({
          path: '/settle/supplier/settle',
          query: {
            supplierId: row.supplierId,
            startTime: this.searchFormData.orderStartTime || '',
            endTime: this.searchFormData.orderEndTime || '',
          },
        });
      },
      exportExcel() {
        this.loading = true;
        api
          .exportSummary(this.buildQueryParams())
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
      onRefreshPage() {
        this.search();
      },
    },
  });
</script>

<style scoped></style>
