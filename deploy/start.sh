#!/bin/bash
# 启动脚本：./start.sh   停止：pkill -f wecom-ai-bot
cd "$(dirname "$0")"

if [ ! -f env.sh ]; then
    echo "缺少 env.sh —— 先 cp env.example.sh env.sh 并填入真实密钥"; exit 1
fi
if [ ! -f wecom-ai-bot-1.0.0.jar ]; then
    echo "缺少 wecom-ai-bot-1.0.0.jar"; exit 1
fi

source env.sh
nohup java -jar wecom-ai-bot-1.0.0.jar > bot.log 2>&1 &
echo "已启动，日志: tail -f bot.log"
