#!/usr/bin/env bash

# 使用说明：
# 1. 直接修改下面“用户配置区”中的变量值。
# 2. 将 MODE 改为 temporary 或 permanent。
# 3. 运行方式：
#    - 临时环境变量（推荐 source，直接写入当前 shell）：
#      source ./config-env.sh
#    - 或直接执行（会自动进入一个带这些变量的新 shell）：
#      bash ./config-env.sh
#    - 永久环境变量：
#      bash ./config-env.sh
#
# 说明：
# - temporary 模式下，如果你不是用 source 执行，脚本会启动一个新的交互式 shell，
#   变量只在那个 shell 生命周期内有效。
# - permanent 模式会把变量写入 PERSIST_FILE，并自动覆盖同名旧配置。

set -euo pipefail

# =========================
# 用户配置区
# =========================
MODE="temporary" # 可选：temporary / permanent
PERSIST_FILE="${HOME}/.bashrc"

# 参数说明与配置建议：
# - SERVER_PORT：后端监听端口；反向代理部署时建议保持内网端口并由 Nginx/网关对外暴露。
# - DB_URL / DB_USERNAME / DB_PASSWORD：MySQL 连接信息；生产环境建议使用独立账号并限制权限。
# - JWT_SECRET：JWT 签名密钥，必须至少 32 字符；生产环境应使用高强度随机字符串。
# - JWT_COOKIE_SECURE：HTTPS 部署建议设为 true；本地 HTTP 联调可设为 false。
# - JWT_COOKIE_SAME_SITE：同站前后端建议 Strict；跨站部署需结合 HTTPS 和前端域名策略调整。
# - AUTH_CACHE_TYPE：当前后端只支持 redis，保持 redis。
# - SPRING_DATA_REDIS_*：Redis 连接配置；认证验证码、登录失败锁和 JWT 黑名单依赖 Redis。
# - AUTH_EMAIL_CODE_*：验证码 IP/全局限流；校内百级用户通常保留默认即可，公开部署可适当收紧。
# - AUTH_TRUST_FORWARD_HEADERS / AUTH_TRUSTED_PROXIES：反向代理后部署才开启；只信任明确的代理 IP 或网段。
# - APPLICATION_ALLOWED_GRADES：允许报名年级，默认 YEAR_1,YEAR_2。
# - APP_STORAGE_*：本地文件存储目录、分片开关、清理策略和文件类型白名单；生产建议使用独立持久化目录。
# - APP_STORAGE_CHUNK_SIZE：业务分片大小，也是上传模式开关；0B 表示只允许直传，设置为 5MB、8MB 等大于 0 的值表示启用分片上传并禁用直传。
# - 启用分片上传建议：APP_STORAGE_CHUNK_SIZE=5MB，MULTIPART_MAX_FILE_SIZE 不小于该分片大小，MULTIPART_MAX_REQUEST_SIZE 比分片大小略大以容纳 multipart 开销。
# - MULTIPART_*：Servlet 层单次 HTTP 请求上传上限，不是业务分片大小；应不小于业务附件上限或分片大小，具体取决于直传或分片模式。
# - TASK_ATTACHMENT_MAX_SIZE：任务附件和任务提交附件的业务上限。
# - APP_AUDIT_MAJOR_EVENT_LOG_FILE_PATH：重大事件审计日志文件路径，生产建议放在可持久化日志目录。
# - MAIL_SMTP_* / MAIL_ACCOUNT / MAIL_AUTH_CODE：SMTP 邮件服务配置；465 通常配 SSL=true、STARTTLS=false，587 通常配 SSL=false、STARTTLS=true。
# - DEFAULT_ADMIN_*：首次部署且数据库无管理员时用于自动创建管理员；已有管理员后不会重复创建。
# - 当前后端默认按同源部署提供接口；如前后端分离跨域部署，建议通过反向代理统一域名。
# - Flyway 默认启用用于自动建表/迁移；生产首次上线建议保持开启，变更前先备份数据库。
# - MODE=temporary：只影响当前 shell，适合本地调试；MODE=permanent：写入 PERSIST_FILE，适合固定部署环境。
ENV_VARS=$(cat <<'EOF'
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3306/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB
DB_USERNAME=epoch
DB_PASSWORD=123456
JWT_SECRET=please-change-this-to-a-strong-secret-with-at-least-32-chars
JWT_COOKIE_SECURE=false
JWT_COOKIE_SAME_SITE=Strict
AUTH_CACHE_TYPE=redis
SPRING_DATA_REDIS_HOST=127.0.0.1
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_DATABASE=0
AUTH_EMAIL_CODE_IP_SEND_WINDOW_SECONDS=3600
AUTH_EMAIL_CODE_MAX_IP_SEND_COUNT=30
AUTH_EMAIL_CODE_GLOBAL_SEND_WINDOW_SECONDS=60
AUTH_EMAIL_CODE_MAX_GLOBAL_SEND_COUNT=300
AUTH_TRUST_FORWARD_HEADERS=false
AUTH_TRUSTED_PROXIES=
APPLICATION_ALLOWED_GRADES=YEAR_1,YEAR_2
APP_STORAGE_ROOT=./storage
APP_STORAGE_CHUNK_SIZE=0B
APP_STORAGE_CLEANUP_ENABLED=true
APP_STORAGE_TEMP_SESSION_TTL=24h
APP_STORAGE_ORPHAN_FILE_TTL=24h
APP_STORAGE_ALLOWED_EXTENSIONS=pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar,7z,txt,md,png,jpg,jpeg,gif,webp,json,java,py,c,cpp,js,ts
APP_STORAGE_ALLOWED_CONTENT_TYPES=application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation,application/zip,application/x-zip-compressed,application/x-rar-compressed,application/x-7z-compressed,text/plain,text/markdown,text/x-markdown,text/markdown;charset=UTF-8,image/png,image/jpeg,image/gif,image/webp,application/json,text/javascript,application/javascript,text/x-java-source,text/x-python,text/x-c,text/x-c++src
TASK_ATTACHMENT_MAX_SIZE=20MB
MULTIPART_MAX_FILE_SIZE=256MB
MULTIPART_MAX_REQUEST_SIZE=256MB
APP_AUDIT_MAJOR_EVENT_LOG_FILE_PATH=./logs/business-audit.log
MAIL_SMTP_HOST=smtpdm.aliyun.com
MAIL_SMTP_PORT=465
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_STARTTLS_ENABLE=false
MAIL_ACCOUNT=epoch@mail.cuit.dev
MAIL_AUTH_CODE=M7qN2vK8pR4xT9cL6zA3
DEFAULT_ADMIN_ENABLED=true
DEFAULT_ADMIN_USERNAME=epochlab
DEFAULT_ADMIN_PASSWORD=Aa123456
DEFAULT_ADMIN_EMAIL=epoch@mail.cuit.dev
EOF
)

is_sourced() {
    [[ "${BASH_SOURCE[0]}" != "$0" ]]
}

validate_mode() {
    case "${MODE}" in
        temporary|permanent) ;;
        *)
            echo "MODE 只允许为 temporary 或 permanent，当前值：${MODE}" >&2
            exit 1
            ;;
    esac
}

validate_env_vars() {
    if [[ -z "${ENV_VARS}" ]]; then
        echo "ENV_VARS 不能为空。" >&2
        exit 1
    fi
}

shell_escape() {
    printf "%s" "$1" | sed "s/'/'\\\\''/g"
}

apply_temporary() {
    while IFS='=' read -r name value; do
        [[ -z "${name}" ]] && continue
        export "${name}=${value}"
        echo "[temporary] ${name} 已写入当前 shell 环境。"
    done <<< "${ENV_VARS}"

    echo
    echo "临时环境变量配置完成。"

    if ! is_sourced; then
        echo "当前不是 source 执行，下面会进入一个新的交互式 shell。退出该 shell 后，临时变量随之失效。"
        exec "${SHELL:-/bin/bash}" -i
    fi
}

apply_permanent() {
    local tmp_file
    tmp_file="$(mktemp)"

    if [[ -f "${PERSIST_FILE}" ]]; then
        cp "${PERSIST_FILE}" "${tmp_file}"
    else
        touch "${tmp_file}"
    fi

    while IFS='=' read -r name value; do
        [[ -z "${name}" ]] && continue
        grep -vE "^export ${name}=" "${tmp_file}" > "${tmp_file}.next" || true
        mv "${tmp_file}.next" "${tmp_file}"
        printf "export %s='%s'\n" "${name}" "$(shell_escape "${value}")" >> "${tmp_file}"
        echo "[permanent] ${name} 已写入 ${PERSIST_FILE}。"
    done <<< "${ENV_VARS}"

    mv "${tmp_file}" "${PERSIST_FILE}"
    echo
    echo "永久环境变量配置完成。请执行：source ${PERSIST_FILE}，或重新打开终端。"
}

validate_mode
validate_env_vars

case "${MODE}" in
    temporary)
        apply_temporary
        ;;
    permanent)
        apply_permanent
        ;;
esac
