# Linux: 直接启动 JAR
nohup java -jar "{{CURRENT_JAR_PATH}}" > /dev/null 2>&1 &
APP_STARTED=$?

