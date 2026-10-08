$xmlPath = "target/site/jacoco/jacoco.xml"
if (!(Test-Path $xmlPath)) {
    Write-Host "jacoco.xml not found!"
    exit 1
}

[xml]$jacoco = Get-Content $xmlPath

Write-Host "Classes in analytics package with < 80% coverage:"
Write-Host "------------------------------------------------"

foreach ($package in $jacoco.report.package) {
    if ($package.name -like "lk/dmc/disaster/analytics*") {
        foreach ($class in $package.class) {
            $lineCovered = 0
            $lineMissed = 0
            $branchCovered = 0
            $branchMissed = 0

            foreach ($counter in $class.counter) {
                if ($counter.type -eq "LINE") {
                    $lineCovered = [int]$counter.covered
                    $lineMissed = [int]$counter.missed
                }
                if ($counter.type -eq "BRANCH") {
                    $branchCovered = [int]$counter.covered
                    $branchMissed = [int]$counter.missed
                }
            }

            $lineTotal = $lineCovered + $lineMissed
            $linePct = if ($lineTotal -gt 0) { [math]::Round(($lineCovered / $lineTotal) * 100, 2) } else { 100.00 }

            $branchTotal = $branchCovered + $branchMissed
            $branchPct = if ($branchTotal -gt 0) { [math]::Round(($branchCovered / $branchTotal) * 100, 2) } else { 100.00 }

            if (($lineTotal -gt 0 -and $linePct -lt 80) -or ($branchTotal -gt 0 -and $branchPct -lt 80)) {
                Write-Host "Class: $($class.name)"
                if ($lineTotal -gt 0 -and $linePct -lt 80) {
                    Write-Host "  - Line coverage: $linePct% ($lineCovered/$lineTotal covered, $lineMissed missed)"
                }
                if ($branchTotal -gt 0 -and $branchPct -lt 80) {
                    Write-Host "  - Branch coverage: $branchPct% ($branchCovered/$branchTotal covered, $branchMissed missed)"
                }
                Write-Host ""
            }
        }
    }
}
