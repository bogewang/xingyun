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
          <j-form-item label="创建人">
            <span>{{ formData.createBy }}</span>
          </j-form-item>
          <j-form-item label="创建时间" :span="12">
            <span>{{ formData.createTime }}</span>
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
        row-id="productId"
        height="100%"
        :data="tableData"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
      >
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(PlusOutlined)" @click="openProductDialog"
              >批量添加商品</a-button
            >
            <a-button danger :icon="h(DeleteOutlined)" @click="delProduct">删除商品</a-button>
          </a-space>
        </template>

        <!-- 操作 列自定义内容 -->
        <template #operation_default="{ row }">
          <a-space size="small">
            <a-button
              type="link"
              size="small"
              danger
              :icon="h(MinusCircleTwoTone)"
              @click="removeCurrentProduct(row)"
            />
          </a-space>
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

    <!-- 选择商品弹窗 -->
    <a-modal
      v-model:open="productDialogVisible"
      width="70%"
      title="选择商品"
      @ok="addSelectedProducts"
      ><j-form bordered
        ><j-form-item label="商品"
          ><a-input
            v-model:value="productSearch.condition"
            allow-clear
            @press-enter="searchProducts" /></j-form-item
        ><j-form-item
          ><a-button type="primary" @click="searchProducts">查询</a-button></j-form-item
        ></j-form
      ><vxe-grid
        ref="selectorGrid"
        row-id="id"
        height="500"
        :data="productOptions"
        :columns="selectorColumns"
        :checkbox-config="{ trigger: 'row' }"
    /></a-modal>
  </div>
</template>
<script>
  import { defineComponent, h } from 'vue';
  import { DeleteOutlined, MinusCircleTwoTone, PlusOutlined } from '@ant-design/icons-vue';
  import * as api from '@/api/base-data/quote';
  import * as productApi from '@/api/base-data/product/info';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { isEmpty, isFloatGeZero } from '@/utils/utils';
  import { buildQuoteSheetPayload, mergeQuoteProducts } from './quoteSheet';

  export default defineComponent({
    name: 'ModifyQuoteSheet',
    setup() {
      return {
        h,
        PlusOutlined,
        DeleteOutlined,
        MinusCircleTwoTone,
      };
    },
    data() {
      return {
        // 是否显示加载框
        loading: false,
        // 选择商品弹窗
        productDialogVisible: false,
        productOptions: [],
        productSearch: { condition: '' },
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
          { field: 'name', title: '商品名称', minWidth: 180 },
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
        // 选择商品弹窗的列表数据配置
        selectorColumns: [
          { type: 'checkbox', width: 45 },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
        ],
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
            createBy: data.createBy,
            createTime: data.createTime,
            description: data.description,
          };
          this.tableData = data.products || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    methods: {
      openProductDialog() {
        this.productDialogVisible = true;
        this.searchProducts();
      },
      searchProducts() {
        const condition = this.productSearch.condition;
        productApi
          .selector({
            pageIndex: 1,
            pageSize: 100,
            code: condition,
            name: condition,
            skuCode: '',
            shortName: '',
            brandId: '',
            categoryId: '',
            startTime: '',
            endTime: '',
            productType: undefined,
            available: true,
          })
          .then((data) => {
            this.productOptions = data.datas || [];
          });
      },
      addSelectedProducts() {
        const records = this.$refs.selectorGrid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品！');
          return;
        }
        this.tableData = mergeQuoteProducts(this.tableData, records);
        this.productDialogVisible = false;
      },
      delProduct() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品！');
          return;
        }
        const ids = new Set(records.map((item) => item.productId));
        this.tableData = this.tableData.filter((item) => !ids.has(item.productId));
      },
      removeCurrentProduct(row) {
        this.tableData = this.tableData.filter((item) => item.productId !== row.productId);
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
        if (isEmpty(this.tableData)) {
          createError('请添加报价商品！');
          return;
        }
        for (let index = 0; index < this.tableData.length; index += 1) {
          const product = this.tableData[index];
          if (!isFloatGeZero(product.salePrice)) {
            createError(`第${index + 1}行商品销售单价必须是不小于0的数字！`);
            return;
          }
        }
        this.loading = true;
        api
          .update(buildQuoteSheetPayload({ ...this.formData, products: this.tableData }))
          .then(() => {
            createSuccess('保存成功！');
            this.closeDialog();
          })
          .finally(() => {
            this.loading = false;
          });
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
