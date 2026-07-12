<template>
  <div v-permission="['base-data:unit:query']">
    <page-wrapper content-full-height fixed-height>
      <j-border>
        <j-form bordered label-width="80px">
          <j-form-item label="编码">
            <a-input v-model:value="searchFormData.code" allow-clear @press-enter="search" />
          </j-form-item>
          <j-form-item label="名称">
            <a-input v-model:value="searchFormData.name" allow-clear @press-enter="search" />
          </j-form-item>
        </j-form>
      </j-border>
      <vxe-grid
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        :proxy-config="proxyConfig"
        :columns="columns"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{
          layouts: ['Home', 'PrevPage', 'Jump', 'PageCount', 'NextPage', 'End', 'Sizes', 'Total'],
        }"
        :loading="loading"
        height="auto"
      >
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="search">查询</a-button>
            <a-button v-permission="['base-data:unit:add']" type="primary" @click="open()">
              新增
            </a-button>
            <a-button
              v-permission="['base-data:unit:import']"
              :icon="h(CloudUploadOutlined)"
              @click="importer?.openDialog()"
            >
              导入Excel
            </a-button>
            <a-dropdown v-permission="['base-data:unit:delete']">
              <template #overlay>
                <a-menu @click="handleCommand">
                  <a-menu-item key="batchDelete" :icon="h(DeleteOutlined)">批量删除</a-menu-item>
                </a-menu>
              </template>
              <a-button>更多</a-button>
            </a-dropdown>
          </a-space>
        </template>
        <template #action="{ row }">
          <table-action outside :actions="createActions(row)" />
        </template>
      </vxe-grid>
    </page-wrapper>
    <unit-importer ref="importer" @confirm="search" />
    <a-modal
      v-model:open="visible"
      :title="readonly ? '查看单位' : form.id ? '修改单位' : '新增单位'"
      @ok="save"
    >
      <a-form :model="form" :label-col="{ span: 5 }">
        <a-form-item label="编码" required>
          <a-space-compact block>
            <a-input v-model:value="form.code" :disabled="readonly" />
            <a-button v-if="!readonly && !form.id" type="primary" @click="generateCode">
              自动生成
            </a-button>
          </a-space-compact>
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" :disabled="readonly" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="form.description" :disabled="readonly" />
        </a-form-item>
      </a-form>
      <template v-if="readonly" #footer>
        <a-button @click="visible = false">关闭</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts">
  import { defineComponent, h, onMounted, reactive, ref } from 'vue';
  import * as api from '@/api/base-data/unit';
  import { buildSortPageVo } from '@/utils/utils';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import UnitImporter from '@/components/Importor/UnitImporter.vue';
  import {CloudUploadOutlined, DeleteOutlined} from '@ant-design/icons-vue';

  export default defineComponent({
    name: 'ProductUnit',
    methods: {DeleteOutlined},
    components: { UnitImporter },
    setup() {
      const grid = ref<any>();
      const importer = ref<InstanceType<typeof UnitImporter>>();
      const loading = ref(false);
      const visible = ref(false);
      const readonly = ref(false);
      const form = ref<any>({});
      const searchFormData = reactive({ code: '', name: '' });
      const toolbarConfig = { slots: { buttons: 'toolbar_buttons' } };
      const columns = [
        { type: 'checkbox', width: 45 },
        { type: 'seq', title: '序号', width: 60 },
        { field: 'code', title: '编码', width: 200 },
        { field: 'name', title: '名称', minWidth: 100 },
        { field: 'description', title: '备注', minWidth: 200 },
        { title: '操作', width: 180, fixed: 'right', slots: { default: 'action' } },
      ];
      const proxyConfig = {
        props: {
          result: 'datas',
          total: 'totalCount',
        },
        ajax: {
          query: ({ page, sorts }: any) => {
            return api.query(buildQueryParams(page, sorts));
          },
        },
      };

      function search() {
        grid.value.commitProxy('reload');
      }

      function buildQueryParams(page: any, sorts: any) {
        return {
          ...buildSortPageVo(page, sorts),
          ...buildSearchFormData(),
        };
      }

      function buildSearchFormData() {
        return {
          ...searchFormData,
        };
      }

      function open(row?: any) {
        readonly.value = false;
        form.value = row ? { ...row } : {};
        visible.value = true;
        if (!row) generateCode();
      }

      function view(row: any) {
        readonly.value = true;
        form.value = { ...row };
        visible.value = true;
      }

      async function generateCode() {
        form.value.code = await api.generateCode();
      }

      async function save() {
        if (readonly.value) return;
        if (!form.value.code || !form.value.name) return createError('请填写单位编码和名称');
        await (form.value.id ? api.update(form.value) : api.create(form.value));
        createSuccess('保存成功');
        visible.value = false;
        search();
      }

      function del(row: any) {
        createConfirm('确认删除该单位？').then(async () => {
          await api.remove(row.id);
          createSuccess('删除成功');
          search();
        });
      }

      function handleCommand({ key }: any) {
        if (key !== 'batchDelete') return;
        const selected = grid.value?.getCheckboxRecords() || [];
        if (!selected.length) return createError('请选择要删除的单位');
        createConfirm(`确认删除选中的 ${selected.length} 个单位？`).then(async () => {
          await Promise.all(selected.map((row: any) => api.remove(row.id)));
          createSuccess('删除成功');
          search();
        });
      }

      function createActions(row: any) {
        return [
          {
            label: '查看',
            onClick: () => view(row),
          },
          {
            permission: ['base-data:unit:modify'],
            label: '修改',
            onClick: () => open(row),
          },
          {
            permission: ['base-data:unit:delete'],
            danger: true,
            label: '删除',
            onClick: () => del(row),
          },
        ];
      }

      onMounted(() => search());

      return {
        h,
        CloudUploadOutlined,
        DeleteOutlined,
        grid,
        importer,
        loading,
        visible,
        readonly,
        form,
        searchFormData,
        toolbarConfig,
        columns,
        proxyConfig,
        search,
        open,
        view,
        generateCode,
        save,
        del,
        createActions,
        handleCommand,
      };
    },
  });
</script>
