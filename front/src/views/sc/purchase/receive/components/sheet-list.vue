<template>
  <div>
    <div v-permission="['purchase:receive:query']">
      <page-wrapper content-full-height fixed-height dense>
        <!-- 数据列表 -->
        <vxe-grid
          id="ReceiveSheet"
          ref="grid"
          auto-resize
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
          :pager-config="pagerConfig"
          :footer-method="footerMethod"
          :loading="loading"
          :height="gridHeight || 'auto'"
        >
          <template #form>
            <j-border>
              <j-form bordered @collapse="handleFormCollapse" @keyup.enter="search">
                <j-form-item label="订单日期">
                  <a-range-picker
                    v-model:value="orderDateRange"
                    value-format="YYYY-MM-DD"
                    :placeholder="['开始日期', '结束日期']"
                  />
                </j-form-item>
                <j-form-item label="供应商">
                  <supplier-selector
                    v-model:value="searchFormData.supplierId"
                    allow-clear
                    placeholder="请选择供应商"
                  />
                </j-form-item>
                <j-form-item label="商品名称">
                  <a-input v-model:value="searchFormData.productName" allow-clear />
                </j-form-item>
                <template #more>
                  <j-form-item label="单据号">
                    <a-input v-model:value="searchFormData.code" allow-clear />
                  </j-form-item>
                  <j-form-item label="操作人">
                    <a-select
                      v-model:value="searchFormData.createBy"
                      allow-clear
                      show-search
                      :filter-option="filterSelectOption"
                      :options="createByOptions"
                      placeholder="请选择操作人"
                      @focus="loadCreateByOptions()"
                      @search="loadCreateByOptions"
                      @change="onCreateByChange"
                    />
                  </j-form-item>

                  <j-form-item label="审核人">
                    <a-select
                      v-model:value="searchFormData.approveBy"
                      allow-clear
                      show-search
                      :filter-option="filterSelectOption"
                      :options="approveByOptions"
                      placeholder="请选择审核人"
                      @focus="loadApproveByOptions()"
                      @search="loadApproveByOptions"
                      @change="onApproveByChange"
                    />
                  </j-form-item>

                  <j-form-item label="审核日期">
                    <a-range-picker
                      v-model:value="approveDateRange"
                      value-format="YYYY-MM-DD"
                      :placeholder="['开始日期', '结束日期']"
                    />
                  </j-form-item>

                  <j-form-item label="状态">
                    <a-select v-model:value="searchFormData.status" placeholder="全部" allow-clear>
                      <a-select-option
                        v-for="item in RECEIVE_SHEET_STATUS.values()"
                        :key="item.code"
                        :value="item.code"
                        >{{ item.desc }}</a-select-option
                      >
                    </a-select>
                  </j-form-item>

                  <j-form-item label="结算状态">
                    <a-select
                      v-model:value="searchFormData.settleStatus"
                      placeholder="全部"
                      allow-clear
                    >
                      <a-select-option
                        v-for="item in SETTLE_STATUS.values()"
                        :key="item.code"
                        :value="item.code"
                        >{{ item.desc }}</a-select-option
                      >
                    </a-select>
                  </j-form-item>

                  <j-form-item label="是否已结清">
                    <a-select
                      v-model:value="searchFormData.fullyPaid"
                      placeholder="全部"
                      allow-clear
                    >
                      <a-select-option :value="true">已结清</a-select-option>
                      <a-select-option :value="false">未结清</a-select-option>
                    </a-select>
                  </j-form-item>
                </template>
              </j-form>
            </j-border>
          </template>
          <!-- 工具栏 -->
          <template #toolbar_buttons>
            <a-space>
              <a-button @click="resetSearchForm">清空</a-button>
              <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
              <a-button
                v-permission="['purchase:receive:add']"
                type="primary"
                :icon="h(PlusOutlined)"
                @click="openAddDialog"
                >新增</a-button
              >
              <a-button
                v-permission="['purchase:receive:approve']"
                :icon="h(CheckOutlined)"
                @click="batchApprovePass"
                >审核通过</a-button
              >
              <a-button
                v-permission="['purchase:receive:approve']"
                :icon="h(CloseOutlined)"
                @click="batchApproveRefuse"
                >审核拒绝</a-button
              >
              <a-button
                v-permission="['purchase:receive:delete']"
                danger
                :icon="h(DeleteOutlined)"
                @click="batchDelete"
                >批量删除</a-button
              >
              <a-button
                v-permission="['purchase:receive:export']"
                :icon="h(CloudUploadOutlined)"
                @click="$refs.importer.openDialog()"
                >导入Excel</a-button
              >
              <a-button
                v-permission="['purchase:receive:export']"
                :icon="h(DownloadOutlined)"
                @click="exportList"
                >导出</a-button
              >
            </a-space>
          </template>

          <!-- 单据号 列自定义内容 -->
          <template #code_default="{ row }">
            <template v-if="canModifySheet(row)">
              <a v-permission="['purchase:receive:modify']" @click="openModifyDialog(row)">{{
                row.code
              }}</a>
              <span v-no-permission="['purchase:receive:modify']">{{ row.code }}</span>
            </template>
            <span v-else>{{ row.code }}</span>
          </template>

          <!-- 采购订单号 列自定义内容 -->
          <template #purchaseOrderCode_default="{ row }">
            <span v-if="isEmpty(row.purchaseOrderCode)">-</span>
            <span v-else>
              <a
                v-permission="['purchase:order:query']"
                @click="viewPurchaseOrderDetail(row.purchaseOrderId)"
                >{{ row.purchaseOrderCode }}</a
              >
              <span v-no-permission="['purchase:order:query']">{{ row.purchaseOrderCode }}</span>
            </span>
          </template>

          <!-- 操作 列自定义内容 -->
          <template #action_default="{ row }">
            <table-action outside :actions="createActions(row)" />
          </template>
        </vxe-grid>
      </page-wrapper>

      <!-- 查看窗口 -->
      <detail :id="id" ref="viewDialog" />

      <approve-refuse ref="approveRefuseDialog" @confirm="doApproveRefuse" />
      <receive-sheet-query-importer ref="importer" @confirm="handleImportSuccess" />

      <!-- 采购订单查看窗口 -->
      <purchase-order-detail :id="purchaseOrderId" ref="viewPurchaseOrderDetailDialog" />
    </div>
    <receive-sheet-pay-type-importer ref="importer2" />
    <a-modal
      v-model:open="descriptionModal.visible"
      title="修改备注"
      :confirm-loading="descriptionModal.loading"
      @ok="submitDescription"
      @cancel="closeDescriptionDialog"
    >
      <a-textarea
        v-model:value.trim="descriptionModal.description"
        maxlength="200"
        :rows="4"
        allow-clear
      />
    </a-modal>
    <!-- 批量操作 -->
    <batch-handler
      ref="batchApprovePassHandlerDialog"
      :table-column="[
        { field: 'code', title: '单据号', width: 180 },
        { field: 'supplierCode', title: '供应商编号', width: 100 },
        { field: 'supplierName', title: '供应商名称', width: 120 },
        { field: 'purchaserName', title: '采购员', width: 100 },
      ]"
      title="审核通过"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchApprovePass"
      @confirm="search"
    />
    <batch-handler
      ref="batchApproveRefuseHandlerDialog"
      :table-column="[
        { field: 'code', title: '单据号', width: 180 },
        { field: 'supplierCode', title: '供应商编号', width: 100 },
        { field: 'supplierName', title: '供应商名称', width: 120 },
        { field: 'purchaserName', title: '采购员', width: 100 },
      ]"
      title="审核拒绝"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchApproveRefuse"
      @confirm="search"
    />
    <batch-handler
      ref="batchDeleteHandlerDialog"
      :table-column="[
        { field: 'code', title: '单据号', width: 180 },
        { field: 'supplierCode', title: '供应商编号', width: 100 },
        { field: 'supplierName', title: '供应商名称', width: 120 },
        { field: 'purchaserName', title: '采购员', width: 100 },
      ]"
      title="批量删除"
      :tableData="batchHandleDatas"
      :handle-fn="doBatchDelete"
      @confirm="search"
    />
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import Detail from '../detail.vue';
  import ApproveRefuse from '@/components/ApproveRefuse';
  import PurchaseOrderDetail from '@/views/sc/purchase/order/detail.vue';
  import moment from 'moment';
  import {
    CheckOutlined,
    CloseOutlined,
    CloudUploadOutlined,
    DeleteOutlined,
    DownloadOutlined,
    PlusOutlined,
    SearchOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/purchase/receive';
  import * as configApi from '@/api/sc/purchase/config';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { isEmpty, buildSortPageVo } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestSupplierSelectOptions, requestUserSelectOptions } from '@/utils/labelSelect';
  import { createSuccess, createError, createConfirm } from '@/hooks/web/msg';
  import ReceiveSheetPayTypeImporter from '@/components/Importor/ReceiveSheetPayTypeImporter.vue';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { PURCHASE_ORDER_STATUS } from '@/enums/biz/purchaseOrderStatus';
  import BatchHandler from '@/components/BatchHandler';
  import ReceiveSheetQueryImporter from '@/components/Importor/PurchaseOrderQueryImporter.vue';
  import SupplierSelector from '@/components/Selector/SupplierSelector.vue';

  export default defineComponent({
    name: 'ReceiveSheetSheetList',
    components: {
      ReceiveSheetQueryImporter,
      Detail,
      ApproveRefuse,
      PurchaseOrderDetail,
      ReceiveSheetPayTypeImporter,
      BatchHandler,
      SupplierSelector,
    },
    mixins: [multiplePageMix, gridCollapseHeightMix],
    setup() {
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        CheckOutlined,
        CloseOutlined,
        DeleteOutlined,
        DownloadOutlined,
        isEmpty,
        RECEIVE_SHEET_STATUS,
        SETTLE_STATUS,
      };
    },
    data() {
      return {
        loading: false,
        // 当前行数据
        id: '',
        purchaseOrderId: '',
        // 查询列表的查询条件
        searchFormData: {
          code: '',
          productName: '',
          supplierId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          purchaser: '',
          purchaseOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
        },
        orderDateRange: this.getDefaultOrderDateRange(),
        approveDateRange: [],
        supplierOptions: [],
        supplierOptionMap: {},
        createByOptions: [],
        createByOptionMap: {},
        approveByOptions: [],
        approveByOptionMap: {},
        // 工具栏配置
        toolbarConfig: {
          // 自定义左侧工具栏
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        pagerConfig: {
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        },
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          {
            field: 'code',
            title: '单据号',
            width: 180,
            sortable: true,
            slots: { default: 'code_default' },
          },
          { field: 'supplierCode', title: '供应商编号', width: 100 },
          { field: 'supplierName', title: '供应商名称', width: 120 },
          { field: 'purchaserName', title: '采购员', width: 100 },
          { field: 'orderDate', title: '订单日期', width: 120 },
          { field: 'totalNum', title: '商品数量', align: 'right', width: 120 },
          { field: 'totalAmount', title: '单据总金额', align: 'right', width: 100 },
          { field: 'paidAmount', title: '本单已付', align: 'right', width: 100 },
          { field: 'unpaidAmount', title: '未付金额', align: 'right', width: 100 },
          { field: 'createTime', title: '操作时间', width: 170, sortable: true },
          { field: 'createBy', title: '操作人', width: 100 },
          // {
          //   field: 'status',
          //   title: '状态',
          //   width: 100,
          //   formatter: ({ cellValue }) => {
          //     return RECEIVE_SHEET_STATUS.getDesc(cellValue);
          //   },
          // },
          // { field: 'approveTime', title: '审核时间', width: 170, sortable: true },
          // { field: 'approveBy', title: '审核人', width: 100 },
          {
            field: 'settleStatus',
            title: '结算状态',
            width: 100,
            formatter: ({ cellValue }) => {
              return SETTLE_STATUS.getDesc(cellValue);
            },
          },
          { field: 'description', title: '备注', width: 200 },
          { title: '操作', width: 350, fixed: 'right', slots: { default: 'action_default' } },
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
        batchRefuseReason: '',
        descriptionModal: {
          visible: false,
          loading: false,
          id: '',
          description: '',
        },
      };
    },
    created() {},
    methods: {
      CloudUploadOutlined,
      footerMethod({ columns, data }) {
        const totalAmount = this.sumByField(data, 'totalAmount');
        const totalNum = this.sumByField(data, 'totalNum');

        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }

            if (column.field === 'totalAmount') {
              return this.formatAmount(totalAmount);
            }

            if (column.field === 'totalNum') {
              return this.formatQuantity(totalNum);
            }

            return '';
          }),
        ];
      },
      sumByField(data, field) {
        return (data || []).reduce((total, item) => {
          const value = Number(item?.[field] ?? 0);
          return total + (Number.isNaN(value) ? 0 : value);
        }, 0);
      },
      formatAmount(value) {
        return this.toFixedNumber(value, 2);
      },
      formatQuantity(value) {
        return this.toFixedNumber(value, 2, true);
      },
      toFixedNumber(value, digits = 2, trimZero = false) {
        const text = Number(value || 0).toFixed(digits);
        return trimZero ? text.replace(/\.?0+$/, '') : text;
      },
      // 列表发生查询时的事件
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      getDefaultOrderDateRange() {
        return [
          moment().startOf('month').format('YYYY-MM-DD'),
          moment().add(2, 'd').format('YYYY-MM-DD'),
        ];
      },
      resetSearchForm() {
        this.searchFormData = {
          code: '',
          productName: '',
          supplierId: undefined,
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          purchaser: '',
          purchaseOrderCode: '',
          settleStatus: undefined,
          fullyPaid: undefined,
        };
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.approveDateRange = [];
        this.search();
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
        const params = Object.assign({}, this.searchFormData, {
          supplierId: this.searchFormData.supplierId,
          createBy: this.searchFormData.createBy,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
          approveBy: this.searchFormData.approveBy,
          approveStartTime: this.approveDateRange?.[0]
            ? `${this.approveDateRange[0]} 00:00:00`
            : '',
          approveEndTime: this.approveDateRange?.[1] ? `${this.approveDateRange[1]} 23:59:59` : '',
          purchaserId: this.searchFormData.purchaser,
        });

        return params;
      },
      filterSelectOption(input, option) {
        return filterSelectOption(input, option);
      },
      async updateSelectOptions(keyword, requestFn, optionMapKey, optionsKey, selectedValueKey) {
        const options = await requestFn(keyword);
        const optionMap = mergeSelectOptionMap(this[optionMapKey], options);

        this[optionMapKey] = optionMap;
        this[optionsKey] = buildVisibleSelectOptions(
          this.searchFormData[selectedValueKey],
          optionMap,
          options,
        );
      },
      async requestSupplierOptions(keyword = '') {
        return requestSupplierSelectOptions(keyword);
      },
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
      },
      async loadSupplierOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestSupplierOptions,
          'supplierOptionMap',
          'supplierOptions',
          'supplierId',
        );
      },
      async loadCreateByOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestUserOptions,
          'createByOptionMap',
          'createByOptions',
          'createBy',
        );
      },
      async loadApproveByOptions(keyword = '') {
        await this.updateSelectOptions(
          keyword,
          this.requestUserOptions,
          'approveByOptionMap',
          'approveByOptions',
          'approveBy',
        );
      },
      normalizeSelectValue(value, optionMap) {
        return normalizeSelectValue(value, optionMap);
      },
      onSupplierChange(value) {
        this.searchFormData.supplierId = this.normalizeSelectValue(value, this.supplierOptionMap);
      },
      onCreateByChange(value) {
        this.searchFormData.createBy = this.normalizeSelectValue(value, this.createByOptionMap);
      },
      onApproveByChange(value) {
        this.searchFormData.approveBy = this.normalizeSelectValue(value, this.approveByOptionMap);
      },
      openAddDialog() {
        configApi.get().then((res) => {
          if (res.receiveRequirePurchase) {
            this.openChildPage('/purchase/receive/add/require');
          } else {
            this.openChildPage('/purchase/receive/add/un-require');
          }
        });
      },
      openModifyDialog(row) {
        if (!isEmpty(row.purchaseOrderId)) {
          this.openChildPage('/purchase/receive/modify/require/' + row.id);
        } else {
          this.openChildPage('/purchase/receive/modify/un-require/' + row.id);
        }
      },
      canModifySheet(row) {
        return (
          (RECEIVE_SHEET_STATUS.CREATED.equalsCode(row.status) ||
            RECEIVE_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)) &&
          SETTLE_STATUS.UN_CHECK_BILL.equalsCode(row.settleStatus)
        );
      },
      openDescriptionDialog(row) {
        this.descriptionModal = {
          visible: true,
          loading: false,
          id: row.id,
          description: row.description || '',
        };
      },
      closeDescriptionDialog() {
        this.descriptionModal.visible = false;
        this.descriptionModal.loading = false;
      },
      submitDescription() {
        this.descriptionModal.loading = true;
        api
          .updateDescription({
            id: this.descriptionModal.id,
            description: this.descriptionModal.description,
          })
          .then(() => {
            createSuccess('保存成功！');
            this.closeDescriptionDialog();
            this.search();
          })
          .finally(() => {
            this.descriptionModal.loading = false;
          });
      },
      // 删除订单
      deleteOrder(row) {
        createConfirm('对选中的采购收货单执行删除操作？').then(() => {
          this.loading = true;
          api
            .deleteById(row.id)
            .then(() => {
              createSuccess('删除成功！');
              this.search();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      doBatchDelete(row) {
        return api.batchDelete(row.id);
      },
      // 批量删除
      batchDelete() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的采购收货单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个采购收货单已审核通过，不允许执行删除操作！');
            return;
          }
        }

        this.batchHandleDatas = records;

        this.$refs.batchDeleteHandlerDialog.openDialog();
      },
      doBatchApprovePass(row) {
        return api.batchApprovePass({
          id: row.id,
        });
      },
      // 批量审核通过
      batchApprovePass() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的采购收货单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个采购单已审核通过，不允许继续执行审核！');
            return;
          }
        }

        this.batchHandleDatas = records;

        this.$refs.batchApprovePassHandlerDialog.openDialog();
      },
      // 批量审核拒绝
      batchApproveRefuse() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的采购收货单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (PURCHASE_ORDER_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个采购收货单已审核通过，不允许继续执行审核！');
            return;
          }

          if (PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个采购收货单已审核拒绝，不允许继续执行审核！');
            return;
          }
        }

        this.$refs.approveRefuseDialog.openDialog();
      },
      doBatchApproveRefuse(row) {
        return api.batchApproveRefuse({
          id: row.id,
          refuseReason: this.batchRefuseReason,
        });
      },
      doApproveRefuse(reason) {
        this.batchHandleDatas = this.$refs.grid.getCheckboxRecords();

        this.batchRefuseReason = reason;

        this.$refs.batchApproveRefuseHandlerDialog.openDialog();
      },
      handleImportSuccess(res) {
        const ids = res?.data || res?.datas || res || [];
        const count = Array.isArray(ids) ? ids.length : 0;
        createSuccess('导入成功，已创建' + count + '张采购订单！');
        this.search();
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
      exportDetails(row) {
        this.loading = true;
        api
          .exportDetail({
            pageIndex: 1,
            pageSize: 2147483647,
            idList: [row.id],
          })
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      viewPurchaseOrderDetail(id) {
        this.purchaseOrderId = id;
        this.$refs.viewPurchaseOrderDetailDialog.openDialog();
      },
      viewDetail(id) {
        this.id = id;
        this.$nextTick(() => this.$refs.viewDialog.openDialog());
      },
      createActions(row) {
        return [
          {
            label: '查看',
            onClick: () => {
              this.viewDetail(row.id);
            },
          },
          {
            label: '导出明细',
            onClick: () => {
              this.exportDetails(row);
            },
          },
          {
            permission: ['purchase:receive:approve'],
            label: '审核',
            ifShow: () => {
              return (
                PURCHASE_ORDER_STATUS.CREATED.equalsCode(row.status) ||
                PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.openChildPage('/purchase/receive/approve/' + row.id);
            },
          },
          {
            permission: ['purchase:receive:modify'],
            label: '修改',
            ifShow: () => this.canModifySheet(row),
            onClick: () => {
              this.openModifyDialog(row);
            },
          },
          {
            permission: ['purchase:receive:modify'],
            label: '修改备注',
            onClick: () => {
              this.openDescriptionDialog(row);
            },
          },
          {
            permission: ['purchase:receive:delete'],
            label: '删除',
            danger: true,
            ifShow: () => {
              return (
                PURCHASE_ORDER_STATUS.CREATED.equalsCode(row.status) ||
                PURCHASE_ORDER_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.deleteOrder(row);
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
