<template>
  <div class="app-card-container sheet-editor-page">
    <div class="sheet-editor-content" v-permission="['sale:out:modify']" v-loading="loading">
      <a-alert
        description="提示：使用回车键可以快速添加商品；使用Tab键可以快速跳转至下一个输入框。"
        type="info"
        show-icon
      />
      <j-border>
        <j-form bordered>
          <j-form-item label="客户" required>
            {{ formData.customer.name }}
          </j-form-item>
          <j-form-item label="销售员">
            <a-select
              v-model:value="formData.salerId"
              allow-clear
              show-search
              :filter-option="filterOption"
              :options="salerOptions"
              placeholder="请选择销售员"
              @focus="loadSalerOptions()"
              @search="loadSalerOptions"
              @change="(value) => handleSelectChange('salerId', value, salerOptionMap)"
            />
          </j-form-item>
          <j-form-item label="订单日期">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder=""
              value-format="YYYY-MM-DD"
            />
          </j-form-item>
          <j-form-item label="销售订单" required>
            {{ formData.saleOrder.code }}
          </j-form-item>
          <j-form-item />
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
            <a-select
              v-model:value="formData.fillAllCost"
              class="cost-status-select"
              style="width: 100%"
              @change="handleFillAllCostChange"
            >
              <a-select-option :value="true">已补全</a-select-option>
              <a-select-option :value="false">未补全</a-select-option>
            </a-select>
          </j-form-item>
          <j-form-item :content-nest="false" label="拒绝理由">
            <a-input
              v-if="SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(formData.status)"
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
      <!-- 数据列表 -->
      <vxe-grid
        class="sheet-editor-grid"
        id="SaleOutSheetModifyRequire"
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
            <a-button :icon="h(NumberOutlined)" @click="batchInputOutNum">批量录入数量</a-button>
            <a-tooltip title="将出库数量设置为剩余出库数量">
              <a-button :icon="h(EditOutlined)" @click="quickSettingOutNum">快捷设置数量</a-button>
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
              v-if="!row.isFixed"
              type="link"
              size="small"
              danger
              :icon="h(MinusCircleTwoTone)"
              @click="removeCurrentProduct(row)"
            />
          </a-space>
        </template>

        <template #inquiryProduct_default="{ row }">
          <span :class="formatInquiryProduct(row.inquiryProduct).className">
            {{ formatInquiryProduct(row.inquiryProduct).text }}
          </span>
        </template>

        <!-- 商品名称 列自定义内容 -->
        <template #productName_default="{ row, rowIndex }">
          <InlineProductSelect
            :ref="'productInputRef' + rowIndex"
            :row="row"
            :row-index="rowIndex"
            biz-type="sale"
            mode="require"
            :sc-id="formData.scId"
            :is-fixed="row.isFixed"
            @select="handleSelectProduct"
            @add-product="addProduct"
            @open-add-product-page="openChildPage('/product/info/add')"
          />
        </template>

        <!-- 单位列自定义内容 -->
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

        <!-- 库存数量 列自定义内容 -->
        <template #stockNum_default="{ row }">
          <span v-if="checkStockNum(row)">{{ row.stockNum }}</span>
          <span v-else style="color: #f5222d">{{ row.stockNum }}</span>
        </template>

        <!-- 剩余出库数量 列自定义内容 -->
        <template #remainNum_default="{ row }">
          <span v-if="isEmpty(row.remainNum)">-</span>
          <span v-else-if="isFloat(row.outNum)">{{
            Math.max(0, sub(row.remainNum, row.outNum))
          }}</span>
          <span v-else>{{ row.remainNum }}</span>
        </template>

        <!-- 出库数量 列自定义内容 -->
        <template #outNum_default="{ row }">
          <a-input
            v-model:value="row.outNum"
            class="number-input"
            @input="(e) => outNumInput(row, e.target.value)"
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
        <template #confirmNum_default="{ row }">
          <a-input
            v-model:value="row.confirmNum"
            class="number-input"
            @input="(e) => confirmNumInput(row, e.target.value)"
          />
        </template>
        <template #confirmAmt_default="{ row }">
          <span>{{ row.confirmAmt }}</span>
        </template>

        <template #costPrice_default="{ row }">
          <a-input
            v-if="canEditCostPrice(row)"
            v-model:value="row.costPrice"
            class="number-input"
            @input="(e) => costPriceInput(row, e.target.value)"
          />
          <span v-else></span>
        </template>

        <template #costStatus_default="{ row }">
          <span :style="{ color: hasCostPrice(row) ? '#52c41a' : '#f5222d' }">
            {{ hasCostPrice(row) ? '已补全' : '未补全' }}
          </span>
        </template>

        <!-- 备注 列自定义内容 -->
        <template #description_default="{ row }">
          <a-input v-model:value="row.description" />
        </template>
      </vxe-grid>

      <j-border title="合计">
        <j-form bordered label-width="140px">
          <j-form-item label="出库数量" :span="8">
            <a-input v-model:value="formData.totalNum" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="含税总金额" :span="8">
            <a-input v-model:value="formData.totalAmount" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="验收数量" :span="8">
            <a-input v-model:value="formData.confirmNum" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="验收金额" :span="8">
            <a-input v-model:value="formData.confirmAmt" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="付款金额" :span="8">
            <a-space>
              <a-input
                v-model:value="formData.paidAmount"
                class="number-input"
                @input="(e) => paidAmountInput(e.target.value)"
              />
              <a-button @click="setPaid">已付款</a-button>
            </a-space>
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
      <order-print-dialog />
      <div
        class="sheet-editor-actions"
        style="text-align: center; background-color: #ffffff; padding: 8px 0"
      >
        <a-space>
          <a-button type="primary" :loading="loading" @click="print">打印</a-button>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
          <a-button :loading="loading" @click="openTimeline">操作记录</a-button>
          <a-button
            v-permission="['sale:out:modify']"
            type="primary"
            :loading="loading"
            @click="updateOrder"
            >保存</a-button
          >
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
  import { h, defineComponent } from 'vue';
  import BatchAddProduct from '@/views/sc/sale/batch-add-product.vue';
  import {
    PlusOutlined,
    DeleteOutlined,
    PlusCircleTwoTone,
    MinusCircleTwoTone,
    NumberOutlined,
    EditOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import { multiplePageMix } from '@/mixins/multiplePageMix';

  import InlineProductSelect from '@/views/sc/shared/inline-product-select.vue';
  import { printMix } from '@/mixins/print.ts';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import PrintDialog from '/@/components/PrintDialog';
  import {
    isEmpty,
    isFloatGeZero,
    sub,
    getNumber,
    mul,
    uuid,
    add,
    isFloat,
    isFloatGtZero,
    isNumberPrecision,
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
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import {
    normalizeConfirmNum,
    syncConfirmAmount,
    sumConfirmFields,
  } from './components/saleOutConfirm';
  import {
    calculateUnitPrice,
    calculateUnitStockNum,
    getUnitConversionRate,
  } from '@/utils/productUnitConversion';

  export default defineComponent({
    name: 'ModifySaleOutSheetRequire',
    components: {
      BatchAddProduct,
      OrderTimeLine,
      OrderPrintDialog: PrintDialog,
      InlineProductSelect,
    },
    mixins: [multiplePageMix, printMix],
    setup() {
      return {
        h,
        PlusOutlined,
        PlusCircleTwoTone,
        DeleteOutlined,
        MinusCircleTwoTone,
        NumberOutlined,
        EditOutlined,
        isEmpty,
        isFloatGeZero,
        sub,
        getNumber,
        mul,
        formatInquiryProduct,
        hasCostPrice: (row) =>
          row && row.costPrice !== null && row.costPrice !== undefined && row.costPrice !== '',
        canEditCostPrice: (row) =>
          row &&
          (row.manualInputCost === true ||
            row.costPrice === null ||
            row.costPrice === undefined ||
            row.costPrice === ''),
        SALE_OUT_SHEET_STATUS,
      };
    },
    data() {
      return {
        id: this.$route.params.id,
        // 是否显示加载框
        loading: false,
        // 表单数据
        formData: {},
        originalFillAllCost: false,
        paidAmountDirty: false,
        timelineVisible: false,
        salerOptions: [],
        salerOptionMap: {},
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
          { field: 'mainProductName', title: '所属组合商品', width: 120 },
          { field: 'oriPrice', title: '参考销售价（元）', align: 'right', width: 140 },
          {
            field: 'stockNum',
            title: '库存数量',
            align: 'right',
            width: 140,
            slots: { default: 'stockNum_default' },
          },
          { field: 'taxPrice', title: '价格（元）', align: 'right', width: 140 },
          {
            field: 'costPrice',
            title: '成本单价',
            align: 'right',
            width: 140,
            slots: { default: 'costPrice_default' },
          },
          {
            field: 'orderNum',
            title: '销售数量',
            align: 'right',
            width: 140,
            formatter: ({ cellValue }) => {
              return isEmpty(cellValue) ? '-' : cellValue;
            },
          },
          {
            field: 'remainNum',
            title: '剩余出库数量',
            align: 'right',
            width: 140,
            slots: { default: 'remainNum_default' },
          },
          {
            field: 'outNum',
            title: '出库数量',
            align: 'right',
            width: 140,
            slots: { default: 'outNum_default' },
          },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 140,
            slots: { default: 'taxAmount_default' },
          },
          {
            field: 'confirmNum',
            title: '验收数量',
            align: 'right',
            width: 120,
            slots: { default: 'confirmNum_default' },
          },
          {
            field: 'confirmAmt',
            title: '验收金额',
            align: 'right',
            width: 120,
            slots: { default: 'confirmAmt_default' },
          },
          {
            field: 'costStatus',
            title: '成本状态',
            width: 100,
            slots: { default: 'costStatus_default' },
          },
          { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
          {
            field: 'productRemark',
            title: '商品备注',
            width: 200,
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
    computed: {},
    created() {
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
      // 返回销售出库查询页，避免回到缓存中的其他页面
      goQueryPage() {
        this.closeCurrentPage();
      },
      // 初始化表单数据
      initFormData() {
        this.formData = {
          sc: {},
          customer: {},
          saleOrder: {},
          salerId: '',
          orderDate: '',
          totalNum: 0,
          totalAmount: 0,
          confirmNum: 0,
          confirmAmt: 0,
          paidAmount: 0,
          fillAllCost: false,
          description: '',
        };

        this.paidAmountDirty = false;
        this.originalFillAllCost = false;
        this.tableData = [];
      },
      // 加载数据
      loadData() {
        this.loading = true;
        api
          .get(this.id)
          .then((res) => {
            if (
              !SALE_OUT_SHEET_STATUS.CREATED.equalsCode(res.status) &&
              !SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(res.status)
            ) {
              createError('销售出库单已审核通过，无法修改！');
              this.closeDialog();
              return;
            }
            this.formData = Object.assign(this.formData, {
              sc: {
                id: res.scId,
                name: res.scName,
              },
              customer: {
                id: res.customerId,
                name: res.customerName,
              },
              salerId: res.salerId || '',
              orderDate: res.orderDate || '',
              saleOrder: {
                id: res.saleOrderId,
                code: res.saleOrderCode,
              },
              description: res.description,
              paidAmount: res.paidAmount,
              status: res.status,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: res.totalAmount || 0,
              fillAllCost: !!res.fillAllCost,
            });
            this.originalFillAllCost = !!res.fillAllCost;

            const tableData = res.details || [];
            tableData.forEach((item) => {
              item.isFixed = !isEmpty(item.saleOrderDetailId);

              if (item.isFixed) {
                // 接口返回的剩余出库数量是最新的数据，应加上当前出库数量
                item.remainNum = add(item.outNum, item.remainNum);
              }

              return item;
            });
            this.tableData = tableData.map((item) => Object.assign(this.emptyProduct(), item));
            this.calcSum();

            this.paidAmountDirty = true;
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
          oriPrice: '',
          taxPrice: 0,
          stockNum: '',
          orderNum: '',
          remainNum: '',
          outNum: '',
          taxRate: '',
          taxAmount: '',
          confirmNum: 0,
          confirmAmt: 0,
          costPrice: '',
          manualInputCost: false,
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
        if (isEmpty(this.formData.saleOrder)) {
          createError('请先选择销售订单！');
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
          createError('销售订单中的商品，不允许删除！');
          return;
        }
        this.tableData = this.tableData.filter((item) => item.id !== row.id);
        this.calcSum();
      },
      // 选择商品（从表格中点击）
      handleSelectProduct(index, product) {
        const baseUnit = product.units?.find((item) => item.baseUnit);
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          productRemark: product.remark,
          oriPrice: product.salePrice,
          // 如果行内已有有效的价格(>0)，则保留原价格，不被最新售价覆盖
          taxPrice: isFloatGtZero(this.tableData[index].taxPrice)
            ? this.tableData[index].taxPrice
            : product.latestSalePrice,
          baseSalePrice: product.latestSalePrice,
          baseStockNum: product.stockNum,
          unitId: baseUnit?.id || '',
          unit: baseUnit?.unitName || product.unit || '',
          editingProduct: false,
          productQuery: '',
        });
        resetInlineProductSelect(this.tableData[index]);

        this.taxPriceInput(this.tableData[index], this.tableData[index].taxPrice);
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
            createError('第' + (i + 1) + '行商品是销售订单中的商品，不允许删除！');
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
        if (isEmpty(this.formData.saleOrder)) {
          createError('请先选择销售订单！');
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
      async loadSalerOptions(keyword = '') {
        const options = await this.requestUserOptions(keyword);
        this.salerOptionMap = mergeSelectOptionMap(this.salerOptionMap, options);
        this.salerOptions = buildVisibleSelectOptions(
          this.formData.salerId,
          this.salerOptionMap,
          options,
        );
      },
      taxPriceInput(_row, _value) {
        clearManualSheetAmount(_row, 'outNum', 'taxPrice');
        syncConfirmAmount(_row);
        this.calcSum();
      },
      /** 验收数量输入后同步验收金额 */
      confirmNumInput(row, value) {
        row.confirmNum = value;
        syncConfirmAmount(row);
        this.calcSum();
      },
      // 手工录入金额，并根据出库数量反算销售单价
      taxAmountInput(row, value) {
        applyManualSheetAmount(row, value, 'outNum', 'taxPrice');
        this.calcSum();
      },
      hasWarningAmount(row) {
        return hasSheetAmountWarning(row, 'taxPrice', 'outNum');
      },
      getTableRowClassName({ row }) {
        return this.hasWarningAmount(row) ? 'sheet-price-warning-row' : '';
      },
      getCellClassName({ row, column }) {
        return getSheetAmountCellClass(row, column.field, 'taxPrice', 'outNum');
      },
      selectUnit(row, unitId) {
        const unit = (row.units || []).find((item) => item.id === unitId);
        if (!unit) return;
        const rate = Number(unit.conversionRate) || 1;
        const oldRate = Number(row.conversionRate) || 1;
        const stockNum = calculateUnitStockNum(row.stockNum, row.baseStockNum, oldRate, rate);
        const salePrice = calculateUnitPrice(row.taxPrice, row.baseSalePrice, oldRate, rate);
        row.baseStockNum = stockNum.baseStockNum;
        row.baseSalePrice = salePrice.basePrice;
        row.conversionRate = rate;
        row.unit = unit.unitName;
        row.taxPrice = salePrice.unitPrice;
        row.oriPrice = row.taxPrice;
        row.stockNum = stockNum.stockNum;
        clearManualSheetAmount(row, 'outNum', 'taxPrice');
        this.calcSum();
      },
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
      },
      costPriceInput(row, value) {
        row.costPrice = value;
        row.manualInputCost = !isEmpty(row.costPrice);
        this.calcSum();
      },
      outNumInput(row, value) {
        if (value === undefined) {
          clearManualSheetAmount(row, 'outNum', 'taxPrice');
          this.calcSum();
          return;
        }
        row.outNum = value;
        clearManualSheetAmount(row, 'outNum', 'taxPrice');
        this.calcSum();
      },
      handleFillAllCostChange(value) {
        this.formData.fillAllCost = value;
      },
      // 计算汇总数据
      calcSum() {
        this.tableData.forEach((row) => syncConfirmAmount(row));
        let totalNum = 0;
        let totalAmount = 0;
        this.tableData
          .filter((t) => {
            return t.manualTaxAmount || (isFloatGeZero(t.taxPrice) && isFloat(t.outNum));
          })
          .forEach((t) => {
            const num = parseFloat(t.outNum);
            totalNum = add(totalNum, num);
            totalAmount = add(totalAmount, getSheetLineAmount(t, 'outNum', 'taxPrice'));
          });

        this.formData.totalNum = totalNum;
        this.formData.totalAmount = totalAmount;
        const confirm = sumConfirmFields(this.tableData);
        this.formData.confirmNum = confirm.confirmNum;
        this.formData.confirmAmt = confirm.confirmAmt;
      },
      setPaid() {
        this.formData.paidAmount = this.formData.totalAmount || 0;
        this.paidAmountDirty = true;
      },
      // 批量录入数量
      batchInputOutNum() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        createPrompt('请输入出库数量', {
          inputPattern: PATTERN_IS_FLOAT,
          inputErrorMessage: '出库数量必须是数字',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.outNum = value;

            this.outNumInput(t, value);
          });
        });
      },
      // 快捷设置数量
      quickSettingOutNum() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          const record = records[i];
          if (record.isFixed) {
            this.outNumInput(record, record.remainNum);
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
        if (isEmpty(this.formData.customer.id)) {
          createError('客户不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.saleOrder.id)) {
          createError('销售订单不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.paidAmount)) {
          createError('付款金额不允许为空！');
          return false;
        }

        if (!isFloat(this.formData.paidAmount)) {
          createError('付款金额必须是数字！');
          return false;
        }

        // 付款金额允许负数，不再校验不小于0

        if (!isNumberPrecision(this.formData.paidAmount, 6)) {
          createError('付款金额最多允许6位小数！');
          return false;
        }

        if (
          Math.abs(parseFloat(this.formData.paidAmount)) >
          Math.abs(parseFloat(this.formData.totalAmount || 0))
        ) {
          createError('付款金额绝对值不允许大于含税总金额绝对值！');
          return false;
        }

        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));

        if (isEmpty(validTableData)) {
          createError('请录入商品！');
          return false;
        }

        for (let i = 0; i < validTableData.length; i++) {
          const product = validTableData[i];

          if (!isEmpty(product.taxPrice)) {
            if (!isFloat(product.taxPrice)) {
              createError('第' + (i + 1) + '行商品价格必须是数字！');
              return false;
            }

            if (!isFloatGeZero(product.taxPrice)) {
              createError('第' + (i + 1) + '行商品价格不允许小于0！');
              return false;
            }

            if (!isNumberPrecision(product.taxPrice, 6)) {
              createError('第' + (i + 1) + '行商品价格最多允许6位小数！');
              return false;
            }
          }

          if (!isEmpty(product.outNum)) {
            if (!isEmpty(product.costPrice)) {
              if (!isFloat(product.costPrice)) {
                createError('第' + (i + 1) + '行商品成本单价必须是数字！');
                return false;
              }

              if (!isFloatGeZero(product.costPrice)) {
                createError('第' + (i + 1) + '行商品成本单价不允许小于0！');
                return false;
              }

              if (!isNumberPrecision(product.costPrice, 6)) {
                createError('第' + (i + 1) + '行商品成本单价最多允许6位小数！');
                return false;
              }
            }

            if (!isFloat(product.outNum)) {
              createError('第' + (i + 1) + '行商品出库数量必须是数字！');
              return false;
            }

            if (!isNumberPrecision(product.outNum, 8)) {
              createError('第' + (i + 1) + '行商品出库数量最多允许8位小数！');
              return false;
            }

            if (product.isFixed) {
              if (product.outNum > product.remainNum) {
                createError(
                  '第' +
                    (i + 1) +
                    '行商品累计出库数量为' +
                    sub(product.orderNum, product.remainNum) +
                    '，剩余出库数量为' +
                    product.remainNum +
                    '，本次出库数量不允许大于' +
                    product.remainNum +
                    '！',
                );
                return false;
              }
            }
          }
        }

        if (
          validTableData.filter((item) => isFloat(item.outNum) && Number(item.outNum) !== 0)
            .length === 0
        ) {
          createError('销售订单中的商品必须全部或部分出库！');
          return false;
        }

        return true;
      },
      // 打印
      async print() {
        this.loading = true;
        try {
          const res = await api.print(this.id);
          await this.vgPrintPreview(PRINT_TYPE.SALE_OUT.code, res);
        } finally {
          this.loading = false;
        }
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
          customerId: this.formData.customer.id,
          salerId: this.formData.salerId || '',
          orderDate: this.formData.orderDate || '',
          paidAmount: this.formData.paidAmount,
          saleOrderId: this.formData.saleOrder.id,
          fillAllCost: this.formData.fillAllCost,
          fillAllCostModified: this.formData.fillAllCost !== this.originalFillAllCost,
          description: this.formData.description,
          products: validTableData.map((t) => {
            const product = {
              productId: t.productId,
              unit: t.unit,
              unitId: t.unitId,
              orderNum: t.outNum,
              confirmNum: normalizeConfirmNum(t.confirmNum),
              description: t.description,
              oriPrice: t.oriPrice,
              taxPrice: t.taxPrice,
              costPrice: this.canEditCostPrice(t) && !isEmpty(t.costPrice) ? t.costPrice : null,
            };

            if (t.isFixed) {
              product.saleOrderDetailId = t.saleOrderDetailId;
            }

            return product;
          }),
        };

        const doUpdate = () => {
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
        };

        if (params.fillAllCostModified) {
          createConfirm(
            '你正在手动修改单据成本状态，保存后将覆盖系统自动计算的“已补全/未补全”结果，是否继续？',
          ).then(() => doUpdate());
          return;
        }

        doUpdate();
      },
      // 检查库存数量
      checkStockNum(row) {
        const checkArr = this.tableData
          .filter((item) => item.productId === row.productId)
          .map((item) => mul(item.outNum || 0, getUnitConversionRate(item)));
        if (isEmpty(checkArr)) {
          checkArr.push(0);
        }
        const totalOutNum = checkArr.reduce((total, item) => {
          const outNum = isFloatGtZero(item) ? item : 0;
          return add(total, outNum);
        }, 0);

        return totalOutNum <= (row.baseStockNum ?? mul(row.stockNum || 0, row.conversionRate || 1));
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

  .cost-status-select :deep(.ant-select-arrow) {
    top: 50%;
    transform: translateY(-50%);
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
