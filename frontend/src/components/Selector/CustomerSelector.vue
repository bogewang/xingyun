<template>
  <div>
    <dialog-table
      ref="selector"
      :request="getList"
      :load="getLoad"
      :option="{ label: 'label', value: 'value' }"
      :column-option="{ label: 'label', value: 'value' }"
      :table-column="tableColumns"
      :page-size="50"
      :request-params="_requestParams"
      v-bind="$attrs"
    >
      <template #form>
        <!-- 查询条件 -->
        <j-border>
          <j-form bordered>
            <j-form-item v-if="isEmpty(requestParams.label)" label="客户">
              <a-input v-model:value="searchParams.label" allow-clear />
            </j-form-item>
            <j-form-item
              v-if="showDescriptionFilter && isEmpty(requestParams.description)"
              label="备注"
            >
              <a-input v-model:value="searchParams.description" allow-clear />
            </j-form-item>
          </j-form>
        </j-border>
      </template>
      <!-- 工具栏 -->
      <template #toolbar_buttons>
        <a-space class="operator">
          <a-button type="primary" @click="$refs.selector.search()">
            <template #icon>
              <SearchOutlined />
            </template>
            查询</a-button
          >
        </a-space>
      </template>
    </dialog-table>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import { SearchOutlined } from '@ant-design/icons-vue';
  import * as api from '@/api/base-data/customer';
  import { isEmpty } from '@/utils/utils';

  export default defineComponent({
    name: 'CustomerSelector',
    components: { SearchOutlined },
    props: {
      requestParams: {
        type: Object,
        default: () => {
          return {};
        },
      },
      showDescriptionFilter: {
        type: Boolean,
        default: false,
      },
    },
    setup() {
      return {
        isEmpty,
      };
    },
    data() {
      return {
        searchParams: {
          label: '',
          description: '',
        },
      };
    },
    computed: {
      tableColumns() {
        const columns = [{ field: 'label', title: '客户', minWidth: 220 }];
        if (this.showDescriptionFilter) {
          columns.push({ field: 'description', title: '客户备注', minWidth: 260 });
        }
        return columns;
      },
      _requestParams() {
        return {
          ...this.searchParams,
          ...this.requestParams,
          orderByDescription: this.showDescriptionFilter,
        };
      },
    },
    methods: {
      getList(params) {
        return api.selector({
          ...params,
          ...this._requestParams,
        });
      },
      getLoad(ids) {
        return api.loadCustomer(ids);
      },
    },
  });
</script>

<style lang="less"></style>
