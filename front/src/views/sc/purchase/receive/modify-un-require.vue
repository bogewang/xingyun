<template>
  <div class="app-card-container sheet-editor-page">
    <div
      class="sheet-editor-content"
      v-permission="['purchase:receive:modify']"
      v-loading="loading"
    >
      <j-border>
        <j-form bordered>
          <j-form-item label="供应商" required>
            <supplier-selector v-model:value="formData.supplierId" />
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
          id="ReceiveSheetModifyUnRequire"
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
              <a-button :icon="h(NumberOutlined)" @click="batchInputReceiveNum"
                >批量录入数量</a-button
              >
              <a-button :icon="h(EditOutlined)" @click="batchInputPurchasePrice"
                >批量调整采购价</a-button
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
                    <vxe-column field="spec" title="规格" width="80" />
                    <vxe-column field="unit" title="单位" width="80" />
                    <vxe-column field="stockNum" title="库存数量" width="140" align="right" />
                    <vxe-column
                      field="latestPurchasePrice"
                      title="最新采购价（元）"
                      width="140"
                      align="right"
                    />
                    <vxe-column
                      field="purchasePrice"
                      title="参考采购价（元）"
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

          <!-- 采购价 列自定义内容 -->
          <template #purchasePrice_default="{ row, rowIndex }">
            <a-input
              :ref="'purchasePriceInputRef' + rowIndex"
              v-model:value="row.purchasePrice"
              class="number-input"
              @input="(e) => purchasePriceInput(e.target.value)"
            />
          </template>

          <!-- 数量 列自定义内容 -->
          <template #receiveNum_default="{ row, rowIndex }">
            <a-input
              :ref="'receiveNumInputRef' + rowIndex"
              v-model:value="row.receiveNum"
              class="number-input"
              @input="(e) => receiveNumInput(e.target.value)"
            />
          </template>

          <!-- 含税金额 列自定义内容 -->
          <template #taxAmount_default="{ row }">
            <span v-if="isFloatGeZero(row.purchasePrice) && isFloatGeZero(row.receiveNum)">{{
              getNumber(mul(row.purchasePrice, row.receiveNum), 2)
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
          <j-form-item label="数量" :span="8">
            <a-input v-model:value="formData.totalNum" class="number-input" readonly />
          </j-form-item>
          <j-form-item label="折后金额" :span="8">
            <a-input
              v-model:value="formData.totalAmount"
              class="number-input"
              @input="(e) => totalAmountInput(e.target.value)"
            />
          </j-form-item>
          <j-form-item label="本次付款" :span="8">
            <a-input
              v-model:value="formData.paidAmount"
              class="number-input"
              @input="(e) => paidAmountInput(e.target.value)"
            />
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
  </div>
</template>
<script>
  import { h, defineComponent } from 'vue';
  import BatchAddProduct from '@/views/sc/purchase/batch-add-product.vue';
  import Moment from 'moment';
  import {
    PlusOutlined,
    DeleteOutlined,
    PlusCircleTwoTone,
    MinusCircleTwoTone,
    NumberOutlined,
    EditOutlined,
    StarTwoTone,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/purchase/receive';
  import * as purchaseApi from '@/api/sc/purchase/order';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import {
    isEmpty,
    isFloatGeZero,
    getNumber,
    mul,
    add,
    isFloat,
    isFloatGtZero,
    isNumberPrecision,
    uuid,
    PATTERN_IS_FLOAT_GT_ZERO,
    PATTERN_IS_PRICE,
  } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { focusTableInput, focusVxeGridRow } from '@/utils/vxeGrid';
  import {
    getInlineProductSelectRowClass,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';
  import { requestSupplierSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm, createPrompt } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import JFormItem from '@/components/JFormItem';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';
  import * as saleApi from "@/api/sc/sale/order";

  export default defineComponent({
    name: 'ModifyPurchaseReceiveSheetUnRequire',
    components: {
      JFormItem,
      BatchAddProduct,
      OrderTimeLine,
      SupplierSelector,
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
        getNumber,
        mul,
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
        paidAmountDirty: false,
        totalAmountDirty: false,
        timelineVisible: false,
        supplierOptions: [],
        supplierOptionMap: {},
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
          { field: 'productCode', title: '商品编号', width: 120 },
          {
            field: 'productName',
            title: '商品名称',
            width: 150,
            slots: { default: 'productName_default' },
          },
          // { field: 'skuCode', title: '商品SKU编号', width: 120 },
          // { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 80 },
          // { field: 'brandName', title: '商品品牌', width: 120 },
          // { field: 'taxCostPrice', title: '含税成本价（元）', align: 'right', width: 140 },
          { field: 'stockNum', title: '库存数量', align: 'right', width: 140 },
          {
            field: 'receiveNum',
            title: '数量',
            align: 'right',
            width: 80,
            slots: { default: 'receiveNum_default' },
          },
          {
            field: 'purchasePrice',
            title: '单价（元）',
            align: 'right',
            width: 80,
            slots: { default: 'purchasePrice_default' },
          },
          {
            field: 'taxAmount',
            title: '金额',
            align: 'right',
            width: 80,
            slots: { default: 'taxAmount_default' },
          },
          // { field: 'taxRate', title: '税率（%）', align: 'right', width: 100 },
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
          scId: '',
          supplierId: '',
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

        this.paidAmountDirty = false;
        this.totalAmountDirty = false;
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
              scId: res.scId,
              supplierId: res.supplierId,
              purchaserId: res.purchaserId || '',
              orderDate: res.orderDate || '',
              receiveDate: res.receiveDate,
              purchaseOrder: {},
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
              totalAmount: 0,
            });
            this.paidAmountDirty = false;
            this.totalAmountDirty = false;

            if (!isEmpty(res.supplierId) && !isEmpty(res.supplierName)) {
              const selectedSupplierOptions = [
                {
                  label: res.supplierName,
                  value: res.supplierId,
                  keywords: [res.supplierName, res.supplierId].filter((value) => !!value).join(' '),
                },
              ];
              this.supplierOptionMap = mergeSelectOptionMap(
                this.supplierOptionMap,
                selectedSupplierOptions,
              );
              this.supplierOptions = buildVisibleSelectOptions(
                this.formData.supplierId,
                this.supplierOptionMap,
                selectedSupplierOptions,
              );
            }

            const tableData = res.details || [];
            tableData.forEach((item) => {
              item.isFixed = false;

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
          purchasePrice: '',
          taxCostPrice: '',
          stockNum: '',
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
        this.tableData.push(this.emptyProduct());
        this.focusProductRow(this.tableData.length - 1);
      },
      insertProduct(index) {
        const insertedIndex = index + 1;
        this.tableData.splice(insertedIndex, 0, this.emptyProduct());
        this.focusProductRow(insertedIndex);
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
        const purchasePrice = !isEmpty(product.latestPurchasePrice)
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
        this.focusRowInput('receiveNumInputRef', index);
      },
      handleProductSelectKeydown(event, row, rowIndex) {
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
      async requestSupplierOptions(keyword = '') {
        return requestSupplierSelectOptions(keyword);
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
      totalAmountInput(value) {
        this.formData.totalAmount = value;
        this.totalAmountDirty = true;
      },
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
      },
      purchasePriceInput(_row, _value) {
        this.calcSum();
      },
      receiveNumInput(_value) {
        this.calcSum();
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
        if (!this.totalAmountDirty) {
          this.formData.totalAmount = totalAmount;
        }
      },
      // 批量录入数量
      batchInputReceiveNum() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        createPrompt('请输入数量', {
          inputPattern: PATTERN_IS_FLOAT_GT_ZERO,
          inputErrorMessage: '数量必须是数字并且大于0',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.receiveNum = value;

            this.receiveNumInput(value);
          });
        });
      },
      // 批量录入采购价
      batchInputPurchasePrice() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        createPrompt('请输入采购价（元）', {
          inputPattern: PATTERN_IS_PRICE,
          inputErrorMessage: '采购价（元）必须是数字并且不小于0，最多允许6位小数',
          title: '批量调整采购价',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.purchasePrice = value;

            this.purchasePriceInput(t, value);
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
      // 校验数据
      validData() {
        if (isEmpty(this.formData.supplierId)) {
          createError('供应商不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.totalAmount)) {
          createError('折后金额不允许为空！');
          return false;
        }

        if (!isFloat(this.formData.totalAmount)) {
          createError('折后金额必须是数字！');
          return false;
        }

        if (!isFloatGeZero(this.formData.totalAmount)) {
          createError('折后金额不允许小于0！');
          return false;
        }

        if (!isNumberPrecision(this.formData.totalAmount, 2)) {
          createError('折后金额最多允许2位小数！');
          return false;
        }

        if (isEmpty(this.formData.paidAmount)) {
          createError('本次付款不允许为空！');
          return false;
        }

        if (!isFloat(this.formData.paidAmount)) {
          createError('本次付款必须是数字！');
          return false;
        }

        if (!isFloatGeZero(this.formData.paidAmount)) {
          createError('本次付款不允许小于0！');
          return false;
        }

        if (!isNumberPrecision(this.formData.paidAmount, 6)) {
          createError('本次付款最多允许6位小数！');
          return false;
        }

        if (parseFloat(this.formData.paidAmount) > parseFloat(this.formData.totalAmount || 0)) {
          createError('本次付款不允许大于折后金额！');
          return false;
        }

        const validTableData = this.tableData.filter((item) => !isEmpty(item.productId));

        if (isEmpty(validTableData)) {
          createError('请录入商品！');
          return false;
        }

        for (let i = 0; i < validTableData.length; i++) {
          const product = validTableData[i];

          if (isEmpty(product.purchasePrice)) {
            createError('第' + (i + 1) + '行商品采购价不允许为空！');
            return false;
          }

          if (!isFloat(product.purchasePrice)) {
            createError('第' + (i + 1) + '行商品采购价必须是数字！');
            return false;
          }

          // if (!isFloatGtZero(product.purchasePrice)) {
          //   createError('第' + (i + 1) + '行商品采购价必须大于0！');
          //   return false;
          // }

          if (!isNumberPrecision(product.purchasePrice, 6)) {
            createError('第' + (i + 1) + '行商品采购价最多允许6位小数！');
            return false;
          }

          if (!isEmpty(product.receiveNum)) {
            if (!isFloat(product.receiveNum)) {
              createError('第' + (i + 1) + '行商品数量必须是数字！');
              return false;
            }

            if (!isFloatGtZero(product.receiveNum)) {
              createError('第' + (i + 1) + '行商品数量必须大于0！');
              return false;
            }

            if (!isNumberPrecision(product.receiveNum, 8)) {
              createError('第' + (i + 1) + '行商品数量最多允许8位小数！');
              return false;
            }
          } else {
            createError('第' + (i + 1) + '行商品数量不允许为空！');
            return false;
          }
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
          scId: this.formData.scId,
          supplierId: this.formData.supplierId,
          purchaserId: this.formData.purchaserId || '',
          orderDate: this.formData.orderDate || '',
          receiveDate: this.formData.receiveDate,
          totalAmount: this.formData.totalAmount,
          paidAmount: this.formData.paidAmount,
          description: this.formData.description,
          products: validTableData
            .filter((t) => isFloatGtZero(t.receiveNum))
            .map((t) => {
              const product = {
                productId: t.productId,
                purchasePrice: t.purchasePrice,
                receiveNum: t.receiveNum,
                description: t.description,
              };

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
    min-height: 0;
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
</style>
