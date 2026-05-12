<template>
  <div>
    <dialog-table
      ref="selector"
      :request="getList"
      :load="getLoad"
      :option="{ label: 'label', value: 'value' }"
      :column-option="{ label: 'label', value: 'value' }"
      :table-column="[{ field: 'label', title: '用户', minWidth: 220 }]"
      :request-params="_requestParams"
      v-bind="$attrs"
    >
      <template #form>
        <!-- 查询条件 -->
        <j-border>
          <j-form bordered>
            <j-form-item v-if="isEmpty(requestParams.label)" label="用户">
              <a-input v-model:value="searchParams.label" allow-clear />
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
  import * as api from '@/api/system/user';
  import { isEmpty } from '@/utils/utils';

  export default defineComponent({
    name: 'UserSelector',
    components: { SearchOutlined },
    props: {
      requestParams: {
        type: Object,
        default: () => {
          return {};
        },
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
        },
      };
    },
    computed: {
      _requestParams() {
        return { ...this.searchParams, ...this.requestParams };
      },
    },
    methods: {
      getList(params) {
        return api.selector({
          ...params,
          ...this.searchParams,
          ...this.requestParams,
        });
      },
      getLoad(ids) {
        return api.loadUser(ids);
      },
    },
  });
</script>

<style lang="less"></style>
