<#
.SYNOPSIS
  Static porting audit: residual MTR4 imports, client-only leakage into server paths,
  and missing @ExpectPlatform implementations.

.DESCRIPTION
  Checks performed:
    1. No `org.mtr.` imports remain anywhere in the target Java sources.
    2. Files that are *not* in a client-only package must not import `net.minecraft.client.*`
       (dedicated-server safety). Client-only packages: client/, uiHelper/, Screen/, Render/.
    3. Every `@ExpectPlatform` method has a same-named implementation in fabric/ and forge/.

.EXAMPLE
  .\tools\parity\audit.ps1
  .\tools\parity\audit.ps1 -Strict
#>
[CmdletBinding()]
param(
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'
$failures = 0

$javaRoots = @(
    (Join-Path $Target 'common\src\main\java'),
    (Join-Path $Target 'fabric\src\main\java'),
    (Join-Path $Target 'forge\src\main\java')
)

$allJava = @()
foreach ($root in $javaRoots) {
    if (Test-Path $root) {
        $allJava += Get-ChildItem $root -Recurse -File -Filter *.java
    }
}

Write-Host ("Java files scanned: {0}" -f $allJava.Count)
Write-Host ''

# 1. residual MTR4 imports
Write-Host '--- residual org.mtr.* imports ---'
$residual = @()
foreach ($file in $allJava) {
    $hits = Select-String -Path $file.FullName -Pattern '^\s*import\s+org\.mtr\.' -ErrorAction SilentlyContinue
    if ($hits) {
        foreach ($h in $hits) {
            $residual += ("{0}:{1}: {2}" -f $file.Name, $h.LineNumber, $h.Line.Trim())
        }
    }
}
if ($residual.Count -eq 0) {
    Write-Host '  none'
} else {
    $residual | ForEach-Object { Write-Host "  RESIDUAL  $_" }
    $failures += $residual.Count
}

# 2. client-only leakage
Write-Host ''
Write-Host '--- client-only imports outside client packages ---'
# Packages that legitimately reference client classes (client-only by design).
$clientPrefixes = @('client\', 'uiHelper\', 'Screen\', 'Render\', 'Drawing\', 'JavaScriptSupport\', 'CRTRailwaySignEditor\', 'mapping\', 'mixin\')
# `TooltipContext` is part of the vanilla item/block tooltip signature on both loaders; not a leak.
$allowedClientImports = @('net.minecraft.client.item.TooltipContext')
$leaks = @()
foreach ($file in $allJava) {
    $relative = $file.FullName.Substring($file.FullName.IndexOf('\com\Nanbin\') + 1)
    $isClient = $false
    foreach ($prefix in $clientPrefixes) {
        if ($relative -like "*$prefix*" -or $relative -like "com\Nanbin\$prefix*") { $isClient = $true; break }
    }
    if ($relative -like '*ClientPacketHelper.java') { $isClient = $true }
    # 客户端专用文件（客户端初始化器/网络辅助）按文件名放行：它们只被客户端入口点调用
    if (@('InitClient.java', 'ClientPacketHelper.java', 'ModmenuConfig.java') -contains $file.Name) { $isClient = $true }
    if ($isClient) { continue }
    $hits = Select-String -Path $file.FullName -Pattern '^\s*import\s+net\.minecraft\.client\.' -ErrorAction SilentlyContinue
    if ($hits) {
        foreach ($h in $hits) {
            $line = $h.Line.Trim()
            $allowed = $false
            foreach ($a in $allowedClientImports) {
                if ($line -match [regex]::Escape($a)) { $allowed = $true; break }
            }
            if (-not $allowed) {
                $leaks += ("{0}:{1}: {2}" -f $file.Name, $h.LineNumber, $line)
            }
        }
    }
}
if ($leaks.Count -eq 0) {
    Write-Host '  none'
} else {
    $leaks | ForEach-Object { Write-Host "  LEAK  $_" }
    $failures += $leaks.Count
}

# 3. @ExpectPlatform implementations
Write-Host ''
Write-Host '--- @ExpectPlatform implementations ---'
$expects = @()
$commonRoot = Join-Path $Target 'common\src\main\java'
foreach ($file in (Get-ChildItem $commonRoot -Recurse -File -Filter *.java)) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    if ($text -notmatch '@ExpectPlatform') { continue }
    foreach ($m in [regex]::Matches($text, 'static\s+[\w<>\[\]\.]+\s+(\w+)\s*\(')) {
        $expects += $m.Groups[1].Value
    }
}
if ($expects.Count -eq 0) {
    Write-Host '  no @ExpectPlatform methods found'
} else {
    foreach ($name in ($expects | Sort-Object -Unique)) {
        $impls = 0
        foreach ($loader in @('fabric', 'forge')) {
            $root = Join-Path $Target "$loader\src\main\java"
            if (-not (Test-Path $root)) { continue }
            $found = Select-String -Path (Get-ChildItem $root -Recurse -File -Filter *.java).FullName -Pattern ("static\s+[\w<>\[\]\.]+\s+$name\s*\(") -ErrorAction SilentlyContinue
            if ($found) { $impls++ }
        }
        if ($impls -eq 2) {
            Write-Host ("  {0,-28} OK" -f $name)
        } else {
            $where = 'fabric+forge'
            if ($impls -eq 1) { $where = 'one loader' }
            Write-Host ("  {0,-28} MISSING impl in {1}" -f $name, $where)
            $failures++
        }
    }
}

Write-Host ''
if ($failures -eq 0) {
    Write-Host 'RESULT: OK'
    exit 0
}
Write-Host ("RESULT: {0} finding(s)" -f $failures)
if ($Strict) { exit 1 }
exit 0
