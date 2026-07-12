<template>
  <div v-permission="['sale:out:query']">
    <a-tabs v-model:activeKey="activeKey" class="query-tabs">
      <a-tab-pane key="sheet" tab="单据查询">
        <sheet-list />
      </a-tab-pane>
      <a-tab-pane key="detail" tab="明细查询">
        <detail-list />
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
