; Inno Setup Script for SchoolBell
#define AppName "SchoolBell"
#define AppVersion "1.2.8"
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
CloseApplications=force
PrivilegesRequired=admin

[Languages]
Name: "ukrainian"; MessagesFile: "compiler:Languages\Ukrainian.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#DistDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Dirs]
Name: "{commonappdata}\{#AppName}\updater"; Permissions: users-modify
Name: "{commonappdata}\{#AppName}\updater\downloads"; Permissions: users-modify
Name: "{commonappdata}\{#AppName}\updater\events"; Permissions: users-modify

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{sys}\schtasks.exe"; Parameters: "/Delete /TN ""SchoolBellUpdaterAtLogon"" /F"; Flags: runhidden waituntilterminated; RunOnceId: "DeleteSchoolBellUpdaterAtLogon"
Filename: "{sys}\schtasks.exe"; Parameters: "/Delete /TN ""SchoolBellUpdaterEvery6Hours"" /F"; Flags: runhidden waituntilterminated; RunOnceId: "DeleteSchoolBellUpdaterEvery6Hours"

[Code]
procedure RunScheduledTaskCommand(const Parameters, ActionName: String);
var
  ResultCode: Integer;
begin
  Log(ActionName + ' — параметри: ' + Parameters);
  if Exec(ExpandConstant('{sys}\schtasks.exe'), Parameters, '', SW_HIDE,
    ewWaitUntilTerminated, ResultCode) then begin
    if ResultCode = 0 then
      Log(ActionName + ' успішно. Код: 0')
    else
      Log(ActionName + ' завершилося з кодом: ' + IntToStr(ResultCode));
  end else
    Log('Не вдалося запустити schtasks для операції "' + ActionName + '".');
end;

function InitializeSetup(): Boolean;
var
  ResultCode: Integer;
begin
  Result := True;
  Log('InitializeSetup: Перевірка та завершення процесів SchoolBell...');
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /T /IM SchoolBell.exe', '', SW_HIDE,
    ewWaitUntilTerminated, ResultCode);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
begin
  Result := '';
  Log('PrepareToInstall: Примусове завершення процесів SchoolBell перед оновленням файлів...');
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /T /IM SchoolBell.exe', '', SW_HIDE,
    ewWaitUntilTerminated, ResultCode);
  Sleep(1000);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  UpdaterExe, CreateAtLogon, CreateEvery6h, RestartFlag: String;
  ResultCode: Integer;
begin
  if CurStep = ssPostInstall then begin
    UpdaterExe := ExpandConstant('{app}\SchoolBellUpdaterScheduled.exe');

    { Remove task names created by older installer builds. }
    RunScheduledTaskCommand('/Delete /TN "SchoolBell\UpdaterAtLogon" /F',
      'Видалення застарілого завдання SchoolBell\UpdaterAtLogon');
    RunScheduledTaskCommand('/Delete /TN "SchoolBell\UpdaterEvery6Hours" /F',
      'Видалення застарілого завдання SchoolBell\UpdaterEvery6Hours');

    { schtasks /TR requires outer quotes AND backslash-escaped inner quotes }
    { when the command path contains spaces: /TR "\"C:\Path With Spaces\app.exe\"" }
    CreateAtLogon := '/Create /TN "SchoolBellUpdaterAtLogon"'
      + ' /TR ' + #34 + '\"' + UpdaterExe + '\"' + #34
      + ' /SC ONLOGON /RU SYSTEM /RL HIGHEST /F';
    CreateEvery6h := '/Create /TN "SchoolBellUpdaterEvery6Hours"'
      + ' /TR ' + #34 + '\"' + UpdaterExe + '\"' + #34
      + ' /SC HOURLY /MO 6 /RU SYSTEM /RL HIGHEST /F';

    RunScheduledTaskCommand(CreateAtLogon,
      'Створення завдання SchoolBellUpdaterAtLogon');
    RunScheduledTaskCommand(CreateEvery6h,
      'Створення завдання SchoolBellUpdaterEvery6Hours');

    { Check if SchoolBell was running with autostart enabled before the update. }
    { If restart.flag exists, restart SchoolBell.exe in the active user's session. }
    RestartFlag := ExpandConstant('{commonappdata}\{#AppName}\updater\restart.flag');
    if FileExists(RestartFlag) then begin
      DeleteFile(RestartFlag);
      Log('Знайдено restart.flag. Запуск SchoolBell.exe для активного користувача...');
      Exec(ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe'),
        '-ExecutionPolicy Bypass -NoProfile -Command "$u = (Get-CimInstance Win32_ComputerSystem).UserName; if ($u) { schtasks /Create /TN \"SchoolBellRestart\" /TR \"\"\"' + ExpandConstant('{app}\SchoolBell.exe') + '\"\"\" /SC ONCE /ST 23:59 /RU $u /IT /F; schtasks /Run /TN \"SchoolBellRestart\"; Start-Sleep -Seconds 2; schtasks /Delete /TN \"SchoolBellRestart\" /F }"',
        '', SW_HIDE, ewNoWait, ResultCode);
    end;
  end;
end;
