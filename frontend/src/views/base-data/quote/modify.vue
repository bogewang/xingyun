<template>
  <div class="app-card-container sheet-editor-page">
    <div class="sheet-editor-content" v-permission="['base-data:quote:modify']" v-loading="loading">
      <j-border>
        <j-form bordered>
          <j-form-item label="名称" required>
            <a-input v-model:value.trim="formData.name" placeholder="请输入名称" />
          </j-form-item>
          <j-form-item label="生效日期" required>
            <a-range-picker
              v-model:value="dateRange"
              :placeholder="['开始日期', '结束日期']"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </j-form-item>
          <j-form-item label="状态">
            <a-select :value="formData.status" style="width: 100%" @change="changeStatus">
              <a-select-option value="ENABLED">启用</a-select-option>
              <a-select-option value="DISABLED">停用</a-select-option>
            </a-select>
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 报价商品列表 -->
      <div class="sheet-editor-grid-wrapper">
        <vxe-grid
          class="sheet-editor-grid"
          id="QuoteSheetModify"
          ref="grid"
          resizable
          show-overflow
          highlight-hover-row
          keep-source
          row-id="id"
          height="100%"
          :scroll-y="{ enabled: false }"
          @wheel.capture.stop
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
          </a-space>
        </template>

        <!-- 操作 列自定义内容 -->
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
          <InlineProductSelect
            :ref="'productInputRef' + rowIndex"
            biz-type="quote"
            mode="unrequire"
            :row="row"
            :row-index="rowIndex"
            :unit-name-map="unitNameMap"
            @select="handleSelectProduct"
            @add-product="addProduct"
          />
        </template>

        <!-- 销售单价 列自定义内容 -->
        <template #salePrice_default="{ row }">
          <a-input v-model:value="row.salePrice" class="number-input" />
        </template>
        </vxe-grid>
      </div>

      <j-border>
        <j-form bordered label-width="140px">
          <j-form-item label="备注" :span="24" :content-nest="false">
            <a-textarea v-model:value.trim="formData.description" maxlength="200" />
          </j-form-item>
        </j-form>
      </j-border>

      <div
        class="sheet-editor-actions"
        style="text-align: center; background-color: #ffffff; padding: 8px 0"
      >
        <a-space>
          <a-button type="primary" :loading="loading" @click="updateSheet">保存</a-button>
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
  import { defineComponent, h } from 'vue';
  import {
    DeleteOutlined,
    MinusCircleTwoTone,
    PlusCircleTwoTone,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/base-data/quote';
  import * as unitApi from '@/api/base-data/unit';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import { isEmpty, isFloatGeZero, uuid } from '@/utils/utils';
  import { resetInlineProductSelect } from '@/utils/inlineProductSelect';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { buildQuoteSheetPayload } from './quoteSheet';
  import InlineProductSelect from '@/views/sc/shared/inline-product-select.vue';

  export default defineComponent({
    name: 'QuoteSheetModify',
    components: {
      InlineProductSelect,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        PlusOutlined,
        DeleteOutlined,
        PlusCircleTwoTone,
        MinusCircleTwoTone,
      };
    },
    data() {
      return {
        // 是否显示加载框
        loading: false,
        // 计量单位 ID 到名称的映射，用于展示商品主单位。
        unitNameMap: {},
        // 表单数据
        formData: {},
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
          { field: 'code', title: '商品编号', width: 120 },
          {
            field: 'name',
            title: '商品名称',
            width: 300,
            slots: { default: 'productName_default' },
          },
          { field: 'spec', title: '规格', width: 80 },
          { field: 'unit', title: '单位', width: 80 },
          {
            field: 'salePrice',
            title: '销售单价（元）',
            align: 'right',
            width: 140,
            slots: { default: 'salePrice_default' },
          },
        ],
        tableData: [],
      };
    },
    computed: {
      // 将日期范围组件的数组值映射为后端需要的开始、结束日期字段。
      dateRange: {
        get() {
          return this.formData.startDate || this.formData.endDate
            ? [this.formData.startDate, this.formData.endDate]
            : [];
        },
        set(value) {
          this.formData.startDate = value?.[0] || '';
          this.formData.endDate = value?.[1] || '';
        },
      },
    },
    created() {
      this.loading = true;
      this.loadUnitNames();
      api
        .get(this.$route.params.id)
        .then((data) => {
          this.formData = {
            id: data.id,
            name: data.name,
            startDate: data.startDate,
            endDate: data.endDate,
            status: data.status,
            description: data.description,
          };
          this.tableData = (data.products || []).map((item) => ({
            ...item,
            unit: this.getUnitName(item.unit),
            id: uuid(),
            editingProduct: false,
            productQuery: '',
            products: [],
            productOptions: [],
            activeProductIndex: -1,
          }));
        })
        .finally(() => {
          this.loading = false;
        });
    },
    methods: {
      // 加载计量单位名称，避免报价单商品行展示单位 ID。
      loadUnitNames() {
        unitApi.query({ pageIndex: 1, pageSize: 1000 }).then((data) => {
          this.unitNameMap = (data.datas || []).reduce((result, item) => {
            result[item.id] = item.name;
            return result;
          }, {});
          this.tableData.forEach((item) => {
            item.unit = this.getUnitName(item.unit);
          });
        });
      },
      // 获取单位显示名称；兼容历史数据中已保存的单位名称。
      getUnitName(unit) {
        return this.unitNameMap[unit] || unit;
      },
      emptyProduct() {
        return {
          id: uuid(),
          productId: '',
          code: '',
          name: '',
          skuCode: '',
          spec: '',
          unit: '',
          salePrice: '',
          editingProduct: false,
          productQuery: '',
          products: [],
          productOptions: [],
          activeProductIndex: -1,
        };
      },
      // 新增商品行：一次性新增50行空行
      addProduct() {
        const startIndex = this.tableData.length;
        for (let index = 0; index < 50; index += 1) {
          this.tableData.push(this.emptyProduct());
        }
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + startIndex];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
      },
      // 在指定行后插入商品行
      insertProduct(index) {
        this.tableData.splice(index + 1, 0, this.emptyProduct());
        this.$nextTick(() => {
          const productInputRef = this.$refs['productInputRef' + (index + 1)];
          if (productInputRef) {
            productInputRef.focus();
          }
        });
      },
      // 删除当前行商品
      removeCurrentProduct(row) {
        this.tableData = this.tableData.filter((item) => item.id !== row.id);
      },
      // 选择商品
      handleSelectProduct(index, product) {
        // 同一报价单商品不能重复
        const exists = this.tableData.some(
          (item, itemIndex) => itemIndex !== index && item.productId === product.id,
        );
        if (exists) {
          createError('该商品已存在！');
          return;
        }

        this.tableData[index] = Object.assign(this.tableData[index], {
          productId: product.id,
          code: product.code,
          name: product.name,
          skuCode: product.skuCode,
          spec: product.spec,
          unit: this.getUnitName(product.unit),
          editingProduct: false,
          productQuery: '',
        });
        resetInlineProductSelect(this.tableData[index]);
      },
      // 删除勾选商品
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
        });
      },
      updateSheet() {
        if (isEmpty(this.formData.name)) {
          createError('请输入名称！');
          return;
        }
        if (isEmpty(this.formData.startDate)) {
          createError('请选择生效开始日期！');
          return;
        }
        if (isEmpty(this.formData.endDate)) {
          createError('请选择生效结束日期！');
          return;
        }
        if (this.formData.startDate > this.formData.endDate) {
          createError('生效开始日期不能晚于生效结束日期！');
          return;
        }
        const products = this.validProducts();
        if (!products) return;
        this.loading = true;
        api
          .update(buildQuoteSheetPayload({ ...this.formData, products }))
          .then(() => {
            createSuccess('保存成功！');
            this.closeDialog();
          })
          .finally(() => {
            this.loading = false;
          });
      },
      // 切换报价单启用状态，启用时由后端复核报价周期和商品明细。
      changeStatus(status) {
        if (status === this.formData.status) return;
        const enabled = status === 'ENABLED';
        const text = enabled ? '启用' : '停用';
        const request = enabled ? api.enable : api.disable;
        createConfirm(`是否确定${text}该报价单？`).then(() => {
          this.loading = true;
          request(this.formData.id)
            .then(() => {
              this.formData.status = status;
              createSuccess(`${text}成功！`);
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      validProducts() {
        // 未选择商品的空行不参与保存
        const products = this.tableData.filter((t) => !isEmpty(t.productId));
        if (isEmpty(products)) {
          createError('请添加报价商品！');
          return null;
        }
        for (let index = 0; index < products.length; index += 1) {
          const product = products[index];
          if (!isFloatGeZero(product.salePrice)) {
            createError(`第${index + 1}行商品销售单价必须是不小于0的数字！`);
            return null;
          }
        }
        return products;
      },
      closeDialog() {
        this.closeCurrentPage();
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
