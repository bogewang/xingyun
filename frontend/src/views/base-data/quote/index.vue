<template>
  <div v-permission="['base-data:quote:query']">
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        ref="grid"
        row-id="id"
        height="auto"
        :proxy-config="proxyConfig"
        :columns="columns"
        :toolbar-config="toolbarConfig"
        :pager-config="{
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        }"
      >
        <template #form
          ><j-border
            ><j-form bordered
              ><j-form-item label="名称"
                ><a-input v-model:value="searchForm.name" allow-clear /></j-form-item
              ><j-form-item label="状态"
                ><a-select v-model:value="searchForm.status" allow-clear
                  ><a-select-option value="ENABLED">启用</a-select-option
                  ><a-select-option value="DISABLED">停用</a-select-option></a-select
                ></j-form-item
              ></j-form
            ></j-border
          ></template
        >
        <template #toolbar_buttons
          ><a-space
            ><a-button type="primary" @click="search">查询</a-button
            ><a-button v-permission="['base-data:quote:add']" type="primary" @click="edit()"
              >新增</a-button
            ></a-space
          ></template
        >
        <template #status_default="{ row }"
          ><a-tag :color="row.status === 'ENABLED' ? 'green' : 'default'">{{
            row.status === 'ENABLED' ? '启用' : '停用'
          }}</a-tag></template
        >
        <template #action_default="{ row }"
          ><table-action outside :actions="actions(row)"
        /></template>
      </vxe-grid>
    </page-wrapper>
  </div>
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';
  import { buildSortPageVo } from '@/utils/utils';
  import { createConfirm, createSuccess } from '@/hooks/web/msg';

  export default defineComponent({
    name: 'QuoteSheet',
    data() {
      return {
        searchForm: { name: '', status: '' },
        toolbarConfig: { slots: { buttons: 'toolbar_buttons' } },
        columns: [
          { field: 'name', title: '名称', minWidth: 180 },
          { field: 'startDate', title: '生效开始日期', width: 130 },
          { field: 'endDate', title: '生效结束日期', width: 130 },
          { field: 'status', title: '状态', width: 90, slots: { default: 'status_default' } },
          { field: 'description', title: '备注', minWidth: 180 },
          { field: 'createBy', title: '创建人', width: 100 },
          { field: 'createTime', title: '创建时间', width: 170 },
          { title: '操作', width: 220, fixed: 'right', slots: { default: 'action_default' } },
        ],
        proxyConfig: {
          props: { result: 'datas', total: 'totalCount' },
          ajax: {
            query: ({ page, sorts }) =>
              api.query({
                ...buildSortPageVo(page, sorts),
                name: this.searchForm.name || undefined,
                status: this.searchForm.status || undefined,
              }),
          },
        },
      };
    },
    methods: {
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      edit(id = '') {
        this.$router.push({ name: 'QuoteSheetEdit', params: id ? { id } : {} });
      },
      detail(id) {
        this.$router.push({ name: 'QuoteSheetDetail', params: { id } });
      },
      changeStatus(row) {
        const fn = row.status === 'ENABLED' ? api.disable : api.enable;
        const text = row.status === 'ENABLED' ? '停用' : '启用';
        createConfirm(`是否确定${text}该报价单？`).then(() =>
          fn(row.id).then(() => {
            createSuccess(`${text}成功！`);
            this.search();
          }),
        );
      },
      remove(id) {
        createConfirm('是否确定删除该报价单？').then(() =>
          api.deleteById(id).then(() => {
            createSuccess('删除成功！');
            this.search();
          }),
        );
      },
      actions(row) {
        return [
          { label: '查看', onClick: () => this.detail(row.id) },
          {
            permission: ['base-data:quote:modify'],
            label: '修改',
            onClick: () => this.edit(row.id),
          },
          {
            permission: ['base-data:quote:modify'],
            label: row.status === 'ENABLED' ? '停用' : '启用',
            onClick: () => this.changeStatus(row),
          },
          {
            permission: ['base-data:quote:delete'],
            label: '删除',
            danger: true,
            onClick: () => this.remove(row.id),
          },
        ];
      },
    },
  });
</script>
