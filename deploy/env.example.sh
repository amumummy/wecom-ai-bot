#!/bin/bash
# 企微三件套之外的回调密钥——复制本文件为 env.sh 后填入真实值
# Token / EncodingAESKey 来自企微后台「接收消息 → 设置API接收」页
# Secret 来自应用详情页（项目收尾时记得在后台重置一次）
# PAT 为扣子「个人访问令牌」，权限勾选 bot + conversation 读写
export WECOM_TOKEN="填后台拿到的Token"
export WECOM_AES_KEY="填43位EncodingAESKey"
export WECOM_SECRET="填应用Secret"
export WECOM_COZE_PAT="填Coze个人令牌"
