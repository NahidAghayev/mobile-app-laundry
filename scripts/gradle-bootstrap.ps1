param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$GradleVersion = '9.5.0'
$DistributionRoot = Join-Path $env:USERPROFILE ".gradle\wrapper\dists\laundry-ledger\gradle-$GradleVersion"
$GradleHome = Join-Path $DistributionRoot "gradle-$GradleVersion"
$GradleExecutable = Join-Path $GradleHome 'bin\gradle.bat'

if (-not (Test-Path -LiteralPath $GradleExecutable)) {
    New-Item -ItemType Directory -Path $DistributionRoot -Force | Out-Null
    $Archive = Join-Path $DistributionRoot "gradle-$GradleVersion-bin.zip"
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $Archive
    Expand-Archive -LiteralPath $Archive -DestinationPath $DistributionRoot -Force
    Remove-Item -LiteralPath $Archive -Force
}

if (-not $env:ANDROID_HOME) {
    $DefaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $DefaultSdk) { $env:ANDROID_HOME = $DefaultSdk }
}
if (-not $env:JAVA_HOME -and $env:ProgramFiles) {
    $StudioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $StudioJdk 'bin\java.exe')) { $env:JAVA_HOME = $StudioJdk }
}

& $GradleExecutable @GradleArgs
exit $LASTEXITCODE
