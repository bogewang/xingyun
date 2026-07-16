import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ContentTypeEnum } from '@/enums/httpEnum';
import * as productApi from '@/api/base-data/product/info';
import { QueryProductBo } from '@/api/base-data/product/info/model/queryProductBo';
import { QueryProductVo } from '@/api/base-data/product/info/model/queryProductVo';
import { ConcurrentPromise } from '@/utils/concurrentPromise';
import { buildProductAvailabilityRequest } from '../productAvailability';
import BatchHandler from '../../../../../components/BatchHandler/src/BatchHandler.vue';

const httpPut = vi.hoisted(() => vi.fn());

vi.mock('/@/utils/http/axios', () => ({
  defHttp: {
    put: httpPut,
  },
}));

vi.mock('@/hooks/web/msg', () => ({
  createConfirm: vi.fn(() => Promise.resolve()),
  createErrorDialog: vi.fn(),
}));

interface BatchRow {
  /** 商品 ID。 */
  id: string;
  /** 批量处理状态。 */
  __status?: number;
  /** 批量处理错误信息。 */
  __errorMsg?: string;
}

interface BatchContext {
  /** 是否正在批量处理。 */
  loading: boolean;
  /** 复制后的批量处理数据。 */
  copyedTableData: BatchRow[];
  /** 当前处理索引。 */
  currentIndex: number;
  /** 并发处理器。 */
  concurrentPromise: undefined;
  /** 批量处理回调。 */
  batchHandleFn?: (records: BatchRow[]) => Promise<void>;
  /** 并发数量。 */
  concurrency: number;
  /** 单行处理回调。 */
  handleFn: ReturnType<typeof vi.fn>;
  /** 事件触发函数。 */
  emit: ReturnType<typeof vi.fn>;
  /** 错误信息解析函数。 */
  resolveErrorMessage: (error: unknown) => string;
  /** Vue 事件触发函数。 */
  $emit: ReturnType<typeof vi.fn>;
}

interface BatchMethods {
  /** 执行批量处理。 */
  onBegin(this: BatchContext): void;
  /** 解析错误信息。 */
  resolveErrorMessage(this: BatchContext, error: unknown): string;
}

const batchMethods = (BatchHandler as unknown as { methods: BatchMethods }).methods;

describe('商品批量状态请求', () => {
  it('全部状态使用空值，启用和禁用使用布尔值', () => {
    const enabled: Pick<QueryProductVo, 'available'> = { available: true };
    const disabled: Pick<QueryProductVo, 'available'> = { available: false };
    const all: Pick<QueryProductVo, 'available'> = { available: '' };
    const product: Pick<QueryProductBo, 'available'> = { available: true };

    expect(enabled).toEqual({ available: true });
    expect(disabled).toEqual({ available: false });
    expect(all).toEqual({ available: '' });
    expect(product).toEqual({ available: true });
  });

  it('使用 JSON PUT 发送批量状态请求', async () => {
    const request = { ids: ['p-1'], available: true };
    httpPut.mockResolvedValue(undefined);

    await productApi.updateAvailable(request);

    expect(httpPut).toHaveBeenCalledWith(
      {
        url: '/basedata/product/available',
        data: request,
      },
      {
        contentType: ContentTypeEnum.JSON,
        region: 'cloud-api',
      },
    );
  });

  it('去重商品 ID 并保留目标状态', () => {
    expect(
      buildProductAvailabilityRequest([{ id: 'p-1' }, { id: 'p-1' }, { id: 'p-2' }], false),
    ).toEqual({ ids: ['p-1', 'p-2'], available: false });
  });

  describe('批量处理组件', () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it('成功时仅调用一次批量回调并逐行确认', async () => {
      const batchHandleFn = vi.fn().mockResolvedValue(undefined);
      const context = createBatchContext(batchHandleFn);

      batchMethods.onBegin.call(context);
      await flushPromises();

      expect(batchHandleFn).toHaveBeenCalledTimes(1);
      expect(batchHandleFn).toHaveBeenCalledWith(context.copyedTableData);
      expect(context.handleFn).toHaveBeenCalledTimes(0);
      expect(context.copyedTableData.map((item) => item.__status)).toEqual([2, 2]);
      expect(context.emit.mock.calls).toEqual([
        ['confirm-row', context.copyedTableData[0]],
        ['confirm-row', context.copyedTableData[1]],
        ['confirm'],
      ]);
    });

    it('失败时仅调用一次批量回调并将所有行标记为失败', async () => {
      const batchHandleFn = vi.fn().mockRejectedValue(new Error('批量更新失败'));
      const context = createBatchContext(batchHandleFn);

      batchMethods.onBegin.call(context);
      await flushPromises();

      expect(batchHandleFn).toHaveBeenCalledTimes(1);
      expect(context.copyedTableData.map((item) => item.__status)).toEqual([3, 3]);
      expect(context.copyedTableData.map((item) => item.__errorMsg)).toEqual([
        '批量更新失败',
        '批量更新失败',
      ]);
      expect(context.emit.mock.calls).toEqual([
        ['confirm-row', context.copyedTableData[0]],
        ['confirm-row', context.copyedTableData[1]],
        ['confirm'],
      ]);
    });

    it('缺省批量回调时逐行调用旧回调并按旧分支确认', async () => {
      const context = createBatchContext(undefined);

      batchMethods.onBegin.call(context);
      await flushPromises();

      expect(context.concurrentPromise).toBeInstanceOf(ConcurrentPromise);
      expect(context.handleFn).toHaveBeenCalledTimes(context.copyedTableData.length);
      expect(context.handleFn).toHaveBeenNthCalledWith(1, context.copyedTableData[0]);
      expect(context.handleFn).toHaveBeenNthCalledWith(2, context.copyedTableData[1]);
      expect(context.copyedTableData.map((item) => item.__status)).toEqual([2, 2]);
      expect(context.emit.mock.calls).toEqual([
        ['confirm-row', context.copyedTableData[0]],
        ['confirm-row', context.copyedTableData[1]],
        ['confirm'],
      ]);
    });
  });
});

/**
 * 创建批量处理组件方法测试上下文。
 * @param batchHandleFn 批量处理回调
 */
function createBatchContext(
  batchHandleFn: BatchContext['batchHandleFn'] | undefined,
): BatchContext {
  const emit = vi.fn();
  const context: BatchContext = {
    loading: false,
    copyedTableData: [{ id: 'p-1' }, { id: 'p-2' }],
    currentIndex: 0,
    concurrentPromise: undefined,
    batchHandleFn,
    concurrency: 2,
    handleFn: vi.fn().mockResolvedValue(undefined),
    emit,
    resolveErrorMessage: batchMethods.resolveErrorMessage,
    $emit: emit,
  };

  return context;
}
