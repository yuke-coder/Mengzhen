import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 清洁文本：去除多余空行和冗余空白，保持段落结构清晰
 * - 统一换行符为 \n
 * - 合并连续空行为单空行（保留段落分隔）
 * - 去除每行首尾空白
 */
export function cleanDisplayText(text: string): string {
  const normalized = text.replace(/\r\n?/g, '\n');
  const lines = normalized
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);
  return lines.join('\n\n');
}
