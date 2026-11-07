<#
.SYNOPSIS
  One-shot acceptance harness for the MTR 4.0 -> MTR 3.6.3 port.

.DESCRIPTION
  Runs every gate in order and prints a PASS/FAIL summary:

    1. class inventory parity       (tools/parity/classes.ps1 -Strict)
    2. resource + lang parity       (tools/parity/resources.ps1 -Strict)
    3. registry x resource x recipe (tools/parity/registry.ps1 -Strict)
    4. static porting audit         (tools/parity/audit.ps1 -Strict)
    5. :common:compileJava
    6. :fabric:compileJava + :forge:compileJava
    7. offline draw test            (:common:drawTest, expects 10 PNGs)
    8. full build + release jars    (unless -SkipBuild)
    9. jar content spot check       (fabric.mod.json / mods.toml / crt_signs.json / crt_js_signs_config.json)

.EXAMPLE
  .\tools\parity\accept.ps1
  .\tools\parity\accept.ps1 -SkipBuild
#>
[CmdletBinding()]
param(
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Continue'
$results = New-Object System.Collections.ArrayList

function Add-Result([string]$name, [bool]$ok, [string]$detail) {
    [void]$results.Add([pscustomobject]@{ Gate = $name; Ok = $ok; Detail = $detail })
    $tag = if ($ok) { 'PASS' } else { 'FAIL' }
    Write-Host ("[{0}] {1} {2}" -f $tag, $name, $detail)
}

function Run-Gradle([string]$arguments) {
    Push-Location $Target
    try {
        $out = & .\gradlew.bat $arguments --console=plain 2>&1
        return $out
    } finally {
        Pop-Location
    }
}

Push-Location $Target
try {
    Write-Host '=== 1/9 class inventory ==='
    $out = & .\tools\parity\classes.ps1 2>&1
    $line = ($out | Where-Object { $_ -match '^MISSING' } | Select-Object -First 1)
    $ok = ($LASTEXITCODE -eq 0) -and ($line -match 'MISSING\s*:\s*0')
    Add-Result 'classes' $ok ($line -replace '\s+', ' ')

    Write-Host ''
    Write-Host '=== 2/9 resources + lang ==='
    $out = & .\tools\parity\resources.ps1 2>&1
    $bad = @($out | Where-Object { $_ -match 'missing=\s*[1-9]' -or $_ -match 'missing=\s*[1-9]\d*' })
    $ok = ($LASTEXITCODE -eq 0) -and ($bad.Count -eq 0)
    Add-Result 'resources' $ok ("missing groups: {0}" -f $bad.Count)

    Write-Host ''
    Write-Host '=== 3/9 registry x resources x recipes ==='
    $out = & .\tools\parity\registry.ps1 2>&1
    $resultLine = ($out | Where-Object { $_ -match '^RESULT' } | Select-Object -First 1)
    $ok = ($LASTEXITCODE -eq 0) -and ($resultLine -match 'RESULT:\s*OK')
    Add-Result 'registry' $ok $resultLine

    Write-Host ''
    Write-Host '=== 4/9 static audit ==='
    $out = & .\tools\parity\audit.ps1 2>&1
    $resultLine = ($out | Where-Object { $_ -match '^RESULT' } | Select-Object -First 1)
    $ok = ($LASTEXITCODE -eq 0) -and ($resultLine -match 'RESULT:\s*OK')
    Add-Result 'audit' $ok $resultLine

    Write-Host ''
    Write-Host '=== 5/9 :common:compileJava ==='
    $out = Run-Gradle ':common:compileJava'
    $ok = ($out -join "`n") -match 'BUILD SUCCESSFUL'
    Add-Result 'compile-common' $ok (($out | Where-Object { $_ -match 'BUILD (SUCCESSFUL|FAILED)' } | Select-Object -Last 1))

    Write-Host ''
    Write-Host '=== 6/9 platform compile ==='
    $out = Run-Gradle ':fabric:compileJava :forge:compileJava'
    $ok = ($out -join "`n") -match 'BUILD SUCCESSFUL'
    Add-Result 'compile-platforms' $ok (($out | Where-Object { $_ -match 'BUILD (SUCCESSFUL|FAILED)' } | Select-Object -Last 1))

    Write-Host ''
    Write-Host '=== 7/9 offline draw test ==='
    $out = Run-Gradle ':common:drawTest'
    $pngCount = @(Get-ChildItem (Join-Path $Target 'common\build\draw-test') -File -Filter *.png -ErrorAction SilentlyContinue).Count
    $ok = (($out -join "`n") -match 'BUILD SUCCESSFUL') -and ($pngCount -ge 10)
    Add-Result 'draw-test' $ok ("png={0}" -f $pngCount)

    if (-not $SkipBuild) {
        Write-Host ''
        Write-Host '=== 8/9 full build ==='
        $out = Run-Gradle 'build'
        $ok = ($out -join "`n") -match 'BUILD SUCCESSFUL'
        Add-Result 'build' $ok (($out | Where-Object { $_ -match 'BUILD (SUCCESSFUL|FAILED)' } | Select-Object -Last 1))

        Write-Host ''
        Write-Host '=== 9/9 release jars ==='
        $jars = @(Get-ChildItem (Join-Path $Target 'build\release') -File -Filter *.jar -ErrorAction SilentlyContinue)
        $fabric = @($jars | Where-Object { $_.Name -match 'fabric' })
        $forge = @($jars | Where-Object { $_.Name -match 'forge' })
        $ok = ($jars.Count -ge 2) -and ($fabric.Count -ge 1) -and ($forge.Count -ge 1)
        Add-Result 'release-jars' $ok (($jars | ForEach-Object { $_.Name }) -join ', ')

        if ($fabric.Count -ge 1) {
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $jar = $fabric[0].FullName
            $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
            try {
                $names = $zip.Entries | ForEach-Object { $_.FullName }
                $required = @('fabric.mod.json', 'nanbin.mixins.json', 'assets/nanbin/crt_signs.json', 'assets/nanbin/crt_js_signs_config.json')
                $missing = @($required | Where-Object { $names -notcontains $_ })
                Add-Result 'jar-content' ($missing.Count -eq 0) ("missing: " + ($missing -join ', '))
            } finally {
                $zip.Dispose()
            }
        }
    } else {
        Write-Host ''
        Write-Host '=== 8/9, 9/9 skipped (-SkipBuild) ==='
    }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host '================ ACCEPTANCE SUMMARY ================'
$failed = @($results | Where-Object { -not $_.Ok })
$results | ForEach-Object {
    $tag = if ($_.Ok) { 'PASS' } else { 'FAIL' }
    Write-Host ("  {0,-20} {1}  {2}" -f $_.Gate, $tag, $_.Detail)
}
Write-Host '===================================================='
if ($failed.Count -eq 0) {
    Write-Host 'ALL GATES PASSED'
    exit 0
}
Write-Host ("{0} gate(s) FAILED" -f $failed.Count)
exit 1
