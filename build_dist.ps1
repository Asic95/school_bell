$ErrorActionPreference = "Stop"

# Configuration
$NAME = "SchoolBell"
$VERSION = "1.1.0"
$VENDOR = "SchoolBell Team"
$DESC = "School Bell Management System"
$MAIN_CLASS = "com.schoolbell.Launcher"
$MAIN_JAR = "untitled-1.1.0.jar"
$ICON = "icon.ico"

# Paths
$JAVA_HOME = $env:JAVA_HOME
if (-not $JAVA_HOME) { $JAVA_HOME = "C:\Program Files\Java\jdk-21" }
$JPACKAGE = Join-Path $JAVA_HOME "bin\jpackage.exe"

if (-not (Test-Path $JPACKAGE)) {
    throw "jpackage.exe not found at $JPACKAGE. Please ensure JDK 21 is installed and JAVA_HOME is set."
}

Write-Host "--- Step 1: Building with Maven ---" -ForegroundColor Cyan
mvn clean package -DskipTests

Write-Host "`n--- Step 1.5: Generating multi-size ICO ---" -ForegroundColor Cyan
if (Test-Path "src\main\java\com\schoolbell\IcoWrapper.java") {
    javac src\main\java\com\schoolbell\IcoWrapper.java -d target\classes
    java -cp target\classes com.schoolbell.IcoWrapper
} elseif (Test-Path $ICON) {
    Write-Host "$ICON already exists, using it."
} else {
    throw "IcoWrapper.java not found and $ICON missing."
}

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
    --description $DESC `
    --icon $ICON

Write-Host "`n--- Step 4: Building Installer (Inno Setup) ---" -ForegroundColor Cyan
$ISCC = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
if (Test-Path $ISCC) {
    & $ISCC schoolbell_installer.iss
    Write-Host "`n✅ INSTALLER SUCCESS! Created in 'installer_output\'" -ForegroundColor Green
} else {
    Write-Host "`n⚠️ ISCC.exe not found at $ISCC. Skipping installer build." -ForegroundColor Yellow
    Write-Host "You can build it manually by opening 'schoolbell_installer.iss' in Inno Setup." -ForegroundColor Gray
}

Write-Host "`n✅ BUILD COMPLETE!" -ForegroundColor Green
