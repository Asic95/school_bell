param (
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [Parameter(Mandatory=$true)]
    [string]$Changelog
)

$ErrorActionPreference = "Stop"

Write-Host "Starting release process for version $Version..."

# 1. Update versions in files
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$utf8Encoding = [System.Text.Encoding]::UTF8

Write-Host "Updating version in MainApp.java..."
$mainAppPath = "src/main/java/com/schoolbell/MainApp.java"
$content = [System.IO.File]::ReadAllText($mainAppPath, $utf8Encoding) -replace 'public static final String VERSION = ".*";', "public static final String VERSION = `"$Version`";"
[System.IO.File]::WriteAllText($mainAppPath, $content, $utf8NoBom)

Write-Host "Updating version in build_dist.ps1..."
$buildDistPath = "build_dist.ps1"
$content = [System.IO.File]::ReadAllText($buildDistPath, $utf8Encoding) -replace '\$VERSION = ".*"', "`$VERSION = `"$Version`"" -replace '\$MAIN_JAR = ".*"', "`$MAIN_JAR = `"untitled-$Version.jar`""
[System.IO.File]::WriteAllText($buildDistPath, $content, $utf8NoBom)

Write-Host "Updating version in schoolbell_installer.iss..."
$issPath = "schoolbell_installer.iss"
$content = [System.IO.File]::ReadAllText($issPath, $utf8Encoding) -replace '#define AppVersion ".*"', "#define AppVersion `"$Version`""
[System.IO.File]::WriteAllText($issPath, $content, $utf8NoBom)

Write-Host "Updating version in pom.xml..."
$pomPath = "pom.xml"
$pomContent = [System.IO.File]::ReadAllLines($pomPath, $utf8Encoding)
$newPomContent = @()
$foundArtifact = $false
foreach ($line in $pomContent) {
    if (-not $foundArtifact -and $line -match "<artifactId>untitled</artifactId>") {
        $newPomContent += $line
        $foundArtifact = $true
        continue
    }
    if ($foundArtifact -and $line -match "<version>.*</version>") {
        $newPomContent += "    <version>$Version</version>"
        $foundArtifact = $false # Stop looking after replacement
        continue
    }
    $newPomContent += $line
}
[System.IO.File]::WriteAllLines($pomPath, $newPomContent, $utf8NoBom)

Write-Host "Updating version in README.md..."
$readmePath = "README.md"
$utf8Encoding = [System.Text.Encoding]::UTF8
$readmeText = [System.IO.File]::ReadAllText($readmePath, $utf8Encoding)
$updatedReadmeText = $readmeText -replace 'SchoolBell_Setup_v[0-9]+\.[0-9]+\.[0-9]+\.exe', "SchoolBell_Setup_v$Version.exe" -replace 'releases/download/v[0-9]+\.[0-9]+\.[0-9]+/SchoolBell_Setup_v[0-9]+\.[0-9]+\.[0-9]+\.exe', "releases/download/v$Version/SchoolBell_Setup_v$Version.exe"
[System.IO.File]::WriteAllText($readmePath, $updatedReadmeText, $utf8NoBom)

# 2. Build project
Write-Host "Running build_dist.ps1..."
./build_dist.ps1

# 3. Check build result
$exeName = "SchoolBell_Setup_v$Version.exe"
$exePath = "installer_output/$exeName"

if (-not (Test-Path $exePath)) {
    Write-Error "Installer file not found at: $exePath"
}

# 4. Calculate SHA-256 hash
Write-Host "Calculating checksum..."
$hash = (Get-FileHash $exePath -Algorithm SHA256).Hash.ToLower()
Write-Host "Hash: $hash"

# 5. Create GitHub release and upload file
Write-Host "Uploading release to GitHub..."

$ghPath = "gh"
if (-not (Get-Command $ghPath -ErrorAction SilentlyContinue)) {
    $commonPath = "C:\Program Files\GitHub CLI\gh.exe"
    if (Test-Path $commonPath) {
        $ghPath = $commonPath
    } else {
        Write-Error "GitHub CLI (gh.exe) not found. Please install it or ensure it's in your PATH."
    }
}

& $ghPath release create "v$Version" $exePath --title "SchoolBell v$Version" --notes "$Changelog"

# Get download URL
$repo = & $ghPath repo view --json nameWithOwner -q .nameWithOwner
$downloadUrl = "https://github.com/$repo/releases/download/v$Version/$exeName"

# 6. Update updates.json
Write-Host "Updating updates.json..."
$manifestPath = "updates.json"

if (-not (Test-Path $manifestPath)) {
    $baseJson = @{
        latest_version = $Version
        release_date = (Get-Date -Format "yyyy-MM-dd")
        critical = $false
        changelog = @($Changelog)
        download_url = $downloadUrl
        checksum = $hash
    }
} else {
    $rawJson = [System.IO.File]::ReadAllText($manifestPath, $utf8Encoding)
    $baseJson = $rawJson | ConvertFrom-Json
    $baseJson.latest_version = $Version
    $baseJson.release_date = (Get-Date -Format "yyyy-MM-dd")
    $baseJson.changelog = @($Changelog)
    $baseJson.download_url = $downloadUrl
    $baseJson.checksum = $hash
}

$jsonString = $baseJson | ConvertTo-Json -Depth 10
[System.IO.File]::WriteAllText($manifestPath, $jsonString, $utf8NoBom)

# 7. Push changes
Write-Host "Pushing changes to repository..."
$currentBranch = (git branch --show-current).Trim()
git add $mainAppPath $buildDistPath $issPath $manifestPath $pomPath $readmePath
git commit -m "Release v$Version"
git push origin $currentBranch

Write-Host "SUCCESS! Version $Version published."
