import { createVNode } from 'vue';
import { message as Message, Modal, Input } from 'ant-design-vue';
import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
import * as utils from '@/utils/utils';

const renderMultilineContent = (message: string) =>
  createVNode(
    'div',
    {
      style: {
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
      },
    },
    message,
  );

export const createError = function (message: string): void {
  Message.error(renderMultilineContent(message));
};

export const createErrorDialog = function (message: string, title: string = '错误提示'): void {
  Modal.error({
    title: title,
    content: renderMultilineContent(message),
  });
};

export const createWarning = function (message: string): void {
  Message.warning(message);
};

export const createWarningDialog = function (message: string, title: string = '提示信息'): void {
  Modal.warning({
    title: title,
    content: renderMultilineContent(message),
  });
};

export type ConfirmOptions = {
  okText?: string;
  cancelText?: string;
};

export const createConfirm = function (
  message: string,
  title: string = '提示信息',
  options: ConfirmOptions = {},
): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const defaultOptions = {
      okText: '确定',
      cancelText: '取消',
    } as ConfirmOptions;
    const finalOptions = { ...defaultOptions, ...options };
    Modal.confirm({
      title: title,
      content: renderMultilineContent(message),
      onOk: () => resolve(),
      onCancel: () => reject(),
      okText: finalOptions.okText,
      cancelText: finalOptions.cancelText,
    });
  });
};

export const createSuccess = function (message: string, duration = 1000): void {
  createSuccessAutoClose(message, duration);
};

export const createSuccessAutoClose = function (message: string, duration = 1000): void {
  const modal = Modal.success({
    title: '提示信息',
    content: renderMultilineContent(message),
  });

  setTimeout(() => {
    modal.destroy();
  }, duration);
};

export const createSuccessTip = function (message: string): void {
  Message.success(message);
};

export const createPrompt = function (
  message: string,
  {
    inputPattern,
    inputErrorMessage,
    title,
    inputValue,
    required,
    confirmOnEnter,
    autoFocus,
  }: {
    inputPattern: RegExp;
    inputErrorMessage: string;
    title: string;
    inputValue?: any;
    required?: boolean;
    confirmOnEnter?: boolean;
    autoFocus?: boolean;
  },
): Promise<{ value: string }> {
  return new Promise<{ value: string }>((resolve) => {
    const datas: { text: string } = {
      text: '',
    };
    const change = (e) => {
      datas.text = e.target.value;
    };
    const pressEnter = (e: KeyboardEvent) => {
      if (!confirmOnEnter) {
        return;
      }

      const input = e.currentTarget as HTMLElement;
      input
        .closest('.ant-modal-content')
        ?.querySelector<HTMLButtonElement>('.ant-modal-confirm-btns .ant-btn-primary')
        ?.click();
    };
    Modal.confirm({
      title: title,
      content: createVNode('div', null, [
        createVNode(Input, { onInput: change, onPressEnter: pressEnter }),
      ]),
      icon: createVNode(ExclamationCircleOutlined),
      autoFocusButton: autoFocus ? null : 'ok',
      wrapClassName: autoFocus ? 'prompt-auto-focus-wrap' : undefined,
      okText: '确定',
      cancelText: '取消',
      onOk() {
        return new Promise<void>((r, j) => {
          if (required) {
            if (utils.isEmpty(datas.text)) {
              const errorMsg = message || '请输入信息';
              createError(errorMsg);
              return j();
            }
          }
          if (utils.isEmpty(datas.text)) {
            datas.text = inputValue || '';
          }
          if (inputPattern) {
            if (!inputPattern.test(datas.text)) {
              const errorMsg = inputErrorMessage || '输入信息格式有误';
              createError(errorMsg);
              return j();
            }
          }

          r();
          resolve({ value: datas.text });
        });
      },
    });
    if (autoFocus) {
      setTimeout(() => {
        const dialogs = document.querySelectorAll('.prompt-auto-focus-wrap');
        const dialog = dialogs[dialogs.length - 1];
        dialog?.querySelector<HTMLInputElement>('input')?.focus();
      }, 300);
    }
  });
};
