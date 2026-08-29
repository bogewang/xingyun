<template>
  <page-wrapper
    ><div v-permission="['base-data:quote:add', 'base-data:quote:modify']" v-loading="loading"
      ><j-border :title="isEdit ? '修改报价单' : '新增报价单'"
        ><a-form ref="formRef" :model="form" :rules="rules" layout="vertical"
          ><a-row :gutter="16"
            ><a-col :span="6"
              ><a-form-item label="名称" name="name"
                ><a-input v-model:value.trim="form.name" /></a-form-item></a-col
            ><a-col :span="6"
              ><a-form-item label="生效开始日期" name="startDate"
                ><a-date-picker
                  v-model:value="form.startDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%" /></a-form-item></a-col
            ><a-col :span="6"
              ><a-form-item label="生效结束日期" name="endDate"
                ><a-date-picker
                  v-model:value="form.endDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%" /></a-form-item></a-col
            ><a-col :span="24"
              ><a-form-item label="备注"
                ><a-textarea
                  v-model:value.trim="form.description"
                  :maxlength="255" /></a-form-item></a-col></a-row></a-form></j-border
      ><j-border title="报价商品"
        ><template #extra
          ><a-space
            ><a-button type="primary" @click="openProductDialog">批量添加商品</a-button
            ><a-button @click="removeProducts">删除商品</a-button></a-space
          ></template
        ><vxe-grid
          ref="productGrid"
          row-id="productId"
          :data="form.products"
          :columns="productColumns"
          :checkbox-config="{ trigger: 'row' }"
          ><template #salePrice_default="{ row }"
            ><a-input
              v-model:value="row.salePrice"
              class="number-input" /></template></vxe-grid></j-border
      ><div class="footer"
        ><a-space
          ><a-button type="primary" :loading="loading" @click="save">保存</a-button
          ><a-button @click="$router.back()">取消</a-button></a-space
        ></div
      ></div
    >
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
  </page-wrapper>
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';
  import * as productApi from '@/api/base-data/product/info';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import { isEmpty, isFloatGeZero } from '@/utils/utils';
  import { buildQuoteSheetPayload, mergeQuoteProducts } from './quoteSheet';

  export default defineComponent({
    name: 'QuoteSheetEdit',
    data() {
      return {
        loading: false,
        productDialogVisible: false,
        productOptions: [],
        productSearch: { condition: '' },
        form: {
          id: '',
          name: '',
          startDate: '',
          endDate: '',
          description: '',
          products: [],
        },
        rules: {
          name: [{ required: true, message: '请输入名称' }],
          startDate: [{ required: true, message: '请选择生效开始日期' }],
          endDate: [{ required: true, message: '请选择生效结束日期' }],
        },
        productColumns: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', title: '序号', width: 60 },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
          {
            field: 'salePrice',
            title: '销售单价（元）',
            width: 150,
            align: 'right',
            slots: { default: 'salePrice_default' },
          },
        ],
        selectorColumns: [
          { type: 'checkbox', width: 45 },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
        ],
      };
    },
    computed: {
      isEdit() {
        return !!this.$route.params.id;
      },
    },
    created() {
      if (this.isEdit) {
        this.loading = true;
        api
          .get(this.$route.params.id)
          .then((data) => {
            this.form = { ...data, products: data.products || [] };
          })
          .finally(() => {
            this.loading = false;
          });
      }
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
        this.form.products = mergeQuoteProducts(this.form.products, records);
        this.productDialogVisible = false;
      },
      removeProducts() {
        const records = this.$refs.productGrid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品！');
          return;
        }
        const ids = new Set(records.map((item) => item.productId));
        this.form.products = this.form.products.filter((item) => !ids.has(item.productId));
      },
      validProducts() {
        if (isEmpty(this.form.products)) {
          createError('请添加报价商品！');
          return false;
        }
        for (let index = 0; index < this.form.products.length; index += 1) {
          const product = this.form.products[index];
          if (!isFloatGeZero(product.salePrice)) {
            createError(`第${index + 1}行商品销售单价必须是不小于0的数字！`);
            return false;
          }
        }
        return true;
      },
      save() {
        this.$refs.formRef.validate().then(() => {
          if (this.form.startDate > this.form.endDate) {
            createError('生效开始日期不能晚于结束日期！');
            return;
          }
          if (!this.validProducts()) return;
          this.loading = true;
          const data = buildQuoteSheetPayload(this.form);
          (this.isEdit ? api.update(data) : api.create(data))
            .then(() => {
              createSuccess('保存成功！');
              this.$router.back();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
    },
  });</script
><style scoped>
  .footer {
    padding: 16px;
    text-align: center;
  }
</style>
