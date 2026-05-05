<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="40%"
    title="复制模板"
    :style="{ top: '20px' }"
    :footer="null"
  >
    <div v-if="visible" v-permission="['base-data:print-template:add']" v-loading="loading">
      <a-form
        ref="form"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        :model="formData"
        :rules="rules"
      >
        <a-form-item label="名称" name="name">
          <a-input v-model:value.trim="formData.name" allow-clear />
        </a-form-item>
        <a-form-item label="业务类型" name="bizType">
          <a-select v-model:value="formData.bizType" placeholder="请选择业务类型" allow-clear>
            <a-select-option
              v-for="item in printTypeOptions"
              :key="item.code"
              :value="String(item.code)"
            >
              {{ item.desc }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div class="form-modal-footer">
          <a-space>
            <a-button type="primary" :loading="loading" html-type="submit" @click="submit">
              保存
            </a-button>
            <a-button :loading="loading" @click="closeDialog">取消</a-button>
          </a-space>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>

<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/print-template';
  import { createSuccess } from '@/hooks/web/msg';
  import { PRINT_TYPE } from '@/enums/biz/printType';

  export default defineComponent({
    name: 'CopyPrintTemplate',
    props: {
      source: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        visible: false,
        loading: false,
        formData: {},
        rules: {
          name: [{ required: true, message: '请输入名称' }],
          bizType: [{ required: true, message: '请选择业务类型' }],
        },
      };
    },
    computed: {
      printTypeOptions() {
        return PRINT_TYPE.values();
      },
    },
    created() {
      this.initFormData();
    },
    methods: {
      openDialog() {
        this.visible = true;
        this.$nextTick(() => this.open());
      },
      closeDialog() {
        this.visible = false;
        this.$emit('close');
      },
      initFormData() {
        this.formData = {
          sourceId: '',
          name: '',
          bizType: '',
        };
      },
      open() {
        this.formData = {
          sourceId: this.source.id,
          name: this.source.name ? `${this.source.name}-副本` : '',
          bizType:
            this.source.bizType === null || this.source.bizType === undefined
              ? ''
              : String(this.source.bizType),
        };
      },
      submit() {
        this.$refs.form.validate().then((valid) => {
          if (valid) {
            this.loading = true;
            api
              .copy(this.formData)
              .then(() => {
                createSuccess('复制成功！');
                this.$emit('confirm');
                this.visible = false;
              })
              .finally(() => {
                this.loading = false;
              });
          }
        });
      },
    },
  });
</script>
