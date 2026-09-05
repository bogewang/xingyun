<template>
  <div class="app-card-container sheet-editor-page">
    <div class="sheet-editor-content" v-permission="['sale:out:add']" v-loading="loading">
      <a-alert
        description="提示：使用回车键可以快速添加商品；使用Tab键可以快速跳转至下一个输入框。"
        type="info"
        show-icon
      />
      <j-border>
        <j-form bordered>
          <j-form-item label="客户" required>
            <a-select
              v-model:value="formData.customerId"
              allow-clear
              show-search
              disabled
              :filter-option="filterOption"
              :options="customerOptions"
              placeholder="请选择客户"
            />
          </j-form-item>
          <j-form-item label="销售员">
            <a-select
              v-model:value="formData.salerId"
              allow-clear
              show-search
              :disabled="isEmpty(formData.saleOrderId)"
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
            <a-select
              v-model:value="formData.saleOrderId"
              allow-clear
              show-search
              :filter-option="filterOption"
              :options="saleOrderOptions"
              placeholder="请选择销售订单"
              @focus="loadSaleOrderOptions()"
              @search="loadSaleOrderOptions"
              @change="saleOrderChange"
            />
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 数据列表 -->
      <vxe-grid
        class="sheet-editor-grid"
        id="SaleOutSheetAddRequire"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        height="100%"
        :data="tableData"
        :columns="visibleTableColumn"
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

        <template #productCode_default="{ row }">
          <a-tag v-if="row.quoteUnmatched" color="error">未匹配</a-tag>
          <span v-else>{{ row.productCode }}</span>
        </template>

        <template #inquiryProduct_default="{ row }">
          <span :class="formatInquiryProduct(row.inquiryProduct).className">
            {{ formatInquiryProduct(row.inquiryProduct).text }}
          </span>
        </template>

        <!-- 商品名称 列自定义内容 -->
        <template #productName_default="{ row, rowIndex }">
          <InlineProductSelect
            :key="row.id"
            :ref="'productInputRef' + rowIndex"
            :row="row"
            :row-index="rowIndex"
            biz-type="sale"
            mode="require"
            :sc-id="formData.scId"
            :is-fixed="row.isFixed"
            :order-date="formData.orderDate"
            @select="handleSelectProduct"
            @add-product="addProduct"
            @open-add-product-page="openChildPage('/product/info/add')"
          />
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
        <template #outNum_default="{ row, rowIndex }">
          <a-input
            :ref="'outNumInputRef' + rowIndex"
            v-model:value="row.outNum"
            class="number-input"
            @input="(e) => outNumInput(row, e.target.value)"
            @keydown="(e) => handleTableInputKeyDown(e, 'outNumInputRef', rowIndex)"
          />
        </template>

        <!-- 含税金额 列自定义内容 -->
        <template #taxAmount_default="{ row, rowIndex }">
          <a-input
            :ref="'taxAmountInputRef' + rowIndex"
            v-model:value="row.taxAmount"
            class="number-input"
            @input="(e) => taxAmountInput(row, e.target.value)"
            @keydown="(e) => handleTableInputKeyDown(e, 'taxAmountInputRef', rowIndex)"
          />
        </template>

        <template #confirmNum_default="{ row, rowIndex }">
          <a-input
            :ref="'confirmNumInputRef' + rowIndex"
            v-model:value="row.confirmNum"
            class="number-input"
            @input="(e) => confirmNumInput(row, e.target.value)"
            @keydown="(e) => handleTableInputKeyDown(e, 'confirmNumInputRef', rowIndex)"
          />
        </template>
        <template #confirmAmt_default="{ row }">
          <span>{{ row.confirmAmt }}</span>
        </template>

        <!-- 备注 列自定义内容 -->
        <template #description_default="{ row, rowIndex }">
          <a-input
            :ref="'descriptionInputRef' + rowIndex"
            v-model:value="row.description"
            @keydown="(e) => handleTableInputKeyDown(e, 'descriptionInputRef', rowIndex)"
          />
        </template>
        <template #planDate_default="{ row }">
          <a-date-picker v-model:value="row.planDate" value-format="YYYY-MM-DD" />
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
          <j-form-item label="验收数量" :span="8"
            ><a-input v-model:value="formData.confirmNum" class="number-input" readonly
          /></j-form-item>
          <j-form-item label="验收金额" :span="8"
            ><a-input v-model:value="formData.confirmAmt" class="number-input" readonly
          /></j-form-item>
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
        :sc-id="formData.scId"
        :order-date="formData.orderDate"
        @confirm="batchAddProduct"
      />
      <div
        class="sheet-editor-actions"
        style="text-align: center; background-color: #ffffff; padding: 8px 0"
      >
        <a-space>
          <a-button
            v-permission="['sale:out:add']"
            type="primary"
            :loading="loading"
            @click="createOrder"
            >保存</a-button
          >
          <a-button
            v-permission="['sale:out:approve']"
            type="primary"
            :loading="loading"
            @click="directApprovePassOrder"
            >审核通过</a-button
          >
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
  import { defineComponent, h } from 'vue';
  import BatchAddProduct from '@/views/sc/sale/batch-add-product.vue';
  import Moment from 'moment';
  import {
    DeleteOutlined,
    EditOutlined,
    MinusCircleTwoTone,
    NumberOutlined,
    PlusCircleTwoTone,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import { multiplePageMix } from '@/mixins/multiplePageMix';

  import InlineProductSelect from '@/views/sc/shared/inline-product-select.vue';
  import { focusTableInput } from '@/utils/vxeGrid';
  import {
    add,
    formatDate,
    getNumber,
    isEmpty,
    isFloat,
    isFloatGeZero,
    isFloatGtZero,
    isNumberPrecision,
    mul,
    PATTERN_IS_FLOAT,
    sub,
    uuid,
  } from '@/utils/utils';
  import {
    buildSelectKeywords,
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import { getSheetAmountCellClass, hasSheetAmountWarning } from '@/utils/sheetAmountWarning';
  import {
    applyManualSheetAmount,
    clearManualSheetAmount,
    getSheetLineAmount,
  } from '@/utils/sheetAmountInput';
  import { createConfirm, createError, createPrompt, createSuccess } from '@/hooks/web/msg';
  import { resetInlineProductSelect } from '@/utils/inlineProductSelect';
  import { shouldAddProductByEnter } from '@/utils/productAddShortcut';
  import { buildRequiredSaleOutProducts } from './components/saleOutProductParams';
  import { syncConfirmAmount, sumConfirmFields } from './components/saleOutConfirm';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import { getSelectedSaleOutPrice } from './saleOutPrice';
  import { markProductsOutsideQuoteSheet } from '@/utils/quoteProductMismatch';

  export default defineComponent({
    name: 'AddSaleOutSheetRequire',
    components: {
      BatchAddProduct,
      InlineProductSelect,
    },
    mixins: [multiplePageMix],
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
      };
    },
    data() {
      return {
        // 是否显示加载框
        loading: false,
        // 表单数据
        formData: {},
        paidAmountDirty: false,
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
          {
            field: 'productCode',
            title: '商品编号',
            width: 120,
            slots: { default: 'productCode_default' },
          },
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
          { field: 'unit', title: '单位', width: 80 },
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
          {
            field: 'planDate',
            title: '计划日期',
            width: 130,
            slots: { default: 'planDate_default' },
          },
        ],
        tableData: [],
        useUniquePrice: false,
        showPlanDate: true,
        customerOptions: [],
        customerOptionMap: {},
        salerOptions: [],
        salerOptionMap: {},
        saleOrderOptions: [],
        saleOrderOptionMap: {},
      };
    },
    computed: {
      visibleTableColumn() {
        return this.tableColumn.filter(
          (column) => column.field !== 'planDate' || this.showPlanDate,
        );
      },
    },
    watch: {
      'formData.orderDate'() {
        this.validateQuoteProductsByOrderDate();
      },
    },
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
      /** 按订单日期校验当前表格商品是否在生效报价单内。 */
      async validateQuoteProductsByOrderDate() {
        const orderDate = this.formData.orderDate;
        if (!orderDate || !this.tableData.some((item) => !isEmpty(item.productId))) {
          markProductsOutsideQuoteSheet(this.tableData, [], false);
          return;
        }
        try {
          const [enabled, quoteProducts] = await Promise.all([
            api.getPriceUniqueConfig(),
            api.queryQuoteProducts({ orderDate }),
          ]);
          if (orderDate === this.formData.orderDate) {
            markProductsOutsideQuoteSheet(this.tableData, quoteProducts, enabled);
          }
        } catch {
          // 查询报价单失败时不影响当前明细编辑。
        }
      },
      /** 获取表格输入框对应的原生输入元素。 */
      getTableInputElement(refName, rowIndex) {
        const inputRef = this.$refs[refName + rowIndex];
        const target = Array.isArray(inputRef) ? inputRef[0] : inputRef;
        return target?.input || target?.$el?.querySelector?.('input,textarea') || null;
      },
      /** 处理明细输入列上下方向键，跳转至本列相邻行并选中内容。 */
      async handleTableInputKeyDown(event, refName, rowIndex) {
        const rowOffset = event.key === 'ArrowUp' ? -1 : event.key === 'ArrowDown' ? 1 : 0;
        const targetRowIndex = rowIndex + rowOffset;
        if (!rowOffset || targetRowIndex < 0 || targetRowIndex >= this.tableData.length) {
          return;
        }

        event.preventDefault();
        event.stopPropagation();
        if (await focusTableInput(this, refName, targetRowIndex)) {
          await this.$nextTick();
          this.getTableInputElement(refName, targetRowIndex)?.select?.();
        }
      },
      handleKeyDown(event) {
        if (!shouldAddProductByEnter(event)) {
          return;
        }

        this.addProduct();
      },
      // 打开对话框 由父页面触发
      async openDialog() {
        // 初始化表单数据
        await this.initFormData();
      },
      // 关闭对话框
      closeDialog() {
        this.closeCurrentPage();
      },
      // 返回销售出库查询页，避免在未缓存父页时回到默认首页
      goQueryPage() {
        this.closeCurrentPage();
      },
      // 初始化表单数据
      async initFormData() {
        this.formData = {
          scId: '',
          customerId: '',
          saleOrderId: '',
          salerId: '',
          orderDate: formatDate(Moment()),
          totalNum: 0,
          totalAmount: 0,
          confirmNum: 0,
          confirmAmt: 0,
          paidAmount: 0,
          description: '',
        };

        this.paidAmountDirty = false;
        this.tableData = [];
        await Promise.all([this.loadUseUniquePrice(), this.loadShowPlanDate()]);
      },
      /** 加载销售出库唯一售价配置。 */
      async loadUseUniquePrice() {
        try {
          this.useUniquePrice = await api.getPriceUniqueConfig();
        } catch (e) {
          this.useUniquePrice = false;
        }
      },
      /** 加载销售出库计划日期展示配置。 */
      async loadShowPlanDate() {
        try {
          this.showPlanDate = await api.getPlanDateDisplayConfig();
        } catch (e) {
          this.showPlanDate = true;
        }
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
          description: '',
          isFixed: false,
          editingProduct: false,
          productQuery: '',
          products: [],
          productOptions: [],
          activeProductIndex: -1,
        };
      },
      // 新增商品
      addProduct() {
        if (isEmpty(this.formData.saleOrderId)) {
          createError('请先选择销售订单！');
          return;
        }
        this.tableData.push(this.emptyProduct());
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + (this.tableData.length - 1)];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
      },
      insertProduct(index) {
        this.tableData.splice(index + 1, 0, this.emptyProduct());
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + (index + 1)];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
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
        const selectedPrice = getSelectedSaleOutPrice(product, this.useUniquePrice);
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          productRemark: product.remark,
          // 参考价=》商品的售价，价格=》最新价格
          oriPrice: product.salePrice,
          taxPrice: selectedPrice,
          editingProduct: false,
          productQuery: '',
          quoteUnmatched: false,
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
        if (isEmpty(this.formData.saleOrderId)) {
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
      async requestCustomerOptions(keyword = '') {
        return requestCustomerSelectOptions(keyword);
      },
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
      },
      async requestSaleOrderOptions(keyword = '') {
        const response = await saleApi.queryWithOut({
          pageIndex: 1,
          pageSize: 20,
          code: keyword,
          scId: '',
          customerId: this.formData.customerId || '',
          createBy: '',
          createStartTime: '',
          createEndTime: '',
        });

        return (response.datas || []).map((item) => ({
          label: item.code,
          value: item.id,
          keywords: buildSelectKeywords(item.code, item.customerCode, item.customerName),
        }));
      },
      async loadCustomerOptions(keyword = '') {
        const options = await this.requestCustomerOptions(keyword);
        this.customerOptionMap = mergeSelectOptionMap(this.customerOptionMap, options);
        this.customerOptions = buildVisibleSelectOptions(
          this.formData.customerId,
          this.customerOptionMap,
          options,
        );
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
      async loadSaleOrderOptions(keyword = '') {
        const options = await this.requestSaleOrderOptions(keyword);
        this.saleOrderOptionMap = mergeSelectOptionMap(this.saleOrderOptionMap, options);
        this.saleOrderOptions = buildVisibleSelectOptions(
          this.formData.saleOrderId,
          this.saleOrderOptionMap,
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
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
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
        if (isEmpty(this.formData.customerId)) {
          createError('客户不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.saleOrderId)) {
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
      buildParams() {
        return {
          scId: this.formData.scId,
          customerId: this.formData.customerId,
          salerId: this.formData.salerId || '',
          orderDate: this.formData.orderDate || '',
          paidAmount: this.formData.paidAmount,
          saleOrderId: this.formData.saleOrderId,
          description: this.formData.description,
          required: true,
          products: buildRequiredSaleOutProducts(this.tableData),
        };
      },
      // 创建订单
      createOrder() {
        if (!this.validData()) {
          return;
        }

        const params = this.buildParams();

        this.loading = true;
        api
          .create(params)
          .then(() => {
            createSuccess('保存成功！');

            this.$emit('confirm');
            this.goQueryPage();
          })
          .finally(() => {
            this.loading = false;
          });
      },
      // 直接审核通过订单
      directApprovePassOrder() {
        if (!this.validData()) {
          return;
        }

        const params = this.buildParams();

        createConfirm('对销售出库单执行审核通过操作？').then(() => {
          this.loading = true;
          api
            .directApprovePass(params)
            .then(() => {
              createSuccess('审核通过！');

              this.$emit('confirm');
              this.goQueryPage();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      // 选择销售订单
      saleOrderChange(e) {
        // 只要选择了销售订单，清空所有商品，然后将销售订单中所有的明细列出来
        if (!isEmpty(e)) {
          this.loading = true;
          saleApi
            .getWithOut(e)
            .then((res) => {
              const tableData = this.tableData.filter((item) => !item.isFixed);
              let saleDetails = res.details || [];
              saleDetails = saleDetails.map((item) => {
                item.isFixed = true;

                return Object.assign(this.emptyProduct(), item);
              });

              this.tableData = [...saleDetails, ...tableData];
              this.calcSum();

              this.formData.scId = res.scId;

              this.formData.customerId = res.customerId;

              if (!isEmpty(res.salerId)) {
                this.formData.salerId = res.salerId;
              }
            })
            .finally(() => {
              this.loading = false;
            });
        }
      },
      // 检查库存数量
      checkStockNum(row) {
        const checkArr = this.tableData
          .filter((item) => item.productId === row.productId)
          .map((item) => item.outNum);
        if (isEmpty(checkArr)) {
          checkArr.push(0);
        }
        const totalOutNum = checkArr.reduce((total, item) => {
          const outNum = isFloatGtZero(item) ? item : 0;
          return add(total, outNum);
        }, 0);

        return totalOutNum <= row.stockNum;
      },
    },
  });
</script>
<style scoped>
  .sheet-editor-page {
    height: calc(100vh - 112px);
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
