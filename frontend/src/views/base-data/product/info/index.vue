<template>
  <div>
    <div v-show="visible" v-permission="['base-data:product:info:query']">
      <page-wrapper content-full-height fixed-height>
        <!-- 数据列表 -->
        <vxe-grid
          id="ProductInfo"
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
          :pager-config="{
            layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
          }"
          :loading="loading"
          height="auto"
        >
          <template #form>
            <j-border>
              <j-form bordered @collapse="$refs.grid.refreshColumn()">
                <j-form-item label="名称">
                  <a-input v-model:value="searchFormData.name" allow-clear @press-enter="search" />
                </j-form-item>
                <j-form-item label="编号">
                  <a-input v-model:value="searchFormData.code" allow-clear @press-enter="search" />
                </j-form-item>
                <j-form-item label="商品分类">
                  <product-category-selector
                    v-model:value="searchFormData.categoryId"
                    :only-final="false"
                  />
                </j-form-item>
                <j-form-item label="商品品牌">
                  <product-brand-selector v-model:value="searchFormData.brandId" />
                </j-form-item>
                <j-form-item label="询价商品">
                  <a-select v-model:value="searchFormData.inquiryProduct" allow-clear>
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option :value="true">是</a-select-option>
                    <a-select-option :value="false">否</a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="状态">
                  <a-select v-model:value="searchFormData.available">
                    <a-select-option :value="AVAILABLE.ENABLE.code">启用</a-select-option>
                    <a-select-option :value="AVAILABLE.UNABLE.code">禁用</a-select-option>
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
                v-permission="['base-data:product:info:add']"
                type="primary"
                :icon="h(PlusOutlined)"
                @click="openChildPage('/product/info/add')"
                >新增</a-button
              >
              <a-button
                v-permission="['base-data:product:info:import']"
                :icon="h(CloudUploadOutlined)"
                @click="$refs.importer.openDialog()"
                >导入Excel</a-button
              >
              <a-button
                v-permission="['base-data:product:info:import']"
                :icon="h(DownloadOutlined)"
                @click="exportList"
                >导出</a-button
              >
              <a-dropdown>
                <template #overlay>
                  <a-menu @click="handleCommand">
                    <a-menu-item
                      v-permission="['base-data:product:info:modify']"
                      key="batchEnable"
                      :icon="h(CheckOutlined)"
                    >
                      批量启用
                    </a-menu-item>
                    <a-menu-item
                      v-permission="['base-data:product:info:modify']"
                      key="batchDisable"
                      :icon="h(StopOutlined)"
                    >
                      批量禁用
                    </a-menu-item>
                    <a-menu-item
                      v-permission="['base-data:product:info:delete']"
                      key="batchDelete"
                      :icon="h(DeleteOutlined)"
                    >
                      批量删除
                    </a-menu-item>
                  </a-menu>
                </template>
                <a-button
                  v-permission="['base-data:product:info:modify', 'base-data:product:info:delete']"
                >
                  更多<DownOutlined />
                </a-button>
              </a-dropdown>
            </a-space>
          </template>

          <!-- 操作 列自定义内容 -->
          <template #action_default="{ row }">
            <table-action outside :actions="createActions(row)" />
          </template>

          <!-- 状态列自定义内容 -->
          <template #available_default="{ row }">
            <available-tag :available="row.available" />
          </template>
        </vxe-grid>
      </page-wrapper>

      <!-- 查看窗口 -->
      <detail :id="id" ref="viewDialog" />
    </div>

    <product-importer ref="importer" @confirm="search" />

    <!-- 批量操作 -->
    <batch-handler
      ref="batchDeleteHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 120 },
        { field: 'name', title: '名称', minWidth: 160 },
      ]"
      title="批量删除"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchDelete"
      @confirm="search"
    />

    <batch-handler
      ref="batchEnableHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 120 },
        { field: 'name', title: '名称', minWidth: 160 },
      ]"
      title="批量启用"
      :table-data="batchHandleDatas"
      :handle-fn="doBatchAvailableItem"
      :batch-handle-fn="batchEnableHandle"
    />

    <batch-handler
      ref="batchDisableHandlerDialog"
      :table-column="[
        { field: 'code', title: '编号', width: 120 },
        { field: 'name', title: '名称', minWidth: 160 },
      ]"
      title="批量禁用"
      :table-data="batchHandleDatas"
      :handle-fn="doBatchAvailableItem"
      :batch-handle-fn="batchDisableHandle"
    />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import Detail from './detail.vue';
  import * as api from '@/api/base-data/product/info';
  import {
    CloudUploadOutlined,
    DownloadOutlined,
    DeleteOutlined,
    CheckOutlined,
    DownOutlined,
    PlusOutlined,
    SearchOutlined,
    StopOutlined,
  } from '@ant-design/icons-vue';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { buildSortPageVo, isEmpty, isEqualWithStr } from '@/utils/utils';
  import ProductImporter from '@/components/Importor/ProductImporter.vue';
  import ProductBrandSelector from '@/components/Selector/ProductBrandSelector.vue';
  import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';
  import { PRODUCT_TYPE } from '@/enums/biz/productType';
  import { AVAILABLE } from '@/enums/biz/available';
  import AvailableTag from '@/components/Tag/AvailableTag.vue';
  import { buildProductAvailabilityRequest } from './productAvailability';
  import BatchHandler from '@/components/BatchHandler';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import PageWrapper from '@/components/Page/src/PageWrapper.vue';
  import JFormItem from '@/components/JFormItem';
  import JBorder from '@/components/JBorder';
  import JForm from '@/components/JForm';
  import { TableAction } from '@/components/Table';

  export default defineComponent({
    name: 'ProductInfo',
    components: {
      TableAction,
      JForm,
      JBorder,
      JFormItem,
      PageWrapper,
      BatchHandler,
      AvailableTag,
      DownOutlined,
      Detail,
      ProductImporter,
      ProductBrandSelector,
      ProductCategorySelector,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        CloudUploadOutlined,
        DownloadOutlined,
        PlusOutlined,
        SearchOutlined,
        PRODUCT_TYPE,
        DeleteOutlined,
        CheckOutlined,
        StopOutlined,
        AVAILABLE,
      };
    },
    data() {
      return {
        loading: false,
        visible: true,
        // 当前行数据
        id: '',
        ids: [],
        // 查询列表的查询条件
        searchFormData: {
          code: '',
          name: '',
          skuCode: '',
          categoryId: '',
          brandId: '',
          inquiryProduct: '',
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
          { field: 'id', title: 'ID', width: 180, sortable: true },
          { field: 'code', title: '编号', width: 120, sortable: true },
          { field: 'name', title: '名称', minWidth: 160, sortable: true },
          { field: 'alias', title: '别名', minWidth: 180 },
          { field: 'categoryName', title: '分类', width: 120 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'unit', title: '单位', width: 100 },
          {
            field: 'inquiryProduct',
            title: '询价商品',
            width: 100,
            formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
          },
          { field: 'available', title: '状态', width: 80, slots: { default: 'available_default' } },
          { field: 'purchasePrice', title: '采购价', width: 120 },
          { field: 'latestPurchasePrice', title: '最新采购价', width: 120 },
          { field: 'salePrice', title: '销售价', width: 120 },
          { field: 'latestSalePrice', title: '最新售价', width: 120 },
          { field: 'remark', title: '备注', width: 180 },
          { field: 'remark2', title: '备注二', width: 180 },
          { field: 'defaultSupplierName', title: '默认供应商', minWidth: 160 },
          { field: 'brandName', title: '品牌', minWidth: 120 },
          { field: 'createTime', title: '创建时间', width: 170, sortable: true },
          { field: 'updateTime', title: '修改时间', width: 170, sortable: true },
          { title: '操作', minWidth: 250, fixed: 'right', slots: { default: 'action_default' } },
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
        // 兼容“全部分类”的语义：如果回传 0 / '0'，则不作为筛选条件下发
        if (isEqualWithStr(0, this.searchFormData.categoryId)) {
          this.searchFormData.categoryId = '';
        }

        return {
          ...this.searchFormData,
        };
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
       * 执行批量状态更新请求。
       * @param records 选中的商品记录
       * @param available 目标状态
       */
      doBatchAvailable(records, available) {
        return api.updateAvailable(buildProductAvailabilityRequest(records, available)).then(() => {
          this.search();
          this.$refs.grid.clearCheckboxRow();
        });
      },
      /**
       * 批量状态处理组件的单行回调占位。
       * @returns 已完成的 Promise
       */
      doBatchAvailableItem() {
        return Promise.resolve();
      },
      /**
       * 批量启用选中商品。
       * @param records 选中的商品记录
       */
      batchEnableHandle(records) {
        return this.doBatchAvailable(records, true);
      },
      /**
       * 批量禁用选中商品。
       * @param records 选中的商品记录
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
        const action = available ? '启用' : '禁用';

        if (isEmpty(records)) {
          createError(`请选择要${action}的商品！`);
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
       * 打开批量禁用确认窗口。
       */
      batchDisable() {
        this.openBatchAvailableDialog(false);
      },
      doBatchDelete(row) {
        return api.deleteById(row.id);
      },
      // 批量删除
      batchDelete() {
        const records = this.$refs.grid.getCheckboxRecords();

        if (isEmpty(records)) {
          createError('请选择要删除的商品！');
          return;
        }

        this.batchHandleDatas = records;

        this.$refs.batchDeleteHandlerDialog.openDialog();
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
          {
            permission: ['base-data:product:info:add'],
            label: '新增',
            onClick: () => this.openChildPage('/product/info/add'),
          },
          {
            permission: ['base-data:product:info:modify'],
            label: '修改',
            onClick: () => {
              this.openChildPage('/product/info/modify/' + row.id);
            },
          },
          {
            permission: ['base-data:product:info:delete'],
            label: '删除',
            danger: true,
            onClick: () => {
              createConfirm(`确认删除商品“${row.name}”？`).then(() => {
                api.deleteById(row.id).then(() => {
                  createSuccess('删除成功！');
                  this.search();
                });
              });
            },
          },
        ];
      },
      onRefreshPage() {
        this.search();
      },
    },
  });
</script>
<style scoped></style>
