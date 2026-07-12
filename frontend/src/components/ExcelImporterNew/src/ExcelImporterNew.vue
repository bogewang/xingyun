<!--跟ExcelImportor不同之处是不使用taskId, 不查询进度-->
<template>
  <div>
    <a-modal
      v-model:open="visible"
      :mask-closable="false"
      :get-container="getContainer"
      :wrap-class-name="wrapClassName"
      width="40%"
      title="导入"
      :style="{ top: '20px' }"
      :footer="null"
      @cancel="visible = false"
    >
      <div v-loading="loading">
        <div>
          <a-upload-dragger
            name="file"
            accept=".xls,.xlsx"
            :custom-request="doUpload"
            :show-upload-list="false"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text"> 点击或拖拽文件进行导入 </p>
            <p class="ant-upload-hint"> 仅支持xls、xlsx格式 </p>
          </a-upload-dragger>
          <div style="margin-bottom: 8px"></div>
          <slot name="form"></slot>
          <div style="padding: 0 5px">
            <span
              v-if="!isEmpty(tipMsg)"
              style="font-size: 12px; color: #999999; white-space: pre-wrap"
              >{{ tipMsg }}</span
            >
          </div>
          <div class="content-wrapper">
            <a-space>
              <a-button type="link" block @click="doDownloadTemplate"> 下载模板文件</a-button>
            </a-space>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>
<script>
  import { defineComponent } from 'vue';
  import { InboxOutlined } from '@ant-design/icons-vue';
  import { isEmpty } from '@/utils/utils';
  import { createError } from '@/hooks/web/msg';

  export default defineComponent({
    name: 'ExcelImporter',
    components: {
      InboxOutlined,
    },
    props: {
      downloadTemplateUrl: {
        type: Function,
        required: true,
      },
      uploadUrl: {
        type: Function,
        required: true,
      },
      tipMsg: {
        type: String,
        default: '',
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
      getContainer: {
        type: [Function, Boolean],
        default: undefined,
      },
      localContainer: {
        type: Boolean,
        default: false,
      },
      hideOnDeactivated: {
        type: Boolean,
        default: false,
      },
      closeAfterFinish: {
        type: Boolean,
        default: true,
      },
    },
    setup() {
      return {
        isEmpty,
      };
    },
    data() {
      return {
        visible: false,
        loading: false,
        restoreVisibleOnActivated: false,
      };
    },
    computed: {
      wrapClassName() {
        return this.localContainer ? 'excel-importer-local-wrap' : undefined;
      },
    },
    activated() {
      if (!this.hideOnDeactivated || !this.restoreVisibleOnActivated) {
        return;
      }

      this.visible = true;
      this.restoreVisibleOnActivated = false;
    },
    deactivated() {
      if (!this.hideOnDeactivated || !this.visible) {
        return;
      }

      this.restoreVisibleOnActivated = true;
      this.visible = false;
    },
    methods: {
      async resolveErrorMessage(err) {
        if (!err) {
          return '导入失败，请稍后重试！';
        }

        const parseTextMessage = (text) => {
          if (!text) {
            return '';
          }

          try {
            const data = JSON.parse(text);
            return data?.msg || data?.message || data?.error?.message || text;
          } catch (e) {
            return text;
          }
        };

        if (err instanceof Blob) {
          return parseTextMessage(await err.text());
        }

        if (err?.data instanceof Blob) {
          return parseTextMessage(await err.data.text());
        }

        if (typeof err === 'string') {
          return parseTextMessage(err);
        }

        return (
          err?.msg ||
          err?.message ||
          err?.error?.message ||
          err?.data?.msg ||
          err?.data?.message ||
          '导入失败，请稍后重试！'
        );
      },
      openDialog() {
        this.visible = true;
      },
      closeDialog() {
        this.visible = false;
      },
      doDownloadTemplate() {
        this.loading = true;
        this.downloadTemplateUrl(this.formData).finally(() => {
          this.loading = false;
        });
      },
      doUpload(e) {
        this.loading = true;
        this.uploadUrl(
          Object.assign(
            {
              file: e.file,
            },
            this.formData,
          ),
        )
          .then((res) => {
            this.$emit('confirm', res);
            if (this.closeAfterFinish) {
              this.closeDialog();
            }
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  });
</script>
<style lang="less" scoped>
  .content-wrapper {
    text-align: center;
  }

  :global(.excel-importer-local-container) {
    position: relative;
    overflow: hidden;
  }

  :global(.excel-importer-local-container .ant-modal-mask),
  :global(.excel-importer-local-container .excel-importer-local-wrap) {
    position: absolute !important;
    inset: 0;
  }
</style>
