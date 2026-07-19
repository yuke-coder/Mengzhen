const fs = require('fs');
const p = 'D:/梦枕/projects/src/lib/background-scheduler.ts';
let c = fs.readFileSync(p, 'utf8');
if (!c.includes('isNativeEnvironment')) {
  c = c.replace(
    'import { getAllTasks } from "./task-store";',
    'import { getAllTasks } from "./task-store";\nimport { isNativeEnvironment } from "./native-scheduler";'
  );
  fs.writeFileSync(p, c);
  console.log('added');
} else {
  console.log('already present');
}
