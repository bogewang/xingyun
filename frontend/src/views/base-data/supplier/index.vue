<template>
  <div v-permission="['base-data:supplier:query']">
    <page-wrapper content-full-height fixed-height>
      <!-- 数据列表 -->
      <vxe-grid
        id="Supplier"
        ref="grid"
        resizable
        show-overflow
        show-footer
        highlight-hover-row
        keep-source
        row-id="id"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        }"
        :footer-method="footerMethod"
        :loading="loading"
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form
              bordered
              label-width="80px"
              @collapse="$refs.grid.refreshColumn()"
              @keyup.enter="search"
            >
              <j-form-item label="编号">
                <a-input v-model:value="searchFormData.code" allow-clear />
              </j-form-item>
              <j-form-item label="名称">
                <a-input v-model:value="searchFormData.name" allow-clear />
              </j-form-item>
              <j-form-item label="状态">
                <a-select v-model:value="searchFormData.available">
                  <a-select-option :value="AVAILABLE.ENABLE.code">启用</a-select-option>
                  <a-select-option :value="AVAILABLE.UNABLE.code">停用</a-select-option>
                  <a-select-option value="">全部</a-select-option>
                </a-select>
              </j-form-item>
            </j-form>
          </j-border>
        </template>
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['base-data:supplier:add']"
              type="primary"
              :icon="h(PlusOutlined)"
              @click="$refs.addDialog.openDialog()"
              >新增</a-button
            >
            <a-button
              v-permission="['base-data:supplier:import']"
              :icon="h(CloudUploadOutlined)"
              @click="$refs.importer.openDialog()"
              >导入Excel</a-button
            >
            <a-button
              v-permission="['base-data:supplier:import']"
              :icon="h(DownloadOutlined)"
              @click="exportList"
              >导出</a-button
            >
            <a-dropdown>
              <template #overlay>
                <a-menu @click="handleCommand">
                  <a-menu-item
                    v-permission="['base-data:supplier:modify']"
                    key="batchEnable"
                    :icon="h(CheckOutlined)"
                    >批量启用</a-menu-item
                  >
                  <a-menu-item
                    v-permission="['base-data:supplier:modify']"
                    key="batchDisable"
                    :icon="h(StopOutlined)"
                    >批量停用</a-menu-item
                  >
                  <a-menu-item
                    v-permission="['base-data:supplier:delete']"
                    key="batchDelete"
                    :icon="h(DeleteOutlined)"
                    >批量删除</a-menu-item
                  >
                </a-menu>
              </template>
              <a-button v-permission="['base-data:supplier:modify', 'base-data:supplier:delete']"
                >更多<DownOutlined
              /></a-button>
            </a-dropdown>
          </a-space>
        </template>

        <!-- 操作 列自定义内容 -->
        <template #action_default="{ row }">
          <table-action outside :actions="createActions(row)" />
        </template>
        <template #available_default="{ row }">
          <available-tag :available="row.available" />
        </template>
      </vxe-grid>
    </page-wrapper>

    <!-- 新增窗口 -->
    <add ref="addDialog" @confirm="search" />

    <!-- 修改窗口 -->
    <modify :id="id" ref="updateDialog" @confirm="search" />

    <!-- 查看窗口 -->
    <detail :id="id" ref="viewDialog" />

    <supplier-importer ref="importer" @confirm="search" />

    <!-- 批量操作 -->
    <batch-handler
      ref="batchDeleteHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 100 },
        { field: 'name', title: '名称', minWidth: 180 },
      ]"
      title="批量删除"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchDelete"
      @confirm="search"
    />
    <batch-handler
      ref="batchEnableHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 100 },
        { field: 'name', title: '名称', minWidth: 180 },
      ]"
      title="批量启用"
      :table-data="batchHandleDatas"
      :handle-fn="doBatchAvailableItem"
      :batch-handle-fn="batchEnableHandle"
    />
    <batch-handler
      ref="batchDisableHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 100 },
        { field: 'name', title: '名称', minWidth: 180 },
      ]"
      title="批量停用"
      :table-data="batchHandleDatas"
      :handle-fn="doBatchAvailableItem"
      :batch-handle-fn="batchDisableHandle"
    />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import Add from './add.vue';
  import Modify from './modify.vue';
  import Detail from './detail.vue';
  import * as api from '@/api/base-data/supplier';
  import {
    CheckOutlined,
    CloudUploadOutlined,
    DownOutlined,
    DownloadOutlined,
    PlusOutlined,
    SearchOutlined,
    SettingOutlined,
    DeleteOutlined,
    ThunderboltOutlined,
    StopOutlined,
  } from '@ant-design/icons-vue';
  import { isEmpty, buildSortPageVo } from '@/utils/utils';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import { AVAILABLE } from '@/enums/biz/available';
  import AvailableTag from '@/components/Tag/AvailableTag.vue';
  import { buildSupplierAvailabilityRequest } from './supplierAvailability';
  import SupplierImporter from '@/components/Importor/SupplierImporter.vue';
  import BatchHandler from '@/components/BatchHandler';

  export default defineComponent({
    name: 'Supplier',
    components: {
      Add,
      Modify,
      Detail,
      DownOutlined,
      SupplierImporter,
      BatchHandler,
      AvailableTag,
    },
    setup() {
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        ThunderboltOutlined,
        SettingOutlined,
        CheckOutlined,
        StopOutlined,
        DeleteOutlined,
        CloudUploadOutlined,
        DownloadOutlined,
        AVAILABLE,
      };
    },
    data() {
      return {
        loading: false,
        // 当前行数据
        id: '',
        ids: [],
        // 查询列表的查询条件
        searchFormData: {
          available: AVAILABLE.ENABLE.code,
        },
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
          { type: 'seq', width: 45 },
          { field: 'code', title: '编号', width: 100, sortable: true },
          { field: 'name', title: '名称', minWidth: 180, sortable: true },
          { field: 'available', title: '状态', width: 80, slots: { default: 'available_default' } },
          { field: 'description', title: '备注', minWidth: 200 },
          { field: 'createBy', title: '创建人', width: 100 },
          { field: 'createTime', title: '创建时间', width: 170, sortable: true },
          { field: 'updateBy', title: '修改人', width: 100 },
          { field: 'updateTime', title: '修改时间', width: 170, sortable: true },
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
        batchHandleDatas: [],
      };
    },
    created() {},
    methods: {
      // 列表发生查询时的事件
      search() {
        this.$refs.grid.commitProxy('reload');
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
      handleCommand({ key }) {
        if (key === 'batchEnable') {
          this.batchEnable();
        } else if (key === 'batchDisable') {
          this.batchDisable();
        } else if (key === 'batchDelete') {
          this.batchDelete();
        }
      },
      /**
       * 执行批量供应商状态更新请求。
       * @param records 选中的供应商记录
       * @param available 目标状态
       */
      doBatchAvailable(records, available) {
        return api.updateAvailable(buildSupplierAvailabilityRequest(records, available)).then(() => {
          this.search();
          this.$refs.grid.clearCheckboxRow();
        });
      },
      /**
       * 批量处理组件的单行回调占位。
       * @returns 已完成的 Promise
       */
      doBatchAvailableItem() {
        return Promise.resolve();
      },
      /**
       * 批量启用供应商。
       * @param records 选中的供应商记录
       */
      batchEnableHandle(records) {
        return this.doBatchAvailable(records, true);
      },
      /**
       * 批量停用供应商。
       * @param records 选中的供应商记录
       */
      batchDisableHandle(records) {
        return this.doBatchAvailable(records, false);
      },
      /**
       * 打开批量状态确认窗口。
       * @param available 目标状态
       */
      openBatchAvailableDialog(available) {
        const records = this.$refs.grid.getCheckboxRecords();
        const action = available ? '启用' : '停用';

        if (isEmpty(records)) {
          createError(`请选择要${action}的供应商！`);
          return;
        }

        this.batchHandleDatas = records;
        this.$refs[
          available ? 'batchEnableHandlerDialog' : 'batchDisableHandlerDialog'
        ].openDialog();
      },
      /**
       * 打开批量启用确认窗口。
       */
      batchEnable() {
        this.openBatchAvailableDialog(true);
      },
      /**
       * 打开批量停用确认窗口。
       */
      batchDisable() {
        this.openBatchAvailableDialog(false);
      },
      /**
       * 停用单个供应商。
       * @param row 供应商列表行数据
       */
      disableSupplier(row) {
        createConfirm(`确认停用供应商“${row.name}”？`).then(() => {
          api.updateAvailable(buildSupplierAvailabilityRequest([row], false)).then(() => {
            createSuccess('停用成功！');
            this.search();
          });
        });
      },
      doBatchDelete(row) {
        return api.deleteById(row.id);
      },
      footerMethod({ columns }) {
        return [
          columns.map((column) => {
            if (column.field === 'code') {
              return '合计';
            }
            return '';
          }),
        ];
      },
      // 批量删除
      batchDelete() {
        const records = this.$refs.grid.getCheckboxRecords();

        if (isEmpty(records)) {
          createError('请选择要删除的供应商！');
          return;
        }

        this.batchHandleDatas = records;

        this.$refs.batchDeleteHandlerDialog.openDialog();
      },
      exportList() {
        this.loading = true;
        api
          .exportList(this.buildQueryParams({}))
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      createActions(row) {
        return [
          {
            label: '查看',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.viewDialog.openDialog());
            },
          },
          ...(row.available === true
            ? [
                {
                  permission: ['base-data:supplier:modify'],
                  label: '停用',
                  danger: true,
                  onClick: () => this.disableSupplier(row),
                },
              ]
            : []),
          {
            permission: ['base-data:supplier:modify'],
            label: '修改',
            onClick: () => {
              this.id = row.id;
              this.$nextTick(() => this.$refs.updateDialog.openDialog());
            },
          },
        ];
      },
    },
  });
</script>
<style scoped></style>
