[CmdletBinding()]
param()

<#
使用说明：
1. 直接修改下面“用户配置区”中的变量值。
2. 将 $Mode 改为 "Temporary" 或 "Permanent"。
3. 在 PowerShell 中运行：
   - 临时环境变量：.\config-env.ps1
   - 永久环境变量：.\config-env.ps1

说明：
- Temporary 模式会把环境变量写入当前 PowerShell 进程。
- Permanent 模式会把环境变量写入当前用户级环境变量，重开终端后生效。
- 如果你是从 cmd、IDE 或双击脚本启动的独立 PowerShell 进程，
  Temporary 模式只会影响那个 PowerShell 进程本身。
#>

# =========================
# 用户配置区
# =========================
$Mode = "Temporary" # 可选：Temporary / Permanent

$EnvironmentVariables = [ordered]@{
    DB_URL                    = "jdbc:mysql://localhost:3306/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB"
    DB_USERNAME               = "epoch"
    DB_PASSWORD               = "123456"
    JWT_SECRET                = "please-change-this-to-a-strong-secret-with-at-least-32-chars"
    JWT_COOKIE_SECURE         = "false"
    JWT_COOKIE_SAME_SITE      = "Strict"
    AUTH_CACHE_TYPE           = "redis"
    MAIL_SMTP_HOST            = "smtpdm.aliyun.com"
    MAIL_SMTP_PORT            = "465"
    MAIL_SMTP_SSL_ENABLE      = "true"
    MAIL_SMTP_STARTTLS_ENABLE = "false"
    MAIL_ACCOUNT              = "epoch@mail.cuit.dev"
    MAIL_AUTH_CODE            = "M7qN2vK8pR4xT9cL6zA3"
    DEFAULT_ADMIN_ENABLED     = "true"
    DEFAULT_ADMIN_USERNAME    = "epochlab"
    DEFAULT_ADMIN_PASSWORD    = "Aa123456"
    DEFAULT_ADMIN_EMAIL       = "epoch@mail.cuit.dev"
}

function Test-ModeValid {
    param(
        [string]$Value
    )

    return $Value -in @("Temporary", "Permanent")
}

function Set-TemporaryEnvironmentVariables {
    param(
        [System.Collections.IDictionary]$Variables
    )

    foreach ($entry in $Variables.GetEnumerator()) {
        $name = [string]$entry.Key
        $value = [string]$entry.Value
        Set-Item -Path "Env:$name" -Value $value
        Write-Host "[Temporary] $name 已写入当前 PowerShell 会话。" -ForegroundColor Green
    }
}

function Set-PermanentEnvironmentVariables {
    param(
        [System.Collections.IDictionary]$Variables
    )

    foreach ($entry in $Variables.GetEnumerator()) {
        $name = [string]$entry.Key
        $value = [string]$entry.Value
        [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::User)
        Write-Host "[Permanent] $name 已写入当前用户环境变量。" -ForegroundColor Yellow
    }
}

if (-not (Test-ModeValid -Value $Mode)) {
    throw "Mode 只允许为 Temporary 或 Permanent，当前值：$Mode"
}

if ($EnvironmentVariables.Count -eq 0) {
    throw "EnvironmentVariables 不能为空。"
}

foreach ($entry in $EnvironmentVariables.GetEnumerator()) {
    if ([string]::IsNullOrWhiteSpace([string]$entry.Key)) {
        throw "检测到空的环境变量名，请修正脚本中的 EnvironmentVariables。"
    }
}

switch ($Mode) {
    "Temporary" {
        Set-TemporaryEnvironmentVariables -Variables $EnvironmentVariables
        Write-Host ""
        Write-Host "临时环境变量配置完成。" -ForegroundColor Green
        Write-Host "如需验证，可执行：Get-ChildItem Env: | Where-Object Name -in $($EnvironmentVariables.Keys -join ', ')" -ForegroundColor DarkGray
    }
    "Permanent" {
        Set-PermanentEnvironmentVariables -Variables $EnvironmentVariables
        Write-Host ""
        Write-Host "永久环境变量配置完成。请重新打开终端后再启动项目。" -ForegroundColor Yellow
    }
}
