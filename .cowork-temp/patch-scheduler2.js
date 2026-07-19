const fs = require('fs');
const p = 'D:/梦枕/projects/src/lib/background-scheduler.ts';
let c = fs.readFileSync(p, 'utf8');
c = c.replace(
  'private async executeTask(taskId: string) {\n    const task = this.tasks.get(taskId);\n    if (!task || task.status !== \'pending\') return;',
  'private async executeTask(taskId: string) {\n    // 原生环境：播放由原生 MediaPlayer 处理，JS 层跳过\n    if (isNativeEnvironment()) return;\n    const task = this.tasks.get(taskId);\n    if (!task || task.status !== \'pending\') return;'
);
fs.writeFileSync(p, c);
console.log('done');
