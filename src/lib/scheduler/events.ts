/**
 * 事件发射器 - 精简版
 */
import { SchedulerEvent } from './core';

type EventCallback = (event: SchedulerEvent) => void;

export class EventEmitter {
  private listeners = new Map<string, Set<EventCallback>>();

  on(type: string, callback: EventCallback): () => void {
    if (!this.listeners.has(type)) this.listeners.set(type, new Set());
    this.listeners.get(type)!.add(callback);
    return () => this.off(type, callback);
  }

  off(type: string, callback: EventCallback): void {
    this.listeners.get(type)?.delete(callback);
  }

  emit(event: SchedulerEvent): void {
    this.listeners.get(event.type)?.forEach(cb => {
      try { cb(event); } catch {}
    });
  }
}
