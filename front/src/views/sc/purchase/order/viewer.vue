<template>
  <div v-permission="['purchase:order:query']" v-loading="loading">
    <j-border>
      <j-form bordered>
        <j-form-item label="仓库">
          {{ formData.scName }}
        </j-form-item>
        <j-form-item label="供应商">
          {{ formData.supplierName }}
        </j-form-item>
        <j-form-item label="采购员">
          {{ formData.purchaserName }}
        </j-form-item>
        <j-form-item label="状态">
          <span
            v-if="PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(formData.status)"
            style="color: #52c41a"
            >{{ PURCHASE_ORDER_STATUS.getDesc(formData.status) }}</span
          >
          <span
            v-else-if="PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
            style="color: #f5222d"
            >{{ PURCHASE_ORDER_STATUS.getDesc(formData.status) }}</span
          >
          <span v-else style="color: #303133">{{
            PURCHASE_ORDER_STATUS.getDesc(formData.status)
          }}</span>
        </j-form-item>
        <j-form-item label="拒绝理由" :content-nest="false">
          <a-input
            v-if="PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
            v-model:value="formData.refuseReason"
            readonly
          />
        </j-form-item>
        <j-form-item label="操作人">
          <span>{{ formData.createBy }}</span>
        </j-form-item>
        <j-form-item label="操作时间" :span="16">
          <span>{{ formData.createTime }}</span>
        </j-form-item>
        <j-form-item
          v-if="
            PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
            PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
          "
          label="审核人"
        >
          <span>{{ formData.approveBy }}</span>
        </j-form-item>
        <j-form-item
          v-if="
            PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(formData.status) ||
            PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(formData.status)
          "
          label="审核时间"
          :span="16"
        >
          <span>{{ formData.approveTime }}</span>
        </j-form-item>
      </j-form>
    </j-border>
    <!-- 数据列表 -->
    <vxe-grid
      id="PurchaseOrderViewer"
      ref="grid"
      resizable
      show-overflow
      highlight-hover-row
      keep-source
      row-id="id"
      height="380"
      :data="tableData"
      :columns="tableColumn"
      :toolbar-config="toolbarConfig"
      :custom-config="{}"
    >
      <!-- 采购含税金额 列自定义内容 -->
      <template #purchaseAmount_default="{ row }">
        <span v-if="isFloatGeZero(row.purchasePrice) && isFloatGeZero(row.purchaseNum)">{{
          getNumber(mul(row.purchasePrice, row.purchaseNum), 2)
        }}</span>
      </template>
    </vxe-grid>

    <j-border title="合计">
      <j-form bordered label-width="140px">
        <j-form-item label="采购数量" :span="6">
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
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/sc/purchase/order';
  import { isFloatGeZero, getNumber, mul, add } from '@/utils/utils';
  import { PURCHASE_ORDER_STATUS } from '@/enums/biz/purchaseOrderStatus';
  import JFormItem from '@/components/JFormItem';

  export default defineComponent({
    components: { JFormItem },
    props: {
      id: {
        type: String,
        required: true,
      },
      isForm: {
        type: Boolean,
        default: false,
      },
    },
    setup() {
      return {
        isFloatGeZero,
        getNumber,
        mul,
        PURCHASE_ORDER_STATUS,
      };
    },
    data() {
      return {
        // 是否显示加载框
        loading: false,
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
          { field: 'shortName', title: '简称', width: 120 },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          { field: 'purchasePrice', title: '采购价（元）', align: 'right', width: 120 },
          { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
          { field: 'purchaseNum', title: '采购数量', align: 'right', width: 100 },
          {
            field: 'purchaseAmount',
            title: '采购含税金额',
            align: 'right',
            width: 120,
            slots: { default: 'purchaseAmount_default' },
          },
          { field: 'description', title: '备注', width: 200 },
        ],
        tableData: [],
      };
    },
    computed: {},
    created() {
      // 初始化表单数据
      this.open();
    },
    methods: {
      // 初始化表单数据
      initFormData() {
        this.formData = {
          scName: '',
          supplierName: '',
          purchaserName: '',
          totalNum: 0,
          totalAmount: 0,
          description: '',
          flowInstanceId: '',
        };

        this.tableData = [];
      },
      // 加载数据
      loadData() {
        this.loading = true;
        api
          .get(this.id, this.isForm)
          .then((res) => {
            this.formData = {
              id: res.id,
              scName: res.scName,
              supplierName: res.supplierName,
              purchaserName: res.purchaserName,
              description: res.description,
              status: res.status,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: 0,
              flowInstanceId: res.flowInstanceId,
            };
            this.tableData = res.details || [];

            this.calcSum();

            this.$emit('load-data-complete', this.formData);
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
            return isFloatGeZero(t.purchasePrice) && isFloatGeZero(t.purchaseNum);
          })
          .forEach((t) => {
            const num = parseFloat(t.purchaseNum);
            totalNum = add(totalNum, num);

            // 先将每行的金额格式化成2位小数，然后再累加
            const rowAmount = getNumber(mul(num, t.purchasePrice), 2);
            totalAmount = add(totalAmount, rowAmount);
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
      },
      getFormData() {
        return this.formData;
      },
      exportDetails() {
        // exportPurchaseOrderDetails(this.tableData);
      },
    },
  });
</script>
<style></style>
