<template>
  <div class="app-card-container sheet-editor-page">
    <div
      class="sheet-editor-content"
      v-permission="['purchase:receive:modify']"
      v-loading="loading"
    >
      <a-alert
        description="提示：使用回车键可以快速添加商品；使用Tab键可以快速跳转至下一个输入框。"
        type="info"
        show-icon
      />
      <j-border>
        <j-form bordered>
          <j-form-item label="供应商" required>
            {{ formData.supplier.name }}
          </j-form-item>
          <j-form-item label="采购员">
            <a-select
              v-model:value="formData.purchaserId"
              allow-clear
              show-search
              :filter-option="filterOption"
              :options="purchaserOptions"
              placeholder="请选择采购员"
              @focus="loadPurchaserOptions()"
              @search="loadPurchaserOptions"
              @change="(value) => handleSelectChange('purchaserId', value, purchaserOptionMap)"
            />
          </j-form-item>
          <j-form-item label="订单日期">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder=""
              value-format="YYYY-MM-DD"
            />
          </j-form-item>
          <j-form-item label="采购订单" required>
            <div v-if="!isEmpty(formData.purchaseOrder.code)">
              <a
                v-permission="['purchase:order:query']"
                @click="(e) => $refs.viewPurchaseOrderDetailDialog.openDialog()"
                >{{ formData.purchaseOrder.code }}</a
              >
              <span v-no-permission="['purchase:order:query']">{{
                formData.purchaseOrder.code
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
          <j-form-item :span="16" :content-nest="false" label="拒绝理由">
            <a-input
              v-if="RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
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
        </j-form>
      </j-border>
      <!-- 数据列表 -->
      <vxe-grid
        class="sheet-editor-grid"
        id="ReceiveSheetModifyRequire"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        height="100%"
        :data="tableData"
        :columns="tableColumn"
        :row-class-name="getTableRowClassName"
        :cell-class-name="getCellClassName"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
      >
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(PlusOutlined)" @click="addProduct">新增</a-button>
            <a-button danger :icon="h(DeleteOutlined)" @click="delProduct">删除</a-button>
            <a-button :icon="h(PlusOutlined)" @click="openBatchAddProductDialog"
              >批量添加商品</a-button
            >
            <a-button :icon="h(NumberOutlined)" @click="batchInputReceiveNum"
              >批量录入数量</a-button
            >
            <a-tooltip title="将收货数量设置为剩余收货数量">
              <a-button :icon="h(EditOutlined)" @click="quickSettingReceiveNum"
                >快捷设置数量</a-button
              >
            </a-tooltip>
          </a-space>
        </template>

        <template #operation_default="{ row, rowIndex }">
          <a-space size="small">
            <a-button
              type="link"
              size="small"
              :icon="h(PlusCircleTwoTone)"
              @click="insertProduct(rowIndex)"
            />
            <a-button
              v-show="!row.isFixed"
              type="link"
              size="small"
              danger
              :icon="h(MinusCircleTwoTone)"
              @click="removeCurrentProduct(row)"
            />
          </a-space>
        </template>

        <!-- 商品名称 列自定义内容 -->
        <template #inquiryProduct_default="{ row }">
          <span :class="formatInquiryProduct(row.inquiryProduct).className">
            {{ formatInquiryProduct(row.inquiryProduct).text }}
          </span>
        </template>

        <template #productName_default="{ row, rowIndex }">
          <InlineProductSelect
            :ref="'productInputRef' + rowIndex"
            :row="row"
            :row-index="rowIndex"
            biz-type="purchase"
            mode="require"
            :sc-id="formData.sc.id"
            :is-fixed="row.isFixed"
            @select="handleSelectProduct"
            @add-product="addProduct"
            @open-add-product-page="openChildPage('/product/info/add')"
          />
        </template>

        <!-- 采购价 列自定义内容 -->
        <template #unit_default="{ row }">
          <a-select
            v-model:value="row.unitId"
            size="small"
            style="width: 90px"
            @change="(value) => selectUnit(row, value)"
          >
            <a-select-option v-for="item in row.units || []" :key="item.id" :value="item.id">{{
              item.unitName
            }}</a-select-option>
          </a-select>
        </template>

        <template #purchasePrice_default="{ row }">
          <span>{{ row.purchasePrice }}</span>
        </template>

        <!-- 剩余收货数量 列自定义内容 -->
        <template #remainNum_default="{ row }">
          <span v-if="isEmpty(row.remainNum)">-</span>
          <span v-else-if="isFloat(row.receiveNum)">{{
            Math.max(0, sub(row.remainNum, row.receiveNum))
          }}</span>
          <span v-else>{{ row.remainNum }}</span>
        </template>

        <!-- 收货数量 列自定义内容 -->
        <template #receiveNum_default="{ row }">
          <a-input
            v-model:value="row.receiveNum"
            class="number-input"
            @input="(e) => receiveNumInput(row, e.target.value)"
          />
        </template>

        <!-- 含税金额 列自定义内容 -->
        <template #taxAmount_default="{ row }">
          <a-input
            v-model:value="row.taxAmount"
            class="number-input"
            @input="(e) => taxAmountInput(row, e.target.value)"
          />
        </template>

        <template #productionDate_default="{ row }">
          <a-input v-model:value="row.productionDate" />
        </template>

        <!-- 备注 列自定义内容 -->
        <template #description_default="{ row }">
          <a-input v-model:value="row.description" />
        </template>
      </vxe-grid>

      <j-border title="合计">
        <j-form bordered label-width="140px">
          <j-form-item label="收货数量" :span="8">
            <a-input v-model:value="formData.totalNum" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="含税总金额" :span="8">
            <a-input v-model:value="formData.totalAmount" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="已结算金额" :span="8">
            <a-input v-model:value="formData.paidAmount" class="number-input" readonly />
          </j-form-item>
        </j-form>
      </j-border>

      <j-border>
        <j-form bordered label-width="140px">
          <j-form-item label="备注" :span="24" :content-nest="false">
            <a-textarea v-model:value.trim="formData.description" maxlength="200" />
          </j-form-item>
        </j-form>
      </j-border>
      <batch-add-product
        ref="batchAddProductDialog"
        :show-inquiry-product="true"
        :sc-id="formData.sc.id"
        @confirm="batchAddProduct"
      />
      <a-modal
        v-model:open="timelineVisible"
        title="操作记录"
        :footer="null"
        :width="640"
        destroy-on-close
      >
        <order-time-line v-if="timelineVisible" :id="id" :expand-all="true" />
      </a-modal>

      <div
        class="sheet-editor-actions"
        style="text-align: center; background-color: #ffffff; padding: 8px 0"
      >
        <a-space>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
          <a-button :loading="loading" @click="openTimeline">操作记录</a-button>
          <a-button
            v-permission="['purchase:receive:modify']"
            type="primary"
            :loading="loading"
            @click="updateOrder"
            >保存</a-button
          >
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </div>
    <!-- 采购订单查看窗口 -->
    <purchase-order-detail :id="formData.purchaseOrder.id" ref="viewPurchaseOrderDetailDialog" />
  </div>
</template>
<script>
  import { h, defineComponent } from 'vue';
  import BatchAddProduct from '@/views/sc/purchase/batch-add-product.vue';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import Moment from 'moment';
  import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
  import {
    PlusOutlined,
    DeleteOutlined,
    PlusCircleTwoTone,
    MinusCircleTwoTone,
    NumberOutlined,
    EditOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/purchase/receive';
  import { multiplePageMix } from '@/mixins/multiplePageMix';

  import InlineProductSelect from '@/views/sc/shared/inline-product-select.vue';
  import {
    isEmpty,
    isFloatGeZero,
    isFloatGtZero,
    isFloat,
    isNumberPrecision,
    getNumber,
    mul,
    add,
    sub,
    uuid,
    PATTERN_IS_FLOAT,
  } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { focusVxeGridRow } from '@/utils/vxeGrid';
  import { getSheetAmountCellClass, hasSheetAmountWarning } from '@/utils/sheetAmountWarning';
  import {
    applyManualSheetAmount,
    clearManualSheetAmount,
    getSheetLineAmount,
  } from '@/utils/sheetAmountInput';
  import { resetInlineProductSelect } from '@/utils/inlineProductSelect';
  import { shouldAddProductByEnter } from '@/utils/productAddShortcut';
  import { requestUserSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm, createPrompt } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import { calculateUnitPrice, calculateUnitStockNum } from '@/utils/productUnitConversion';

  export default defineComponent({
    name: 'ModifyPurchaseReceiveSheetRequire',
    components: {
      PurchaseOrderDetail,
      BatchAddProduct,
      OrderTimeLine,
      InlineProductSelect,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        formatInquiryProduct,
        PlusOutlined,
        PlusCircleTwoTone,
        DeleteOutlined,
        MinusCircleTwoTone,
        NumberOutlined,
        EditOutlined,
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
        sub,
        RECEIVE_SHEET_STATUS,
        SETTLE_STATUS,
      };
    },
    data() {
      return {
        id: this.$route.params.id,
        // 是否显示加载框
        loading: false,
        // 表单数据
        formData: {},
        timelineVisible: false,
        purchaserOptions: [],
        purchaserOptionMap: {},
        // 工具栏配置
        toolbarConfig: {
          // 缩放
          zoom: false,
          // 自定义表头
          custom: true,
          // 右侧是否显示刷新按钮
          refresh: false,
          // 自定义左侧工具栏
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          {
            field: 'operation',
            title: '操作',
            width: 140,
            slots: { default: 'operation_default' },
          },
          { field: 'productCode', title: '商品编号', width: 120 },
          {
            field: 'productName',
            title: '商品名称',
            width: 260,
            slots: { default: 'productName_default' },
          },
          {
            field: 'inquiryProduct',
            title: '是否询价商品',
            width: 120,
            slots: { default: 'inquiryProduct_default' },
          },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 100, slots: { default: 'unit_default' } },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          {
            field: 'purchasePrice',
            title: '采购价（元）',
            align: 'right',
            width: 140,
            slots: { default: 'purchasePrice_default' },
          },
          { field: 'taxCostPrice', title: '含税成本价（元）', align: 'right', width: 140 },
          { field: 'stockNum', title: '库存数量', align: 'right', width: 140 },
          {
            field: 'orderNum',
            title: '采购数量',
            align: 'right',
            width: 140,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          {
            field: 'remainNum',
            title: '剩余收货数量',
            align: 'right',
            width: 140,
            slots: { default: 'remainNum_default' },
          },
          {
            field: 'receiveNum',
            title: '收货数量',
            align: 'right',
            width: 140,
            slots: { default: 'receiveNum_default' },
          },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 140,
            slots: { default: 'taxAmount_default' },
          },
          { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
          {
            field: 'productionDate',
            title: '生产日期',
            width: 120,
            slots: { default: 'productionDate_default' },
          },
          {
            field: 'description',
            title: '备注',
            width: 200,
            slots: { default: 'description_default' },
          },
        ],
        tableData: [],
      };
    },
    computed: {
      moment() {
        return Moment;
      },
    },
    created() {
      // 初始化表单数据
      this.openDialog();
    },
    activated() {
      // 仅在当前页面激活时监听回车快捷键，避免缓存页面误响应。
      document.addEventListener('keydown', this.handleKeyDown);
    },
    deactivated() {
      document.removeEventListener('keydown', this.handleKeyDown);
    },
    beforeUnmount() {
      document.removeEventListener('keydown', this.handleKeyDown);
    },
    methods: {
      handleKeyDown(event) {
        if (!shouldAddProductByEnter(event)) {
          return;
        }

        this.addProduct();
      },
      // 打开对话框 由父页面触发
      openDialog() {
        // 初始化表单数据
        this.initFormData();
        this.loadData();
      },
      // 关闭对话框
      closeDialog() {
        this.closeCurrentPage();
      },
      // 返回采购收货查询页，避免保存后回到其他缓存页面
      goQueryPage() {
        this.closeCurrentPage();
      },
      // 初始化表单数据
      initFormData() {
        this.formData = {
          sc: {},
          supplier: {},
          purchaseOrder: {},
          purchaserId: '',
          orderDate: '',
          receiveDate: '',
          totalNum: 0,
          totalAmount: 0,
          paidAmount: 0,
          settleStatus: '',
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
            if (
              !RECEIVE_SHEET_STATUS.CREATED.equalsCode(res.status) &&
              !RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(res.status)
            ) {
              createError('采购收货单已审核通过，无法修改！');
              this.closeDialog();
              return;
            }
            if (
              Number(res.paidAmount || 0) > 0 ||
              SETTLE_STATUS.PART_SETTLE.equalsCode(res.settleStatus) ||
              SETTLE_STATUS.SETTLED.equalsCode(res.settleStatus)
            ) {
              createError('采购收货单已有结算金额，无法修改！');
              this.closeDialog();
              return;
            }
            if (!SETTLE_STATUS.UN_CHECK_BILL.equalsCode(res.settleStatus)) {
              createError('采购收货单已进入对账/结算流程，无法修改！');
              this.closeDialog();
              return;
            }
            this.formData = Object.assign(this.formData, {
              sc: {
                id: res.scId,
                name: res.scName,
              },
              supplier: {
                id: res.supplierId,
                name: res.supplierName,
              },
              purchaserId: res.purchaserId || '',
              orderDate: res.orderDate || '',
              receiveDate: res.receiveDate,
              purchaseOrder: {
                id: res.purchaseOrderId,
                code: res.purchaseOrderCode,
              },
              description: res.description,
              paidAmount: res.paidAmount,
              status: res.status,
              settleStatus: res.settleStatus,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: res.totalAmount || 0,
            });

            const tableData = res.details || [];
            tableData.forEach((item) => {
              item.isFixed = !isEmpty(item.purchaseOrderDetailId);

              if (item.isFixed) {
                // 接口返回的剩余收货数量是最新的数据，应加上当前收货数量
                item.remainNum = add(item.remainNum, item.receiveNum);
              }

              return item;
            });
            this.tableData = tableData.map((item) => Object.assign(this.emptyProduct(), item));
            this.calcSum();
          })
          .finally(() => {
            this.loading = false;
          });
      },
      emptyProduct() {
        return {
          id: uuid(),
          productId: '',
          productCode: '',
          productName: '',
          skuCode: '',
          externalCode: '',
          unit: '',
          spec: '',
          categoryName: '',
          brandName: '',
          purchasePrice: 0,
          taxCostPrice: '',
          stockNum: '',
          orderNum: '',
          remainNum: '',
          receiveNum: '',
          taxRate: '',
          taxAmount: '',
          description: '',
          isFixed: false,
          editingProduct: false,
          productQuery: '',
          products: [],
          productOptions: [],
          activeProductIndex: -1,
        };
      },
      async focusProductRow(index) {
        await focusVxeGridRow({
          grid: this.$refs.grid,
          row: this.tableData[index],
          rowIndex: index,
          nextTick: () => this.$nextTick(),
          focus: () => this.$refs['productInputRef' + index]?.focus(),
        });
      },
      // 新增商品
      addProduct() {
        if (isEmpty(this.formData.purchaseOrder)) {
          createError('请先选择采购订单！');
          return;
        }
        this.tableData.push(this.emptyProduct());
        this.focusProductRow(this.tableData.length - 1);
      },
      insertProduct(index) {
        const insertedIndex = index + 1;
        this.tableData.splice(insertedIndex, 0, this.emptyProduct());
        this.focusProductRow(insertedIndex);
      },
      removeCurrentProduct(row) {
        if (row.isFixed) {
          createError('采购订单中的商品，不允许删除！');
          return;
        }
        this.tableData = this.tableData.filter((item) => item.id !== row.id);
        this.calcSum();
      },
      // 选择商品（从表格中点击）
      handleSelectProduct(index, product) {
        // 如果行内已有有效的采购价(>0)，则保留原价格，不被最新采购价覆盖
        const purchasePrice = isFloatGtZero(this.tableData[index].purchasePrice)
          ? this.tableData[index].purchasePrice
          : !isEmpty(product.latestPurchasePrice)
          ? product.latestPurchasePrice
          : product.purchasePrice;
        const baseUnit = product.units?.find((item) => item.baseUnit);
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          purchasePrice,
          basePurchasePrice: purchasePrice,
          baseStockNum: product.stockNum,
          unitId: baseUnit?.id || '',
          unit: baseUnit?.unitName || product.unit || '',
          editingProduct: false,
          productQuery: '',
        });
        resetInlineProductSelect(this.tableData[index]);

        this.purchasePriceInput(this.tableData[index], this.tableData[index].purchasePrice);
      },
      // 删除商品
      delProduct() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要删除的商品数据！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (records[i].isFixed) {
            createError('第' + (i + 1) + '行商品是采购订单中的商品，不允许删除！');
            return;
          }
        }
        createConfirm('是否确定删除选中的商品？').then(() => {
          const tableData = this.tableData.filter((t) => {
            const tmp = records.filter((item) => item.id === t.id);
            return isEmpty(tmp);
          });

          this.tableData = tableData;

          this.calcSum();
        });
      },
      openBatchAddProductDialog() {
        if (isEmpty(this.formData.purchaseOrder)) {
          createError('请先选择采购订单！');
          return;
        }
        this.$refs.batchAddProductDialog.openDialog();
      },
      filterOption(input, option) {
        return filterSelectOption(input, option);
      },
      handleSelectChange(field, value, optionMap) {
        this.formData[field] = normalizeSelectValue(value, optionMap);
      },
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
      },
      async loadPurchaserOptions(keyword = '') {
        const options = await this.requestUserOptions(keyword);
        this.purchaserOptionMap = mergeSelectOptionMap(this.purchaserOptionMap, options);
        this.purchaserOptions = buildVisibleSelectOptions(
          this.formData.purchaserId,
          this.purchaserOptionMap,
          options,
        );
      },
      purchasePriceInput(_row, _value) {
        clearManualSheetAmount(_row, 'receiveNum', 'purchasePrice');
        this.calcSum();
      },
      taxAmountInput(row, value) {
        applyManualSheetAmount(row, value, 'receiveNum', 'purchasePrice');
        this.calcSum();
      },
      hasWarningAmount(row) {
        return hasSheetAmountWarning(row, 'purchasePrice', 'receiveNum');
      },
      getTableRowClassName({ row }) {
        return this.hasWarningAmount(row) ? 'sheet-price-warning-row' : '';
      },
      getCellClassName({ row, column }) {
        return getSheetAmountCellClass(row, column.field, 'purchasePrice', 'receiveNum');
      },
      selectUnit(row, unitId) {
        const unit = (row.units || []).find((item) => item.id === unitId);
        if (!unit) return;
        const rate = Number(unit.conversionRate) || 1;
        const oldRate = Number(row.conversionRate) || 1;
        const stockNum = calculateUnitStockNum(row.stockNum, row.baseStockNum, oldRate, rate);
        const purchasePrice = calculateUnitPrice(
          row.purchasePrice,
          row.basePurchasePrice,
          oldRate,
          rate,
        );
        row.baseStockNum = stockNum.baseStockNum;
        row.basePurchasePrice = purchasePrice.basePrice;
        row.conversionRate = rate;
        row.unit = unit.unitName;
        row.purchasePrice = purchasePrice.unitPrice;
        row.stockNum = stockNum.stockNum;
        clearManualSheetAmount(row, 'receiveNum', 'purchasePrice');
        this.calcSum();
      },
      receiveNumInput(row, value) {
        if (value === undefined) {
          clearManualSheetAmount(row, 'receiveNum', 'purchasePrice');
          this.calcSum();
          return;
        }
        row.receiveNum = value;
        clearManualSheetAmount(row, 'receiveNum', 'purchasePrice');
        this.calcSum();
      },
      // 计算汇总数据
      calcSum() {
        let totalNum = 0;
        let totalAmount = 0;
        this.tableData
          .filter((t) => {
            return t.manualTaxAmount || (isFloatGeZero(t.purchasePrice) && isFloat(t.receiveNum));
          })
          .forEach((t) => {
            const num = parseFloat(t.receiveNum);
            totalNum = add(totalNum, num);
            totalAmount = add(totalAmount, getSheetLineAmount(t, 'receiveNum', 'purchasePrice'));
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
      },
      // 批量录入数量
      batchInputReceiveNum() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        createPrompt('请输入收货数量', {
          inputPattern: PATTERN_IS_FLOAT,
          inputErrorMessage: '收货数量必须是数字',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.receiveNum = value;

            this.receiveNumInput(t, value);
          });
        });
      },
      // 快捷设置数量
      quickSettingReceiveNum() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          const record = records[i];
          if (record.isFixed) {
            this.receiveNumInput(record, record.remainNum);
          }
        }

        this.calcSum();
      },
      // 批量新增商品
      batchAddProduct(productList) {
        productList.forEach((item) => {
          this.tableData.push(this.emptyProduct());
          this.handleSelectProduct(this.tableData.length - 1, item);
        });
      },
      // 校验数据
      validData() {
        if (isEmpty(this.formData.supplier.id)) {
          createError('供应商不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.purchaseOrder.id)) {
          createError('采购订单不允许为空！');
          return false;
        }

        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));

        if (isEmpty(validTableData)) {
          createError('请录入商品！');
          return false;
        }

        for (let i = 0; i < validTableData.length; i++) {
          const product = validTableData[i];

          if (!isEmpty(product.purchasePrice)) {
            if (!isFloat(product.purchasePrice)) {
              createError('第' + (i + 1) + '行商品采购价必须是数字！');
              return false;
            }

            if (!isFloatGeZero(product.purchasePrice)) {
              createError('第' + (i + 1) + '行商品采购价不允许小于0！');
              return false;
            }

            if (!isNumberPrecision(product.purchasePrice, 6)) {
              createError('第' + (i + 1) + '行商品采购价最多允许6位小数！');
              return false;
            }
          }

          if (!isEmpty(product.receiveNum)) {
            if (!isFloat(product.receiveNum)) {
              createError('第' + (i + 1) + '行商品收货数量必须是数字！');
              return false;
            }

            if (!isNumberPrecision(product.receiveNum, 8)) {
              createError('第' + (i + 1) + '行商品收货数量最多允许8位小数！');
              return false;
            }

            if (product.isFixed) {
              if (product.receiveNum > product.remainNum) {
                createError(
                  '第' +
                    (i + 1) +
                    '行商品累计收货数量为' +
                    (product.orderNum - product.remainNum) +
                    '，剩余收货数量为' +
                    product.remainNum +
                    '，本次收货数量不允许大于' +
                    product.remainNum +
                    '！',
                );
                return false;
              }
            }
          }
        }

        if (
          validTableData.filter((item) => isFloat(item.receiveNum) && Number(item.receiveNum) !== 0)
            .length === 0
        ) {
          createError('采购订单中的商品必须全部或部分收货！');
          return false;
        }

        return true;
      },
      // 修改订单
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
      openTimeline() {
        this.timelineVisible = true;
      },
      buildQueryParams() {
        return {
          pageIndex: 1,
          pageSize: 2147483647,
          idList: [this.id],
        };
      },
      updateOrder() {
        if (!this.validData()) {
          return;
        }

        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));
        const params = {
          id: this.id,
          scId: this.formData.sc.id,
          supplierId: this.formData.supplier.id,
          purchaserId: this.formData.purchaserId || '',
          orderDate: this.formData.orderDate || '',
          receiveDate: this.formData.receiveDate,
          paidAmount: 0,
          totalAmount: this.formData.totalAmount,
          purchaseOrderId: this.formData.purchaseOrder.id,
          description: this.formData.description,
          products: validTableData.map((t) => {
            const product = {
              productId: t.productId,
              purchasePrice: t.purchasePrice,
              unit: t.unit,
              unitId: t.unitId,
              receiveNum: t.receiveNum,
              productionDate: t.productionDate,
              description: t.description,
            };

            if (t.isFixed) {
              product.purchaseOrderDetailId = t.purchaseOrderDetailId;
            }

            return product;
          }),
        };

        this.loading = true;
        api
          .update(params)
          .then(() => {
            createSuccess('保存成功！');

            this.$emit('confirm');
            this.goQueryPage();
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  });
</script>
<style scoped>
  .sheet-editor-page {
    height: calc(100vh - 150px);
    min-height: 640px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .sheet-editor-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  .sheet-editor-grid {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .sheet-editor-actions {
    margin-top: auto;
  }

  :deep(.vxe-body--row.sheet-price-warning-row) {
    background-color: #ffd8d6 !important;
  }

  :deep(.sheet-price-warning-row td) {
    border-color: #ff7875;
  }

  :deep(.vxe-body--column.sheet-zero-warning-cell),
  :deep(.sheet-zero-warning-cell .ant-input) {
    color: #f5222d !important;
  }
</style>
