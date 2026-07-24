<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="75%"
    title="查看"
    :style="{ top: '20px' }"
  >
    <div>
      <a-tabs v-model:activeKey="activeKey" v-if="visible">
        <a-tab-pane key="detail" tab="详情"
          ><viewer ref="viewerRef" :id="id" @load-data-complete="(e) => (formData = e)"
        /></a-tab-pane>
        <a-tab-pane key="orderTimeLine" v-if="isEmpty(formData.flowInstanceId)" tab="变动记录"
          ><order-time-line :id="id"
        /></a-tab-pane>
        <a-tab-pane key="approveHis" v-if="!isEmpty(formData.flowInstanceId)" tab="审批历史"
          ><bpm-approve-his :business-id="formData.id"
        /></a-tab-pane>
      </a-tabs>
    </div>
    <template #footer>
      <div class="form-modal-footer">
        <a-space>
          <a-button type="primary" :loading="loading" @click="print">打印</a-button>
          <a-button :loading="loading" @click="exportDetails">导出明细</a-button>
          <a-button :loading="loading" @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </template>
  </a-modal>
  <order-print-dialog />
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/sc/purchase/order';
  import { printMix } from '@/mixins/print.ts';
  import Viewer from './viewer.vue';
  import { isEmpty } from '@/utils/utils';
  import { PRINT_TYPE } from '@/enums/biz/printType';
  import OrderTimeLine from '@/components/OrderTimeLine';
  import PrintDialog from '/@/components/PrintDialog';
  import { createSuccess } from '@/hooks/web/msg';

  export default defineComponent({
    components: {
      Viewer,
      OrderTimeLine,
      OrderPrintDialog: PrintDialog,
    },
    mixins: [printMix],
    props: {
      id: {
        type: String,
        required: true,
      },
    },
    setup() {
      return {
        isEmpty,
      };
    },
    data() {
      return {
        // 是否可见
        visible: false,
        // 是否显示加载框
        loading: false,
        // 当前激活的标签
        activeKey: 'detail',
        formData: {},
      };
    },
    computed: {},
    created() {
      // 初始化表单数据
      this.initFormData();
    },
    methods: {
      // 打开对话框 由父页面触发
      openDialog() {
        this.visible = true;

        this.$nextTick(() => this.open());
      },
      // 关闭对话框
      closeDialog() {
        this.visible = false;
        this.$emit('close');
      },
      // 初始化表单数据
      initFormData() {
        this.formData = {};
        this.activeKey = 'detail';
      },
      // 页面显示时触发
      open() {
        // 初始化表单数据
        this.initFormData();
      },
      async print() {
        this.loading = true;
        try {
          const res = await api.print(this.id);
          await this.vgPrintPreview(PRINT_TYPE.PURCHASE_ORDER.code, res);
        } finally {
          this.loading = false;
        }
      },
      exportDetails() {
        this.loading = true;
        api
          .exportDetail(this.buildQueryParams({}))
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      // 查询前构建查询参数结构
      buildQueryParams() {
        return {
          pageIndex: 1,
          pageSize: 2147483647,
          // 订单编号列表
          idList: [this.id],
        };
      },
    },
  });
</script>
<style></style>
