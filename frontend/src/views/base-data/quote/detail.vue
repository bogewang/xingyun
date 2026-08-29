<template
  ><page-wrapper
    ><j-border title="报价单详情" v-loading="loading"
      ><j-form bordered
        ><j-form-item label="编号" :span="6">{{ form.code }}</j-form-item
        ><j-form-item label="名称" :span="6">{{ form.name }}</j-form-item
        ><j-form-item label="生效日期" :span="6"
          >{{ form.startDate }} 至 {{ form.endDate }}</j-form-item
        ><j-form-item label="状态" :span="6">{{
          form.status === 'ENABLED' ? '启用' : '停用'
        }}</j-form-item
        ><j-form-item label="备注" :span="24">{{ form.description }}</j-form-item></j-form
      ></j-border
    ><j-border title="报价商品"><vxe-grid :data="form.products" :columns="columns" /></j-border
    ><div class="footer"><a-button @click="$router.back()">返回</a-button></div></page-wrapper
  ></template
>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';

  export default defineComponent({
    name: 'QuoteSheetDetail',
    data() {
      return {
        loading: false,
        form: { products: [] },
        columns: [
          { type: 'seq', title: '序号', width: 60 },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 90 },
          { field: 'salePrice', title: '销售单价（元）', width: 140, align: 'right' },
        ],
      };
    },
    created() {
      this.loading = true;
      api
        .get(this.$route.params.id)
        .then((data) => {
          this.form = data;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  });</script
><style scoped>
  .footer {
    padding: 16px;
    text-align: center;
  }
</style>
