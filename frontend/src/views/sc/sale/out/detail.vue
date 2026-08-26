<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    wrap-class-name="order-detail-modal-wrap"
    width="75%"
    title="查看"
    :style="{ top: '2px', paddingBottom: 0 }"
    :body-style="{ flex: 1, minHeight: 0, overflow: 'hidden' }"
  >
    <div
      v-if="visible"
      v-permission="['sale:out:query']"
      v-loading="loading"
      class="order-detail-modal-content"
    >
      <a-tabs v-model:activeKey="activeKey" class="order-detail-tabs">
        <a-tab-pane key="detail" tab="详情">
          <div class="order-detail-pane">
            <j-border>
              <j-form bordered>
                <j-form-item label="客户">
                  {{ formData.customerName }}
                </j-form-item>
                <j-form-item label="订单日期">
                  {{ formData.orderDate }}
                </j-form-item>
                <j-form-item label="成本状态">
                  <span :style="{ color: formData.fillAllCost ? '#52c41a' : '#f5222d' }">
                    {{ formData.fillAllCost ? '已补全' : '未补全' }}
                  </span>
                </j-form-item>

                <j-form-item label="操作人">
                  <span>{{ formData.createBy }}</span>
                </j-form-item>
                <j-form-item label="操作时间">
                  <span>{{ formData.createTime }}</span>
                </j-form-item>
              </j-form>
            </j-border>
            <div class="order-detail-grid-wrap">
              <vxe-grid
                id="SaleOutSheetDetail"
                ref="grid"
                resizable
                show-overflow
                highlight-hover-row
                keep-source
                row-id="id"
                height="100%"
                :data="tableData"
                :columns="visibleTableColumn"
                :toolbar-config="toolbarConfig"
                :custom-config="{}"
              >
                <template #inquiryProduct_default="{ row }">
                  <span :class="formatInquiryProduct(row.inquiryProduct).className">
                    {{ formatInquiryProduct(row.inquiryProduct).text }}
                  </span>
                </template>
                <template #taxAmount_default="{ row }">
                  <span v-if="isFloatGeZero(row.taxPrice) && isFloat(row.outNum)">{{
                    getNumber(mul(row.taxPrice, row.outNum), 2)
                  }}</span>
                </template>
                <template #confirmNum_default="{ row }">
                  <span :class="{ 'confirm-num-warning': isConfirmNumMismatch(row) }">
                    {{ row.confirmNum }}
                  </span>
                </template>
                <template #taxPrice_default="{ row }">
                  <span :style="{ color: isNegativeProfit(row) ? '#f5222d' : undefined }">
                    {{ row.taxPrice }}
                  </span>
                </template>
                <template #costStatus_default="{ row }">
                  <span :style="{ color: hasCostPrice(row) ? '#52c41a' : '#f5222d' }">
                    {{ hasCostPrice(row) ? '已补全' : '未补全' }}
                  </span>
                </template>
                <template #totalProfit_default="{ row }">
                  <span
                    :style="{
                      color: isNegativeProfit(row) ? '#f5222d' : undefined,
                    }"
                  >
                    {{ Number(row.totalProfit || 0).toFixed(2) }}
                  </span>
                </template>
                <template #costAmount_default="{ row }">
                  {{ formatAmount(calcCostAmount(row)) }}
                </template>
                <template #profitRate_default="{ row }">
                  <span :style="{ color: isNegativeProfit(row) ? '#f5222d' : undefined }">
                    {{ calcProfitRate(row.totalProfit, calcTaxAmount(row)) }}
                  </span>
                </template>
              </vxe-grid>
            </div>

            <j-border title="合计">
              <j-form bordered label-width="140px">
                <j-form-item label="数量" :span="6">
                  <a-input v-model:value="formData.totalNum" class="number-input" readonly />
                </j-form-item>
                <j-form-item label="含税总金额" :span="6">
                  <a-input v-model:value="formData.totalAmount" class="number-input" readonly />
                </j-form-item>
                <j-form-item label="总利润" :span="6">
                  <a-input v-model:value="formData.totalProfit" class="number-input" readonly />
                </j-form-item>
                <j-form-item label="已付金额" :span="6">
                  <a-input v-model:value="formData.paidAmount" class="number-input" readonly />
                </j-form-item>
                <j-form-item label="备注" :span="24" :content-nest="false">
                  <a-input v-model:value.trim="formData.description" maxlength="200" readonly />
                </j-form-item>
              </j-form>
            </j-border>
          </div>
        </a-tab-pane>
        <a-tab-pane key="orderTimeLine" tab="变动记录">
          <order-time-line :id="id" />
        </a-tab-pane>
      </a-tabs>
    </div>
    <template #footer>
      <div class="form-modal-footer">
        <a-space>
          <a-button type="primary" :loading="loading" @click="print">打印</a-button>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </template>
    <!-- 销售订单查看窗口 -->
    <sale-order-detail :id="formData.saleOrderId" ref="viewSaleOrderDetailDialog" />
  </a-modal>
  <order-print-dialog />
</template>
<script>
  import { defineComponent } from 'vue';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import * as api from '@/api/sc/sale/out';
  import { printMix } from '@/mixins/print.ts';
  import { add, getNumber, isEmpty, isFloat, isFloatGeZero, mul, sub } from '@/utils/utils';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import PrintDialog from '/@/components/PrintDialog';
  import JFormItem from '@/components/JFormItem';
  import { createSuccess } from '@/hooks/web/msg';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';

  export default defineComponent({
    components: {
      JFormItem,
      SaleOrderDetail,
      OrderTimeLine,
      OrderPrintDialog: PrintDialog,
    },
    mixins: [printMix],
    props: {
      id: {
        type: String,
        required: true,
      },
    },
    setup() {
      const { hasPermission } = usePermission();
      return {
        isEmpty,
        isFloat,
        isFloatGeZero,
        getNumber,
        mul,
        formatInquiryProduct,
        hasPermission,
        hasCostPrice: (row) =>
          row && row.costPrice !== null && row.costPrice !== undefined && row.costPrice !== '',
        SALE_OUT_SHEET_STATUS,
      };
    },
    data() {
      return {
        // 是否可见
        visible: false,
        // 是否显示加载框
        loading: false,
        activeKey: 'detail',
        // 表单数据
        formData: {},
        // 工具栏配置
        toolbarConfig: {
          zoom: false,
          custom: true,
          refresh: false,
        },
        // 列表数据配置
        tableColumn: [
          { type: 'seq', width: 50 },
          { field: 'productCode', title: '商品编号', width: 120 },
          { field: 'productName', title: '商品名称', width: 150 },
          {
            field: 'inquiryProduct',
            title: '是否询价商品',
            width: 120,
            slots: { default: 'inquiryProduct_default' },
          },
          { field: 'categoryName', title: '商品分类', width: 80 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'outNum', title: '数量', align: 'right', width: 100 },
          {
            field: 'confirmNum',
            title: '验收数量',
            align: 'right',
            width: 100,
            slots: { default: 'confirmNum_default' },
          },
          {
            field: 'taxPrice',
            title: '价格（元）',
            align: 'right',
            width: 120,
            slots: { default: 'taxPrice_default' },
          },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 120,
            slots: { default: 'taxAmount_default' },
          },
          {
            field: 'costAmount',
            title: '成本',
            align: 'right',
            width: 100,
            slots: { default: 'costAmount_default' },
          },
          {
            field: 'totalProfit',
            title: '利润',
            align: 'right',
            width: 100,
            slots: { default: 'totalProfit_default' },
          },
          {
            field: 'profitRate',
            title: '毛利率',
            align: 'right',
            width: 100,
            slots: { default: 'profitRate_default' },
          },
          {
            field: 'costStatus',
            title: '成本状态',
            width: 100,
            slots: { default: 'costStatus_default' },
          },
          // { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
          { field: 'productRemark', title: '商品备注', width: 200 },
          { field: 'description', title: '备注', width: 200 },
        ],
        tableData: [],
      };
    },
    computed: {
      visibleTableColumn() {
        return this.tableColumn.filter((column) => {
          if (['costAmount', 'totalProfit', 'profitRate'].includes(column.field)) {
            return this.hasPermission('sale:out:profit', false);
          }
          return true;
        });
      },
    },
    created() {
      // 初始化表单数据
      this.initFormData();
    },
    methods: {
      // 打开对话框 由父页面触发
      openDialog() {
        this.visible = true;

        this.$nextTick(() => this.open());
      },
      // 关闭对话框
      closeDialog() {
        this.visible = false;
        this.$emit('close');
      },
      // 初始化表单数据
      initFormData() {
        this.activeKey = 'detail';
        this.formData = {
          scName: '',
          customerName: '',
          salerName: '',
          orderDate: '',
          saleOrderId: '',
          saleOrderCode: '',
          totalNum: 0,
          totalAmount: 0,
          totalProfit: 0,
          paidAmount: 0,
          unpaidAmount: 0,
          fillAllCost: false,
          description: '',
        };

        this.tableData = [];
      },
      // 加载数据
      loadData() {
        this.loading = true;
        api
          .get(this.id)
          .then((res) => {
            this.formData = {
              scName: res.scName,
              customerName: res.customerName,
              salerName: res.salerName || '',
              orderDate: res.orderDate || '',
              saleOrderId: res.saleOrderId || '',
              saleOrderCode: res.saleOrderCode || '',
              description: res.description,
              status: res.status,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: res.totalAmount || 0,
              totalProfit: res.totalProfit || 0,
              paidAmount: res.paidAmount || 0,
              unpaidAmount: sub(res.totalAmount || 0, res.paidAmount || 0),
              fillAllCost: !!res.fillAllCost,
            };
            this.tableData = res.details || [];
          })
          .finally(() => {
            this.loading = false;
          });
      },
      // 页面显示时触发
      open() {
        // 初始化表单数据
        this.initFormData();
        this.loadData();
      },
      // 计算汇总数据
      calcSum() {
        let totalNum = 0;
        let totalAmount = 0;

        this.tableData
          .filter((t) => {
            return isFloatGeZero(t.taxPrice) && isFloat(t.outNum);
          })
          .forEach((t) => {
            const num = parseFloat(t.outNum);
            totalNum = add(totalNum, num);
            totalAmount = add(totalAmount, getNumber(mul(num, t.taxPrice), 2));
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
        this.formData.unpaidAmount = sub(
          this.formData.totalAmount || 0,
          this.formData.paidAmount || 0,
        );
      },
      calcTaxAmount(row) {
        if (!isFloatGeZero(row?.taxPrice) || !isFloat(row?.outNum)) {
          return 0;
        }
        return getNumber(mul(row.taxPrice, row.outNum), 2);
      },
      calcCostAmount(row) {
        return this.calcTaxAmount(row) - Number(row?.totalProfit || 0);
      },
      calcProfitRate(profit, amount) {
        const amountNumber = Number(amount || 0);
        if (!amountNumber) {
          return '0.00%';
        }
        return `${((Number(profit || 0) / amountNumber) * 100).toFixed(2)}%`;
      },
      isNegativeProfit(row) {
        return Number(row?.totalProfit || 0) < 0;
      },
      /** 判断验收数量是否大于零且与出库数量不一致。 */
      isConfirmNumMismatch(row) {
        return Number(row?.confirmNum) > 0 && Number(row.confirmNum) !== Number(row.outNum);
      },
      formatAmount(value) {
        return Number(value || 0).toFixed(2);
      },
      buildPrintData(printData) {
        const res = {
          ...printData,
        };

        const details = Array.isArray(printData?.details) ? printData.details : [];
        res.details = details.map((item, index) => ({
          ...item,
          seq: index + 1,
        }));

        return res;
      },
      async print() {
        this.loading = true;
        try {
          const res = await api.print(this.id);
          await this.vgDefaultBrowserPrint(PRINT_TYPE.SALE_OUT.code, this.buildPrintData(res));
        } finally {
          this.loading = false;
        }
      },
      exportDetails() {
        this.loading = true;
        api
          .exportDetail(this.buildQueryParams())
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      buildQueryParams() {
        return {
          pageIndex: 1,
          pageSize: 2147483647,
          idList: [this.id],
        };
      },
    },
  });
</script>
<style scoped>
  :global(.order-detail-modal-wrap .ant-modal) {
    top: 2px !important;
    padding-bottom: 0;
  }

  :global(.order-detail-modal-wrap .ant-modal-content) {
    height: calc(100vh - 4px);
    display: flex;
    flex-direction: column;
  }

  :global(.order-detail-modal-wrap .ant-modal-body) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .order-detail-modal-content {
    height: 100%;
    overflow: hidden;
  }

  .order-detail-tabs {
    height: 100%;
  }

  .order-detail-pane {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow-x: hidden;
    overflow-y: auto;
  }

  .order-detail-grid-wrap {
    flex: 0 0 500px;
    overflow: hidden;
  }

  .order-detail-tabs :deep(.ant-tabs-content-holder),
  .order-detail-tabs :deep(.ant-tabs-content),
  .order-detail-tabs :deep(.ant-tabs-tabpane) {
    height: 100%;
  }

  .order-detail-tabs :deep(.ant-tabs-nav) {
    margin-bottom: 12px;
  }

  .confirm-num-warning {
    color: #f5222d;
  }
</style>
