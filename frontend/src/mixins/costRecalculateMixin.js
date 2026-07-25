import moment from 'moment';
import { startMonthEndRecalculate, stepMonthEndRecalculate } from '@/api/sc/sale/out';
import { createSuccess } from '@/hooks/web/msg';

/**
 * 成本重算进度管理 mixin
 *
 * 提供逐天成本重算流程：启动 → 逐天 step → 进度展示 → 失败重试。
 * 宿主组件需提供：
 *   - this.costRefreshDateRange   选择的重算日期范围 [start, end]
 *   - this.search()               重算完成后的列表刷新方法
 */
export const costRecalculateMixin = {
  data() {
    return {
      // 成本重算进度相关
      recalculating: false,
      recalcProgress: 0,
      recalcTotalDays: 0,
      recalcTaskId: '',
      recalcFailedDate: '',
      recalcErrorMsg: '',
      recalcAccumulatedSheets: 0,
      recalcAccumulatedDetails: 0,
      recalcAccumulatedNotFilled: 0,
    };
  },
  computed: {
    /**
     * 成本重算进度百分比
     */
    recalcProgressPercent() {
      if (!this.recalcTotalDays) return 0;
      return Math.round((this.recalcProgress / this.recalcTotalDays) * 100);
    },
    /**
     * 成本重算遮罩提示文案
     */
    recalcLoadingTip() {
      if (this.recalcFailedDate) {
        return `重算失败于 ${this.recalcFailedDate}，已完成 ${this.recalcProgress}/${this.recalcTotalDays} 天`;
      }
      return `正在进行成本重算... ${this.recalcProgress}/${this.recalcTotalDays}`;
    },
  },
  methods: {
    /**
     * 发起成本重算 —— 关闭弹窗后启动进度遮罩，逐天执行重算
     */
    async recalculate() {
      const [calcBeginDate, calcEndDate] = this.costRefreshDateRange || [];
      if (!calcBeginDate || !calcEndDate) {
        return;
      }

      this.costRefreshVisible = false;
      this.recalcResetState();

      try {
        // 1. 启动任务，获取缓存的均价
        const startRes = await startMonthEndRecalculate({
          calcBeginDate,
          calcEndDate,
        });
        this.recalcTaskId = startRes.taskId;
        this.recalcTotalDays = startRes.totalDays;

        // 2. 逐天执行
        let current = moment(calcBeginDate);
        const end = moment(calcEndDate);

        while (current.isSameOrBefore(end)) {
          const processDate = current.format('YYYY-MM-DD');
          const stepRes = await stepMonthEndRecalculate({
            taskId: this.recalcTaskId,
            processDate,
          });

          if (stepRes.hasError) {
            this.recalcFailedDate = processDate;
            this.recalcErrorMsg = stepRes.errorMsg || '未知错误';
            return;
          }

          this.recalcProgress++;
          this.recalcAccumulatedSheets += stepRes.updatedSheetCount || 0;
          this.recalcAccumulatedDetails += stepRes.updatedDetailCount || 0;
          this.recalcAccumulatedNotFilled += stepRes.notFilledCount || 0;
          current.add(1, 'day');
        }

        // 全部完成
        this.recalculating = false;
        createSuccess(
          `重算完成：更新单据 ${this.recalcAccumulatedSheets} 条，明细 ${this.recalcAccumulatedDetails} 条` +
            (this.recalcAccumulatedNotFilled > 0 ? `，${this.recalcAccumulatedNotFilled} 条未填充` : ''),
        );
        this.search();
      } catch (e) {
        this.recalcFailedDate = this.costRefreshDateRange?.[0] || '';
        this.recalcErrorMsg = e?.message || '网络请求失败';
      }
    },
    /**
     * 从失败日期重新开始重算，复用缓存的均价（taskId 不变）
     */
    async retryRecalculate() {
      if (!this.recalcTaskId || !this.recalcFailedDate) return;

      const failedMoment = moment(this.recalcFailedDate);
      const end = moment(this.costRefreshDateRange[1]);

      this.recalcErrorMsg = '';
      this.recalcFailedDate = '';

      try {
        let current = failedMoment;

        while (current.isSameOrBefore(end)) {
          const processDate = current.format('YYYY-MM-DD');
          const stepRes = await stepMonthEndRecalculate({
            taskId: this.recalcTaskId,
            processDate,
          });

          if (stepRes.hasError) {
            this.recalcFailedDate = processDate;
            this.recalcErrorMsg = stepRes.errorMsg || '未知错误';
            return;
          }

          this.recalcProgress++;
          this.recalcAccumulatedSheets += stepRes.updatedSheetCount || 0;
          this.recalcAccumulatedDetails += stepRes.updatedDetailCount || 0;
          this.recalcAccumulatedNotFilled += stepRes.notFilledCount || 0;
          current.add(1, 'day');
        }

        this.recalculating = false;
        createSuccess(
          `重算完成：更新单据 ${this.recalcAccumulatedSheets} 条，明细 ${this.recalcAccumulatedDetails} 条` +
            (this.recalcAccumulatedNotFilled > 0 ? `，${this.recalcAccumulatedNotFilled} 条未填充` : ''),
        3000);
        this.search();
      } catch (e) {
        this.recalcFailedDate = failedMoment.format('YYYY-MM-DD');
        this.recalcErrorMsg = e?.message || '网络请求失败';
      }
    },
    /**
     * 取消重算，关闭遮罩并刷新列表
     */
    cancelRecalculate() {
      this.recalculating = false;
      this.search();
    },
    /**
     * 重置重算相关状态
     */
    recalcResetState() {
      this.recalculating = true;
      this.recalcFailedDate = '';
      this.recalcErrorMsg = '';
      this.recalcProgress = 0;
      this.recalcTotalDays = 0;
      this.recalcTaskId = '';
      this.recalcAccumulatedSheets = 0;
      this.recalcAccumulatedDetails = 0;
      this.recalcAccumulatedNotFilled = 0;
    },
  },
};
