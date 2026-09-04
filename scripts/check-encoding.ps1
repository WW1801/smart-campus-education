param(
    [switch]$StagedOnly
)

$ErrorActionPreference = 'Stop'
$utf8 = [System.Text.UTF8Encoding]::new($false, $true)
$gbk = [System.Text.Encoding]::GetEncoding(936, [System.Text.EncoderExceptionFallback]::new(), [System.Text.DecoderExceptionFallback]::new())
$extensionPattern = '\.(java|vue|js|ts|json|yml|yaml|xml|sql|md|txt|ps1|sh|properties|html|css)$'

if ($StagedOnly) {
    $paths = git diff --cached --name-only --diff-filter=ACMR
} else {
    $paths = git ls-files
}

$problems = [System.Collections.Generic.List[string]]::new()
foreach ($path in $paths) {
    if ($path -notmatch $extensionPattern) { continue }
    if (-not (Test-Path -LiteralPath $path)) { continue }

    try {
        $content = $utf8.GetString([System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $path)))
    } catch {
        $problems.Add("$($path): file is not valid UTF-8.")
        continue
    }

    $lines = $content -split '\r?\n', -1
    for ($index = 0; $index -lt $lines.Length; $index++) {
        $line = $lines[$index]
        if ($line -match '[^\x00-\x7F]') {
            try {
                $recovered = $utf8.GetString($gbk.GetBytes($line))
                if ($recovered -match '[\p{IsCJKUnifiedIdeographs}]' -and $recovered -ne $line) {
                    $problems.Add("$($path):$($index + 1): possible GBK/UTF-8 mojibake; recovered text: $recovered")
                }
            } catch { }
        }
        if ($lines[$index].Contains([char]0xFFFD)) {
            $problems.Add("$($path):$($index + 1): contains Unicode replacement character U+FFFD.")
        }
    }
}

if ($problems.Count -gt 0) {
    $problems | ForEach-Object { Write-Error $_ }
    throw "Encoding check failed: found $($problems.Count) issue(s). Save files as UTF-8 before committing."
}

Write-Output 'Encoding check passed: UTF-8 validity and common GBK/UTF-8 mojibake were checked.'
