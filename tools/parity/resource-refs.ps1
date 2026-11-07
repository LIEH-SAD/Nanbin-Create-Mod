<#
.SYNOPSIS
  Reference-integrity check for assets\nanbin: blockstate -> model -> texture links.

.DESCRIPTION
  Verifies, for both the MTR 4.0 source tree and this tree, that
    * every blockstate "model" / parent reference resolves to an existing model JSON,
    * every model "parent" resolves to an existing model JSON,
    * every model "textures" / element face texture resolves to an existing PNG.
  Only nanbin: references are checked (mtr:/minecraft: belong to other mods).

  Groups the resulting problems: a reference that is broken in the SOURCE is an
  upstream gap (expected, reported for information); a reference broken only in the
  TARGET is a regression introduced by the port and makes the script exit 1.

.NOTES
  ConvertFrom-Json in PowerShell 5.1 cannot parse the empty variant key "" used by
  many blockstates, hence JavaScriptSerializer is used here.

.EXAMPLE
  .\tools\parity\resource-refs.ps1
  .\tools\parity\resource-refs.ps1 -Strict
#>
[CmdletBinding()]
param(
    [string]$Source = 'D:\NCM+mtr4.0\Nanbin',
    [string]$Target = 'D:\NCMOD1.20.1\ymtr\Nanbin',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Web.Extensions
$js = New-Object System.Web.Script.Serialization.JavaScriptSerializer

function Get-ReferenceProblems([string]$resRoot) {
    $assets = Join-Path $resRoot 'assets\nanbin'
    $problems = New-Object System.Collections.Generic.List[string]
    $exists = { param($path) Test-Path -LiteralPath (Join-Path $assets $path) }

    $modelRoot = Join-Path $assets 'models'
    $modelFiles = Get-ChildItem -LiteralPath $modelRoot -Recurse -File -Filter *.json
    foreach ($f in $modelFiles) {
        $raw = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
        $o = $js.DeserializeObject($raw)
        $rel = $f.FullName.Substring($modelRoot.Length + 1)
        if ($o.ContainsKey('parent') -and $o['parent'] -is [string] -and $o['parent'] -match '^nanbin:(.+)$') {
            $parent = $matches[1]
            if (-not (& $exists "models\$parent.json")) { $problems.Add("model $rel -> missing parent nanbin:$parent") }
        }
        if ($o.ContainsKey('textures')) {
            foreach ($k in $o.textures.Keys) {
                $v = [string]$o.textures[$k]
                if ($v -match '^nanbin:(.+)$' -and -not (& $exists "textures\$($matches[1]).png")) {
                    $problems.Add("model $rel texture '$k' -> missing textures/$($matches[1]).png")
                }
            }
        }
        if ($o.ContainsKey('elements')) {
            foreach ($el in $o.elements) {
                if (-not $el.ContainsKey('faces')) { continue }
                foreach ($fk in $el.faces.Keys) {
                    $face = $el.faces[$fk]
                    if ($face.ContainsKey('texture') -and [string]$face.texture -match '^nanbin:(.+)$' -and -not (& $exists "textures\$($matches[1]).png")) {
                        $problems.Add("model $rel face texture -> missing textures/$($matches[1]).png")
                    }
                }
            }
        }
    }

    $bsRoot = Join-Path $assets 'blockstates'
    $bsFiles = Get-ChildItem -LiteralPath $bsRoot -Recurse -File -Filter *.json
    foreach ($f in $bsFiles) {
        $raw = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
        foreach ($mm in [regex]::Matches($raw, '"model"\s*:\s*"nanbin:([^"]+)"')) {
            $m = $mm.Groups[1].Value
            if (-not (& $exists "models\$m.json")) { $problems.Add("blockstate $($f.Name) -> missing model nanbin:$m") }
        }
    }

    return [pscustomobject]@{
        Models     = $modelFiles.Count
        Blockstates = $bsFiles.Count
        Problems   = @($problems | Sort-Object -Unique)
    }
}

$src = Get-ReferenceProblems (Join-Path $Source 'fabric\src\main\resources')
$tgt = Get-ReferenceProblems (Join-Path $Target 'common\src\main\resources')

Write-Host ("SOURCE  models={0,3} blockstates={1,3} broken-refs={2,3}" -f $src.Models, $src.Blockstates, $src.Problems.Count)
Write-Host ("TARGET  models={0,3} blockstates={1,3} broken-refs={2,3}" -f $tgt.Models, $tgt.Blockstates, $tgt.Problems.Count)
Write-Host ''

$regressions = @(Compare-Object $src.Problems $tgt.Problems | Where-Object SideIndicator -eq '=>' | ForEach-Object InputObject)
$fixed = @(Compare-Object $src.Problems $tgt.Problems | Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject)

if ($regressions.Count -gt 0) {
    Write-Host '--- broken in TARGET only (regressions) ---'
    $regressions | ForEach-Object { Write-Host "  NEW $_" }
}
if ($fixed.Count -gt 0) {
    Write-Host '--- broken in SOURCE only (fixed here) ---'
    $fixed | ForEach-Object { Write-Host "  FIXED $_" }
}
if ($regressions.Count -eq 0 -and $fixed.Count -eq 0) {
    Write-Host 'No differences: target matches source reference integrity exactly.'
}
Write-Host ''
Write-Host ("Upstream (source) gaps inherited, informational: {0}" -f $src.Problems.Count)

if ($Strict -and $regressions.Count -gt 0) { exit 1 }
exit 0
