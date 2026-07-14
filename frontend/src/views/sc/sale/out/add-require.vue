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
        :columns="tableColumn"
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

        <!-- 商品名称 列自定义内容 -->
        <template #productName_default="{ row, rowIndex }">
          <a-auto-complete
            v-if="!row.isFixed && (isEmpty(row.productId) || row.editingProduct)"
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
                  <vxe-column field="productCode" title="商品编号" width="120" />
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
                  <vxe-column field="skuCode" title="商品SKU编号" width="120" />
                  <vxe-column field="spec" title="规格" width="80" />
                  <vxe-column field="unit" title="单位" width="80" />
                  <vxe-column
                    field="salePrice"
                    title="参考销售价（元）"
                    width="140"
                    align="right"
                  />
                  <vxe-column
                    field="latestSalePrice"
                    title="最新销售价（元）"
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

        <!-- 库存数量 列自定义内容 -->
        <template #stockNum_default="{ row }">
          <span v-if="checkStockNum(row)">{{ row.stockNum }}</span>
          <span v-else style="color: #f5222d">{{ row.stockNum }}</span>
        </template>

        <!-- 剩余出库数量 列自定义内容 -->
        <template #remainNum_default="{ row }">
          <span v-if="isEmpty(row.remainNum)">-</span>
          <span v-else-if="isFloatGeZero(row.outNum)">{{
            Math.max(0, sub(row.remainNum, row.outNum))
          }}</span>
          <span v-else>{{ row.remainNum }}</span>
        </template>

        <!-- 出库数量 列自定义内容 -->
        <template #outNum_default="{ row }">
          <a-input
            v-model:value="row.outNum"
            class="number-input"
            @input="(e) => outNumInput(e.target.value)"
          />
        </template>

        <!-- 含税金额 列自定义内容 -->
        <template #taxAmount_default="{ row }">
          <span v-if="isFloatGeZero(row.taxPrice) && isFloatGeZero(row.outNum)">{{
            getNumber(mul(row.taxPrice, row.outNum), 2)
          }}</span>
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
        :sc-id="formData.scId"
        @confirm="batchAddProduct"
      />
      <div class="sheet-editor-actions" style="text-align: center; background-color: #ffffff; padding: 8px 0">
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
    StarTwoTone,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import * as saleApi from '@/api/sc/sale/order';
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
    PATTERN_IS_FLOAT_GE_ZERO,
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
  import { createConfirm, createError, createPrompt, createSuccess } from '@/hooks/web/msg';
  import {
    getInlineProductSelectRowClass,
    handleEmptyProductInputEnter,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';
  import { shouldAddProductByEnter } from '@/utils/productAddShortcut';
  import { buildRequiredSaleOutProducts } from './components/saleOutProductParams';

  export default defineComponent({
    name: 'AddSaleOutSheetRequire',
    components: {
      BatchAddProduct,
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
        isEmpty,
        isFloatGeZero,
        sub,
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
          { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
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
        salerOptions: [],
        salerOptionMap: {},
        saleOrderOptions: [],
        saleOrderOptionMap: {},
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
          taxPrice: 0,
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
        // 将选中的商品数据赋值给当前行
        this.tableData[index] = Object.assign(this.tableData[index], product, {
          // 参考价=》商品的售价，价格=》最新价格
          oriPrice: product.salePrice,
          taxPrice: product.latestSalePrice,
          editingProduct: false,
          productQuery: '',
        });
        resetInlineProductSelect(this.tableData[index]);

        this.taxPriceInput(this.tableData[index], this.tableData[index].taxPrice);
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
          inputPattern: PATTERN_IS_FLOAT_GE_ZERO,
          inputErrorMessage: '出库数量必须是数字并且不小于0',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.outNum = value;

            this.outNumInput(value);
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
            record.outNum = record.remainNum;
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
              if (!isFloatGeZero(product.outNum)) {
                createError('第' + (i + 1) + '行商品出库数量不允许小于0！');
                return false;
              }
            } else {
              if (!isFloatGeZero(product.outNum)) {
                createError('第' + (i + 1) + '行商品出库数量不允许小于0！');
                return false;
              }
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

        if (validTableData.filter((item) => isFloatGtZero(item.outNum)).length === 0) {
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

  .sheet-editor-grid {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .sheet-editor-actions {
    margin-top: auto;
  }
</style>
