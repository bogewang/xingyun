<template>
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
                <j-form-item label="销售订单">
                  <div v-if="!isEmpty(formData.saleOrderCode)">
                    <a
                      v-permission="['sale:order:query']"
                      @click="(e) => $refs.viewSaleOrderDetailDialog.openDialog()"
                      >{{ formData.saleOrderCode }}</a
                    >
                    <span v-no-permission="['sale:order:query']">{{ formData.saleOrderCode }}</span>
                  </div>
                </j-form-item>
                <j-form-item label="状态">
                  <span
                    v-if="SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status)"
                    style="color: #52c41a"
                    >{{ SALE_OUT_SHEET_STATUS.getDesc(formData.status) }}</span
                  >
                  <span
                    v-else-if="SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
                    style="color: #f5222d"
                    >{{ SALE_OUT_SHEET_STATUS.getDesc(formData.status) }}</span
                  >
                  <span v-else style="color: #303133">{{
                    SALE_OUT_SHEET_STATUS.getDesc(formData.status)
                  }}</span>
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
                <j-form-item label="拒绝理由" :content-nest="false">
                  <a-input
                    v-if="SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
                    v-model:value="formData.refuseReason"
                    readonly
                  />
                </j-form-item>
                <j-form-item
                  v-if="
                    SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
                    SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
                  "
                  label="审核人"
                >
                  <span>{{ formData.approveBy }}</span>
                </j-form-item>
                <j-form-item
                  v-if="
                    SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
                    SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
                  "
                  label="审核时间"
                  :span="16"
                >
                  <span>{{ formData.approveTime }}</span>
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
                :columns="tableColumn"
                :toolbar-config="toolbarConfig"
                :custom-config="{}"
              >
                <template #taxAmount_default="{ row }">
                  <span v-if="isFloatGeZero(row.taxPrice) && isFloatGeZero(row.outNum)">{{
                    getNumber(mul(row.taxPrice, row.outNum), 2)
                  }}</span>
                </template>
                <template #costStatus_default="{ row }">
                  <span :style="{ color: hasCostPrice(row) ? '#52c41a' : '#f5222d' }">
                    {{ hasCostPrice(row) ? '已补全' : '未补全' }}
                  </span>
                </template>
              </vxe-grid>
            </div>

            <j-border title="合计">
              <j-form bordered label-width="140px">
                <j-form-item label="出库数量" :span="6">
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
                  <a-textarea v-model:value.trim="formData.description" maxlength="200" readonly />
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
  import { add, getNumber, isEmpty, isFloatGeZero, mul, sub } from '@/utils/utils';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import PrintDialog from '/@/components/PrintDialog';
  import JFormItem from '@/components/JFormItem';
  import { createSuccess } from '@/hooks/web/msg';

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
      return {
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
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
          { field: 'productName', title: '商品名称', width: 260 },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 80 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          { field: 'mainProductName', title: '所属组合商品', width: 120 },
          { field: 'oriPrice', title: '参考销售价（元）', align: 'right', width: 150 },
          { field: 'taxPrice', title: '价格（元）', align: 'right', width: 80 },
          {
            field: 'orderNum',
            title: '销售数量',
            align: 'right',
            width: 80,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          {
            field: 'remainNum',
            title: '剩余出库数量',
            align: 'right',
            width: 120,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          { field: 'outNum', title: '出库数量', align: 'right', width: 100 },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 80,
            slots: { default: 'taxAmount_default' },
          },
          { field: 'costPrice', title: '成本单价', align: 'right', width: 100 },
          {
            field: 'costStatus',
            title: '成本状态',
            width: 100,
            slots: { default: 'costStatus_default' },
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
              totalAmount: 0,
              totalProfit: res.totalProfit || 0,
              paidAmount: res.paidAmount || 0,
              unpaidAmount: 0,
              fillAllCost: !!res.fillAllCost,
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
            return isFloatGeZero(t.taxPrice) && isFloatGeZero(t.outNum);
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
      async print() {
        this.loading = true;
        try {
          const res = await api.print(this.id);
          await this.vgPrintPreview(PRINT_TYPE.SALE_OUT.code, res);
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
