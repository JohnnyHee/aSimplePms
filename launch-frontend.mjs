/**
 * 前端启动器：直接用 node 调 vite，绕开 pnpm 的执行封装与 .cmd 的中文编码问题。
 *   node launch-frontend.mjs
 * 由仓库根目录的 start-frontend.cmd 调用，也可以自己直接跑。
 */
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const FRONTEND = join(dirname(fileURLToPath(import.meta.url)), 'frontend');
const VITE = join(FRONTEND, 'node_modules', 'vite', 'bin', 'vite.js');

function line(text = '') {
  process.stdout.write(text + '\n');
}

line('============================================');
line(' 启动前端  pms-frontend');
line('============================================');
line(`目录：${FRONTEND}`);

if (!existsSync(VITE)) {
  line();
  line('[错误] 找不到 node_modules/vite，依赖还没安装。');
  line(`       请先执行：cd "${FRONTEND}"`);
  line('                 pnpm install --node-linker hoisted');
  process.exit(1);
}

const nodeVersion = spawnSync(process.execPath, ['-v'], { encoding: 'utf8' });
line(`[ok] node ${(nodeVersion.stdout || '').trim()}`);
line(`[ok] vite = ${VITE}`);
line();
line('打开 http://localhost:5173    默认管理员 admin / Admin@123');
line('前提：后端已在 127.0.0.1:8080 运行（否则登录会提示网络错误）');
line('停止服务：在本窗口按 Ctrl+C');
line();

const result = spawnSync(process.execPath, [VITE, '--port', '5173'], {
  cwd: FRONTEND,
  stdio: 'inherit'
});

line();
line('============================================');
line(` 前端已退出（exit code ${result.status ?? '未知'}）`);
line('============================================');
