<template>
  <div class="app-card-container sheet-editor-page">
    <div class="sheet-editor-content" v-permission="['base-data:quote:modify']" v-loading="loading">
      <j-border>
        <j-form bordered>
          <j-form-item label="名称" required>
            <a-input v-model:value.trim="formData.name" placeholder="请输入名称" />
          </j-form-item>
          <j-form-item label="生效开始日期" required>
            <a-date-picker
              v-model:value="formData.startDate"
              placeholder=""
              value-format="YYYY-MM-DD"
            />
          </j-form-item>
          <j-form-item label="生效结束日期" required>
            <a-date-picker
              v-model:value="formData.endDate"
              placeholder=""
              value-format="YYYY-MM-DD"
            />
          </j-form-item>
          <j-form-item />
          <j-form-item label="状态">
            <span v-if="formData.status === 'ENABLED'" style="color: #52c41a">启用</span>
            <span v-else style="color: #303133">停用</span>
          </j-form-item>
        </j-form>
      </j-border>
      <!-- 报价商品列表 -->
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
            @select="handleSelectProduct"
            @add-product="addProduct"
          />
        </template>

        <!-- 销售单价 列自定义内容 -->
        <template #salePrice_default="{ row }">
          <a-input v-model:value="row.salePrice" class="number-input" />
        </template>
      </vxe-grid>

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
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import { isEmpty, isFloatGeZero, uuid } from '@/utils/utils';
  import { resetInlineProductSelect } from '@/utils/inlineProductSelect';
  import { buildQuoteSheetPayload } from './quoteSheet';
  import InlineProductSelect from '@/views/sc/shared/inline-product-select.vue';

  export default defineComponent({
    name: 'ModifyQuoteSheet',
    components: {
      InlineProductSelect,
    },
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
            minWidth: 180,
            slots: { default: 'productName_default' },
          },
          { field: 'skuCode', title: '商品SKU编号', width: 120 },
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
    created() {
      this.loading = true;
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
          unit: product.unit,
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
        this.$router.back();
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
</style>
