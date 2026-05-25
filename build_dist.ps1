$ErrorActionPreference = "Stop"

# Configuration
$NAME = "SchoolBell"
$VERSION = "1.0.0"
$VENDOR = "SchoolBell Team"
$DESC = "School Bell Management System"
$MAIN_CLASS = "com.schoolbell.Launcher"
$MAIN_JAR = "untitled-1.0-SNAPSHOT.jar"

# Paths
$JAVA_HOME = $env:JAVA_HOME
if (-not $JAVA_HOME) { $JAVA_HOME = "C:\Program Files\Java\jdk-21" }
$JPACKAGE = Join-Path $JAVA_HOME "bin\jpackage.exe"

if (-not (Test-Path $JPACKAGE)) {
    throw "jpackage.exe not found at $JPACKAGE. Please ensure JDK 21 is installed and JAVA_HOME is set."
}

Write-Host "--- Step 1: Building with Maven ---" -ForegroundColor Cyan
mvn clean package -DskipTests

Write-Host "`n--- Step 2: Preparing jpackage input ---" -ForegroundColor Cyan
if (-not (Test-Path "jpackage_input")) { New-Item -ItemType Directory "jpackage_input" | Out-Null }
Copy-Item "target\$MAIN_JAR" "jpackage_input\"

Write-Host "`n--- Step 3: Packaging as App Image (EXE + Runtime) ---" -ForegroundColor Cyan
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }

& $JPACKAGE --type app-image `
    --input jpackage_input `
    --dest dist `
    --name $NAME `
    --main-jar $MAIN_JAR `
    --main-class $MAIN_CLASS `
    --vendor $VENDOR `
    --app-version $VERSION `
    --description $DESC

Write-Host "`n✅ SUCCESS! EXE distribution created in 'dist\$NAME'" -ForegroundColor Green
Write-Host "Run 'dist\$NAME\$NAME.exe' to test." -ForegroundColor Yellow

if ($Host.Name -eq "ConsoleHost") {
    Read-Host "Press Enter to exit"
}