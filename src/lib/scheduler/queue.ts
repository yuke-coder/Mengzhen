/**
 * 优先队列 - 精炼高效版
 * 二叉堆实现，O(log n) 插入/删除
 */
export class PriorityQueue<T> {
  private heap: T[] = [];
  constructor(private compare: (a: T, b: T) => number) {}

  get size(): number { return this.heap.length; }
  peek(): T | undefined { return this.heap[0]; }

  push(item: T): void {
    this.heap.push(item);
    this.bubbleUp(this.heap.length - 1);
  }

  pop(): T | undefined {
    if (this.heap.length === 0) return;
    if (this.heap.length === 1) return this.heap.pop();
    const result = this.heap[0];
    this.heap[0] = this.heap.pop()!;
    this.bubbleDown(0);
    return result;
  }

  clear(): void { this.heap = []; }

  private bubbleUp(i: number): void {
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (this.compare(this.heap[i], this.heap[p]) >= 0) break;
      [this.heap[i], this.heap[p]] = [this.heap[p], this.heap[i]];
      i = p;
    }
  }

  private bubbleDown(i: number): void {
    const len = this.heap.length;
    while (true) {
      let s = i;
      const l = (i << 1) + 1;
      const r = l + 1;
      if (l < len && this.compare(this.heap[l], this.heap[s]) < 0) s = l;
      if (r < len && this.compare(this.heap[r], this.heap[s]) < 0) s = r;
      if (s === i) break;
      [this.heap[i], this.heap[s]] = [this.heap[s], this.heap[i]];
      i = s;
    }
  }
}
