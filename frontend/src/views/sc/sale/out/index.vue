<template>
  <div v-permission="['sale:out:query']">
    <a-tabs v-model:activeKey="activeKey" class="query-tabs" @change="syncActiveGridHeight">
      <a-tab-pane key="sheet" tab="单据查询">
        <sheet-list ref="sheetList" />
      </a-tab-pane>
      <a-tab-pane key="detail" tab="明细查询">
        <detail-list ref="detailList" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import SheetList from './components/sheet-list.vue';
  import DetailList from './components/detail-list.vue';

  export default defineComponent({
    name: 'SaleOutSheet',
    components: {
      SheetList,
      DetailList,
    },
    data() {
      return {
        activeKey: 'sheet',
      };
    },
    methods: {
      /** 切换标签页后同步当前表格高度，确保分页栏始终位于可视区域。 */
      syncActiveGridHeight(activeKey) {
        this.$nextTick(() => {
          const gridList = activeKey === 'detail' ? this.$refs.detailList : this.$refs.sheetList;
          gridList?.syncGridHeight();
        });
      },
    },
  });
</script>

<style lang="less" scoped>
  .query-tabs {
    :deep(.ant-tabs-nav) {
      padding-left: 50px;
      margin-bottom: 0;
    }
  }
</style>
