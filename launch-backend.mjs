/**
 * 后端启动器（跨过 .cmd 的中文编码坑）
 *   node launch-backend.mjs
 * 由仓库根目录的 start-backend.cmd 调用，也可以自己直接跑。
 */
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));

// Spring Boot 3.5.x 支持 Java 17~24，不支持 Java 25，所以必须校验主版本号而不是「目录存在就用」
const REQUIRED_JAVA_MAJOR = 21;
const REQUIRED_JAVA_RANGE = '17~24';
const JAVA_MAJOR_MAX = 24;

// 本机 JDK 的常见位置；也可以用环境变量 PMS_JDK21 显式指定
const JDK_CANDIDATES = [
  process.env.PMS_JDK21,
  'C:\\Program Files\\Java\\jdk-21.0.12',
  'C:\\Program Files\\Java\\jdk-21',
  'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12+7',
  process.env.JAVA_HOME
].filter(Boolean);

function line(text = '') {
  process.stdout.write(text + '\n');
}

// Windows 上 Node 不能直接 spawn 一个 .cmd/.bat（会立刻失败），必须过一层 cmd.exe /c。
function run(command, args, options) {
  const windows = process.platform === 'win32';
  const isBatch = /\.(cmd|bat)$/i.test(command);
  if (windows && isBatch) {
    return spawnSync('cmd.exe', ['/d', '/s', '/c', command, ...args], options);
  }
  return spawnSync(command, args, options);
}

// 在 PATH 里找一个可执行文件（用 where/which，避开 Windows 上 spawn 探测器时的各种坑）
function findOnPath(name) {
  const probe = run(process.platform === 'win32' ? 'where.exe' : 'which', [name], { encoding: 'utf8' });
  const first = `${probe.stdout || ''}`.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)[0];
  return first && existsSync(first) ? first : null;
}

// 读某个 java.exe 的主版本号；读不到返回 null
function javaMajor(dir) {
  const exe = join(dir, 'bin', 'java.exe');
  const probe = run(exe, ['-version'], { encoding: 'utf8' });
  const out = `${probe.stderr || ''}${probe.stdout || ''}`;
  const m = out.match(/version "(\d+)(?:\.(\d+))?/);
  if (!m) return null;
  const major = Number(m[1]);
  // 1.8.0_xxx 这种旧格式要换算成 8
  return major === 1 && m[2] ? Number(m[2]) : major;
}

function findJdk() {
  const dirs = [...JDK_CANDIDATES];
  const javaRoot = 'C:\\Program Files\\Java';
  if (existsSync(javaRoot)) {
    for (const name of readdirSync(javaRoot).filter((n) => /^jdk-?\d/i.test(n))) {
      dirs.push(join(javaRoot, name));
    }
  }
  let lastRejected = null;
  let firstWith21 = null;
  let probed = false;
  for (const dir of dirs) {
    if (!existsSync(join(dir, 'bin', 'java.exe'))) continue;
    if (/\b21\b|jdk-?21/i.test(dir)) firstWith21 ??= { dir, major: undefined };
    const major = javaMajor(dir);
    if (major === null) continue; // 读不到版本（受限环境），跳过
    probed = true;
    if (major === REQUIRED_JAVA_MAJOR) return { dir, major };
    lastRejected = { dir, major };
  }
  if (probed) return lastRejected ? { rejected: lastRejected } : null;
  // 一个版本都读不出来（沙箱禁止父进程抓子进程输出）：退化成按目录名判断
  return firstWith21;
}

line('============================================');
line(' 启动后端  a-simple-pms');
line('============================================');
line();

// 1) JDK：Spring Boot 3.5.x 只支持 Java 17~24，而本机默认 JAVA_HOME 指向 JDK 25。
//    这里按「主版本号」挑，不按目录是否存在挑 —— 否则会选中 JAVA_HOME 里的 JDK 25。
const jdk = findJdk();
if (!jdk || jdk.rejected) {
  line('[错误] 没找到可用的 JDK 21。');
  if (jdk?.rejected) {
    line(`       候选里最新的是 JDK ${jdk.rejected.major}：${jdk.rejected.dir}`);
    line(`       但 Spring Boot 3.5.x 只支持 Java ${REQUIRED_JAVA_RANGE}，不能用。`);
  }
  line('       请安装 JDK 21，或用环境变量 PMS_JDK21 指向它，例如：');
  line('         set PMS_JDK21=C:\\Program Files\\Java\\jdk-21.0.12');
  process.exit(1);
}
const JDK21 = jdk.dir;
if (jdk.major === undefined) {
  line(`[ok] JDK（按目录名判定 21）= ${JDK21}`);
  line('[提示] 当前环境读不到 java -version，已跳过主版本校验');
} else {
  line(`[ok] JDK ${jdk.major} = ${JDK21}`);
  if (jdk.major !== REQUIRED_JAVA_MAJOR) {
    line(`[提示] 建议直接用 JDK ${REQUIRED_JAVA_MAJOR}（Spring Boot 3.5.x 官方支持版本）`);
  }
}

// 2) Maven：优先用 PATH 里的 mvn；找不到才回退到项目内自带的 .tools/apache-maven-3.9.9。
//    注意 .tools/ 已被 .gitignore 排除，别人 clone 之后没有它，所以正常开发机请自装 Maven 3.9+。
const BUNDLED_MAVEN = join(ROOT, '.tools', 'apache-maven-3.9.9', 'bin', 'mvn.cmd');
const systemMaven = findOnPath('mvn.cmd') ?? findOnPath('mvn');
const MAVEN = systemMaven ?? BUNDLED_MAVEN;

if (!existsSync(MAVEN)) {
  line('[错误] 没找到 Maven。');
  line('       请安装 Maven 3.9+ 并确保 mvn 在 PATH 里，');
  line('       或把 Maven 解压到 .tools/apache-maven-3.9.9/ 下。');
  process.exit(1);
}
line(`[ok] Maven = ${MAVEN}${systemMaven ? '（来自 PATH）' : '（项目内自带）'}`);

// 3) 依赖仓库：项目内自带 Maven 时用 .tools/m2repo（受限环境写不了 C:\Users\<你>\.m2）；
//    若用的是 PATH 里的系统 Maven 且项目内没有 .tools（例如别人刚 clone 下来），
//    就交给 Maven 用自己的默认仓库，不强行指定。
const bundledRepo = join(ROOT, '.tools', 'm2repo');
const useBundledRepo = !systemMaven || existsSync(bundledRepo);
const REPO_ARGS = useBundledRepo ? [`-Dmaven.repo.local=${bundledRepo}`] : [];
line(useBundledRepo
  ? `[ok] 本地仓库 = ${bundledRepo}`
  : '[ok] 本地仓库 = Maven 默认（~/.m2/repository）');
line();
line('服务地址 http://127.0.0.1:8080    接口文档 http://127.0.0.1:8080/swagger-ui.html');
line('默认管理员 admin / Admin@123');
line('停止服务：在本窗口按 Ctrl+C');
line();

const result = run(
  MAVEN,
  ['-B', '-ntp', '-f', join(ROOT, 'pom.xml'), ...REPO_ARGS, '-pl', 'backend', 'spring-boot:run'],
  {
    cwd: ROOT,
    stdio: 'inherit',
    env: { ...process.env, JAVA_HOME: JDK21, MAVEN_OPTS: '-Dfile.encoding=UTF-8' }
  }
);

line();
line('============================================');
if (result.error) {
  line(` 启动失败：${result.error.message}`);
} else {
  line(` 后端已退出（exit code ${result.status ?? '未知'}）`);
}
line('============================================');
