<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="60%"
    title="授权"
    :style="{ top: '20px' }"
  >
    <div v-if="visible" v-permission="['system:role:permission']" v-loading="loading">
      <vxe-grid
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        :max-height="$vh * 80"
        :tree-config="{
          expandAll: true,
        }"
        :export-config="{}"
        :data="tableData"
        :checkbox-config="{
          trigger: 'row',
          checkRowKeys: selectedRows,
          checkStrictly: true,
          range: true,
          highlight: true,
        }"
        :columns="tableColumn"
        @checkbox-change="syncSelection"
      >
        <!-- 类型 列自定义内容 -->
        <template #menuDisplay_default="{ row }">
          <menu-display-tag :menu-display="row.display" />
        </template>

        <!-- 状态 列自定义内容 -->
        <template #available_default="{ row }">
          <available-tag :available="row.available" />
        </template>
      </vxe-grid>
    </div>

    <template #footer>
      <div class="form-modal-footer">
        <a-space>
          <a-button type="primary" :loading="loading" @click="submit">保存</a-button>
          <a-button :loading="loading" @click="closeDialog">取消</a-button>
        </a-space>
      </div>
    </template>
  </a-modal>
</template>
<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/system/role-menu';
  import { isEmpty, toArrayTree, eachTree, union } from '@/utils/utils';
  import { createSuccess } from '@/hooks/web/msg';
  import AvailableTag from '@/components/Tag/AvailableTag.vue';
  import MenuDisplayTag from '@/components/Tag/MenuDisplayTag.vue';

  export default defineComponent({
    // 使用组件
    components: {
      AvailableTag,
      MenuDisplayTag,
    },

    props: {
      ids: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        // 是否可见
        visible: false,
        // 是否显示加载框
        loading: false,
        // 表格数据
        tableData: [],
        selectedRows: [],
        menuRowMap: {},
        syncingSelection: false,
        // 表格列配置
        tableColumn: [
          { type: 'checkbox', width: 45 },
          { field: 'code', title: '编号', width: 100 },
          { field: 'title', title: '标题', treeNode: true },
          { field: 'display', title: '类型', width: 80, slots: { default: 'menuDisplay_default' } },
          { field: 'permission', title: '权限', width: 220 },
          { field: 'available', title: '状态', width: 80, slots: { default: 'available_default' } },
        ],
      };
    },
    created() {},
    methods: {
      // 打开对话框 由父页面触发
      openDialog() {
        this.visible = true;

        this.open();
      },
      // 关闭对话框
      closeDialog() {
        this.visible = false;
        this.$emit('close');
      },
      // 页面显示时触发
      open() {
        // 查询数据
        this.tableData = [];
        this.selectedRows = [];
        this.query();
      },
      // 列表查询数据
      query() {
        this.loading = true;
        const params = {};
        if (!isEmpty(this.ids) && this.ids.length === 1) {
          params.roleId = this.ids[0];
        }
        api
          .menus(params.roleId)
          .then((res) => {
            // 将带层级的列表转成树结构
            res = toArrayTree(res, {
              key: 'id',
              parentKey: 'parentId',
              children: 'children',
              strict: true,
            });

            this.tableData = res;
            this.buildMenuTreeIndex();

            const selectedMenus = [];
            eachTree(res, (item) => {
              if (item.selected) {
                selectedMenus.push(item.id);
              }
            });

            this.selectedRows = this.expandSelectedRows(selectedMenus);
          })
          .finally(() => {
            this.loading = false;
          });
      },
      buildMenuTreeIndex() {
        const menuRowMap = {};
        const walkTree = (rows, parentRowId = '') => {
          rows.forEach((item) => {
            item.parentRowId = parentRowId;
            menuRowMap[item.id] = item;
            if (!isEmpty(item.children)) {
              walkTree(item.children, item.id);
            }
          });
        };

        walkTree(this.tableData);
        this.menuRowMap = menuRowMap;
      },
      expandSelectedRows(selectedMenuIds) {
        const selectedSet = new Set(selectedMenuIds);
        selectedMenuIds.forEach((menuId) => {
          let current = this.menuRowMap[menuId];
          while (current && current.parentRowId) {
            selectedSet.add(current.parentRowId);
            current = this.menuRowMap[current.parentRowId];
          }
        });

        return Array.from(selectedSet);
      },
      getAncestorRows(row) {
        const rows = [];
        let current = this.menuRowMap[row.id];
        while (current && current.parentRowId) {
          current = this.menuRowMap[current.parentRowId];
          if (current) {
            rows.push(current);
          }
        }

        return rows;
      },
      getDescendantRows(row) {
        const rows = [];
        eachTree(row.children || [], (item) => {
          rows.push(item);
        });
        return rows;
      },
      syncSelection({ checked, row }) {
        if (this.syncingSelection) {
          return;
        }

        const grid = this.$refs.grid;
        if (!grid || !row) {
          return;
        }

        this.syncingSelection = true;
        try {
          if (checked) {
            const ancestorRows = this.getAncestorRows(row);
            if (!isEmpty(ancestorRows)) {
              grid.setCheckboxRow(ancestorRows, true);
            }

            const descendantRows = this.getDescendantRows(row);
            if (!isEmpty(descendantRows)) {
              grid.setCheckboxRow(descendantRows, true);
            }
          } else {
            const descendantRows = this.getDescendantRows(row);
            if (!isEmpty(descendantRows)) {
              grid.setCheckboxRow(descendantRows, false);
            }
          }
        } finally {
          this.syncingSelection = false;
        }
      },
      // 提交数据
      submit() {
        this.loading = true;
        const records = union(
          this.$refs.grid.getCheckboxRecords(true),
          this.$refs.grid.getCheckboxIndeterminateRecords(true),
        );
        const menuIds = isEmpty(records) ? [] : records.map((item) => item.id);
        api
          .setting({
            roleIds: this.ids,
            menuIds: menuIds,
          })
          .then(() => {
            createSuccess('授权成功！');
            this.$emit('confirm');
            this.visible = false;
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  });
</script>
