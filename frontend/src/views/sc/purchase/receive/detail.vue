<template>
  <div>
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
        v-permission="['purchase:receive:query']"
        v-loading="loading"
        class="order-detail-modal-content"
      >
        <a-tabs v-model:activeKey="activeKey" class="order-detail-tabs">
          <a-tab-pane key="detail" tab="详情">
            <div class="order-detail-pane">
              <j-border>
                <j-form bordered>
                  <j-form-item label="供应商">
                    {{ formData.supplierName }}
                  </j-form-item>
                  <j-form-item label="订单日期">
                    {{ formData.orderDate }}
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
                  id="ReceiveSheetDetail"
                  ref="grid"
                  resizable
                  show-overflow
                  highlight-hover-row
                  keep-source
                  row-id="id"
                  height="100%"
                  :data="tableData"
                  :columns="tableColumn"
                  :toolbar-config="toolbarConfig"
                  :custom-config="{}"
                >
                  <template #taxAmount_default="{ row }">
                    <span v-if="isFloatGeZero(row.purchasePrice) && isFloat(row.receiveNum)">{{
                      getNumber(mul(row.purchasePrice, row.receiveNum), 2)
                    }}</span>
                  </template>
                  <template #inquiryProduct_default="{ row }">
                    <span :class="formatInquiryProduct(row.inquiryProduct).className">
                      {{ formatInquiryProduct(row.inquiryProduct).text }}
                    </span>
                  </template>
                </vxe-grid>
              </div>

              <j-border title="合计">
                <j-form bordered label-width="140px">
                  <j-form-item label="收货数量" :span="8">
                    <a-input v-model:value="formData.totalNum" class="number-input" readonly />
                  </j-form-item>
                  <j-form-item label="折后金额" :span="8">
                    <a-input v-model:value="formData.totalAmount" class="number-input" readonly />
                  </j-form-item>
                  <j-form-item label="本次付款" :span="8">
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
    </a-modal>
    <!-- 采购订单查看窗口 -->
    <purchase-order-detail :id="formData.purchaseOrderId" ref="viewPurchaseOrderDetailDialog" />
    <order-print-dialog />
  </div>
</template>
<script>
  import { defineComponent } from 'vue';
  import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
  import * as api from '@/api/sc/purchase/receive';
  import { printMix } from '@/mixins/print.ts';
  import { previewReceiveSheetPrint } from './print';
  import { add, getNumber, isEmpty, isFloat, isFloatGeZero, mul, sub } from '@/utils/utils';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import PrintDialog from '/@/components/PrintDialog';
  import { createSuccess } from '@/hooks/web/msg';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';

  export default defineComponent({
    components: {
      PurchaseOrderDetail,
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
      return {
        isEmpty,
        isFloat,
        isFloatGeZero,
        getNumber,
        mul,
        formatInquiryProduct,
        RECEIVE_SHEET_STATUS,
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
          { field: 'productName', title: '商品名称', width: 260 },
          {
            field: 'inquiryProduct',
            title: '是否询价商品',
            width: 120,
            slots: { default: 'inquiryProduct_default' },
          },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'purchasePrice', title: '采购价（元）', align: 'right', width: 120 },
          { field: 'productionDate', title: '生产日期', width: 120 },
          {
            field: 'receiveNum',
            title: '数量',
            align: 'right',
            width: 100,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          {
            field: 'taxAmount',
            title: '金额',
            align: 'right',
            width: 120,
            slots: { default: 'taxAmount_default' },
          },
          { field: 'description', title: '备注', width: 200 },
        ],
        tableData: [],
      };
    },
    computed: {},
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
          supplierName: '',
          purchaserName: '',
          orderDate: '',
          receiveDate: '',
          purchaseOrderId: '',
          purchaseOrderCode: '',
          totalNum: 0,
          totalAmount: 0,
          paidAmount: 0,
          unpaidAmount: 0,
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
              supplierName: res.supplierName,
              purchaserName: res.purchaserName || '',
              orderDate: res.orderDate || '',
              receiveDate: res.receiveDate,
              purchaseOrderId: res.purchaseOrderId || '',
              purchaseOrderCode: res.purchaseOrderCode || '',
              description: res.description,
              status: res.status,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: res.totalAmount || 0,
              paidAmount: res.paidAmount || 0,
              unpaidAmount: sub(res.totalAmount || 0, res.paidAmount || 0),
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
            return isFloatGeZero(t.purchasePrice) && isFloat(t.receiveNum);
          })
          .forEach((t) => {
            const num = parseFloat(t.receiveNum);
            totalNum = add(totalNum, num);
            totalAmount = add(totalAmount, getNumber(mul(num, t.purchasePrice), 2));
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
        this.formData.unpaidAmount = sub(
          this.formData.totalAmount || 0,
          this.formData.paidAmount || 0,
        );
      },
      async print() {
        this.loading = true;
        try {
          await previewReceiveSheetPrint(this.id, api.print, (type, data) => {
            return this.vgPrintPreview(type, data);
          });
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
</style>
