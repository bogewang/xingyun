<template>
  <div
    ref="importerContainer"
    class="app-card-container sheet-editor-page excel-importer-local-container"
  >
    <div class="sheet-editor-content" v-permission="['sale:out:add']" v-loading="loading">
      <j-border>
        <j-form bordered>
          <j-form-item label="订单日期">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder=""
              value-format="YYYY-MM-DD"
            />
          </j-form-item>
          <j-form-item label="客户" required>
            <a-select
              v-model:value="formData.customerId"
              allow-clear
              show-search
              :filter-option="filterOption"
              :options="customerOptions"
              placeholder="请选择客户"
              @focus="loadCustomerOptions()"
              @search="loadCustomerOptions"
              @change="(value) => handleSelectChange('customerId', value, customerOptionMap)"
            />
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 数据列表 -->
      <div class="sheet-editor-grid-wrapper">
        <vxe-grid
          class="sheet-editor-grid"
          id="SaleOutSheetAddUnRequire"
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
              <a-button type="primary" :icon="h(PlusOutlined)" @click="clickAddProduct"
                >新增</a-button
              >
              <a-button danger :icon="h(DeleteOutlined)" @click="delProduct">删除</a-button>
              <a-button :icon="h(PlusOutlined)" @click="openBatchAddProductDialog"
                >批量添加商品
              </a-button>
              <a-button :icon="h(NumberOutlined)" @click="batchInputOutNum">批量录入数量</a-button>
              <a-button :icon="h(EditOutlined)" @click="batchInputTaxPrice">批量调整价格</a-button>
              <a-button :icon="h(CloudUploadOutlined)" @click="$refs.importer.openDialog()"
                >导入Excel
              </a-button>
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
                type="link"
                size="small"
                danger
                :icon="h(MinusCircleTwoTone)"
                @click="removeCurrentProduct(row)"
              />
            </a-space>
          </template>

          <!-- 商品编号 列自定义内容 -->
          <template #productCode_default="{ row }">
            <a-tag v-if="isImportUnmatchedProduct(row)" color="error">未匹配</a-tag>
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
              :ref="'productInputRef' + rowIndex"
              :row="row"
              :row-index="rowIndex"
              biz-type="sale"
              mode="unrequire"
              :sc-id="formData.scId"
              @select="handleSelectProduct"
              @add-product="addProduct"
              @open-add-product-page="openChildPage('/product/info/add')"
            />
          </template>

          <!-- 价格 列自定义内容 -->
          <template #unit_default="{ row }">
            <a-select
              v-model:value="row.unitId"
              size="small"
              style="width: 100%"
              @change="(value) => selectUnit(row, value)"
            >
              <a-select-option v-for="item in row.units || []" :key="item.id" :value="item.id">
                {{ item.unitName }}
              </a-select-option>
            </a-select>
          </template>

          <!-- 价格 列自定义内容 -->
          <template #taxPrice_default="{ row, rowIndex }">
            <a-input
              :ref="'taxPriceInputRef' + rowIndex"
              v-model:value="row.taxPrice"
              class="number-input"
              @input="(e) => taxPriceInput(row, e.target.value)"
              @keydown="(e) => handleTableInputKeyDown(e, 'taxPriceInputRef', rowIndex)"
            />
          </template>

          <!-- 数量 列自定义内容 -->
          <template #outNum_default="{ row, rowIndex }">
            <a-input
              :ref="'outNumInputRef' + rowIndex"
              v-model:value="row.outNum"
              class="number-input"
              @input="(e) => outNumInput(row, e.target.value)"
              @keydown="(e) => handleTableInputKeyDown(e, 'outNumInputRef', rowIndex, true)"
            />
          </template>

          <!-- 库存数量 列自定义内容 -->
          <template #stockNum_default="{ row }">
            <span v-if="checkStockNum(row)">{{ row.stockNum }}</span>
            <span v-else style="color: #f5222d">{{ row.stockNum }}</span>
          </template>

          <!-- 金额 列自定义内容 -->
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
          <template #confirmAmt_default="{ row }"
            ><span>{{ row.confirmAmt }}</span></template
          >

          <!-- 备注 列自定义内容 -->
          <template #description_default="{ row, rowIndex }">
            <a-input
              :ref="'descriptionInputRef' + rowIndex"
              v-model:value="row.description"
              @keydown="(e) => handleTableInputKeyDown(e, 'descriptionInputRef', rowIndex)"
            />
          </template>
        </vxe-grid>
      </div>

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
              <a-button type="primary" @click="setPaid">已付款</a-button>
            </a-space>
          </j-form-item>
          <j-form-item label="备注" :span="4" :content-nest="false">
            <a-input v-model:value.trim="formData.description" maxlength="200" />
          </j-form-item>
        </j-form>
      </j-border>

      <batch-add-product
        ref="batchAddProductDialog"
        :show-inquiry-product="true"
        :sc-id="formData.scId"
        @confirm="batchAddProduct"
      />
      <sale-out-sheet-importer
        ref="importer"
        :get-container="getImporterContainer"
        local-container
        hide-on-deactivated
        @confirm="handleImportConfirm"
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
            >保存
          </a-button>
          <a-button
            v-permission="['sale:out:approve']"
            type="primary"
            :loading="loading"
            @click="directApprovePassOrder"
            >审核通过
          </a-button>
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
    CloudUploadOutlined,
    DeleteOutlined,
    EditOutlined,
    MinusCircleTwoTone,
    NumberOutlined,
    PlusCircleTwoTone,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import SaleOutSheetImporter from '@/components/Importor/SaleOutSheetImporter.vue';
  import * as api from '@/api/sc/sale/out';
  import * as sysParameterApi from '@/api/system/parameter';
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
    isNumberPrecision,
    mul,
    PATTERN_IS_FLOAT,
    PATTERN_IS_PRICE,
    uuid,
  } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestCustomerSelectOptions } from '@/utils/labelSelect';
  import { getSheetAmountCellClass, hasSheetAmountWarning } from '@/utils/sheetAmountWarning';
  import {
    applyManualSheetAmount,
    clearManualSheetAmount,
    getSheetLineAmount,
  } from '@/utils/sheetAmountInput';
  import {
    createConfirm,
    createError,
    createPrompt,
    createSuccess,
    createSuccessAutoClose,
  } from '@/hooks/web/msg';
  import { resetInlineProductSelect } from '@/utils/inlineProductSelect';
  import { shouldAddProductByEnter, stopGridDeleteFromInput } from '@/utils/productAddShortcut';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { buildUnrequiredSaleOutProducts } from './components/saleOutProductParams';
  import { syncConfirmAmount, sumConfirmFields } from './components/saleOutConfirm';
  import { isSaleOutStockEnough } from './components/saleOutStock';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import { calculateUnitPrice, calculateUnitStockNum } from '@/utils/productUnitConversion';

  export default defineComponent({
    name: 'AddSaleOutSheetUnRequire',
    components: {
      BatchAddProduct,
      SaleOutSheetImporter,
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
        CloudUploadOutlined,
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
        formatInquiryProduct,
        stopGridDeleteFromInput,
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
            width: 80,
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
            width: 150,
            slots: { default: 'productName_default' },
          },
          {
            field: 'inquiryProduct',
            title: '是否询价商品',
            width: 120,
            slots: { default: 'inquiryProduct_default' },
          },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 80, slots: { default: 'unit_default' } },
          {
            field: 'stockNum',
            title: '库存数量',
            align: 'right',
            width: 80,
            slots: { default: 'stockNum_default' },
          },
          {
            field: 'outNum',
            title: '数量',
            align: 'right',
            width: 80,
            slots: { default: 'outNum_default' },
          },
          {
            field: 'taxPrice',
            title: '价格（元）',
            align: 'right',
            width: 80,
            slots: { default: 'taxPrice_default' },
          },
          {
            field: 'taxAmount',
            title: '金额',
            align: 'right',
            width: 80,
            slots: { default: 'taxAmount_default' },
          },
          {
            field: 'description',
            title: '备注',
            width: 200,
            slots: { default: 'description_default' },
          },
          {
            field: 'confirmNum',
            title: '验收数量',
            align: 'right',
            width: 80,
            slots: { default: 'confirmNum_default' },
          },
          {
            field: 'confirmAmt',
            title: '验收金额',
            align: 'right',
            width: 80,
            slots: { default: 'confirmAmt_default' },
          },
          {
            field: 'productRemark',
            title: '商品备注',
            width: 200,
          },
        ],
        tableData: [],
        customerOptions: [],
        customerOptionMap: {},
        saleOutPriceUseUniquePrice: false,
      };
    },
    computed: {},
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
      getImporterContainer() {
        return this.$refs.importerContainer;
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
        await this.loadSaleOutPriceUseUniquePrice();
      },
      async loadSaleOutPriceUseUniquePrice() {
        const tenantId = (await useUserStoreWithOut().getTenantRequire())?.tenantId;
        if (!tenantId) {
          this.saleOutPriceUseUniquePrice = false;
          return;
        }

        const res = await sysParameterApi.query({
          pageIndex: 1,
          pageSize: 1,
          tenantId,
          pmKey: 'sale_out_price_use_unique_price',
          createTimeStart: '',
          createTimeEnd: '',
        });

        const parameter = res.datas?.[0];
        this.saleOutPriceUseUniquePrice = parameter?.pmValue === 'true';
      },
      getSelectedProductPrice(product) {
        return this.saleOutPriceUseUniquePrice ? product.salePrice : product.latestSalePrice;
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
          taxPrice: '',
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
          importUnmatched: false,
        };
      },
      // 新增商品
      addProduct() {
        this.tableData.push(this.emptyProduct());
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + (this.tableData.length - 1)];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
      },
      // 固定新增50行商品
      clickAddProduct() {
        const startIndex = this.tableData.length;
        for (let i = 0; i < 50; i++) {
          this.tableData.push(this.emptyProduct());
        }
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + startIndex];
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
        this.tableData = this.tableData.filter((item) => item.id !== row.id);
        this.calcSum();
      },
      focusRowInput(refName, index) {
        return focusTableInput(this, refName, index);
      },
      /** 获取表格输入框对应的原生输入元素。 */
      getTableInputElement(refName, rowIndex) {
        const inputRef = this.$refs[refName + rowIndex];
        const target = Array.isArray(inputRef) ? inputRef[0] : inputRef;
        return (
          target?.input ||
          target?.resizableTextArea?.textArea ||
          target?.$el?.querySelector?.('input,textarea') ||
          null
        );
      },
      /** 聚焦并选中表格输入框内容，便于直接覆盖原值。 */
      async focusAndSelectTableInput(refName, rowIndex) {
        const focused = await focusTableInput(this, refName, rowIndex);
        if (!focused) {
          return;
        }

        await this.$nextTick();
        this.getTableInputElement(refName, rowIndex)?.select?.();
      },
      /** 处理明细输入列方向下键，跳转到本列下一行输入框并选中内容。 */
      async handleTableInputKeyDown(event, refName, rowIndex, stopDelete = false) {
        if (stopDelete) {
          stopGridDeleteFromInput(event);
        }
        if (event.key !== 'ArrowDown') {
          return;
        }

        event.preventDefault();
        await this.focusAndSelectTableInput(refName, rowIndex + 1);
      },
      // 选择商品（从表格中点击）
      handleSelectProduct(index, product) {
        const baseUnit = product.units?.find((item) => item.baseUnit);
        const selectedPrice = this.getSelectedProductPrice(product);
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          productRemark: product.remark,
          oriPrice: product.salePrice,
          taxPrice: selectedPrice,
          baseSalePrice: selectedPrice,
          baseStockNum: product.stockNum,
          unitId: baseUnit?.id || '',
          unit: baseUnit?.unitName || product.unit || '',
          editingProduct: false,
          productQuery: '',
          importUnmatched: false,
        });
        resetInlineProductSelect(this.tableData[index]);

        this.taxPriceInput(this.tableData[index], this.tableData[index].taxPrice);
        this.focusRowInput('outNumInputRef', index);
      },
      selectUnit(row, unitId) {
        const unit = (row.units || []).find((item) => item.id === unitId);
        if (unit) {
          const stockNum = calculateUnitStockNum(
            row.stockNum,
            row.baseStockNum,
            row.conversionRate,
            unit.conversionRate,
          );
          const salePrice = calculateUnitPrice(
            row.taxPrice,
            row.baseSalePrice,
            row.conversionRate,
            unit.conversionRate,
          );
          row.unitId = unit.id;
          row.unit = unit.unitName;
          row.baseStockNum = stockNum.baseStockNum;
          row.conversionRate = unit.conversionRate;
          row.baseSalePrice = salePrice.basePrice;
          row.taxPrice = salePrice.unitPrice;
          row.oriPrice = row.taxPrice;
          row.stockNum = stockNum.stockNum;
          clearManualSheetAmount(row, 'outNum', 'taxPrice');
          this.calcSum();
        }
      },
      isImportUnmatchedProduct(row) {
        return row.importUnmatched && isEmpty(row.productId);
      },
      hasWarningAmount(row) {
        return hasSheetAmountWarning(row, 'taxPrice', 'outNum');
      },
      getCellClassName({ row, column }) {
        return getSheetAmountCellClass(row, column.field, 'taxPrice', 'outNum');
      },
      getTableRowClassName({ row }) {
        const classNames = [];
        if (this.isImportUnmatchedProduct(row)) {
          classNames.push('sale-out-import-unmatched-row');
        }
        if (!isEmpty(row.productCode) && this.hasWarningAmount(row)) {
          classNames.push('sheet-price-warning-row');
        }
        return classNames.join(' ');
      },
      // 删除商品
      delProduct() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要删除的商品数据！');
          return;
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
      async loadCustomerOptions(keyword = '') {
        const options = await this.requestCustomerOptions(keyword);
        this.customerOptionMap = mergeSelectOptionMap(this.customerOptionMap, options);
        this.customerOptions = buildVisibleSelectOptions(
          this.formData.customerId,
          this.customerOptionMap,
          options,
        );
      },
      taxPriceInput(row, value) {
        row.taxPrice = value;
        clearManualSheetAmount(row, 'outNum', 'taxPrice');
        syncConfirmAmount(row);
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
        if (!Number.isFinite(Number(value))) {
          return;
        }
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
      // 批量录入价格
      batchInputTaxPrice() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        createPrompt('请输入价格（元）', {
          inputPattern: PATTERN_IS_PRICE,
          inputErrorMessage: '价格（元）必须是数字并且不小于0，最多允许6位小数',
          title: '批量调整价格',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.taxPrice = value;

            this.taxPriceInput(t, value);
          });
        });
      },
      // 批量新增商品
      batchAddProduct(productList) {
        productList.forEach((item) => {
          this.tableData.push(this.emptyProduct());
          this.handleSelectProduct(this.tableData.length - 1, item);
        });
      },
      handleImportConfirm(res) {
        const importData = res?.data || res?.datas || res || {};
        if (Array.isArray(importData) && importData.length > 0) {
          this.tableData = importData.map((item) => {
            const importUnmatched = isEmpty(item.productId);
            const row = Object.assign(this.emptyProduct(), item, {
              id: uuid(),
              units: isEmpty(item.unitId)
                ? []
                : [
                    {
                      id: item.unitId,
                      unitName: item.unit,
                      conversionRate: 1,
                    },
                  ],
              isFixed: false,
              importUnmatched,
              productQuery: importUnmatched ? item.productName : '',
              oriPrice: item.oriPrice,
              taxPrice: item.taxPrice,
              outNum: item.orderNum,
            });
            // 导入后自动计算金额 = 数量 × 单价
            clearManualSheetAmount(row, 'outNum', 'taxPrice');
            return row;
          });
        } else {
          this.tableData = [];
        }

        this.calcSum();
        createSuccessAutoClose('导入成功！');
      },
      // 校验数据
      validData() {
        if (isEmpty(this.formData.customerId)) {
          createError('客户不允许为空！');
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
        const unmatchedIndex = this.tableData.findIndex((item) =>
          this.isImportUnmatchedProduct(item),
        );
        if (unmatchedIndex >= 0) {
          createError(
            '第' +
              (unmatchedIndex + 1) +
              '行商品未匹配，请确认商品不存在或存在多条匹配后手动选择商品！',
          );
          return false;
        }

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
          }
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
          description: this.formData.description,
          required: false,
          products: buildUnrequiredSaleOutProducts(this.tableData),
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
            .catch((e) => {
              createError(e?.msg || e?.message || e?.error?.message || '审核失败！');
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
              let saleDetails = res.details || [];
              saleDetails = saleDetails.map((item) => {
                item.isFixed = false;

                return Object.assign(this.emptyProduct(), item);
              });

              this.tableData = saleDetails;

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
        return isSaleOutStockEnough(this.tableData, row);
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

  .sheet-editor-grid-wrapper {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .sheet-editor-grid {
    height: 100%;
  }

  .sheet-editor-actions {
    margin-top: auto;
  }

  :deep(.vxe-body--row.sale-out-import-unmatched-row) {
    background-color: #fff1f0 !important;
  }

  :deep(.sale-out-import-unmatched-row td) {
    border-color: #ffa39e;
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
