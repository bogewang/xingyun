<template>
  <div v-permission="['base-data:print-template:query']">
    <page-wrapper content-full-height fixed-height>
      <!-- 数据列表 -->
      <vxe-grid
        id="PrintTemplate"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{}"
        :loading="loading"
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form bordered label-width="80px" @collapse="$refs.grid.refreshColumn()">
              <j-form-item label="名称">
                <a-input v-model:value="searchFormData.name" allow-clear />
              </j-form-item>
            </j-form>
          </j-border>
        </template>
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['base-data:print-template:add']"
              type="primary"
              :icon="h(PlusOutlined)"
              @click="$refs.addDialog.openDialog()"
              >新增</a-button
            >
            <a-dropdown>
              <template #overlay>
                <a-menu @click="handleCommand">
                  <a-menu-item key="batchDelete" :icon="h(DeleteOutlined)"> 批量删除 </a-menu-item>
                </a-menu>
              </template>
              <a-button v-permission="['base-data:print-template:delete']"
                >更多<DownOutlined
              /></a-button>
            </a-dropdown>
          </a-space>
        </template>

        <!-- 操作 列自定义内容 -->
        <template #action_default="{ row }">
          <table-action outside :actions="createActions(row)" />
        </template>
      </vxe-grid>
    </page-wrapper>

    <!-- 新增窗口 -->
    <add ref="addDialog" @confirm="search" />

    <!-- 修改窗口 -->
    <modify :id="id" ref="updateDialog" @confirm="search" />

    <!-- 设置窗口 -->
    <setting :id="id" ref="settingDialog" />

    <demo-data :id="id" ref="demoDataDialog" />

    <batch-handler
      ref="batchDeleteHandlerDialog"
      :table-column="[{ field: 'name', title: '名称', minWidth: 180 }]"
      title="批量删除"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchDelete"
      @confirm="search"
    />
  </div>
</template>

<script>
  import { h, defineComponent } from 'vue';
  import Add from './add.vue';
  import Modify from './modify.vue';
  import Setting from './setting.vue';
  import * as api from '@/api/base-data/print-template';
  import {
    PlusOutlined,
    SearchOutlined,
    DeleteOutlined,
    DownOutlined,
  } from '@ant-design/icons-vue';
  import DemoData from './demo-data.vue';
  import { buildSortPageVo, isEmpty } from '@/utils/utils';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import BatchHandler from '@/components/BatchHandler';
  import { PRINT_TYPE } from '@/enums/biz/printType';

  export default defineComponent({
    name: 'PrintTemplate',
    components: {
      Add,
      Modify,
      Setting,
      DemoData,
      BatchHandler,
    },
    setup() {
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        DeleteOutlined,
        DownOutlined,
        PRINT_TYPE,
      };
    },
    data() {
      return {
        loading: false,
        // 当前行数据
        id: '',
        ids: [],
        // 查询列表的查询条件
        searchFormData: {},
        batchHandleDatas: [],
        // 工具栏配置
        toolbarConfig: {
          // 自定义左侧工具栏
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50 },
          { field: 'name', title: '名称', minWidth: 160, sortable: true },
          {
            field: 'bizType',
            title: '业务类型',
            width: 140,
            formatter: ({ cellValue }) => {
              return PRINT_TYPE.getDesc(cellValue) || cellValue;
            },
          },
          { title: '操作', width: 180, fixed: 'right', slots: { default: 'action_default' } },
        ],
        // 请求接口配置
        proxyConfig: {
          props: {
            // 响应结果列表字段
            result: 'datas',
            // 响应结果总条数字段
            total: 'totalCount',
          },
          ajax: {
            // 查询接口
            query: ({ page, sorts }) => {
              return api.query(this.buildQueryParams(page, sorts));
            },
          },
        },
      };
    },
    created() {},
    methods: {
      // 列表发生查询时的事件
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      handleCommand({ key }) {
        if (key === 'batchDelete') {
          this.batchDelete();
        }
      },
      doBatchDelete(row) {
        return api.deleteById(row.id);
      },
      batchDelete() {
        const records = this.$refs.grid.getCheckboxRecords();

        if (isEmpty(records)) {
          createError('请选择要删除的打印模板！');
          return;
        }

        this.batchHandleDatas = records;
        this.$refs.batchDeleteHandlerDialog.openDialog();
      },
      deleteRow(id) {
        createConfirm('是否确定删除该打印模板？').then(() => {
          this.loading = true;
          api
            .deleteById(id)
            .then(() => {
              createSuccess('删除成功！');
              this.search();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      // 查询前构建查询参数结构
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.buildSearchFormData(),
        };
      },
      // 查询前构建具体的查询参数
      buildSearchFormData() {
        return {
          ...this.searchFormData,
        };
      },
      createActions(row) {
        return [
          {
            permission: ['base-data:print-template:modify'],
            label: '修改',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.updateDialog.openDialog());
            },
          },
          {
            permission: ['base-data:print-template:modify'],
            label: '设置',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.settingDialog.openDialog());
            },
          },
          {
            permission: ['base-data:print-template:modify'],
            label: '示例数据',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.demoDataDialog.openDialog());
            },
          },
          {
            permission: ['base-data:print-template:delete'],
            danger: true,
            label: '删除',
            onClick: () => {
              this.deleteRow(row.id);
            },
          },
        ];
      },
    },
  });
</script>
<style scoped></style>
