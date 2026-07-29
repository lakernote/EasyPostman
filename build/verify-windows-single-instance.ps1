param(
    [Parameter(Mandatory = $true)]
    [string]$AppExe,

    [int]$StartupWaitSeconds = 8,

    [int]$SecondaryExitTimeoutSeconds = 10
)

$ErrorActionPreference = 'Stop'
$resolvedAppExe = (Resolve-Path $AppExe).Path
$projectRoot = Split-Path -Parent $PSScriptRoot
$smokeRoot = Join-Path $projectRoot "target\single-instance-smoke-$([Guid]::NewGuid().ToString('N'))"
$dataRoot = Join-Path $smokeRoot 'data'
$logRoot = Join-Path $smokeRoot 'logs'
$normalizedDataRoot = $dataRoot.Replace('\', '/')
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$primaryProcess = $null
$secondaryProcess = $null
$recoveredProcess = $null

function Start-EasyPostmanProcess {
    param([string]$Name)

    $stdoutPath = Join-Path $logRoot "$Name-stdout.log"
    $stderrPath = Join-Path $logRoot "$Name-stderr.log"
    return Start-Process `
        -FilePath $resolvedAppExe `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath
}

function Assert-ProcessStillRunning {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$Description
    )

    $Process.Refresh()
    if ($Process.HasExited) {
        throw "$Description exited unexpectedly with code $($Process.ExitCode)"
    }
}

function Stop-ProcessIfRunning {
    param([AllowNull()][System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    $Process.Refresh()
    if (-not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force
        $Process.WaitForExit(5000) | Out-Null
    }
}

function Write-SmokeLogs {
    if (-not (Test-Path $logRoot)) {
        return
    }
    Get-ChildItem $logRoot -File | Sort-Object Name | ForEach-Object {
        Write-Host "===== $($_.Name) ====="
        Get-Content $_.FullName -ErrorAction SilentlyContinue
    }
}

try {
    New-Item -ItemType Directory -Path $dataRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
    $env:JAVA_TOOL_OPTIONS = "-Djava.awt.headless=false -DeasyPostman.data.dir=`"$normalizedDataRoot`""

    Write-Host "Starting primary EasyPostman instance..."
    $primaryProcess = Start-EasyPostmanProcess -Name 'primary'
    Start-Sleep -Seconds $StartupWaitSeconds
    Assert-ProcessStillRunning -Process $primaryProcess -Description 'Primary instance'

    Write-Host "Starting secondary EasyPostman instance..."
    $secondaryProcess = Start-EasyPostmanProcess -Name 'secondary'
    if (-not $secondaryProcess.WaitForExit($SecondaryExitTimeoutSeconds * 1000)) {
        throw 'Secondary instance did not exit after notifying the primary instance'
    }
    if ($secondaryProcess.ExitCode -ne 0) {
        throw "Secondary instance exited with code $($secondaryProcess.ExitCode)"
    }
    Assert-ProcessStillRunning -Process $primaryProcess -Description 'Primary instance after secondary launch'
    Write-Host "Single-instance activation passed: secondary exited and primary stayed alive."

    Write-Host "Force-terminating the primary instance to verify crash recovery..."
    Stop-ProcessIfRunning -Process $primaryProcess
    $primaryProcess = $null

    $recoveredProcess = Start-EasyPostmanProcess -Name 'recovered'
    Start-Sleep -Seconds $StartupWaitSeconds
    Assert-ProcessStillRunning -Process $recoveredProcess -Description 'Recovered instance'
    Write-Host "Crash recovery passed: a new instance started after forced termination."
} catch {
    Write-Host "Windows single-instance smoke test failed: $($_.Exception.Message)"
    Write-SmokeLogs
    throw
} finally {
    Stop-ProcessIfRunning -Process $secondaryProcess
    Stop-ProcessIfRunning -Process $primaryProcess
    Stop-ProcessIfRunning -Process $recoveredProcess
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    if (Test-Path $smokeRoot) {
        Remove-Item -Path $smokeRoot -Recurse -Force
    }
}
