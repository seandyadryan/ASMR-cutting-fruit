param([string]$Repository = 'seandyadryan/ASMR-cutting-fruit')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$properties = Join-Path $root 'keystore.properties'
if (-not (Test-Path -LiteralPath $properties)) { throw 'Create ignored keystore.properties first. See README.md.' }
$values = @{}
Get-Content -LiteralPath $properties | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $values[$matches[1].Trim()] = $matches[2].Trim() }
}
foreach ($name in @('KEYSTORE_PATH','KEYSTORE_PASSWORD','KEY_ALIAS','KEY_PASSWORD')) {
    if (-not $values[$name]) { throw "Missing signing value: $name" }
}
$encoded = [Convert]::ToBase64String([IO.File]::ReadAllBytes($values['KEYSTORE_PATH']))
$encoded | gh secret set KEYSTORE_BASE64 --repo $Repository
if ($LASTEXITCODE -ne 0) { throw 'Could not set keystore secret' }
foreach ($name in @('KEYSTORE_PASSWORD','KEY_ALIAS','KEY_PASSWORD')) {
    $values[$name] | gh secret set $name --repo $Repository
    if ($LASTEXITCODE -ne 0) { throw "Could not set $name" }
}
Write-Output 'Four encrypted GitHub Actions signing secrets configured. No credentials printed.'
