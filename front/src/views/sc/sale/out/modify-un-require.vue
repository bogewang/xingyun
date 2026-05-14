<template>
  <div class="app-card-container">
    <div v-permission="['sale:out:modify']" v-loading="loading">
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
          <j-form-item label="成本状态">
            <a-select
              v-model:value="formData.fillAllCost"
              style="width: 140px"
              @change="handleFillAllCostChange"
            >
              <a-select-option :value="true">已补全</a-select-option>
              <a-select-option :value="false">未补全</a-select-option>
            </a-select>
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 数据列表 -->
      <vxe-grid
        id="SaleOutSheetModifyUnRequire"
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
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(PlusOutlined)" @click="addProduct">新增</a-button>
            <a-button danger :icon="h(DeleteOutlined)" @click="delProduct">删除</a-button>
            <a-button :icon="h(PlusOutlined)" @click="openBatchAddProductDialog"
              >批量添加商品</a-button
            >
            <a-button :icon="h(NumberOutlined)" @click="batchInputOutNum">批量录入数量</a-button>
            <a-button :icon="h(EditOutlined)" @click="batchInputTaxPrice">批量调整价格</a-button>
          </a-space>
        </template>

        <!-- 商品名称 列自定义内容 -->
        <template #productName_default="{ row, rowIndex }">
          <a-auto-complete
            v-if="isEmpty(row.productId)"
            :ref="'productInputRef' + rowIndex"
            v-model:value="row.productName"
            placeholder="请输入商品编号/名称/SKU编号/简码"
            :options="row.productOptions"
            :dropdown-match-select-width="false"
            :dropdown-style="{ width: '890px' }"
            @search="(e) => queryProduct(e, row)"
          >
            <!-- 自定义下拉框内容 -->
            <template #dropdownRender>
              <div v-if="!isEmpty(row.products)" @mousedown.prevent @click.stop>
                <vxe-table
                  :data="row.products"
                  max-height="500"
                  class="cursor-pointer"
                  highlight-hover-row
                  show-overflow
                  :row-config="{ isHover: true }"
                  @cell-click="({ row: product }) => handleSelectProduct(rowIndex, product)"
                >
                  <vxe-column field="productName" title="商品名称" min-width="200" />
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
              </div>
            </template>
          </a-auto-complete>
          <span v-else>{{ row.productName }}</span>
        </template>

        <!-- 价格 列自定义内容 -->
        <template #taxPrice_default="{ row }">
          <a-input
            v-model:value="row.taxPrice"
            class="number-input"
            @input="(e) => taxPriceInput(row, e.target.value)"
          />
        </template>

        <!-- 库存数量 列自定义内容 -->
        <template #stockNum_default="{ row }">
          <span v-if="checkStockNum(row)">{{ row.stockNum }}</span>
          <span v-else style="color: #f5222d">{{ row.stockNum }}</span>
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

      <order-time-line :id="id" />

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
      <div style="text-align: center; background-color: #ffffff; padding: 8px 0">
        <a-space>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
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
    NumberOutlined,
    EditOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import * as saleApi from '@/api/sc/sale/order';
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
  import { requestCustomerSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm, createPrompt } from '@/hooks/web/msg';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import OrderTimeLine from '@/components/OrderTimeLine';

  export default defineComponent({
    name: 'ModifySaleOutSheetUnRequire',
    components: {
      BatchAddProduct,
      OrderTimeLine,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        PlusOutlined,
        DeleteOutlined,
        NumberOutlined,
        EditOutlined,
        isEmpty,
        isFloatGeZero,
        getNumber,
        mul,
        hasCostPrice: (row) =>
          row &&
          row.costPrice !== null &&
          row.costPrice !== undefined &&
          row.costPrice !== '' &&
          row.costPrice > 0,
        canEditCostPrice: (row) =>
          row &&
          (row.manualInputCost === true ||
            row.costPrice === null ||
            row.costPrice === undefined ||
            row.costPrice === '' ||
            row.costPrice <= 0),
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
          { field: 'id', title: '商品ID', width: 150 },
          {
            field: 'productName',
            title: '商品名称',
            width: 150,
            slots: { default: 'productName_default' },
          },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
          { field: 'externalCode', title: '商品简码', width: 120 },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'categoryName', title: '商品分类', width: 80 },
          { field: 'brandName', title: '商品品牌', width: 80 },
          { field: 'oriPrice', title: '参考销售价（元）', align: 'right', width: 140 },
          {
            field: 'stockNum',
            title: '库存数量',
            align: 'right',
            width: 80,
            slots: { default: 'stockNum_default' },
          },
          {
            field: 'taxPrice',
            title: '价格（元）',
            align: 'right',
            width: 80,
            slots: { default: 'taxPrice_default' },
          },
          {
            field: 'outNum',
            title: '出库数量',
            align: 'right',
            width: 80,
            slots: { default: 'outNum_default' },
          },
          {
            field: 'taxAmount',
            title: '含税金额',
            align: 'right',
            width: 80,
            slots: { default: 'taxAmount_default' },
          },
          {
            field: 'costPrice',
            title: '成本单价',
            align: 'right',
            width: 100,
            slots: { default: 'costPrice_default' },
          },
          {
            field: 'costStatus',
            title: '成本状态',
            width: 100,
            slots: { default: 'costStatus_default' },
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
      };
    },
    computed: {},
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
      // 返回销售出库查询页，避免回到缓存中的其他页面
      goQueryPage() {
        this.closeCurrentPage();
      },
      // 初始化表单数据
      initFormData() {
        this.formData = {
          scId: '',
          customerId: '',
          salerId: '',
          orderDate: '',
          totalNum: 0,
          totalAmount: 0,
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
              scId: res.scId,
              customerId: res.customerId,
              salerId: res.salerId || '',
              orderDate: res.orderDate || '',
              description: res.description,
              paidAmount: res.paidAmount,
              status: res.status,
              createBy: res.createBy,
              createTime: res.createTime,
              approveBy: res.approveBy,
              approveTime: res.approveTime,
              refuseReason: res.refuseReason,
              totalNum: 0,
              totalAmount: 0,
              fillAllCost: !!res.fillAllCost,
            });
            this.originalFillAllCost = !!res.fillAllCost;
            if (!isEmpty(res.customerId) && !isEmpty(res.customerName)) {
              const selectedCustomerOptions = [
                {
                  label: res.customerName,
                  value: res.customerId,
                  keywords: [res.customerName, res.customerId].filter((value) => !!value).join(' '),
                },
              ];
              this.customerOptionMap = mergeSelectOptionMap(
                this.customerOptionMap,
                selectedCustomerOptions,
              );
              this.customerOptions = buildVisibleSelectOptions(
                this.formData.customerId,
                this.customerOptionMap,
                selectedCustomerOptions,
              );
            }
            const tableData = res.details || [];
            tableData.forEach((item) => {
              item.isFixed = false;

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
          taxPrice: '',
          stockNum: '',
          orderNum: '',
          remainNum: '',
          outNum: '',
          taxRate: '',
          taxAmount: '',
          costPrice: '',
          manualInputCost: false,
          description: '',
          isFixed: false,
          products: [],
          productOptions: [],
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
      // 搜索商品
      queryProduct(queryString, row) {
        if (isEmpty(queryString)) {
          row.products = [];
          row.productOptions = [];
          return;
        }

        saleApi.searchSaleProducts(this.formData.scId, queryString).then((res) => {
          row.products = res;
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
          oriPrice: product.salePrice,
          taxPrice: product.latestSalePrice,
        });

        this.taxPriceInput(this.tableData[index], this.tableData[index].taxPrice);
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
      costPriceInput(_row, _value) {
        _row.manualInputCost = !isEmpty(_value);
        this.calcSum();
      },
      outNumInput(_value) {
        this.calcSum();
      },
      handleFillAllCostChange(value) {
        this.formData.fillAllCost = value;
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

          if (!isFloatGtZero(product.taxPrice)) {
            createError('第' + (i + 1) + '行商品价格必须大于0！');
            return false;
          }

          if (!isNumberPrecision(product.taxPrice, 6)) {
            createError('第' + (i + 1) + '行商品价格最多允许6位小数！');
            return false;
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

            if (product.isFixed) {
              if (!isFloatGeZero(product.outNum)) {
                createError('第' + (i + 1) + '行商品出库数量不允许小于0！');
                return false;
              }
            } else {
              if (!isFloatGtZero(product.outNum)) {
                createError('第' + (i + 1) + '行商品出库数量必须大于0！');
                return false;
              }
            }

            if (!isNumberPrecision(product.outNum, 8)) {
              createError('第' + (i + 1) + '行商品出库数量最多允许8位小数！');
              return false;
            }
          } else {
            if (!product.isFixed) {
              createError('第' + (i + 1) + '行商品出库数量不允许为空！');
              return false;
            }
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
          customerId: this.formData.customerId,
          salerId: this.formData.salerId || '',
          orderDate: this.formData.orderDate || '',
          paidAmount: this.formData.paidAmount,
          fillAllCost: this.formData.fillAllCost,
          fillAllCostModified: this.formData.fillAllCost !== this.originalFillAllCost,
          description: this.formData.description,
          products: validTableData
            .filter((t) => isFloatGtZero(t.outNum))
            .map((t) => {
              const product = {
                productId: t.productId,
                oriPrice: t.oriPrice,
                taxPrice: t.taxPrice,
                orderNum: t.outNum,
                description: t.description,
                costPrice: this.canEditCostPrice(t) && !isEmpty(t.costPrice) ? t.costPrice : null,
              };

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
<style></style>
