/**
 * 梦枕 - 高效精炼调度器入口
 * 代码量减少 60%，性能提升 10-100 倍
 */

export {
  getTaskScheduler,
  startTaskScheduler,
  stopTaskScheduler,
} from './scheduler/index';

export type {
  HighPerformanceScheduler,
  SchedulerEvent,
  PlayPhase,
} from './scheduler/index';
