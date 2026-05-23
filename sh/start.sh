#!/bin/bash

# jvm参数
jvm_ops="-Xms256m -Xmx256m"
script_dir="$(cd "$(dirname "$0")" && pwd)"

# JAR 查找: 优先同目录（发布包），其次 target/（开发环境）
if ls "$script_dir"/easy-unidbg-boot-server-*.jar >/dev/null 2>&1; then
    jar_file="$script_dir/easy-unidbg-boot-server-*.jar"
else
    jar_file="$script_dir/../target/easy-unidbg-boot-server-*.jar"
fi

# 使用方式:
#   1. 基础模式:  ./start.sh
#   2. 热更新模式: ./start.sh hot
#   3. 静态加载:   ./start.sh static ./tasks/DcWtf.class
#   4. 静态+热更:  ./start.sh both ./tasks/DcWtf.class

cd "$script_dir"
mode=${1:-hot}

case "$mode" in
  basic)
    echo "Starting in basic mode (no modules)..."
    java ${jvm_ops} -jar ${jar_file}
    ;;
  static)
    shift
    if [ $# -eq 0 ]; then
      echo "Usage: ./sh/start.sh static <class_file> [class_file2 ...]"
      exit 1
    fi
    args=""
    for f in "$@"; do
      args="$args --F=$f"
    done
    echo "Starting with static files: $@"
    java ${jvm_ops} -jar ${jar_file} ${args}
    ;;
  both)
    shift
    if [ $# -eq 0 ]; then
      echo "Usage: ./sh/start.sh both <class_file> [class_file2 ...]"
      exit 1
    fi
    args=""
    for f in "$@"; do
      args="$args --F=$f"
    done
    echo "Starting with static files + hot reload: $@"
    java ${jvm_ops} -jar ${jar_file} ${args} --U=./tasks
    ;;
  hot|*)
    echo "Starting in hot-reload mode (watch ./tasks)..."
    java ${jvm_ops} -jar ${jar_file} --U=./tasks
    ;;
esac
