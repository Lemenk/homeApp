#!/usr/bin/env bash
# 启动本机 MySQL 8（手动安装，非 brew 管理）
# 用法：./start-mysql.sh [start|stop|status]
set -e

MYSQL_BIN="/Users/zhangxiaotong/mysql-download/mysql-8.0.46-macos15-arm64/bin"
DATADIR="/Users/zhangxiaotong/mysql-data"
PORT=3306
SOCKET=/tmp/mysql.sock

case "${1:-start}" in
  start)
    if lsof -iTCP:${PORT} -sTCP:LISTEN >/dev/null 2>&1; then
      echo "MySQL 已在运行 (端口 ${PORT})"
    else
      "${MYSQL_BIN}/mysqld" \
        --basedir="$(dirname ${MYSQL_BIN})/.." \
        --datadir="${DATADIR}" \
        --port=${PORT} --socket=${SOCKET} \
        --pid-file=/tmp/mysql.pid \
        > /tmp/mysql.log 2>&1 &
      echo "MySQL 启动中 (端口 ${PORT})，日志: /tmp/mysql.log"
    fi
    ;;
  stop)
    if [ -f /tmp/mysql.pid ]; then
      kill "$(cat /tmp/mysql.pid)" && echo "MySQL 已停止" || echo "停止失败"
    else
      echo "未找到 /tmp/mysql.pid"
    fi
    ;;
  status)
    if lsof -iTCP:${PORT} -sTCP:LISTEN >/dev/null 2>&1; then
      echo "MySQL 运行中 (端口 ${PORT})"
      "${MYSQL_BIN}/mysql" -uroot -proot -h127.0.0.1 --port=${PORT} -e "SELECT VERSION();" 2>/dev/null
    else
      echo "MySQL 未运行"
    fi
    ;;
  *)
    echo "用法: $0 [start|stop|status]"; exit 1;;
esac
