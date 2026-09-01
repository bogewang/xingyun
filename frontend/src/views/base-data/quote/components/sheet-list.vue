<template>
  <page-wrapper content-full-height fixed-height>
    <vxe-grid
      ref="grid"
      row-id="id"
      auto-resize
      resizable
      show-overflow
      height="auto"
      :proxy-config="proxyConfig"
      :columns="columns"
      :toolbar-config="toolbarConfig"
      :pager-config="pagerConfig"
    >
      <template #form>
        <j-border>
          <j-form bordered @keyup.enter="search">
            <j-form-item label="名称"><a-input v-model:value="searchForm.name" allow-clear /></j-form-item>
            <j-form-item label="状态">
              <a-select v-model:value="searchForm.status" allow-clear>
                <a-select-option value="ENABLED">启用</a-select-option>
                <a-select-option value="DISABLED">停用</a-select-option>
              </a-select>
            </j-form-item>
          </j-form>
        </j-border>
      </template>
      <template #toolbar_buttons>
        <a-space>
          <a-button type="primary" @click="search">查询</a-button>
          <a-button v-permission="['base-data:quote:add']" type="primary" @click="edit()">新增</a-button>
        </a-space>
      </template>
      <template #status_default="{ row }">
        <a-tag :color="row.status === 'ENABLED' ? 'green' : 'default'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</a-tag>
      </template>
      <template #action_default="{ row }"><table-action outside :actions="actions(row)" /></template>
    </vxe-grid>
  </page-wrapper>
  <detail :id="id" ref="viewDialog" />
</template>

<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/quote';
  import Detail from '../detail.vue';
  import { buildSortPageVo } from '@/utils/utils';
  import { createConfirm, createSuccess } from '@/hooks/web/msg';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { multiplePageMix } from '@/mixins/multiplePageMix';

  export default defineComponent({
    name: 'QuoteSheetList',
    components: { Detail },
    mixins: [gridCollapseHeightMix, multiplePageMix],
    data() {
      return {
        id: '',
        searchForm: { name: '', status: 'ENABLED' },
        toolbarConfig: { slots: { buttons: 'toolbar_buttons' } },
        pagerConfig: { layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'] },
        columns: [
          { field: 'name', title: '名称', minWidth: 180 },
          { field: 'startDate', title: '生效开始日期', width: 130 },
          { field: 'endDate', title: '生效结束日期', width: 130 },
          { field: 'status', title: '状态', width: 90, slots: { default: 'status_default' } },
          { field: 'description', title: '备注', minWidth: 180 },
          { field: 'createBy', title: '创建人', width: 100 },
          { field: 'createTime', title: '创建时间', width: 170 },
          { title: '操作', width: 300, fixed: 'right', slots: { default: 'action_default' } },
        ],
        proxyConfig: {
          props: { result: 'datas', total: 'totalCount' },
          ajax: {
            query: ({ page, sorts }) => api.query({ ...buildSortPageVo(page, sorts), name: this.searchForm.name || undefined, status: this.searchForm.status || undefined }),
          },
        },
      };
    },
    methods: {
      /** 刷新报价单列表。 */
      search() { this.$refs.grid.commitProxy('reload'); },
      /** 打开报价单编辑页面。 */
      edit(id = '') { this.openChildPage(id ? `/base-data/quote/modify/${id}` : '/base-data/quote/add'); },
      /** 打开报价单查看窗口。 */
      detail(id) { this.id = id; this.$nextTick(() => this.$refs.viewDialog.openDialog()); },
      /** 切换报价单状态。 */
      changeStatus(row) {
        const fn = row.status === 'ENABLED' ? api.disable : api.enable;
        const text = row.status === 'ENABLED' ? '停用' : '启用';
        createConfirm(`是否确定${text}该报价单？`).then(() => fn(row.id).then(() => { createSuccess(`${text}成功！`); this.search(); }));
      },
      /** 删除报价单。 */
      remove(id) { createConfirm('是否确定删除该报价单？').then(() => api.deleteById(id).then(() => { createSuccess('删除成功！'); this.search(); })); },
      /** 创建指定报价单的商品明细导出任务。 */
      exportDetails(id) { api.exportDetail({ idList: [id] }).then(() => createSuccess('创建导出任务成功，请前往“导出中心”进行下载。')); },
      /** 生成单据操作按钮。 */
      actions(row) {
        return [
          { label: '查看', onClick: () => this.detail(row.id) },
          { permission: ['base-data:quote:export'], label: '导出明细', onClick: () => this.exportDetails(row.id) },
          { permission: ['base-data:quote:modify'], label: '修改', onClick: () => this.edit(row.id) },
          { permission: ['base-data:quote:modify'], label: row.status === 'ENABLED' ? '停用' : '启用', onClick: () => this.changeStatus(row) },
          { permission: ['base-data:quote:delete'], label: '删除', danger: true, onClick: () => this.remove(row.id) },
        ];
      },
      /** 子页面保存后重新加载报价单列表。 */
      onRefreshPage() { this.search(); },
    },
  });
</script>
