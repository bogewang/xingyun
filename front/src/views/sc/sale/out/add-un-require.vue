<template>
  <div
    ref="importerContainer"
    class="app-card-container sheet-editor-page excel-importer-local-container"
  >
    <div class="sheet-editor-content" v-permission="['sale:out:add']" v-loading="loading">
      <j-border>
        <j-form bordered>
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
          <j-form-item label="订单日期">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder=""
              value-format="YYYY-MM-DD"
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
          :toolbar-config="toolbarConfig"
          :custom-config="{}"
        >
          <!-- 工具栏 -->
          <template #toolbar_buttons>
            <a-space>
              <a-button type="primary" :icon="h(PlusOutlined)" @click="addProduct">新增</a-button>
              <a-button danger :icon="h(DeleteOutlined)" @click="delProduct">删除</a-button>
              <a-button :icon="h(PlusOutlined)" @click="openBatchAddProductDialog"
                >批量添加商品
              </a-button>
              <a-button :icon="h(NumberOutlined)" @click="batchInputOutNum"
                >批量录入数量</a-button
              >
              <a-button :icon="h(EditOutlined)" @click="batchInputTaxPrice"
                >批量调整价格</a-button
              >
              <a-button :icon="h(CloudUploadOutlined)" @click="$refs.importer.openDialog()"
                >导入Excel</a-button
              >
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

          <!-- 商品名称 列自定义内容 -->
          <template #productName_default="{ row, rowIndex }">
            <a-auto-complete
              v-if="isEmpty(row.productId) || row.editingProduct"
              :ref="'productInputRef' + rowIndex"
              v-model:value="row.productQuery"
              placeholder="请输入商品编号/名称/SKU编号/简码"
              :options="row.productOptions"
              :dropdown-match-select-width="false"
              :dropdown-style="{ width: '890px' }"
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
                    <vxe-column field="spec" title="规格" width="80" />
                    <vxe-column field="unit" title="单位" width="80" />
                    <vxe-column field="stockNum" title="库存数量" width="100" align="right" />
                    <vxe-column
                      field="latestSalePrice"
                      title="最新销售价（元）"
                      width="140"
                      align="right"
                    />
                    <vxe-column
                      field="salePrice"
                      title="参考销售价（元）"
                      width="140"
                      align="right"
                    />

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
              style="color: #1677ff; cursor: pointer"
              @click="enableProductEdit(rowIndex)"
              >{{ row.productName }}</span
            >
          </template>

          <!-- 价格 列自定义内容 -->
          <template #taxPrice_default="{ row, rowIndex }">
            <a-input
              :ref="'taxPriceInputRef' + rowIndex"
              v-model:value="row.taxPrice"
              class="number-input"
              @input="(e) => taxPriceInput(row, e.target.value)"
            />
          </template>

          <!-- 数量 列自定义内容 -->
          <template #outNum_default="{ row, rowIndex }">
            <a-input
              :ref="'outNumInputRef' + rowIndex"
              v-model:value="row.outNum"
              class="number-input"
              @input="(e) => outNumInput(e.target.value)"
            />
          </template>

          <!-- 库存数量 列自定义内容 -->
          <template #stockNum_default="{ row }">
            <span v-if="checkStockNum(row)">{{ row.stockNum }}</span>
            <span v-else style="color: #f5222d">{{ row.stockNum }}</span>
          </template>

          <!-- 含税金额 列自定义内容 -->
          <template #taxAmount_default="{ row }">
            <span v-if="isFloatGeZero(row.taxPrice) && isFloatGeZero(row.outNum)">{{
              getNumber(mul(row.taxPrice, row.outNum), 2)
            }}</span>
          </template>

          <!-- 备注 列自定义内容 -->
          <template #description_default="{ row, rowIndex }">
            <a-input :ref="'descriptionInputRef' + rowIndex" v-model:value="row.description" />
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
    StarTwoTone,
  } from '@ant-design/icons-vue';
  import SaleOutSheetImporter from '@/components/Importor/SaleOutSheetImporter.vue';
  import * as api from '@/api/sc/sale/out';
  import * as saleApi from '@/api/sc/sale/order';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
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
    PATTERN_IS_FLOAT_GT_ZERO,
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
  import {
    createConfirm,
    createError,
    createPrompt,
    createSuccess,
    createSuccessAutoClose,
  } from '@/hooks/web/msg';
  import {
    getInlineProductSelectRowClass,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';

  export default defineComponent({
    name: 'AddSaleOutSheetUnRequire',
    components: {
      BatchAddProduct,
      SaleOutSheetImporter,
      StarTwoTone,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        PlusOutlined,
        PlusCircleTwoTone,
        StarTwoTone,
        DeleteOutlined,
        MinusCircleTwoTone,
        NumberOutlined,
        EditOutlined,
        CloudUploadOutlined,
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
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
            width: 260,
            slots: { default: 'productName_default' },
          },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          {
            field: 'stockNum',
            title: '库存数量',
            align: 'right',
            width: 100,
            slots: { default: 'stockNum_default' },
          },
          {
            field: 'outNum',
            title: '数量',
            align: 'right',
            width: 100,
            slots: { default: 'outNum_default' },
          },
          {
            field: 'taxPrice',
            title: '价格（元）',
            align: 'right',
            width: 140,
            slots: { default: 'taxPrice_default' },
          },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 100,
            slots: { default: 'taxAmount_default' },
          },
          {
            field: 'description',
            title: '备注',
            width: 200,
            slots: { default: 'description_default' },
          },
        ],
        tableData: [],
        customerOptions: [],
        customerOptionMap: {},
      };
    },
    computed: {},
    created() {
      // 初始化表单数据
      this.openDialog();
    },
    mounted() {
      // 监听键盘事件，按下回车键时调用addProduct方法
      document.addEventListener('keydown', this.handleKeyDown);
    },
    beforeUnmount() {
      // 移除键盘事件监听
      document.removeEventListener('keydown', this.handleKeyDown);
    },
    methods: {
      getImporterContainer() {
        return this.$refs.importerContainer;
      },
      // 处理键盘事件
      handleKeyDown(event) {
        // 按下回车键时调用addProduct方法
        if (event.key === 'Enter' || event.keyCode === 13) {
          this.addProduct();
        }
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
          oriPrice: '',
          taxPrice: '',
          stockNum: '',
          orderNum: '',
          remainNum: '',
          outNum: '',
          taxRate: '',
          taxAmount: '',
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
      enableProductEdit(index) {
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
      focusRowInput(refName, index) {
        return focusTableInput(this, refName, index);
      },
      // 选择商品（从表格中点击）
      handleSelectProduct(index, product) {
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          oriPrice: product.salePrice,
          taxPrice: product.latestSalePrice,
          editingProduct: false,
          productQuery: '',
          importUnmatched: false,
        });
        resetInlineProductSelect(this.tableData[index]);

        this.taxPriceInput(this.tableData[index], this.tableData[index].taxPrice);
        this.focusRowInput('outNumInputRef', index);
      },
      handleProductSelectKeydown(event, row, rowIndex) {
        handleInlineProductSelectKeydown(event, row, rowIndex, this.handleSelectProduct, () =>
          this.$nextTick(),
        );
      },
      getProductSelectRowClass(row, product) {
        return getInlineProductSelectRowClass(row, product);
      },
      isImportUnmatchedProduct(row) {
        return row.importUnmatched && isEmpty(row.productId);
      },
      getTableRowClassName({ row }) {
        return this.isImportUnmatchedProduct(row) ? 'sale-out-import-unmatched-row' : '';
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
      taxPriceInput(_row, _value) {
        this.calcSum();
      },
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
      },
      outNumInput(_value) {
        this.calcSum();
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
          inputPattern: PATTERN_IS_FLOAT_GT_ZERO,
          inputErrorMessage: '出库数量必须是数字并且大于0',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.outNum = value;

            this.outNumInput(value);
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
            return Object.assign(this.emptyProduct(), item, {
              id: uuid(),
              isFixed: false,
              importUnmatched,
              productQuery: importUnmatched ? item.productName : '',
              oriPrice: item.oriPrice,
              taxPrice: item.taxPrice,
              outNum: item.orderNum,
            });
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

        if (!isFloatGeZero(this.formData.paidAmount)) {
          createError('付款金额不允许小于0！');
          return false;
        }

        if (!isNumberPrecision(this.formData.paidAmount, 6)) {
          createError('付款金额最多允许6位小数！');
          return false;
        }

        if (parseFloat(this.formData.paidAmount) > parseFloat(this.formData.totalAmount || 0)) {
          createError('付款金额不允许大于含税总金额！');
          return false;
        }

        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));
        const unmatchedIndex = this.tableData.findIndex((item) => this.isImportUnmatchedProduct(item));
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

          if (isEmpty(product.taxPrice)) {
            createError('第' + (i + 1) + '行商品价格不允许为空！');
            return false;
          }

          if (!isFloat(product.taxPrice)) {
            createError('第' + (i + 1) + '行商品价格必须是数字！');
            return false;
          }

          // if (!isFloatGtZero(product.taxPrice)) {
          //   createError('第' + (i + 1) + '行商品价格必须大于0！');
          //   return false;
          // }

          if (!isNumberPrecision(product.taxPrice, 6)) {
            createError('第' + (i + 1) + '行商品价格最多允许6位小数！');
            return false;
          }

          if (!isEmpty(product.outNum)) {
            if (!isFloat(product.outNum)) {
              createError('第' + (i + 1) + '行商品出库数量必须是数字！');
              return false;
            }

            if (!isFloatGtZero(product.outNum)) {
              createError('第' + (i + 1) + '行商品出库数量必须大于0！');
              return false;
            }

            if (!isNumberPrecision(product.outNum, 8)) {
              createError('第' + (i + 1) + '行商品出库数量最多允许8位小数！');
              return false;
            }
          } else {
            createError('第' + (i + 1) + '行商品出库数量不允许为空！');
            return false;
          }
        }

        return true;
      },
      buildParams() {
        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));
        return {
          scId: this.formData.scId,
          customerId: this.formData.customerId,
          salerId: this.formData.salerId || '',
          orderDate: this.formData.orderDate || '',
          paidAmount: this.formData.paidAmount,
          description: this.formData.description,
          required: false,
          products: validTableData
            .filter((t) => isFloatGtZero(t.outNum))
            .map((t) => {
              const product = {
                productId: t.productId,
                oriPrice: t.oriPrice,
                taxPrice: t.taxPrice,
                orderNum: t.outNum,
                description: t.description,
              };

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

  :deep(.sale-out-import-unmatched-row) {
    background-color: #fff1f0;
  }

  :deep(.sale-out-import-unmatched-row td) {
    border-color: #ffa39e;
  }
</style>
