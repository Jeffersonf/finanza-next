param(
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$PackageName = "com.finanza.next",
    [string]$LaunchActivity = "com.finanza.next.MainActivity"
)

$ErrorActionPreference = "Stop"

function Resolve-Adb {
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"),
        (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    ) | Where-Object { $_ -and $_.Trim() -ne "" }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    $fromPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    throw "Nao encontrei adb.exe. Conecte o SDK Android ou ajuste ANDROID_SDK_ROOT."
}

function Get-OnlineDevices([string]$adbPath) {
    $lines = & $adbPath devices
    return $lines |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "\tdevice$" } |
        ForEach-Object { ($_ -split "\t")[0].Trim() } |
        Where-Object { $_ -ne "" }
}

$root = Split-Path -Parent $PSScriptRoot
$apkFullPath = Join-Path $root $ApkPath

if (-not (Test-Path $apkFullPath)) {
    throw "APK nao encontrado em: $apkFullPath"
}

$adb = Resolve-Adb
$devices = Get-OnlineDevices $adb

if (-not $devices -or $devices.Count -eq 0) {
    throw "Nenhum dispositivo online. Conecte um aparelho com depuracao USB ou abra um emulador."
}

Write-Host "Usando adb em: $adb"
Write-Host "Instalando APK em: $($devices -join ', ')"

foreach ($device in $devices) {
    & $adb -s $device install -r $apkFullPath | Out-Host
    & $adb -s $device shell am start -n "$PackageName/$LaunchActivity" | Out-Host
}

Write-Host "Instalacao concluida."
