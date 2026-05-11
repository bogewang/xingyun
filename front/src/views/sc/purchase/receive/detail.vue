<template>
  <div>
    <a-modal
      v-model:open="visible"
      :mask-closable="false"
      width="75%"
      title="查看"
      :style="{ top: '20px' }"
      :body-style="{ height: 'calc(100vh - 160px)', overflow: 'hidden' }"
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
                  <j-form-item label="采购订单">
                    <div v-if="!isEmpty(formData.purchaseOrderCode)">
                      <a
                        v-permission="['purchase:order:query']"
                        @click="(e) => $refs.viewPurchaseOrderDetailDialog.openDialog()"
                        >{{ formData.purchaseOrderCode }}</a
                      >
                      <span v-no-permission="['purchase:order:query']">{{
                        formData.purchaseOrderCode
                      }}</span>
                    </div>
                  </j-form-item>
                  <j-form-item label="状态">
                    <span
                      v-if="RECEIVE_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status)"
                      style="color: #52c41a"
                      >{{ RECEIVE_SHEET_STATUS.getDesc(formData.status) }}</span
                    >
                    <span
                      v-else-if="RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
                      style="color: #f5222d"
                      >{{ RECEIVE_SHEET_STATUS.getDesc(formData.status) }}</span
                    >
                    <span v-else style="color: #303133">{{
                      RECEIVE_SHEET_STATUS.getDesc(formData.status)
                    }}</span>
                  </j-form-item>

                  <j-form-item label="操作人">
                    <span>{{ formData.createBy }}</span>
                  </j-form-item>
                  <j-form-item label="操作时间">
                    <span>{{ formData.createTime }}</span>
                  </j-form-item>
                  <j-form-item
                    v-if="
                      RECEIVE_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
                      RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
                    "
                    label="审核人"
                  >
                    <span>{{ formData.approveBy }}</span>
                  </j-form-item>
                  <j-form-item
                    v-if="
                      RECEIVE_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
                      RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
                    "
                    label="审核时间"
                    :span="16"
                  >
                    <span>{{ formData.approveTime }}</span>
                  </j-form-item>
                  <j-form-item label="拒绝理由" :span="16" :content-nest="false">
                    <a-input
                      v-if="RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
                      v-model:value="formData.refuseReason"
                      readonly
                    />
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
                    <span v-if="isFloatGeZero(row.purchasePrice) && isFloatGeZero(row.receiveNum)">{{
                      getNumber(mul(row.purchasePrice, row.receiveNum), 2)
                    }}</span>
                  </template>
                </vxe-grid>
              </div>

              <j-border title="合计">
                <j-form bordered label-width="140px">
                  <j-form-item label="收货数量" :span="6">
                    <a-input v-model:value="formData.totalNum" class="number-input" readonly />
                  </j-form-item>
                  <j-form-item label="含税总金额" :span="6">
                    <a-input v-model:value="formData.totalAmount" class="number-input" readonly />
                  </j-form-item>
                  <j-form-item label="备注" :span="12" :content-nest="false">
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
import {defineComponent} from 'vue';
import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
import * as api from '@/api/sc/purchase/receive';
import {printMix} from '@/mixins/print.ts';
import {add, getNumber, isEmpty, isFloatGeZero, mul} from '@/utils/utils';
import {RECEIVE_SHEET_STATUS} from '@/enums/biz/receiveSheetStatus';
import {PRINT_TYPE} from '@/enums/biz/printType';
import OrderTimeLine from '@/components/OrderTimeLine';
import PrintDialog from '/@/components/PrintDialog';
import { createSuccess } from '@/hooks/web/msg';

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
        isFloatGeZero,
        getNumber,
        mul,
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
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          { field: 'purchasePrice', title: '采购价（元）', align: 'right', width: 120 },
          {
            field: 'orderNum',
            title: '采购数量',
            align: 'right',
            width: 100,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          {
            field: 'remainNum',
            title: '剩余收货数量',
            align: 'right',
            width: 120,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          { field: 'receiveNum', title: '收货数量', align: 'right', width: 100 },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 120,
            slots: { default: 'taxAmount_default' },
          },
          { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
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
          paymentDate: '',
          receiveDate: '',
          purchaseOrderId: '',
          purchaseOrderCode: '',
          totalNum: 0,
          totalAmount: 0,
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
              paymentDate: res.paymentDate || '',
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
              totalAmount: 0,
            };
            this.tableData = res.details || [];

            this.calcSum();
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
            return isFloatGeZero(t.purchasePrice) && isFloatGeZero(t.receiveNum);
          })
          .forEach((t) => {
            const num = parseFloat(t.receiveNum);
            totalNum = add(totalNum, num);
            totalAmount = add(totalAmount, getNumber(mul(num, t.purchasePrice), 2));
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
      },
      async print() {
        this.loading = true;
        try {
          const res = await api.print(this.id);
          await this.vgPrintPreview(PRINT_TYPE.RECEIVE_SHEET.code, res);
        } finally {
          this.loading = false;
        }
      },
      exportDetails() {
        this.loading = true;
        api.exportDetail(this.buildQueryParams())
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
  overflow: hidden;
}

.order-detail-grid-wrap {
  flex: 1;
  min-height: 0;
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
