'use client';

import { useRef, useEffect, useCallback } from 'react';
import { MindMapData, MindMapNode } from '@/lib/mindmap-types';
import { cn } from '@/lib/utils';

// jsMind 类型声明
interface JsMindInstance {
  mind?: JsMindData;
  get_data(): JsMindData;
  enable_edit(): void;
  get_selected_node(): JsMindNode | null;
  select_node(nodeid: string): void;
  toggle_node(nodeid: string): void;
  expand_to_node(nodeid: string): void;
  collapse_to_node(nodeid: string): void;
  resize(): void;
  update_node(nodeid: string, topic: string): void;
}

interface JsMindData {
  meta?: { name?: string; author?: string; version?: string };
  format?: string;
  data: JsMindNode;
}

interface JsMindNode {
  id?: string;
  nodeid?: string;
  topic?: string;
  isroot?: boolean;
  parentid?: string;
  direction?: string;
  expanded?: boolean;
  children?: JsMindNode[];
  data?: Record<string, unknown>;
}

// 获取全局 jsMind 对象
function getJsMind(): { show(options: { container: HTMLElement; theme?: string; [key: string]: unknown }, mind: JsMindData): JsMindInstance } | undefined {
  return (window as unknown as { jsMind?: { show(options: { container: HTMLElement; theme?: string; [key: string]: unknown }, mind: JsMindData): JsMindInstance } }).jsMind;
}

interface MindMapCanvasProps {
  data: MindMapData;
  className?: string;
  editable?: boolean;
  onNodeClick?: (node: MindMapNode) => void;
  onNodeUpdate?: (nodeId: string, newText: string) => void;
}

// 转换为 jsMind 格式
function convertToJsMindFormat(data: MindMapData): JsMindData {
  const convertNode = (node: MindMapNode, parentId?: string): JsMindNode => {
    const jsNode: JsMindNode = {
      id: node.id,
      topic: node.text,
      isroot: !parentId,
      parentid: parentId || undefined,
      expanded: true,
    };

    if (node.children && node.children.length > 0) {
      jsNode.children = node.children.map(child => convertNode(child, node.id));
    }

    return jsNode;
  };

  return {
    meta: {
      name: data.title || '思维导图',
      author: '灵图',
      version: '1.0',
    },
    format: 'node_tree',
    data: data.root ? convertNode(data.root) : { id: 'root', topic: data.title || '无标题', isroot: true, expanded: true },
  };
}

export function MindMapCanvas({
  data,
  className,
  editable = false,
  onNodeClick,
  onNodeUpdate,
}: MindMapCanvasProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const jmRef = useRef<JsMindInstance | null>(null);
  const jmScriptLoaded = useRef(false);

  // jsMind 本地文件路径（优先使用本地，避免 CDN 超时）
  const JSMDIN_JS_URL = '/lib/jsmind/jsmind.js';
  const JSMDIN_CSS_URL = '/lib/jsmind/jsmind.css';

  // 加载 jsMind（带超时和备用）
  const loadJsMind = useCallback((): Promise<void> => {
    return new Promise((resolve, reject) => {
      if (typeof getJsMind() !== 'undefined') {
        resolve();
        return;
      }

      // 超时处理 - 30秒超时
      const timeoutId = setTimeout(() => {
        console.warn('jsMind loading timeout, trying fallback');
        jmScriptLoaded.current = false;
        reject(new Error('Timeout loading jsMind'));
      }, 30000);

      // 尝试加载
      const tryLoadScript = (urls: string[], index: number = 0) => {
        if (index >= urls.length) {
          clearTimeout(timeoutId);
          console.warn('All jsMind sources failed');
          jmScriptLoaded.current = false;
          reject(new Error('Failed to load jsMind from all sources'));
          return;
        }

        // 加载 CSS
        if (!document.querySelector('link[href*="jsmind"]')) {
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.href = JSMDIN_CSS_URL;
          document.head.appendChild(link);
        }

        // 加载 JS
        const script = document.createElement('script');
        script.src = urls[index];
        script.onload = () => {
          clearTimeout(timeoutId);
          // 等待一小段时间确保脚本执行完成
          setTimeout(() => {
            if (typeof getJsMind() !== 'undefined') {
              resolve();
            } else {
              // 脚本加载了但 jsMind 对象未定义，尝试下一个 URL
              console.warn(`jsMind source ${index + 1} loaded but not initialized, trying next...`);
              tryLoadScript(urls, index + 1);
            }
          }, 100);
        };
        script.onerror = () => {
          console.warn(`jsMind source ${index + 1} failed, trying next...`);
          tryLoadScript(urls, index + 1);
        };
        document.head.appendChild(script);
      };

      // 避免重复加载
      if (jmScriptLoaded.current) {
        const check = setInterval(() => {
          if (typeof getJsMind() !== 'undefined') {
            clearInterval(check);
            clearTimeout(timeoutId);
            resolve();
          }
        }, 100);
        return;
      }

      jmScriptLoaded.current = true;
      // 优先使用本地文件，备用 CDN
      const urls = [JSMDIN_JS_URL, '/lib/jsmind/jsmind.js', 'https://cdn.jsdelivr.net/npm/jsmind@0.9.1/js/jsmind.js'];
      tryLoadScript(urls);
    });
  }, []);

  // 初始化 jsMind
  const initJsMind = useCallback(async () => {
    if (!containerRef.current) return;

    try {
      await loadJsMind();

      const jsMind = getJsMind();
      if (!jsMind || !containerRef.current) return;

      // 清理旧实例
      if (jmRef.current) {
        try {
          jmRef.current.mind = undefined;
        } catch {
          // 忽略
        }
        jmRef.current = null;
      }

      // 清空容器
      containerRef.current.innerHTML = '';

      // 创建 jsMind 选项 - 使用 DOM 渲染模式以支持 CSS 样式
      const options = {
        container: containerRef.current,
        theme: 'greensea',
        view: {
          engine: 'dom',
          linewidth: 2,
          lineheight: 20,
          align: 'center',
          dragsize: 20,
          mousedown_factor: 1,
          node_font: '14px PingFang SC, Microsoft YaHei, sans-serif',
          show_icon: true,
          show_remove_icon: true,
        },
        layout: {
          hspace: 80,
          vspace: 30,
          pspace: 20,
          concave: true,
          pack: true,
        },
        shortcut: {
          enable: true,
        },
      };

      // 初始化 jsMind
      const jm = jsMind.show(options, convertToJsMindFormat(data));
      jmRef.current = jm;

      // 设置编辑模式
      if (editable) {
        jm.enable_edit();
      }

      // 监听节点点击
      containerRef.current.addEventListener('click', (e) => {
        const target = e.target as HTMLElement;
        const nodeElement = target.closest('.jsmind-node');
        if (nodeElement) {
          const nodeid = nodeElement.getAttribute('nodeid');
          if (nodeid && jmRef.current) {
            const selectedNode = jmRef.current.get_selected_node();
            if (selectedNode) {
              // 找到对应的 MindMapNode
              const findNode = (nodes: MindMapNode[], id: string): MindMapNode | null => {
                for (const node of nodes) {
                  if (node.id === id) return node;
                  if (node.children) {
                    const found = findNode(node.children, id);
                    if (found) return found;
                  }
                }
                return null;
              };
              const clickedNode = data.root ? findNode([data.root], nodeid) : null;
              if (clickedNode) {
                onNodeClick?.(clickedNode);
              }
            }
          }
        }
      });
    } catch (error) {
      console.error('Failed to initialize jsMind:', error);
    }
  }, [data, editable, loadJsMind, onNodeClick]);

  // 初始化
  useEffect(() => {
    initJsMind();

    return () => {
      if (jmRef.current) {
        try {
          jmRef.current.mind = undefined;
        } catch {
          // 忽略
        }
        jmRef.current = null;
      }
    };
  }, [initJsMind]);

  // 处理节点更新
  useEffect(() => {
    if (onNodeUpdate && jmRef.current && editable) {
      const handleNodeUpdate = (nodeId: string, newText: string) => {
        jmRef.current?.update_node(nodeId, newText);
      };

      // 将回调挂到 window 上供 jsMind 编辑器使用
      (window as unknown as Record<string, unknown>).__jsmind_node_update = handleNodeUpdate;
    }
  }, [editable, onNodeUpdate]);

  return (
    <div className={cn('relative w-full h-full overflow-hidden', className)}>
      <div
        ref={containerRef}
        className="w-full h-full"
        style={{ backgroundColor: '#1a1a2e' }}
      />
      <style jsx global>{`
        .jsmind-container {
          width: 100% !important;
          height: 100% !important;
          overflow: auto !important;
          background-color: #1a1a2e !important;
          position: relative !important;
        }
        .jsmind-inner {
          width: 100% !important;
          height: 100% !important;
          overflow: visible !important;
          position: relative !important;
        }
        
        /* 节点样式 - DOM 渲染模式 */
        .jsmind-node {
          background-color: #16213e !important;
          border: 1px solid #0f3460 !important;
          border-radius: 8px !important;
          color: #e8e8e8 !important;
          font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
          font-size: 14px !important;
          padding: 8px 16px !important;
          min-width: 100px !important;
          max-width: 300px !important;
          white-space: normal !important;
          word-break: break-word !important;
          overflow: visible !important;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
          line-height: 1.5 !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          text-align: center !important;
        }
        
        /* 节点内部文本 */
        .jsmind-node .jsmind-icon,
        .jsmind-node .jsmind-text,
        .jsmind-node span,
        .jsmind-node div,
        .jsmind-node .topic {
          color: #e8e8e8 !important;
          background: transparent !important;
        }
        
        .jsmind-node:hover {
          background-color: #1f4068 !important;
          border-color: #00d4aa !important;
          transform: translateY(-3px) !important;
          box-shadow: 0 6px 20px rgba(0, 212, 170, 0.3) !important;
          transition: all 0.2s ease !important;
        }
        .jsmind-node.selected {
          background: linear-gradient(135deg, #00d4aa, #00a884) !important;
          border: none !important;
          color: #fff !important;
          font-weight: bold !important;
        }
        .jsmind-node.selected .topic {
          color: #fff !important;
        }
        .jsmind-node.root {
          background: linear-gradient(135deg, #00d4aa, #00a884) !important;
          border: none !important;
          color: #fff !important;
          font-weight: bold !important;
          font-size: 16px !important;
          padding: 12px 24px !important;
          min-width: 150px !important;
          max-width: 350px !important;
          box-shadow: 0 4px 16px rgba(0, 212, 170, 0.4) !important;
        }
        .jsmind-node.root .topic {
          color: #fff !important;
        }
        
        /* 连接线 */
        .jsmind-lines {
          stroke: #0f3460 !important;
        }
        
        /* Canvas 样式 */
        .jsmind-canvas {
          background-color: #1a1a2e !important;
        }
        .jsmind-ovcanvas {
          background-color: transparent !important;
        }

        /* 确保容器可以滚动 */
        .jsmind-container::-webkit-scrollbar {
          width: 8px;
          height: 8px;
        }
        .jsmind-container::-webkit-scrollbar-track {
          background: rgba(255,255,255,0.05);
          border-radius: 4px;
        }
        .jsmind-container::-webkit-scrollbar-thumb {
          background: rgba(255,255,255,0.2);
          border-radius: 4px;
        }
        .jsmind-container::-webkit-scrollbar-thumb:hover {
          background: rgba(255,255,255,0.3);
        }

        /* 响应式样式 */
        @media (max-width: 768px) {
          .jsmind-node {
            font-size: 12px !important;
            padding: 6px 12px !important;
            min-width: 80px !important;
            max-width: 200px !important;
          }
          .jsmind-node.root {
            font-size: 14px !important;
            padding: 10px 18px !important;
            min-width: 120px !important;
          }
        }

        /* 触摸设备优化 */
        @media (pointer: coarse) {
          .jsmind-node {
            min-width: 90px !important;
            padding: 10px 14px !important;
          }
        }
      `}</style>
    </div>
  );
}
