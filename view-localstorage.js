
const { JSDOM } = require('jsdom');
const fs = require('fs');
const path = require('path');

// 读取 localStorage 数据
const localStorageFile = path.join(__dirname, 'localStorage.json');
let localStorageData = {};

try {
  if (fs.existsSync(localStorageFile)) {
    const rawData = fs.readFileSync(localStorageFile, 'utf8');
    localStorageData = JSON.parse(rawData);
  }
} catch (error) {
  console.error('读取 localStorage 数据失败:', error);
}

// 创建 jsdom 实例
const dom = new JSDOM('<!DOCTYPE html>', {
  runScripts: 'dangerously',
  resources: 'usable'
});

// 模拟 localStorage
dom.window.localStorage = {
  getItem: (key) => localStorageData[key] || null,
  setItem: (key, value) => { localStorageData[key] = value; },
  removeItem: (key) => { delete localStorageData[key]; },
  clear: () => { localStorageData = {}; },
  length: Object.keys(localStorageData).length,
  key: (index) => Object.keys(localStorageData)[index] || null
};

// 设置全局变量
global.window = dom.window;
global.document = dom.window.document;
global.navigator = dom.window.navigator;

// 尝试读取任务数据
try {
  const { getAllTasks } = require('./src/lib/task-store');
  const tasks = getAllTasks();
  console.log('所有任务数据:', JSON.stringify(tasks, null, 2));

  // 查找特定任务
  const task = tasks.find(t => t.id === 'task_1783599597453_9mfmye');
  if (task) {
    console.log('--------------------------');
    console.log('任务 task_1783599597453_9mfmye 数据:', JSON.stringify(task, null, 2));
  } else {
    console.log('--------------------------');
    console.log('未找到任务 task_1783599597453_9mfmye');
  }
} catch (error) {
  console.error('读取任务数据失败:', error);
}
