<template>
  <div class="app-card-container sheet-editor-page">
    <div class="sheet-editor-content" v-permission="['purchase:receive:add']" v-loading="loading">
      <a-alert
        description="提示：使用回车键可以快速添加商品；使用Tab键可以快速跳转至下一个输入框。"
        type="info"
        show-icon
      />
      <j-border>
        <j-form bordered>
          <j-form-item label="供应商" required>
            <supplier-selector v-model:value="formData.supplierId" disabled />
          </j-form-item>
          <j-form-item label="采购员">
            <a-select
              v-model:value="formData.purchaserId"
              allow-clear
              show-search
              :disabled="isEmpty(formData.purchaseOrderId)"
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
            <a-select
              v-model:value="formData.purchaseOrderId"
              allow-clear
              show-search
              :filter-option="filterOption"
              :options="purchaseOrderOptions"
              placeholder="请选择采购订单"
              @focus="loadPurchaseOrderOptions()"
              @search="loadPurchaseOrderOptions"
              @change="purchaseOrderChange"
            />
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 数据列表 -->
      <vxe-grid
        class="sheet-editor-grid"
        id="ReceiveSheetAddRequire"
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
              v-if="!row.isFixed"
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
          <a-auto-complete
            v-if="!row.isFixed && (isEmpty(row.productId) || row.editingProduct)"
            :ref="'productInputRef' + rowIndex"
            v-model:value="row.productQuery"
            placeholder="请输入商品编号/名称/SKU编号/简码"
            :options="row.productOptions"
            :dropdown-match-select-width="false"
            :dropdown-style="{ width: '900px' }"
            placement="bottomLeft"
            @search="(e) => queryProduct(e, row)"
            @keydown="(e) => handleProductSelectKeydown(e, row, rowIndex)"
          >
            <!-- 自定义下拉框内容 -->
            <template #dropdownRender>
              <div v-if="!isEmpty(row.products)" @mousedown.prevent @click.stop>
                <vxe-table
                  :data="row.products"
                  max-height="360"
                  class="cursor-pointer"
                  highlight-hover-row
                  show-overflow
                  :row-config="{ isHover: true }"
                  :row-class-name="({ row: product }) => getProductSelectRowClass(row, product)"
                  @cell-click="({ row: product }) => handleSelectProduct(rowIndex, product)"
                >
                  <vxe-column type="seq" title="序号" width="60" />
                  <vxe-column field="productName" title="商品名称" min-width="200">
                    <template #default="{ row: product }">
                      <span>{{ product.productName }}</span>
                      <span v-if="product.hotLevel" class="inline-product-hot-stars">
                        <StarTwoTone
                          v-for="star in product.hotLevel"
                          :key="star"
                          two-tone-color="#faad14"
                        />
                      </span>
                    </template>
                  </vxe-column>
                  <vxe-column field="inquiryProduct" title="是否询价商品" width="120">
                    <template #default="{ row: product }">
                      <span :class="formatInquiryProduct(product.inquiryProduct).className">
                        {{ formatInquiryProduct(product.inquiryProduct).text }}
                      </span>
                    </template>
                  </vxe-column>
                  <vxe-column field="spec" title="规格" width="120" />
                  <vxe-column field="unit" title="单位" width="80" />
                  <vxe-column
                    field="purchasePrice"
                    title="参考采购价（元）"
                    width="140"
                    align="right"
                  />
                  <vxe-column
                    field="latestPurchasePrice"
                    title="最新采购价（元）"
                    width="140"
                    align="right"
                  />
                  <vxe-column field="stockNum" title="库存数量" width="140" align="right" />
                </vxe-table>
                <div
                  class="inline-product-select-add"
                  @mousedown.prevent
                  @click.stop="openProductAddPage"
                >
                  + 新增商品
                </div>
              </div>
            </template>
          </a-auto-complete>
          <span
            v-else
            :style="!row.isFixed ? 'color: #1677ff; cursor: pointer' : ''"
            @click="enableProductEdit(rowIndex)"
            >{{ row.productName }}</span
          >
        </template>

        <!-- 采购价 列自定义内容 -->
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
        @confirm="batchAddProduct"
      />
      <div
        class="sheet-editor-actions"
        style="text-align: center; background-color: #ffffff; padding: 8px 0"
      >
        <a-space>
          <a-button
            v-permission="['purchase:receive:add']"
            type="primary"
            :loading="loading"
            @click="createOrder"
            >保存</a-button
          >
          <a-button
            v-permission="['purchase:receive:approve']"
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
  import BatchAddProduct from '@/views/sc/purchase/batch-add-product.vue';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import Moment from 'moment';
  import {
    DeleteOutlined,
    EditOutlined,
    MinusCircleTwoTone,
    NumberOutlined,
    PlusCircleTwoTone,
    PlusOutlined,
    StarTwoTone,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/purchase/receive';
  import * as purchaseApi from '@/api/sc/purchase/order';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
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
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestSupplierSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import { getSheetAmountCellClass, hasSheetAmountWarning } from '@/utils/sheetAmountWarning';
  import {
    applyManualSheetAmount,
    clearManualSheetAmount,
    getSheetLineAmount,
  } from '@/utils/sheetAmountInput';
  import { createConfirm, createError, createPrompt, createSuccess } from '@/hooks/web/msg';
  import {
    getInlineProductSelectRowClass,
    handleEmptyProductInputEnter,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';
  import { shouldAddProductByEnter } from '@/utils/productAddShortcut';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';
  import * as saleApi from '@/api/sc/sale/order';

  export default defineComponent({
    name: 'AddPurchaseReceiveSheetRequire',
    components: {
      BatchAddProduct,
      SupplierSelector,
      StarTwoTone,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        formatInquiryProduct,
        PlusOutlined,
        PlusCircleTwoTone,
        StarTwoTone,
        DeleteOutlined,
        MinusCircleTwoTone,
        NumberOutlined,
        EditOutlined,
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
        sub,
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
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'brandName', title: '商品品牌', width: 120 },
          { field: 'taxCostPrice', title: '含税成本价（元）', align: 'right', width: 140 },
          { field: 'stockNum', title: '库存数量', align: 'right', width: 140 },
          {
            field: 'purchasePrice',
            title: '采购价（元）',
            align: 'right',
            width: 140,
            slots: { default: 'purchasePrice_default' },
          },
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
        supplierOptions: [],
        supplierOptionMap: {},
        purchaserOptions: [],
        purchaserOptionMap: {},
        purchaseOrderOptions: [],
        purchaseOrderOptionMap: {},
      };
    },
    computed: {
      moment() {
        return Moment;
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
      },
      // 关闭对话框
      closeDialog() {
        this.closeCurrentPage();
      },
      // 初始化表单数据
      async initFormData() {
        this.formData = {
          scId: '',
          supplierId: '',
          purchaseOrderId: '',
          purchaserId: '',
          orderDate: formatDate(Moment()),
          receiveDate: formatDate(Moment()),
          totalNum: 0,
          totalAmount: 0,
          paidAmount: 0,
          description: '',
        };

        this.paidAmountDirty = false;
        this.tableData = [];
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
      // 新增商品
      addProduct() {
        if (isEmpty(this.formData.purchaseOrderId)) {
          createError('请先选择采购订单！');
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
          createError('采购订单中的商品，不允许删除！');
          return;
        }
        this.tableData = this.tableData.filter((item) => item.id !== row.id);
        this.calcSum();
      },
      enableProductEdit(index) {
        if (this.tableData[index].isFixed) {
          return;
        }
        this.tableData[index].editingProduct = true;
        this.tableData[index].productQuery = '';
        this.tableData[index].products = [];
        this.tableData[index].productOptions = [];
        resetInlineProductSelect(this.tableData[index]);
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + index];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
      },
      // 搜索商品
      queryProduct(queryString, row) {
        if (isEmpty(queryString)) {
          row.products = [];
          row.productOptions = [];
          resetInlineProductSelect(row);
          return;
        }

        saleApi.searchSaleProducts(this.formData.scId, queryString).then((res) => {
          setInlineProductSelectProducts(row, res);
          row.productOptions = res.map((item) => {
            return {
              value: item.productId,
              label: item.productCode + ' ' + item.productName,
            };
          });
        });
      },
      // 选择商品（从表格中点击）
      handleSelectProduct(index, product) {
        // 如果行内已有有效的采购价(>0)，则保留原价格，不被最新采购价覆盖
        const purchasePrice = isFloatGtZero(this.tableData[index].purchasePrice)
          ? this.tableData[index].purchasePrice
          : !isEmpty(product.latestPurchasePrice)
          ? product.latestPurchasePrice
          : product.purchasePrice;
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          purchasePrice,
          editingProduct: false,
          productQuery: '',
        });
        resetInlineProductSelect(this.tableData[index]);

        this.purchasePriceInput(this.tableData[index], this.tableData[index].purchasePrice);
      },
      handleProductSelectKeydown(event, row, rowIndex) {
        if (handleEmptyProductInputEnter(event, row, this.addProduct)) {
          return;
        }

        handleInlineProductSelectKeydown(event, row, rowIndex, this.handleSelectProduct, () =>
          this.$nextTick(),
        );
      },
      getProductSelectRowClass(row, product) {
        return getInlineProductSelectRowClass(row, product);
      },
      openProductAddPage() {
        this.openChildPage('/product/info/add');
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
        if (isEmpty(this.formData.purchaseOrderId)) {
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
      async requestSupplierOptions(keyword = '') {
        return requestSupplierSelectOptions(keyword);
      },
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
      },
      async requestPurchaseOrderOptions(keyword = '') {
        const response = await purchaseApi.queryWithReceive({
          pageIndex: 1,
          pageSize: 20,
          code: keyword,
          supplierId: this.formData.supplierId || '',
          createBy: '',
          createStartTime: '',
          createEndTime: '',
        });

        return (response.datas || []).map((item) => ({
          label: item.code,
          value: item.id,
          keywords: buildSelectKeywords(item.code, item.supplierCode, item.supplierName),
        }));
      },
      async loadSupplierOptions(keyword = '') {
        const options = await this.requestSupplierOptions(keyword);
        this.supplierOptionMap = mergeSelectOptionMap(this.supplierOptionMap, options);
        this.supplierOptions = buildVisibleSelectOptions(
          this.formData.supplierId,
          this.supplierOptionMap,
          options,
        );
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
      async loadPurchaseOrderOptions(keyword = '') {
        const options = await this.requestPurchaseOrderOptions(keyword);
        this.purchaseOrderOptionMap = mergeSelectOptionMap(this.purchaseOrderOptionMap, options);
        this.purchaseOrderOptions = buildVisibleSelectOptions(
          this.formData.purchaseOrderId,
          this.purchaseOrderOptionMap,
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
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
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
      setPaid() {
        this.formData.paidAmount = this.formData.totalAmount || 0;
        this.paidAmountDirty = true;
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
        if (isEmpty(this.formData.supplierId)) {
          createError('供应商不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.purchaseOrderId)) {
          createError('采购订单不允许为空！');
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
                    sub(product.orderNum, product.remainNum) +
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
      buildParams() {
        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));
        return {
          scId: this.formData.scId,
          supplierId: this.formData.supplierId,
          purchaserId: this.formData.purchaserId || '',
          orderDate: this.formData.orderDate || '',
          receiveDate: this.formData.receiveDate,
          paidAmount: this.formData.paidAmount,
          totalAmount: this.formData.totalAmount,
          purchaseOrderId: this.formData.purchaseOrderId,
          description: this.formData.description,
          required: true,
          products: validTableData.map((t) => {
            const product = {
              productId: t.productId,
              purchasePrice: t.purchasePrice,
              receiveNum: t.receiveNum,
              productionDate: t.productionDate,
              description: t.description,
            };

            if (t.isFixed) {
              product.purchaseOrderDetailId = t.id;
            }

            return product;
          }),
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
            this.closeDialog();
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

        createConfirm('对采购收货单执行审核通过操作？').then(() => {
          this.loading = true;
          api
            .directApprovePass(params)
            .then(() => {
              createSuccess('审核通过！');

              this.$emit('confirm');
              this.closeDialog();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      // 选择采购订单
      purchaseOrderChange(e) {
        // 只要选择了采购订单，清空所有商品，然后将采购订单中所有的明细列出来
        if (!isEmpty(e)) {
          this.loading = true;
          purchaseApi
            .getWithReceive(e)
            .then((res) => {
              const tableData = this.tableData.filter((item) => !item.isFixed);
              let purchaseDetails = res.details || [];
              purchaseDetails = purchaseDetails.map((item) => {
                item.isFixed = true;

                return Object.assign(this.emptyProduct(), item);
              });

              this.tableData = [...purchaseDetails, ...tableData];
              this.calcSum();

              this.formData.scId = res.scId;

              this.formData.supplierId = res.supplierId;

              if (!isEmpty(res.purchaserId)) {
                this.formData.purchaserId = res.purchaserId;
              }
            })
            .finally(() => {
              this.loading = false;
            });
        }
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
