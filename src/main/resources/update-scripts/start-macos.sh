# macOS: 检测是否在 .app 包中运行
if [[ "{{CURRENT_JAR_PATH}}" == *".app/Contents/"* ]]; then
    # 在 .app 包中，需要重启整个应用
    APP_PATH=$(echo "{{CURRENT_JAR_PATH}}" | sed 's|/Contents/.*||')
    echo "Restarting app bundle: $APP_PATH"
    open "$APP_PATH" &
    APP_STARTED=$?
else
    # 独立 JAR 文件，直接启动
    echo "Starting standalone JAR"
    nohup java -jar "{{CURRENT_JAR_PATH}}" > /dev/null 2>&1 &
    APP_STARTED=$?
fi

