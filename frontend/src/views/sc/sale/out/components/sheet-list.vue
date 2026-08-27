<template>
  <div ref="importerContainer" class="excel-importer-local-container">
    <div v-permission="['sale:out:query']">
      <page-wrapper content-full-height fixed-height>
        <!-- 数据列表 -->
        <vxe-grid
          id="SaleOutSheet"
          ref="grid"
          auto-resize
          resizable
          show-overflow
          show-footer
          highlight-hover-row
          keep-source
          row-id="id"
          :proxy-config="proxyConfig"
          :columns="visibleTableColumn"
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
                <j-form-item label="计划日期">
                  <a-range-picker
                    v-model:value="planDateRange"
                    value-format="YYYY-MM-DD"
                    :placeholder="['开始日期', '结束日期']"
                  />
                </j-form-item>
                <j-form-item label="商品名称">
                  <a-input v-model:value="searchFormData.productName" allow-clear />
                </j-form-item>
                <j-form-item label="客户">
                  <customer-selector
                    v-model:value="searchFormData.customerIdList"
                    multiple
                    show-description-filter
                    placeholder="请选择客户"
                  />
                </j-form-item>
                <j-form-item label="单据号">
                  <a-input v-model:value="searchFormData.code" allow-clear />
                </j-form-item>
                <j-form-item label="备注">
                  <a-input v-model:value="searchFormData.sheetDescription" allow-clear />
                </j-form-item>
                <j-form-item label="成本状态">
                  <a-select
                    v-model:value="searchFormData.fillAllCost"
                    placeholder="全部"
                    allow-clear
                  >
                    <a-select-option :value="true">已补全</a-select-option>
                    <a-select-option :value="false">未补全</a-select-option>
                  </a-select>
                </j-form-item>
                <j-form-item label="是否已送货">
                  <a-select v-model:value="searchFormData.delivered" placeholder="全部" allow-clear>
                    <a-select-option :value="true">已送货</a-select-option>
                    <a-select-option :value="false">未送货</a-select-option>
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
                    >
                      {{ item.desc }}
                    </a-select-option>
                  </a-select>
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

                <j-form-item label="已付金额">
                  <a-space class="amount-range-input" :size="4">
                    <a-input-number
                      v-model:value="searchFormData.paidAmountStart"
                      :min="0"
                      :precision="2"
                      placeholder="最小值"
                    />
                    <span>至</span>
                    <a-input-number
                      v-model:value="searchFormData.paidAmountEnd"
                      :min="0"
                      :precision="2"
                      placeholder="最大值"
                    />
                  </a-space>
                </j-form-item>

                <j-form-item label="未付金额">
                  <a-space class="amount-range-input" :size="4">
                    <a-input-number
                      v-model:value="searchFormData.unpaidAmountStart"
                      :min="0"
                      :precision="2"
                      placeholder="最小值"
                    />
                    <span>至</span>
                    <a-input-number
                      v-model:value="searchFormData.unpaidAmountEnd"
                      :min="0"
                      :precision="2"
                      placeholder="最大值"
                    />
                  </a-space>
                </j-form-item>
              </j-form>
            </j-border>
          </template>
          <!-- 工具栏 -->
          <template #toolbar_buttons>
            <a-space>
              <a-button @click="resetSearchForm">清空</a-button>
              <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
              <a-button
                v-permission="['sale:out:add']"
                type="primary"
                :icon="h(PlusOutlined)"
                @click="openAddDialog"
                >新增</a-button
              >
              <a-button
                v-permission="['sale:out:add']"
                :icon="h(CloudUploadOutlined)"
                @click="$refs.importer.openDialog()"
                >导入Excel</a-button
              >
              <a-button
                v-permission="['sale:out:export']"
                :icon="h(DownloadOutlined)"
                @click="exportList"
                >导出</a-button
              >
              <a-button
                v-permission="['wenshan:sale:out:saleexport']"
                :icon="h(DownloadOutlined)"
                @click="exportSales"
                >销售单导出</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(DownloadOutlined)"
                @click="marketBuySummary"
                >买菜汇总</a-button
              >
              <a-button
                v-permission="['report:sale-profit:approve']"
                :icon="h(SyncOutlined)"
                @click="openCostRecalculate"
                >重算成本</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(PrinterOutlined)"
                @click="batchPrint"
                >批量打印</a-button
              >
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(PrinterOutlined)"
                @click="tagPrint"
                >标签打印</a-button
              >
              <a-dropdown>
                <template #overlay>
                  <a-menu @click="handleMoreCommand">
                    <a-menu-item
                      v-if="hasPermission('sale:out:delete', false)"
                      key="batchDelete"
                      :icon="h(DeleteOutlined)"
                      class="danger-menu-item"
                    >
                      批量删除
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:export', false)"
                      key="batchExportDetails"
                      :icon="h(DownloadOutlined)"
                    >
                      批量导出明细
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:query', false)"
                      key="marketBuySummary2"
                      :icon="h(DownloadOutlined)"
                    >
                      买菜汇总-按客户
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:modify', false)"
                      key="mergeOrders"
                      :icon="h(MergeCellsOutlined)"
                    >
                      合并订单
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:modify', false)"
                      key="batchDelivery"
                      :icon="h(CheckOutlined)"
                    >
                      确认送货
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:modify', false)"
                      key="updateDescription"
                      :icon="h(ContainerOutlined)"
                    >
                      更新备注
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:approve', false)"
                      key="openInquiryPriceSync"
                      :icon="h(SyncOutlined)"
                    >
                      同步询价到销售表
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:approve', false)"
                      key="batchApprovePass"
                      :icon="h(CheckOutlined)"
                    >
                      审核通过
                    </a-menu-item>
                    <a-menu-item
                      v-if="hasPermission('sale:out:approve', false)"
                      key="batchApproveRefuse"
                      :icon="h(CloseOutlined)"
                    >
                      审核拒绝
                    </a-menu-item>
                  </a-menu>
                </template>
                <a-button size="middle" class="toolbar-more-button">更多<DownOutlined /></a-button>
              </a-dropdown>
            </a-space>
          </template>

          <!-- 单据号 列自定义内容 -->
          <template #code_default="{ row }">
            <a
              v-if="hasPermission('sale:out:modify', false) && !isSettleLocked(row)"
              @click="openModifyDialog(row)"
            >
              {{ row.code }}
            </a>
            <span v-else>{{ row.code }}</span>
          </template>

          <!-- 总利润 列自定义内容 -->
          <template #total_profit="{ row }">
            <span v-if="isEmpty(row.totalProfit)">-</span>
            <span v-else>
              {{ Number(row.totalProfit || 0).toFixed(2) }}
            </span>
          </template>

          <template #profit_rate="{ row }">
            {{ calcProfitRate(row.totalAmount, row.confirmAmt, row.totalCost) }}
          </template>

          <template #fillAllCost_default="{ row }">
            <span :style="{ color: row.fillAllCost ? '#52c41a' : '#f5222d' }">
              {{ row.fillAllCost ? '已补全' : '未补全' }}
            </span>
          </template>

          <!-- 操作 列自定义内容 -->
          <template #action_default="{ row }">
            <table-action
              outside
              :actions="createActions(row)"
              :drop-down-actions="createMoreActions(row)"
            >
              <template #more>
                <a-button type="link" size="small">更多<DownOutlined /></a-button>
              </template>
            </table-action>
          </template>
        </vxe-grid>
      </page-wrapper>

      <!-- 查看窗口 -->
      <detail :id="id" ref="viewDialog" />

      <approve-refuse ref="approveRefuseDialog" @confirm="doApproveRefuse" />
      <sale-out-sheet-query-importer
        ref="importer"
        :get-container="getImporterContainer"
        local-container
        hide-on-deactivated
        @confirm="handleImportSuccess"
      />

      <!-- 销售订单查看窗口 -->
      <sale-order-detail :id="saleOrderId" ref="viewSaleOrderDetailDialog" />

      <a-modal
        v-model:open="descriptionModal.visible"
        :title="descriptionModal.ids.length ? '批量更新备注' : '修改备注'"
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

      <a-modal
        v-model:open="inquiryPriceSyncModal.visible"
        title="同步询价到销售表"
        ok-text="同步"
        :confirm-loading="inquiryPriceSyncModal.loading"
        @ok="submitInquiryPriceSync"
        @cancel="closeInquiryPriceSync"
      >
        <a-form layout="vertical">
          <a-form-item label="销售出库订单日期" required>
            <a-range-picker
              v-model:value="inquiryPriceSyncModal.dateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 批量操作 -->
      <batch-handler
        ref="batchApprovePassHandlerDialog"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          { field: 'customerName', title: '客户名称', width: 120 },
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
          { field: 'customerName', title: '客户名称', width: 120 },
        ]"
        title="审核拒绝"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchApproveRefuse"
        @confirm="search"
      />
      <batch-handler
        ref="batchDeleteHandlerDialog"
        :concurrency="1"
        :table-column="[
          { field: 'code', title: '单据号', width: 180 },
          { field: 'customerName', title: '客户名称', width: 120 },
        ]"
        title="批量删除"
        :tableData="batchHandleDatas"
        :handle-fn="doBatchDelete"
        @confirm="search"
      />
      <order-print-dialog />

      <!-- 浏览器打印模板选择弹窗 -->
      <a-modal
        v-model:open="browserPrintModal.visible"
        title="浏览器打印"
        ok-text="确认"
        :confirm-loading="browserPrintModal.loading"
        @ok="confirmBrowserPrint"
        @cancel="closeBrowserPrintDialog"
      >
        <a-form layout="vertical">
          <a-form-item label="打印模板" required>
            <a-select v-model:value="browserPrintModal.templateId" placeholder="请选择打印模板">
              <a-select-option
                v-for="item in browserPrintModal.templateList"
                :key="item.id"
                :value="item.id"
              >
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 标签打印分类选择弹窗 -->
      <a-modal
        v-model:open="tagPrintModal.visible"
        title="标签打印 - 选择商品分类"
        :confirm-loading="tagPrintModal.loading"
        @ok="doTagPrintWithCategory"
        @cancel="closeTagPrintModal"
      >
        <a-checkbox
          v-if="tagPrintModal.treeShow"
          :checked="isAllTagPrintCategoryChecked()"
          :indeterminate="isTagPrintCategoryCheckIndeterminate()"
          @change="toggleAllTagPrintCategories"
        >
          全选
        </a-checkbox>
        <a-tree
          v-if="tagPrintModal.treeShow"
          v-model:checkedKeys="tagPrintModal.checkedCategoryIds"
          checkable
          default-expand-all
          :tree-data="tagPrintModal.categoryTreeData"
          :field-names="{ children: 'children', title: 'name', key: 'id' }"
        />
      </a-modal>

      <!-- 买菜汇总选项弹窗 -->
      <a-modal
        v-model:open="marketBuySummaryModal.visible"
        title="买菜汇总"
        :confirm-loading="marketBuySummaryModal.loading"
        @ok="confirmMarketBuySummary"
        @cancel="closeMarketBuySummaryModal"
      >
        <a-checkbox v-model:checked="marketBuySummaryModal.groupByDate">
          是否按日期汇总
        </a-checkbox>
        <a-checkbox v-model:checked="marketBuySummaryModal.mergeSameDayCustomerProduct">
          同一天、同一客户商品合并
        </a-checkbox>
      </a-modal>

      <!-- 月底成本重算弹窗 -->
      <a-modal v-model:open="costRefreshVisible" title="月底成本重算" @ok="recalculate">
        <a-form layout="vertical">
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="costRefreshDateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 成本重算进度遮罩 -->
      <div v-if="recalculating" class="recalc-overlay">
        <div class="recalc-overlay-inner">
          <a-spin v-if="!recalcFailedDate" size="large" />
          <div class="recalc-tip">{{ recalcLoadingTip }}</div>
          <a-progress
            v-if="!recalcFailedDate"
            :percent="recalcProgressPercent"
            status="active"
            class="recalc-progress"
          />
          <div v-if="recalcFailedDate" class="recalc-error">
            <a-alert type="error" :message="recalcErrorMsg" show-icon style="margin-bottom: 16px" />
            <a-space>
              <a-button type="primary" @click="retryRecalculate">从失败日期重试</a-button>
              <a-button @click="cancelRecalculate">取消</a-button>
            </a-space>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { defineComponent, h } from 'vue';
  import Detail from '../detail.vue';
  import ApproveRefuse from '@/components/ApproveRefuse';
  import SaleOrderDetail from '@/views/sc/sale/order/detail.vue';
  import moment from 'moment';
  import {
    CheckOutlined,
    CloseOutlined,
    CloudUploadOutlined,
    ContainerOutlined,
    DeleteOutlined,
    DownloadOutlined,
    DownOutlined,
    MergeCellsOutlined,
    PlusOutlined,
    PrinterOutlined,
    SearchOutlined,
    SyncOutlined,
  } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/sale/out';
  import * as configApi from '@/api/sc/sale/config';
  import * as categoryApi from '@/api/base-data/product/category';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import { gridCollapseHeightMix } from '@/mixins/gridCollapseHeightMix';
  import { printMix } from '@/mixins/print.ts';
  import { buildSortPageVo, isEmpty, toArrayTree } from '@/utils/utils';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
    normalizeSelectValue,
  } from '@/utils/searchSelect';
  import { requestUserSelectOptions } from '@/utils/labelSelect';
  import CustomerSelector from '@/components/Selector/CustomerSelector.vue';
  import {
    createConfirm,
    createError,
    createSuccess,
    createSuccessAutoClose,
  } from '@/hooks/web/msg';
  import { RECEIVE_SHEET_STATUS } from '@/enums/biz/receiveSheetStatus';
  import { SETTLE_STATUS } from '@/enums/biz/settleStatus';
  import { SALE_OUT_SHEET_STATUS } from '@/enums/biz/saleOutSheetStatus';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import BatchHandler from '@/components/BatchHandler';
  import PrintDialog from '/@/components/PrintDialog';
  import SaleOutSheetQueryImporter from '@/components/Importor/SaleOutSheetQueryImporter.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import {
    buildMarketBuySummary2Params,
    buildMarketBuySummaryParams,
  } from './saleOutMarketBuySummary';
  import { calcSaleOutProfitRateByCost } from './saleOutProfit';
  import { costRecalculateMixin } from '@/mixins/costRecalculateMixin';

  /** 是否显示行内“更多”菜单中的打印操作，当前固定隐藏。 */
  const SHOW_MORE_PRINT_ACTION = false;

  export default defineComponent({
    name: 'SaleOutSheetSheetList',
    components: {
      Detail,
      ApproveRefuse,
      SaleOrderDetail,
      BatchHandler,
      CustomerSelector,
      OrderPrintDialog: PrintDialog,
      SaleOutSheetQueryImporter,
    },
    mixins: [multiplePageMix, printMix, gridCollapseHeightMix, costRecalculateMixin],
    setup() {
      const { hasPermission } = usePermission();
      return {
        h,
        SearchOutlined,
        PlusOutlined,
        CheckOutlined,
        ContainerOutlined,
        CloseOutlined,
        CloudUploadOutlined,
        DeleteOutlined,
        DownloadOutlined,
        DownOutlined,
        MergeCellsOutlined,
        PrinterOutlined,
        SyncOutlined,
        isEmpty,
        hasPermission,
        RECEIVE_SHEET_STATUS,
        SETTLE_STATUS,
      };
    },
    data() {
      return {
        loading: false,
        // 买菜汇总导出选项
        marketBuySummaryModal: {
          visible: false,
          loading: false,
          groupByDate: false,
          mergeSameDayCustomerProduct: false,
          pendingRecords: [],
        },
        // 当前行数据
        id: '',
        saleOrderId: '',
        // 查询列表的查询条件
        searchFormData: {
          code: '',
          sheetDescription: '',
          productName: '',
          scId: '',
          customerIdList: [],
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
          fillAllCost: undefined,
          delivered: undefined,
          paidAmountStart: undefined,
          paidAmountEnd: undefined,
          unpaidAmountStart: undefined,
          unpaidAmountEnd: undefined,
        },
        orderDateRange: this.getDefaultOrderDateRange(),
        planDateRange: [],
        approveDateRange: [],
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
          pageSize: 50,
          pageSizes: [20, 50, 100, 200],
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        },
        // 列表数据配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'orderDate', title: '订单日期', width: 120, sortable: true },
          {
            field: 'code',
            title: '单据号',
            width: 180,
            sortable: true,
            slots: { default: 'code_default' },
          },
          { field: 'customerName', title: '客户名称', width: 120, sortable: true },
          { field: 'customerDescription', title: '客户备注', width: 200, sortable: true },
          { field: 'totalAmount', title: '单据总金额', align: 'right', width: 100 },
          {
            field: 'confirmAmt',
            title: '验收金额',
            align: 'right',
            width: 100,
            sortable: true,
            formatter: ({ cellValue }) => this.formatAmount(cellValue),
          },
          { field: 'paidAmount', title: '已付金额', align: 'right', width: 80 },
          { field: 'unpaidAmount', title: '未付金额', align: 'right', width: 80 },
          { field: 'description', title: '备注', width: 200 },
          {
            field: 'delivered',
            title: '是否已送货',
            width: 100,
            formatter: ({ cellValue }) => (cellValue ? '已送货' : '未送货'),
          },
          {
            field: 'settleStatus',
            title: '结算状态',
            width: 100,
            formatter: ({ cellValue }) => SETTLE_STATUS.getDesc(cellValue) || '-',
          },
          {
            field: 'totalProfit',
            title: '总利润',
            align: 'right',
            width: 80,
            slots: { default: 'total_profit' },
          },
          {
            field: 'profitRate',
            title: '毛利率',
            align: 'right',
            width: 80,
            slots: { default: 'profit_rate' },
          },
          { field: 'totalNum', title: '商品数量', align: 'right', width: 80 },
          { field: 'confirmNum', title: '验收数量', align: 'right', width: 100 },
          {
            field: 'fillAllCost',
            title: '成本状态',
            width: 80,
            slots: { default: 'fillAllCost_default' },
          },

          { field: 'createTime', title: '操作时间', width: 150, sortable: true },
          { field: 'createBy', title: '操作人', width: 80 },
          { title: '操作', minWidth: 300, fixed: 'right', slots: { default: 'action_default' } },
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
          ids: [],
          description: '',
        },
        inquiryPriceSyncModal: {
          visible: false,
          loading: false,
          dateRange: this.getDefaultOrderDateRange(),
        },
        // 浏览器打印模板选择弹窗
        browserPrintModal: {
          visible: false,
          loading: false,
          templateId: '',
          templateList: [],
          printData: undefined,
        },
        // 标签打印分类选择弹窗
        tagPrintModal: {
          visible: false,
          loading: false,
          categoryTreeData: [],
          checkedCategoryIds: [],
          treeShow: false,
          pendingRecords: [],
        },
        // 月底成本重算
        costRefreshVisible: false,
        costRefreshDateRange: [],
      };
    },
    computed: {
      visibleTableColumn() {
        return this.tableColumn.filter((column) => {
          if (['totalProfit', 'profitRate'].includes(column.field)) {
            return this.hasPermission('sale:out:profit', false);
          }
          return true;
        });
      },
      canViewProfit() {
        return this.hasPermission('sale:out:profit', false);
      },
    },
    watch: {
      '$route.query': {
        handler() {
          this.applyRouteQuery();
          this.$nextTick(() => this.search());
        },
      },
    },
    created() {
      this.applyRouteQuery();
    },
    methods: {
      getImporterContainer() {
        return this.$refs.importerContainer;
      },
      applyRouteQuery() {
        const { code, orderDateStart, orderDateEnd } = this.$route.query || {};
        if (code !== undefined) {
          this.searchFormData.code = Array.isArray(code) ? code[0] || '' : code || '';
        }
        if (orderDateStart || orderDateEnd) {
          this.orderDateRange = [orderDateStart || orderDateEnd, orderDateEnd || orderDateStart];
        }
      },
      footerMethod({ columns, data }) {
        const totalAmount = this.sumByField(data, 'totalAmount');
        const paidAmount = this.sumByField(data, 'paidAmount');
        const unpaidAmount = this.sumByField(data, 'unpaidAmount');
        const totalProfit = this.sumByField(data, 'totalProfit');
        const totalCost = this.sumByField(data, 'totalCost');
        const totalNum = this.sumByField(data, 'totalNum');
        const confirmNum = this.sumByField(data, 'confirmNum');
        const confirmAmt = this.sumByField(data, 'confirmAmt');
        const totalProfitBaseAmount = (data || []).reduce((total, item) => {
          const confirmAmount = Number(item?.confirmAmt || 0);
          const saleAmount = Number(item?.totalAmount || 0);
          return total + (confirmAmount !== 0 ? confirmAmount : saleAmount);
        }, 0);

        return [
          columns.map((column) => {
            if (column.type === 'seq') {
              return '合计';
            }

            if (column.field === 'totalAmount') {
              return this.formatAmount(totalAmount);
            }

            if (column.field === 'paidAmount') {
              return this.formatAmount(paidAmount);
            }

            if (column.field === 'unpaidAmount') {
              return this.formatAmount(unpaidAmount);
            }

            if (column.field === 'totalProfit') {
              if (!this.canViewProfit) {
                return '';
              }
              return this.formatAmount(totalProfit);
            }

            if (column.field === 'profitRate') {
              return this.canViewProfit
                ? this.calcProfitRate(totalProfitBaseAmount, 0, totalCost)
                : '';
            }

            if (column.field === 'totalNum') {
              return this.formatQuantity(totalNum);
            }
            if (column.field === 'confirmNum') {
              return this.formatQuantity(confirmNum);
            }
            if (column.field === 'confirmAmt') {
              return this.formatAmount(confirmAmt);
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
      calcProfitRate(amount, confirmAmt, totalCost) {
        return calcSaleOutProfitRateByCost(amount, confirmAmt, totalCost);
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
        const today = moment().format('YYYY-MM-DD');
        return [today, today];
      },
      resetSearchForm() {
        this.searchFormData = {
          code: '',
          sheetDescription: '',
          productName: '',
          scId: '',
          customerIdList: [],
          createBy: undefined,
          approveBy: undefined,
          status: undefined,
          saler: '',
          saleOrderCode: '',
          settleStatus: undefined,
          fillAllCost: undefined,
          delivered: undefined,
          paidAmountStart: undefined,
          paidAmountEnd: undefined,
          unpaidAmountStart: undefined,
          unpaidAmountEnd: undefined,
        };
        this.orderDateRange = this.getDefaultOrderDateRange();
        this.planDateRange = [];
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
          customerIdList: this.searchFormData.customerIdList,
          scId: this.searchFormData.scId,
          createBy: this.searchFormData.createBy,
          orderDateStart: this.orderDateRange?.[0] || '',
          orderDateEnd: this.orderDateRange?.[1] || '',
          planDateStart: this.planDateRange?.[0] || '',
          planDateEnd: this.planDateRange?.[1] || '',
          approveBy: this.searchFormData.approveBy,
          approveStartTime: this.approveDateRange?.[0]
            ? `${this.approveDateRange[0]} 00:00:00`
            : '',
          approveEndTime: this.approveDateRange?.[1] ? `${this.approveDateRange[1]} 23:59:59` : '',
          salerId: this.searchFormData.saler,
          fillAllCost: this.searchFormData.fillAllCost,
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
      async requestUserOptions(keyword = '') {
        return requestUserSelectOptions(keyword);
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
      onCreateByChange(value) {
        this.searchFormData.createBy = this.normalizeSelectValue(value, this.createByOptionMap);
      },
      onApproveByChange(value) {
        this.searchFormData.approveBy = this.normalizeSelectValue(value, this.approveByOptionMap);
      },
      openAddDialog() {
        configApi.get().then((res) => {
          if (res.outStockRequireSale) {
            this.openChildPage('/sale/out/add/require');
          } else {
            this.openChildPage('/sale/out/add/un-require');
          }
        });
      },
      // 打开询价商品售价同步窗口
      openInquiryPriceSync() {
        this.inquiryPriceSyncModal = {
          visible: true,
          loading: false,
          dateRange: this.getDefaultOrderDateRange(),
        };
      },
      // 关闭询价商品售价同步窗口
      closeInquiryPriceSync() {
        this.inquiryPriceSyncModal.visible = false;
        this.inquiryPriceSyncModal.loading = false;
      },
      // 按销售出库订单日期同步询价商品售价及金额
      submitInquiryPriceSync() {
        const dateRange = this.inquiryPriceSyncModal.dateRange;
        if (!dateRange?.[0] || !dateRange?.[1]) {
          createError('请选择完整的销售出库订单日期范围！');
          return;
        }
        this.inquiryPriceSyncModal.loading = true;
        api
          .syncInquirySalePrice({
            startDate: dateRange[0],
            endDate: dateRange[1],
          })
          .then(() => {
            createSuccess('询价商品售价同步成功！');
            this.closeInquiryPriceSync();
            this.search();
          })
          .finally(() => {
            this.inquiryPriceSyncModal.loading = false;
          });
      },
      openModifyDialog(row) {
        if (this.isSettleLocked(row)) {
          createError('销售出库单已对账或已结算，无法修改！');
          return;
        }
        if (!isEmpty(row.saleOrderId)) {
          this.openChildPage('/sale/out/modify/require/' + row.id);
        } else {
          this.openChildPage('/sale/out/modify/un-require/' + row.id);
        }
      },
      openDescriptionDialog(row) {
        this.descriptionModal = {
          visible: true,
          loading: false,
          id: row.id,
          ids: [],
          description: row.description || '',
        };
      },
      /** 处理顶部更多菜单操作。 */
      handleMoreCommand({ key }) {
        const commandMap = {
          batchDelete: () => this.batchDelete(),
          batchExportDetails: () => this.batchExportDetails(),
          marketBuySummary2: () => this.marketBuySummary2(),
          mergeOrders: () => this.mergeOrders(),
          batchDelivery: () => this.batchDelivery(),
          updateDescription: () => this.openBatchDescriptionDialog(),
          openInquiryPriceSync: () => this.openInquiryPriceSync(),
          batchApprovePass: () => this.batchApprovePass(),
          batchApproveRefuse: () => this.batchApproveRefuse(),
        };
        commandMap[key]?.();
      },
      /** 打开批量更新备注弹窗。 */
      openBatchDescriptionDialog() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (records.length === 0) {
          createError('请选择需要更新备注的销售出库单！');
          return;
        }
        this.descriptionModal = {
          visible: true,
          loading: false,
          id: '',
          ids: records.map((item) => item.id),
          description: '',
        };
      },
      closeDescriptionDialog() {
        this.descriptionModal.visible = false;
        this.descriptionModal.loading = false;
      },
      submitDescription() {
        this.descriptionModal.loading = true;
        const request = this.descriptionModal.ids.length
          ? api.batchUpdateDescription({
              ids: this.descriptionModal.ids,
              description: this.descriptionModal.description,
            })
          : api.updateDescription({
              id: this.descriptionModal.id,
              description: this.descriptionModal.description,
            });
        request
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
        createConfirm('对选中的销售出库单执行删除操作？').then(() => {
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
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许执行删除操作！');
            return;
          }
          if (this.isSettleLocked(records[i])) {
            createError('第' + (i + 1) + '个销售出库单已对账或已结算，不允许执行删除操作！');
            return;
          }
        }

        this.batchHandleDatas = records;

        this.$refs.batchDeleteHandlerDialog.openDialog();
      },
      // 合并选中的销售出库单
      mergeOrders() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records) || records.length < 2) {
          createError('请选择两张及以上要合并的销售出库单！');
          return;
        }

        const first = records[0];
        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许合并！');
            return;
          }
          if (this.isSettleLocked(records[i])) {
            createError('第' + (i + 1) + '个销售出库单已对账或已结算，不允许合并！');
            return;
          }
          if (records[i].customerName !== first.customerName) {
            createError('仅允许合并相同客户的销售出库单！');
            return;
          }
          if (records[i].scName !== first.scName) {
            createError('仅允许合并相同仓库的销售出库单！');
            return;
          }
        }

        createConfirm(
          '确认合并选中的' +
            records.length +
            '张销售出库单？系统将保留创建时间最早的单据，并删除其余单据。',
        ).then(() => {
          this.loading = true;
          api
            .merge({
              ids: records.map((item) => item.id),
            })
            .then(() => {
              createSuccess('合并成功！');
              this.search();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      doBatchApprovePass(row) {
        return api.batchApprovePass({
          id: row.id,
        });
      },
      // 批量送货
      batchDelivery() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要送货的销售出库单！');
          return;
        }

        createConfirm('确认将选中的' + records.length + '张销售出库单标记为已送货？').then(() => {
          this.loading = true;
          api
            .batchDelivery(records.map((item) => item.id))
            .then(() => {
              createSuccess('送货成功！');
              this.search();
            })
            .finally(() => {
              this.loading = false;
            });
        });
      },
      // 批量审核通过
      batchApprovePass() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许继续执行审核！');
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
          createError('请选择要执行操作的销售出库单！');
          return;
        }

        for (let i = 0; i < records.length; i++) {
          if (SALE_OUT_SHEET_STATUS.APPROVE_PASS.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核通过，不允许继续执行审核！');
            return;
          }

          if (SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(records[i].status)) {
            createError('第' + (i + 1) + '个销售出库单已审核拒绝，不允许继续执行审核！');
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
        createSuccessAutoClose('导入成功，已创建' + count + '张销售出库单！');
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
      /**
       * 批量导出选中销售出库单的明细。
       */
      batchExportDetails() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要导出明细的销售出库单！');
          return;
        }

        this.loading = true;
        api
          .exportDetail({
            idList: records.map((item) => item.id),
          })
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
      exportSales() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要执行销售导出的销售出库单！');
          return;
        }

        this.loading = true;
        api
          .exportSales({
            idList: records.map((item) => item.id),
          })
          .finally(() => {
            this.loading = false;
          });
      },
      buildPrintData(printData) {
        // 基础属性保持不变， details字段需要重新赋值，比如 orderNum=>qty
        const res = {
          ...printData,
        };

        const details = Array.isArray(printData?.details) ? printData.details : [];
        res.details = details.map((item, index) => ({
          // 新生成一个对象，避免修改原对象
          ...item,
          seq: index + 1,
        }));

        return res;
      },
      async printOrder(row) {
        this.loading = true;

        try {
          const res = await api.print(row.id);
          // 将res组装成模板定义和打印数据的格式，然后调用打印预览组件进行预览
          const printData = this.buildPrintData(res);
          await this.vgPrintPreview(PRINT_TYPE.SALE_OUT.code, printData);
        } finally {
          this.loading = false;
        }
      },
      /**
       * 加载选中的销售出库单及可用模板，打开浏览器批量打印模板选择弹窗。
       */
      async batchPrint() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要打印的销售出库单！');
          return;
        }

        this.loading = true;
        try {
          const [printDatas, templateSelection] = await Promise.all([
            Promise.all(
              records.map(async (record) => this.buildPrintData(await api.print(record.id))),
            ),
            this.getPrintTemplateSelection(PRINT_TYPE.SALE_OUT.code),
          ]);
          if (!templateSelection) {
            return;
          }

          this.browserPrintModal.printData = printDatas;
          this.browserPrintModal.templateId = templateSelection.templateId;
          this.browserPrintModal.templateList = templateSelection.templateList;
          this.browserPrintModal.visible = true;
        } finally {
          this.loading = false;
        }
      },
      /**
       * 加载当前销售出库单及可用模板，打开浏览器打印模板选择弹窗。
       */
      async openBrowserPrintDialog(row) {
        this.loading = true;

        try {
          const [res, templateSelection] = await Promise.all([
            api.print(row.id),
            this.getPrintTemplateSelection(PRINT_TYPE.SALE_OUT.code),
          ]);
          if (!templateSelection) {
            return;
          }

          this.browserPrintModal.printData = this.buildPrintData(res);
          this.browserPrintModal.templateId = templateSelection.templateId;
          this.browserPrintModal.templateList = templateSelection.templateList;
          this.browserPrintModal.visible = true;
        } finally {
          this.loading = false;
        }
      },
      /**
       * 使用弹窗内选中的模板调起浏览器打印。
       */
      async confirmBrowserPrint() {
        if (!this.browserPrintModal.templateId) {
          createError('请选择打印模板！');
          return;
        }

        this.browserPrintModal.loading = true;
        try {
          const printCompleted = await this.vgBrowserPrint(
            this.browserPrintModal.printData,
            this.browserPrintModal.templateId,
            { resetPageNumberPerData: Array.isArray(this.browserPrintModal.printData) },
          );
          if (printCompleted) {
            this.closeBrowserPrintDialog();
          }
        } finally {
          this.browserPrintModal.loading = false;
        }
      },
      /**
       * 关闭浏览器打印模板选择弹窗并清理当前单据数据。
       */
      closeBrowserPrintDialog() {
        this.browserPrintModal.visible = false;
        this.browserPrintModal.templateId = '';
        this.browserPrintModal.templateList = [];
        this.browserPrintModal.printData = undefined;
      },
      async tagPrint() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要打印标签的销售出库单！');
          return;
        }
        // 缓存选中记录，打开分类选择弹窗
        this.tagPrintModal.pendingRecords = records;
        this.openTagPrintModal();
      },
      /** 打开标签打印分类选择弹窗 */
      openTagPrintModal() {
        this.tagPrintModal.visible = true;
        this.tagPrintModal.treeShow = false;
        this.tagPrintModal.checkedCategoryIds = [];
        // 从Redis缓存加载上次勾选的分类
        api.getTagPrintCategoryCache().then((cached) => {
          if (cached && cached.length > 0) {
            this.tagPrintModal.checkedCategoryIds = cached;
          }
        });
        // 加载商品分类树
        categoryApi.query().then((res) => {
          this.tagPrintModal.categoryTreeData = toArrayTree(res);
          this.tagPrintModal.treeShow = true;
        });
      },
      /** 关闭标签打印分类选择弹窗 */
      closeTagPrintModal() {
        this.tagPrintModal.visible = false;
        this.tagPrintModal.pendingRecords = [];
      },
      /** 获取标签打印分类树中全部分类ID。 */
      getAllTagPrintCategoryIds() {
        const categoryIds = [];
        const collectCategoryIds = (categories) => {
          categories.forEach((category) => {
            categoryIds.push(category.id);
            if (category.children && category.children.length > 0) {
              collectCategoryIds(category.children);
            }
          });
        };
        collectCategoryIds(this.tagPrintModal.categoryTreeData);
        return categoryIds;
      },
      /** 判断标签打印分类是否已全部勾选。 */
      isAllTagPrintCategoryChecked() {
        const categoryIds = this.getAllTagPrintCategoryIds();
        return (
          categoryIds.length > 0 &&
          categoryIds.every((categoryId) =>
            this.tagPrintModal.checkedCategoryIds.includes(categoryId),
          )
        );
      },
      /** 判断标签打印分类是否处于部分勾选状态。 */
      isTagPrintCategoryCheckIndeterminate() {
        const categoryIds = this.getAllTagPrintCategoryIds();
        const checkedCount = categoryIds.filter((categoryId) =>
          this.tagPrintModal.checkedCategoryIds.includes(categoryId),
        ).length;
        return checkedCount > 0 && checkedCount < categoryIds.length;
      },
      /** 切换标签打印分类的全选状态。 */
      toggleAllTagPrintCategories(event) {
        this.tagPrintModal.checkedCategoryIds = event.target.checked
          ? this.getAllTagPrintCategoryIds()
          : [];
      },
      /** 确认标签打印（携带分类筛选，并直接调起浏览器打印）。 */
      async doTagPrintWithCategory() {
        // 缓存勾选的分类到Redis
        api.saveTagPrintCategoryCache(this.tagPrintModal.checkedCategoryIds);
        this.tagPrintModal.loading = true;
        try {
          const printData = await api.tagPrint({
            ...this.buildQueryParams({}, {}),
            idList: this.tagPrintModal.pendingRecords.map((item) => item.id),
            categoryIdList:
              this.tagPrintModal.checkedCategoryIds.length > 0
                ? this.tagPrintModal.checkedCategoryIds
                : undefined,
          });

          this.closeTagPrintModal();
          await this.vgDefaultBrowserPrint(PRINT_TYPE.SALE_TAG.code, printData);
        } finally {
          this.tagPrintModal.loading = false;
        }
      },
      // 按勾选单据导出买菜汇总
      marketBuySummary() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要汇总的销售出库单！');
          return;
        }

        this.marketBuySummaryModal.pendingRecords = records;
        this.marketBuySummaryModal.groupByDate = false;
        this.marketBuySummaryModal.visible = true;
      },
      /** 确认买菜汇总导出，并根据勾选项决定是否按日期分组。 */
      async confirmMarketBuySummary() {
        this.marketBuySummaryModal.loading = true;
        this.loading = true;
        try {
          await api.exportMarketBuySummary(
            buildMarketBuySummaryParams(
              this.marketBuySummaryModal.pendingRecords,
              this.marketBuySummaryModal.groupByDate,
              this.marketBuySummaryModal.mergeSameDayCustomerProduct,
            ),
          );
          this.closeMarketBuySummaryModal();
        } finally {
          this.marketBuySummaryModal.loading = false;
          this.loading = false;
        }
      },
      /** 关闭买菜汇总选项弹窗并清理已暂存的单据。 */
      closeMarketBuySummaryModal() {
        this.marketBuySummaryModal.visible = false;
        this.marketBuySummaryModal.groupByDate = false;
        this.marketBuySummaryModal.mergeSameDayCustomerProduct = false;
        this.marketBuySummaryModal.pendingRecords = [];
      },
      // 按勾选单据导出买菜汇总2
      marketBuySummary2() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择要汇总的销售出库单！');
          return;
        }

        this.loading = true;
        api.exportMarketBuySummary2(buildMarketBuySummary2Params(records)).finally(() => {
          this.loading = false;
        });
      },
      viewSaleOrderDetail(id) {
        this.saleOrderId = id;
        this.$nextTick(() => this.$refs.viewSaleOrderDetailDialog.openDialog());
      },
      viewDetail(id) {
        this.id = id;
        this.$nextTick(() => this.$refs.viewDialog.openDialog());
      },
      /** 判断销售出库单是否已进入对账或结算流程。 */
      isSettleLocked(row) {
        return [0, 1, 3].includes(Number(row.settleStatus));
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
            label: '打印',
            onClick: () => {
              this.openBrowserPrintDialog(row);
            },
          },
          {
            permission: ['sale:out:approve'],
            label: '审核',
            ifShow: () => {
              return (
                SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)
              );
            },
            onClick: () => {
              this.openChildPage('/sale/out/approve/' + row.id);
            },
          },
        ];
      },
      /**
       * 创建行内“更多”菜单操作，保留原有权限和业务状态限制。
       */
      createMoreActions(row) {
        return [
          {
            label: '客户端打印',
            ifShow: () => SHOW_MORE_PRINT_ACTION,
            onClick: () => {
              this.printOrder(row);
            },
          },
          {
            permission: ['sale:out:modify'],
            label: '修改',
            ifShow: () => {
              return (
                (SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                  SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)) &&
                !this.isSettleLocked(row)
              );
            },
            onClick: () => {
              this.openModifyDialog(row);
            },
          },
          {
            permission: ['sale:out:modify'],
            label: '修改备注',
            onClick: () => {
              this.openDescriptionDialog(row);
            },
          },
          {
            permission: ['sale:out:delete'],
            label: '删除',
            danger: true,
            ifShow: () => {
              return (
                (SALE_OUT_SHEET_STATUS.CREATED.equalsCode(row.status) ||
                  SALE_OUT_SHEET_STATUS.APPROVE_REFUSE.equalsCode(row.status)) &&
                !this.isSettleLocked(row)
              );
            },
            onClick: () => {
              this.deleteOrder(row);
            },
          },
        ];
      },
      onRefreshPage() {
        this.applyRouteQuery();
        this.search();
      },
      /**
       * 打开成本重算弹窗，默认时间范围为月初到今天
       */
      openCostRecalculate() {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const formatDate = (d) => {
          const year = d.getFullYear();
          const month = String(d.getMonth() + 1).padStart(2, '0');
          const day = String(d.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        };
        this.costRefreshDateRange = [formatDate(firstDay), formatDate(now)];
        this.costRefreshVisible = true;
      },
    },
  });
</script>

<style scoped>
  .recalc-overlay {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 9999;
    background: rgba(255, 255, 255, 0.85);
  }

  .recalc-overlay-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
  }

  .recalc-tip {
    margin-top: 24px;
    color: #2f3f48;
    font-size: 16px;
  }

  .recalc-progress {
    width: 320px;
    margin-top: 16px;
  }

  .recalc-error {
    max-width: 420px;
    text-align: center;
  }

  :global(.ant-dropdown-menu-item.danger-menu-item),
  :global(.ant-dropdown-menu-item.danger-menu-item:hover) {
    color: #ff4d4f;
  }

  .toolbar-more-button {
    min-height: 32px;
  }
</style>
