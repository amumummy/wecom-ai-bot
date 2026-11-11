# 作品3 企微机器人 一键部署脚本 v3.1（配合 wecom-ai-bot-deploy.zip 使用）
# 所有失败都会停在屏幕上显示原因，不会闪退
$ErrorActionPreference = "Stop"
$SERVER = "root@<你的服务器IP>"
$REMOTE_DIR = "/opt/wecom-ai-bot-deploy"

function Fail($msg) {
    Write-Host ""
    Write-Host "[X] $msg" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  作品3 企微机器人 一键部署 v3.1" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan

# 0. 前置检查（zip 可能在外层解压目录，自动拷贝过来）
$zipPath = "wecom-ai-bot-deploy.zip"
if (!(Test-Path $zipPath) -and (Test-Path "..\wecom-ai-bot-deploy.zip")) {
    Copy-Item "..\wecom-ai-bot-deploy.zip" . -Force
}
if (!(Test-Path $zipPath)) {
    Fail "未找到 wecom-ai-bot-deploy.zip（当前目录和上级目录都没有）"
}

# 1. 上传部署包
Write-Host ""
Write-Host "[1/5] 上传部署包到服务器 /opt/（按提示输入SSH密码）..." -ForegroundColor Yellow
scp $zipPath "${SERVER}:/opt/"
if ($LASTEXITCODE -ne 0) { Fail "上传失败：检查网络或密码" }

# 2. 远程解压 + 环境检查
Write-Host ""
Write-Host "[2/5] 远程解压 + 环境检查（输入SSH密码）..." -ForegroundColor Yellow
ssh $SERVER "cd /opt && (unzip -o wecom-ai-bot-deploy.zip 2>/dev/null || python3 -m zipfile -e wecom-ai-bot-deploy.zip /opt/) && cd wecom-ai-bot-deploy && chmod +x start.sh && echo '--- 8080端口 ---' && (ss -tlnp | grep ':8080 ' || echo '8080 空闲') && echo '--- 解压部署完成 ---'"
if ($LASTEXITCODE -ne 0) { Fail "远程命令失败（看上方输出）" }

# 2b. Java 检查 + 自动安装
Write-Host ""
Write-Host "[2b/5] 检查服务器 Java 环境（输入SSH密码）..." -ForegroundColor Yellow
$javaCheck = ssh $SERVER "java -version 2>&1 | head -1"
if ("$javaCheck" -notmatch "version") {
    Write-Host "服务器未装 Java，自动安装 OpenJDK 11（约1-3分钟，请耐心等）..." -ForegroundColor Yellow
    ssh $SERVER "apt-get update -qq; (apt-get install -y openjdk-11-jre-headless 2>/dev/null || apt-get install -y openjdk-17-jre-headless) 2>&1 | tail -2; echo '--- 安装结果 ---'; java -version 2>&1 | head -1"
    $verify = ssh $SERVER "java -version 2>&1 | head -1"
    if ("$verify" -notmatch "version") { Fail "Java 自动安装失败。备选：点阿里云控制台右上角【远程连接】进网页终端手动执行：apt-get install -y openjdk-11-jre-headless || apt-get install -y openjdk-17-jre-headless" }
    Write-Host "[OK] Java 安装完成：$verify" -ForegroundColor Green
} else {
    Write-Host "[OK] Java 已存在：$javaCheck" -ForegroundColor Green
}

# 3. 本地填密钥（已填好则跳过；自动补 Coze PAT 占位行）
$needFill = $true
if (Test-Path "env.sh") {
    $existing = Get-Content "env.sh" -Raw -ErrorAction SilentlyContinue
    if ($existing -and !($existing -match "WECOM_COZE_PAT=")) {
        Add-Content "env.sh" "`r`nexport WECOM_COZE_PAT=填Coze个人令牌"
        $existing = Get-Content "env.sh" -Raw
        Write-Host "检测到新增 Coze PAT 配置项，已自动追加占位行" -ForegroundColor Yellow
    }
    if ($existing -and !($existing -match "填后台|填43|填应用|填Coze")) { $needFill = $false }
}
if ($needFill) {
    Copy-Item "env.example.sh" "env.sh" -Force
    Write-Host ""
    Write-Host "[3/5] 记事本已打开 env.sh，请填入三个值后 保存并关闭：" -ForegroundColor Yellow
    Write-Host "      WECOM_TOKEN   = 企微后台回调配置页的 Token" -ForegroundColor Gray
    Write-Host "      WECOM_AES_KEY = 43位 EncodingAESKey" -ForegroundColor Gray
    Write-Host "      WECOM_SECRET  = 应用详情页的 Secret" -ForegroundColor Gray
    notepad "env.sh"
    $envContent = Get-Content "env.sh" -Raw
    if ($envContent -match "填后台|填43|填应用|填Coze") {
        Fail "env.sh 里还有未填的占位符。重新双击运行即可继续"
    }
} else {
    Write-Host ""
    Write-Host "[3/5] 检测到 env.sh 已填好，跳过编辑" -ForegroundColor Green
}

# 4. 上传配置 + 启动
Write-Host ""
Write-Host "[4/5] 上传密钥配置并启动服务（输入SSH密码 x2）..." -ForegroundColor Yellow
scp env.sh "${SERVER}:${REMOTE_DIR}/env.sh"
if ($LASTEXITCODE -ne 0) { Fail "配置上传失败" }
ssh $SERVER "cd $REMOTE_DIR && chmod 600 env.sh && ./start.sh && sleep 6 && echo '--- 启动日志 ---' && tail -8 bot.log"
if ($LASTEXITCODE -ne 0) { Fail "启动失败（看上方日志）" }

# 5. 本机连通性测试（= 验证安全组 8080 已放行）
Write-Host ""
Write-Host "[5/5] 从本机测试 <你的服务器IP>:8080 连通性（约10秒）..." -ForegroundColor Yellow
$r = Test-NetConnection -ComputerName "<你的服务器IP>" -Port 8080 -WarningAction SilentlyContinue
if ($r.TcpTestSucceeded) {
    Write-Host ""
    Write-Host "[OK] 8080 可达，服务已对公网开放！" -ForegroundColor Green
    Write-Host ""
    Write-Host "====> 最后一步：回企微后台 应用详情 -> 接收消息 -> 设置API接收 -> 点【保存】" -ForegroundColor Cyan
    Write-Host "      触发企微验证请求，服务器日志出现 [URL验证] 通过 = 阶段A验收通过！" -ForegroundColor Cyan
} else {
    Fail "8080 不通 —— 服务大概率没起来（看上面第4步日志）。若确认服务已启动仍不通：阿里云轻量服务器控制台 -> 防火墙 -> 添加规则 8080/TCP"
}
Write-Host ""
Read-Host "全部完成，按回车键退出"
