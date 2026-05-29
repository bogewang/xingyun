<template>
  <div class="app-card-container">
    <div v-permission="['purchase:receive:modify']" v-loading="loading">
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
        id="ReceiveSheetModifyRequire"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        height="500"
        :data="tableData"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
      >
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button
              type="primary"
              :icon="h(PlusOutlined)"
              @click="addProduct"
              >新增</a-button
            >
            <a-button
              danger
              :icon="h(DeleteOutlined)"
              @click="delProduct"
              >删除</a-button
            >
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
              <div v-if="!isEmpty(row.products)">
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
          <span v-else-if="isFloatGeZero(row.receiveNum)">{{
            Math.max(0, sub(row.remainNum, row.receiveNum))
          }}</span>
          <span v-else>{{ row.remainNum }}</span>
        </template>

        <!-- 收货数量 列自定义内容 -->
        <template #receiveNum_default="{ row }">
          <a-input
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
        <template #description_default="{ row }">
          <a-input v-model:value="row.description" />
        </template>
      </vxe-grid>

      <order-time-line :id="id" />

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
        :sc-id="formData.sc.id"
        @confirm="batchAddProduct"
      />

      <div style="text-align: center; background-color: #ffffff; padding: 8px 0">
        <a-space>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
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
  import Moment from 'moment';
  import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
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
    isFloatGtZero,
    isFloat,
    isNumberPrecision,
    getNumber,
    mul,
    add,
    sub,
    uuid,
    PATTERN_IS_FLOAT_GE_ZERO,
  } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { focusVxeGridRow } from '@/utils/vxeGrid';
  import {
    getInlineProductSelectRowClass,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';
  import { requestUserSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm, createPrompt } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';

  export default defineComponent({
    name: 'ModifyPurchaseReceiveSheetRequire',
    components: {
      PurchaseOrderDetail,
      BatchAddProduct,
      OrderTimeLine,
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
        paidAmountDirty: false,
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
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
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

        this.paidAmountDirty = false;
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
              totalAmount: 0,
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

        purchaseApi.searchPurchaseProducts(this.formData.sc.id, queryString).then((res) => {
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
        this.calcSum();
      },
      paidAmountInput(value) {
        this.formData.paidAmount = value;
        this.paidAmountDirty = true;
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
          inputPattern: PATTERN_IS_FLOAT_GE_ZERO,
          inputErrorMessage: '收货数量必须是数字并且不小于0',
          title: '批量录入数量',
          required: true,
        }).then(({ value }) => {
          records.forEach((t) => {
            t.receiveNum = value;

            this.receiveNumInput(value);
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
        if (isEmpty(this.formData.supplier.id)) {
          createError('供应商不允许为空！');
          return false;
        }

        if (isEmpty(this.formData.purchaseOrder.id)) {
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

          if (isEmpty(product.purchasePrice)) {
            createError('第' + (i + 1) + '行商品采购价不允许为空！');
            return false;
          }

          if (!isFloat(product.purchasePrice)) {
            createError('第' + (i + 1) + '行商品采购价必须是数字！');
            return false;
          }

          if (!isFloatGtZero(product.purchasePrice)) {
            createError('第' + (i + 1) + '行商品采购价必须大于0！');
            return false;
          }

          if (!isNumberPrecision(product.purchasePrice, 6)) {
            createError('第' + (i + 1) + '行商品采购价最多允许6位小数！');
            return false;
          }

          if (!isEmpty(product.receiveNum)) {
            if (!isFloat(product.receiveNum)) {
              createError('第' + (i + 1) + '行商品收货数量必须是数字！');
              return false;
            }

            if (product.isFixed) {
              if (!isFloatGeZero(product.receiveNum)) {
                createError('第' + (i + 1) + '行商品收货数量不允许小于0！');
                return false;
              }
            } else {
              if (!isFloatGtZero(product.receiveNum)) {
                createError('第' + (i + 1) + '行商品收货数量必须大于0！');
                return false;
              }
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
          } else {
            if (!product.isFixed) {
              createError('第' + (i + 1) + '行商品收货数量不允许为空！');
              return false;
            }
          }
        }

        if (validTableData.filter((item) => isFloatGtZero(item.receiveNum)).length === 0) {
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
          paidAmount: this.formData.paidAmount,
          purchaseOrderId: this.formData.purchaseOrder.id,
          description: this.formData.description,
          products: validTableData
            .filter((t) => isFloatGtZero(t.receiveNum))
            .map((t) => {
              const product = {
                productId: t.productId,
                receiveNum: t.receiveNum,
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
<style></style>
