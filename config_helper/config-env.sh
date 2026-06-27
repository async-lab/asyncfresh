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

ENV_VARS=$(cat <<'EOF'
DB_URL=jdbc:mysql://localhost:3306/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB
DB_USERNAME=epoch
DB_PASSWORD=123456
JWT_SECRET=please-change-this-to-a-strong-secret-with-at-least-32-chars
JWT_COOKIE_SECURE=false
JWT_COOKIE_SAME_SITE=Strict
AUTH_CACHE_TYPE=redis
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
    (return 0 2>/dev/null)
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
