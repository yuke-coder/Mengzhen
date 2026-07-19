// 梦枕 Service Worker — 支持后台定时唤醒
const SW_VERSION = '2.0.0';
const SCHEDULED_TASKS_KEY = 'dream_pillow_scheduled_tasks';
const BG_SYNC_TAG = 'dream-pillow-check-tasks';

// 活跃的定时器
const activeTimers = new Map();

self.addEventListener('install', () => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      await caches.keys().then((keys) => Promise.all(keys.map((key) => caches.delete(key))));
      await self.clients.claim();
      // 恢复之前存储的任务定时
      restoreScheduledTasks();
    })()
  );
});

// ========== 任务调度 ==========

async function restoreScheduledTasks() {
  try {
    const tasks = await getScheduledTasks();
    const now = Date.now();
    for (const task of tasks) {
      const delay = task.nextExecAt - now;
      if (delay > 0) {
        scheduleTaskTimer(task, delay);
      } else if (delay > -60000) {
        // 刚过期 1 分钟内，立即触发
        triggerTask(task);
      }
      // 过期太久的不处理
    }
  } catch (e) {
    console.error('[SW] 恢复定时任务失败:', e);
  }
}

function getScheduledTasks() {
  return new Promise((resolve) => {
    const req = indexedDB.open('DreamPillowSW', 1);
    req.onupgradeneeded = () => {
      req.result.createObjectStore('tasks', { keyPath: 'id' });
    };
    req.onsuccess = () => {
      const db = req.result;
      const tx = db.transaction('tasks', 'readonly');
      const store = tx.objectStore('tasks');
      const getAll = store.getAll();
      getAll.onsuccess = () => resolve(getAll.result || []);
      getAll.onerror = () => resolve([]);
    };
    req.onerror = () => resolve([]);
  });
}

function saveScheduledTasks(tasks) {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open('DreamPillowSW', 1);
    req.onupgradeneeded = () => {
      req.result.createObjectStore('tasks', { keyPath: 'id' });
    };
    req.onsuccess = () => {
      const db = req.result;
      const tx = db.transaction('tasks', 'readwrite');
      const store = tx.objectStore('tasks');
      store.clear();
      for (const task of tasks) {
        store.put(task);
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    };
    req.onerror = () => reject(req.error);
  });
}

function scheduleTaskTimer(task, delay) {
  // 清除旧定时器
  if (activeTimers.has(task.id)) {
    clearTimeout(activeTimers.get(task.id));
  }

  // SW setTimeout 也有上限，但我们依赖 Periodic Sync 来续命
  // 对于超过 5 分钟的延迟，设置一个 5 分钟的检查点
  const MAX_SW_TIMER = 4.5 * 60 * 1000; // 4.5 分钟，留有余地

  if (delay > MAX_SW_TIMER) {
    // 设置中间检查点
    const timerId = setTimeout(() => {
      activeTimers.delete(task.id);
      // 重新评估：重新读取任务，计算剩余延迟
      revisitTask(task.id);
    }, MAX_SW_TIMER);
    activeTimers.set(task.id, timerId);
  } else if (delay > 0) {
    // 直接设置到期定时器
    const timerId = setTimeout(() => {
      activeTimers.delete(task.id);
      triggerTask(task);
    }, delay);
    activeTimers.set(task.id, timerId);
  } else {
    // 立即触发
    triggerTask(task);
  }
}

async function revisitTask(taskId) {
  const tasks = await getScheduledTasks();
  const task = tasks.find(t => t.id === taskId);
  if (!task) return;
  const delay = task.nextExecAt - Date.now();
  if (delay > 0) {
    scheduleTaskTimer(task, delay);
  } else if (delay > -60000) {
    triggerTask(task);
  }
}

async function triggerTask(task) {
  console.log(`[SW] 触发任务: ${task.name} (${task.id})`);

  // 显示通知
  const title = `🌙 ${task.name}`;
  const options = {
    body: `定时播放已到，点击开始播放`,
    icon: '/favicon.png',
    badge: '/favicon.png',
    tag: `task-${task.id}`,
    requireInteraction: true,
    vibrate: [200, 100, 200],
    data: {
      taskId: task.id,
      taskName: task.name,
      action: 'play-task'
    },
    actions: [
      { action: 'play', title: '开始播放' },
      { action: 'dismiss', title: '忽略' }
    ]
  };

  try {
    await self.registration.showNotification(title, options);
  } catch (e) {
    console.error('[SW] 显示通知失败:', e);
  }

  // 尝试找到已打开的客户端并发送消息
  const clients = await self.clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  });

  for (const client of clients) {
    client.postMessage({
      type: 'SW_TASK_TRIGGER',
      taskId: task.id,
      taskName: task.name,
      timestamp: Date.now()
    });
  }

  // 如果没有打开的客户端，打开一个
  if (clients.length === 0) {
    try {
      await self.clients.openWindow('/settings');
    } catch (e) {
      console.error('[SW] 打开窗口失败:', e);
    }
  }

  // 清理该任务
  removeTask(task.id);
}

// ========== 消息处理 ==========

self.addEventListener('message', async (event) => {
  const { type, payload } = event.data || {};

  switch (type) {
    case 'SYNC_TASKS': {
      // 主页面同步任务列表给 SW
      const { tasks } = payload;
      await saveScheduledTasks(tasks);
      // 清除所有旧定时器，重新调度
      activeTimers.forEach((timerId, taskId) => {
        clearTimeout(timerId);
      });
      activeTimers.clear();
      for (const task of tasks) {
        const delay = task.nextExecAt - Date.now();
        if (delay > 0) {
          scheduleTaskTimer(task, delay);
        }
      }
      break;
    }

    case 'REMOVE_TASK': {
      const { taskId } = payload;
      removeTask(taskId);
      break;
    }

    case 'CLEAR_ALL': {
      activeTimers.forEach((timerId) => clearTimeout(timerId));
      activeTimers.clear();
      await saveScheduledTasks([]);
      break;
    }

    case 'PING': {
      // ping, 返回当前状态
      if (event.source) {
        event.source.postMessage({
          type: 'SW_PONG',
          activeTasks: activeTimers.size,
          timestamp: Date.now()
        });
      }
      break;
    }

    case 'REGISTER_SYNC': {
      // 注册 Periodic Background Sync
      try {
        if ('periodicSync' in self.registration) {
          await self.registration.periodicSync.register(BG_SYNC_TAG, {
            minInterval: 5 * 60 * 1000 // 最短 5 分钟
          });
          console.log('[SW] Periodic sync registered');
        }
      } catch (e) {
        console.warn('[SW] Periodic sync not available:', e);
      }
      break;
    }
  }
});

async function removeTask(taskId) {
  if (activeTimers.has(taskId)) {
    clearTimeout(activeTimers.get(taskId));
    activeTimers.delete(taskId);
  }
  const tasks = await getScheduledTasks();
  const filtered = tasks.filter(t => t.id !== taskId);
  await saveScheduledTasks(filtered);
}

// ========== 通知点击处理 ==========

self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  const { taskId, action } = event.notification.data || {};
  const userAction = event.action || action || 'play';

  if (userAction === 'dismiss') {
    return;
  }

  event.waitUntil(
    (async () => {
      const clients = await self.clients.matchAll({
        type: 'window',
        includeUncontrolled: true
      });

      // 找到已有窗口并聚焦
      for (const client of clients) {
        if (client.url.includes('/settings')) {
          await client.focus();
          client.postMessage({
            type: 'SW_NOTIFICATION_CLICK',
            taskId,
            action: 'play'
          });
          return;
        }
      }

      // 没有已有窗口，打开新窗口
      const newClient = await self.clients.openWindow(
        `/settings?taskId=${taskId}&action=play`
      );

      if (newClient) {
        // 稍等一下让页面加载
        await new Promise(r => setTimeout(r, 1000));
        newClient.postMessage({
          type: 'SW_NOTIFICATION_CLICK',
          taskId,
          action: 'play'
        });
      }
    })()
  );
});

// ========== 后台同步 ==========

self.addEventListener('periodicsync', (event) => {
  if (event.tag === BG_SYNC_TAG) {
    event.waitUntil(
      (async () => {
        console.log('[SW] Periodic sync triggered');
        const tasks = await getScheduledTasks();

        if (tasks.length === 0) return;

        const now = Date.now();
        // 清除旧定时器
        activeTimers.forEach((timerId) => clearTimeout(timerId));
        activeTimers.clear();

        // 重新调度所有任务
        for (const task of tasks) {
          const delay = task.nextExecAt - now;
          if (delay > 0) {
            scheduleTaskTimer(task, delay);
          } else if (delay > -60000) {
            triggerTask(task);
          }
        }
      })()
    );
  }
});

// ========== 网络请求 ==========

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  event.respondWith(fetch(event.request));
});
