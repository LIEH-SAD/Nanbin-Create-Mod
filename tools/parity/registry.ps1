<#
.SYNOPSIS
  Cross-check registered block/item ids against resources (blockstates, models) and recipes,
  and compare lang key coverage against the source mod.

.DESCRIPTION
  Reads the ids registered in Blocks.java / Items.java, then verifies:
    * assets/nanbin/blockstates/<id>.json
    * assets/nanbin/models/block/<id>.json  or <id>_*.json  or models/block/<id>/   (multi-model / directory form)
    * assets/nanbin/models/item/<id>.json   (only for ids that are also registered items)
  Then parses data/nanbin/recipes/*.json and reports references to nanbin:<id> that are
  neither a registered block id nor a registered item id.

.EXAMPLE
  .\tools\parity\registry.ps1
#>
[CmdletBinding()]
param(
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [string]$Source = 'D:\NCM+mtr4.0\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

$javaRoot = Join-Path $Target 'common\src\main\java\com\Nanbin'
$resRoot = Join-Path $Target 'common\src\main\resources'
$assets = Join-Path $resRoot 'assets\nanbin'

function Get-RegisteredIds([string]$file) {
    if (-not (Test-Path $file)) { return @() }
    $text = [System.IO.File]::ReadAllText($file)
    [regex]::Matches($text, 'register\(\s*"([a-z0-9_]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
}

$blockIds = Get-RegisteredIds (Join-Path $javaRoot 'Blocks\Blocks.java')
$itemIds = Get-RegisteredIds (Join-Path $javaRoot 'Items\Items.java')

$known = @{}
foreach ($id in $blockIds) { $known[$id] = $true }
foreach ($id in $itemIds) { $known[$id] = $true }
$itemSet = @{}
foreach ($id in $itemIds) { $itemSet[$id] = $true }

Write-Host ("Registered blocks: {0}" -f $blockIds.Count)
Write-Host ("Registered items : {0}" -f $itemIds.Count)
Write-Host ''

$problems = 0

# Resource gaps that also exist upstream (see known-resource-gaps.txt): reported separately, not counted.
$knownGaps = @{}
$gapsFile = Join-Path $PSScriptRoot 'known-resource-gaps.txt'
if (Test-Path $gapsFile) {
    Get-Content $gapsFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $knownGaps[$line] = $true
    }
}

Write-Host '--- resource completeness per block id ---'
$missingRows = New-Object System.Collections.ArrayList
foreach ($id in $blockIds) {
    if ($knownGaps.ContainsKey($id)) { continue }
    $missingHere = @()
    $blockstateFile = Join-Path $assets "blockstates\$id.json"
    if (-not (Test-Path $blockstateFile)) {
        $missingHere += 'blockstate'
    } else {
        # blockstate 里引用的每个 model 都必须存在（模型名不一定等于方块 id）
        $stateText = [System.IO.File]::ReadAllText($blockstateFile)
        $modelRefs = @()
        foreach ($m in [regex]::Matches($stateText, '"model"\s*:\s*"([^"]+)"')) { $modelRefs += $m.Groups[1].Value }
        $missingModels = @()
        foreach ($ref in ($modelRefs | Sort-Object -Unique)) {
            $path = $ref
            if ($path.StartsWith('nanbin:')) { $path = $path.Substring(7) }
            if (-not $path.StartsWith('block/')) { $path = "block/$path" }
            if (-not (Test-Path (Join-Path $assets "models\$path.json"))) { $missingModels += $ref }
        }
        if ($missingModels.Count -gt 0) { $missingHere += ("dangling-model: " + ($missingModels -join '|')) }
    }

    if ($itemSet.ContainsKey($id) -and -not (Test-Path (Join-Path $assets "models\item\$id.json"))) {
        $missingHere += 'item-model'
    }
    if ($missingHere.Count -gt 0) {
        [void]$missingRows.Add("$id : $($missingHere -join ', ')")
    }
}
if ($missingRows.Count -eq 0) {
    Write-Host '  all registered blocks have blockstate + model (+ item model when itemized)'
} else {
    $missingRows | ForEach-Object { Write-Host "  MISSING  $_" }
    $problems += $missingRows.Count
}

Write-Host ''
Write-Host '--- lang keys vs source ---'
$srcLangDir = Join-Path $Source 'fabric\src\main\resources\assets\nanbin\lang'
$tgtLangDir = Join-Path $assets 'lang'
if (Test-Path $srcLangDir) {
    foreach ($srcFile in (Get-ChildItem $srcLangDir -File -Filter *.json)) {
        $tgtFile = Join-Path $tgtLangDir $srcFile.Name
        $srcKeys = @()
        $srcKeys = [System.IO.File]::ReadAllLines($srcFile.FullName) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $matches[1] }
        $tgtKeys = @()
        if (Test-Path $tgtFile) {
            $tgtKeys = [System.IO.File]::ReadAllLines($tgtFile) | Where-Object { $_ -match '^\s*"([^"]+)"\s*:' } | ForEach-Object { $matches[1] }
        }
        $missing = Compare-Object $srcKeys $tgtKeys | Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject
        if ($missing.Count -gt 0) {
            Write-Host ("  {0,-12} missing {1} source key(s), first 5: {2}" -f $srcFile.Name, $missing.Count, (($missing | Select-Object -First 5) -join ', '))
            $problems += 1
        } else {
            Write-Host ("  {0,-12} OK ({1} keys)" -f $srcFile.Name, $tgtKeys.Count)
        }
    }
} else {
    Write-Host '  (source lang dir not found, skipped)'
}

Write-Host ''
Write-Host '--- recipe references ---'
$recipeDir = Join-Path $resRoot 'data\nanbin\recipes'
$unknownRefs = New-Object System.Collections.ArrayList
$recipeFiles = @()
if (Test-Path $recipeDir) { $recipeFiles = Get-ChildItem $recipeDir -File -Filter *.json }
foreach ($recipe in $recipeFiles) {
    $raw = [System.IO.File]::ReadAllText($recipe.FullName)
    foreach ($m in [regex]::Matches($raw, '"nanbin:([a-z0-9_]+)"')) {
        $ref = $m.Groups[1].Value
        if (-not $known.ContainsKey($ref)) {
            [void]$unknownRefs.Add("$($recipe.Name) -> nanbin:$ref")
        }
    }
}
if ($unknownRefs.Count -eq 0) {
    Write-Host ("  {0} recipe file(s), all nanbin: references resolve" -f $recipeFiles.Count)
} else {
    $unknownRefs | Select-Object -First 30 | ForEach-Object { Write-Host "  UNRESOLVED  $_" }
    Write-Host ("  total unresolved: {0}" -f $unknownRefs.Count)
    $problems += $unknownRefs.Count
}

Write-Host ''
if ($problems -eq 0) {
    Write-Host 'RESULT: OK'
    exit 0
}
Write-Host ("RESULT: {0} problem line(s)" -f $problems)
if ($Strict) { exit 1 }
exit 0
