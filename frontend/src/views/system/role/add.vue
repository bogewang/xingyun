<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="40%"
    :title="isCopy ? '复制角色' : '新增'"
    :style="{ top: '20px' }"
    :footer="null"
  >
    <div v-if="visible" v-permission="['system:role:add']" v-loading="loading">
      <a-form
        ref="form"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        :model="formData"
        :rules="rules"
      >
        <a-form-item label="编号" name="code">
          <a-input-group compact>
            <a-input
              v-model:value.trim="formData.code"
              style="width: calc(100% - 75px)"
              allow-clear
            />
            <a-button type="primary" @click="onGenerateCode">点此生成</a-button>
          </a-input-group>
        </a-form-item>
        <a-form-item label="名称" name="name">
          <a-input v-model:value.trim="formData.name" allow-clear />
        </a-form-item>
        <a-form-item label="分类" name="categoryId">
          <sys-role-category-selector v-model:value="formData.categoryId" />
        </a-form-item>
        <a-form-item label="权限" name="permission">
          <a-input v-model:value.trim="formData.permission" allow-clear />
        </a-form-item>
        <a-form-item label="备注" name="description">
          <a-textarea v-model:value.trim="formData.description" />
        </a-form-item>
        <div class="form-modal-footer">
          <a-space>
            <a-button type="primary" :loading="loading" html-type="submit" @click="submit"
              >保存</a-button
            >
            <a-button :loading="loading" @click="closeDialog">取消</a-button>
          </a-space>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
<script>
import {defineComponent} from 'vue';
import {validCode} from '@/utils/validate';
import * as api from '@/api/system/role';
import * as roleMenuApi from '@/api/system/role-menu';
import {generateCode} from '@/api/components';
import {createSuccess, createWarning} from '@/hooks/web/msg';
import SysRoleCategorySelector from '@/components/Selector/SysRoleCategorySelector.vue';
import {GENERATE_CODE_TYPE} from '@/enums/biz/generateCodeType';

export default defineComponent({
    components: {
      SysRoleCategorySelector,
    },
    data() {
      return {
        // 是否可见
        visible: false,
        // 是否显示加载框
        loading: false,
        // 是否复制模式
        isCopy: false,
        sourceRoleId: '',
        sourceMenuIds: [],
        // 表单数据
        formData: {},
        // 表单校验规则
        rules: {
          code: [{ required: true, message: '请输入编号' }, { validator: validCode }],
          name: [{ required: true, message: '请输入名称' }],
          categoryId: [{ required: true, message: '请选择分类' }],
        },
      };
    },
    computed: {},
    created() {
      // 初始化表单数据
      this.initFormData();
    },
    methods: {
      // 打开对话框 由父页面触发
      openDialog(row) {
        this.isCopy = !!row?.id;
        this.sourceRoleId = row?.id || '';
        this.visible = true;

        this.$nextTick(() => this.open());
      },
      // 关闭对话框
      closeDialog() {
        this.visible = false;
        this.isCopy = false;
        this.sourceRoleId = '';
        this.sourceMenuIds = [];
        this.$emit('close');
      },
      // 初始化表单数据
      initFormData() {
        this.formData = {
          code: '',
          permission: '',
          description: '',
          name: '',
          shortName: '',
          categoryId: '',
        };
      },
      // 提交表单事件
      submit() {
        this.$refs.form.validate().then((valid) => {
          if (valid) {
            this.loading = true;
            api
              .create(this.formData)
              .then((res) => {
                if (this.isCopy) {
                  return this.copyRoleMenus(res).then((copiedMenus) => {
                    createSuccess(copiedMenus ? '复制成功！' : '角色已复制，请检查权限是否需要补充。');
                  });
                }

                createSuccess('新增成功！');
              })
              .then(() => {
                // 初始化表单数据
                this.initFormData();
                this.$emit('confirm');
                this.visible = false;
              })
              .finally(() => {
                this.loading = false;
              });
          }
        });
      },
      // 页面显示时触发
      open() {
        // 初始化表单数据
        this.initFormData();
        this.sourceMenuIds = [];

        if (this.isCopy) {
          this.loadCopyData();
          return;
        }

        this.onGenerateCode();
      },
      loadCopyData() {
        this.loading = true;
        Promise.all([api.get(this.sourceRoleId), roleMenuApi.menus(this.sourceRoleId), this.onGenerateCode()])
          .then(([role, menus]) => {
            this.formData = {
              code: this.formData.code,
              permission: '',
              description: role.description || '',
              name: '',
              shortName: '',
              categoryId: role.categoryId || '',
            };
            this.sourceMenuIds = (menus || []).filter((item) => item.selected).map((item) => item.id);
          })
          .finally(() => {
            this.loading = false;
          });
      },
      onGenerateCode() {
        return generateCode(GENERATE_CODE_TYPE.ROLE.code).then((res) => {
          this.formData.code = res;
          return res;
        });
      },
      copyRoleMenus(createRes) {
        if (!this.sourceMenuIds.length) {
          return Promise.resolve(true);
        }

        return this.resolveCreatedRoleId(createRes).then((roleId) => {
          if (!roleId) {
            createWarning('角色已创建，但未能自动复制权限，请手动为新角色授权。');
            return false;
          }

          return roleMenuApi
            .setting({
              roleIds: [roleId],
              menuIds: this.sourceMenuIds,
            })
            .then(() => true);
        });
      },
      resolveCreatedRoleId(createRes) {
        const roleId = typeof createRes === 'string' ? createRes : createRes?.id;
        if (roleId) {
          return Promise.resolve(roleId);
        }

        return api
          .query({
            pageIndex: 1,
            pageSize: 1,
            code: this.formData.code,
            name: '',
            sortField: '',
            sortOrder: '',
          })
          .then((res) => {
            return res?.datas?.[0]?.id || '';
          });
      },
    },
  });
</script>
