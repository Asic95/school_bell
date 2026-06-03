; Inno Setup Script for SchoolBell
#define AppName "SchoolBell"
#define AppVersion "1.1.0"
#define AppPublisher "SchoolBell Team"
#define AppExeName "SchoolBell.exe"
#define DistDir "dist\SchoolBell"

[Setup]
AppId={{D3E6B1D2-8A7C-4C9D-B123-E5A7B8C9D012}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
AllowNoIcons=yes
OutputDir=installer_output
OutputBaseFilename=SchoolBell_Setup_v{#AppVersion}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
SetupIconFile=icon.ico
AppMutex=SchoolBell_App_Instance
UninstallDisplayIcon={app}\{#AppExeName}
LanguageDetectionMethod=none
CloseApplications=yes

[Languages]
Name: "ukrainian"; MessagesFile: "compiler:Languages\Ukrainian.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#DistDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
Filename: "{app}\{#AppExeName}"; Flags: nowait; Check: IsSilent

[Code]
function IsSilent: Boolean;
begin
  Result := WizardSilent;
end;
