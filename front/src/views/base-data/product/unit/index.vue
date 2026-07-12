<template>
  <div v-permission="['base-data:unit:query']">
    <page-wrapper content-full-height fixed-height>
      <j-border>
        <j-form bordered label-width="80px">
          <j-form-item label="编码">
            <a-input v-model:value="searchForm.code" allow-clear @press-enter="load"/>
          </j-form-item>
          <j-form-item label="名称">
            <a-input v-model:value="searchForm.name" allow-clear @press-enter="load"/>
          </j-form-item>
        </j-form>
      </j-border>
      <vxe-grid
        ref="grid"
        :data="rows"
        :columns="columns"
        :loading="loading"
        :toolbar-config="toolbarConfig"
        height="auto"
      >
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" @click="load">查询</a-button>
            <a-button v-permission="['base-data:unit:add']" type="primary" @click="open()">
              新增
            </a-button>
            <a-button
              v-permission="['base-data:unit:import']"
              :icon="h(CloudUploadOutlined)"
              @click="importer?.openDialog()">
              导入Excel
            </a-button>
            <a-dropdown v-permission="['base-data:unit:delete']">
              <template #overlay>
                <a-menu @click="handleCommand">
                  <a-menu-item key="batchDelete">批量删除</a-menu-item>
                </a-menu>
              </template>
              <a-button>更多</a-button>
            </a-dropdown>
          </a-space>
        </template>
        <template #action="{ row }">
          <a @click="view(row)">查看</a>
          <a-divider type="vertical"/>
          <a v-permission="['base-data:unit:modify']" @click="open(row)">修改</a>
          <a-divider type="vertical"/>
          <a v-permission="['base-data:unit:delete']" @click="del(row)">删除</a>
        </template>
      </vxe-grid>
    </page-wrapper>
    <unit-importer ref="importer" @confirm="load"/>
    <a-modal
      v-model:open="visible"
      :title="readonly ? '查看单位' : form.id ? '修改单位' : '新增单位'"
      @ok="save"
    >
      <a-form :model="form" :label-col="{ span: 5 }">
        <a-form-item label="编码" required>
          <a-space-compact block>
            <a-input v-model:value="form.code" :disabled="readonly"/>
            <a-button v-if="!readonly && !form.id" type="primary" @click="generateCode">
              自动生成
            </a-button>
          </a-space-compact>
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" :disabled="readonly"/>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="form.description" :disabled="readonly"/>
        </a-form-item>
      </a-form>
      <template v-if="readonly" #footer>
        <a-button @click="visible = false">关闭</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {h, onMounted, ref} from 'vue';
import * as api from '@/api/base-data/unit';
import {createConfirm, createError, createSuccess} from '@/hooks/web/msg';
import UnitImporter from '@/components/Importor/UnitImporter.vue';
import {CloudUploadOutlined, DownloadOutlined} from "@ant-design/icons-vue";

const grid = ref<any>();
const importer = ref<InstanceType<typeof UnitImporter>>();
const loading = ref(false);
const visible = ref(false);
const readonly = ref(false);
const rows = ref<any[]>([]);
const form = ref<any>({});
const searchForm = ref({code: '', name: ''});
const toolbarConfig = {slots: {buttons: 'toolbar_buttons'}};
const columns = [
  {type: 'checkbox', width: 45},
  {type: 'seq', title: '序号', width: 60},
  {field: 'code', title: '编码', width: 200},
  {field: 'name', title: '名称', minWidth: 100},
  {field: 'description', title: '备注', minWidth: 200},
  {title: '操作', width: 180, fixed: 'right', slots: {default: 'action'}},
];

async function load() {
  loading.value = true;
  try {
    rows.value = await api.query(searchForm.value);
  } finally {
    loading.value = false;
  }
}

function open(row?: any) {
  readonly.value = false;
  form.value = row ? {...row} : {};
  visible.value = true;
  if (!row) generateCode();
}

function view(row: any) {
  readonly.value = true;
  form.value = {...row};
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
  load();
}

function del(row: any) {
  createConfirm('确认删除该单位？').then(async () => {
    await api.remove(row.id);
    createSuccess('删除成功');
    load();
  });
}

function handleCommand({key}: any) {
  if (key !== 'batchDelete') return;
  const selected = grid.value?.getCheckboxRecords() || [];
  if (!selected.length) return createError('请选择要删除的单位');
  createConfirm(`确认删除选中的 ${selected.length} 个单位？`).then(async () => {
    await Promise.all(selected.map((row: any) => api.remove(row.id)));
    createSuccess('删除成功');
    load();
  });
}

onMounted(load);
</script>
