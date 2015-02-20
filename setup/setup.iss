#define OutputName "proxychecker"
#define MyAppName "Proxyblyadok 9000"
#define MyAppVersion "1.0"
#define MyAppPublisher "My Company, Inc."
#define MyAppURL "http://www.example.com/"
#define MyAppExeName "bin\proxychecker.bat"
#define ProjectFolder "..\"

[Setup]
AppId={{63309780-5847-4D35-86E7-6C1E899C099B}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
OutputDir={#ProjectFolder}\target\iss
OutputBaseFilename={#OutputName}-{#MyAppVersion}
SetupIconFile={#ProjectFolder}\setup\icon.ico
Compression=lzma
SolidCompression=true
PrivilegesRequired=admin

[Languages]
Name: english; MessagesFile: compiler:Default.isl
Name: russian; MessagesFile: compiler:Languages\Russian.isl

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}; Languages: 

[Files]
Source: {#ProjectFolder}\target\universal\stage\*; DestDir: {app}; Flags: ignoreversion recursesubdirs
Source: {#ProjectFolder}\setup\icon.ico; DestDir: {app}
Source: {#ProjectFolder}\GeoLite2-City.mmdb; DestDir: {app}

[Icons]
Name: {group}\{#MyAppName}; Filename: {app}\{#MyAppExeName}; IconFilename: {app}\icon.ico; WorkingDir: {app}
Name: {commondesktop}\{#MyAppName}; Filename: {app}\{#MyAppExeName}; Tasks: desktopicon; IconFilename: {app}\icon.ico; WorkingDir: {app}

[Run]
Filename: {app}\{#MyAppExeName}; Description: {cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}; Flags: shellexec postinstall skipifsilent; WorkingDir: {app}; Check: IsJREInstalled

[Code]
#define MinJRE "1.6"
#define WebJRE "https://www.java.com/ru/download/manual.jsp"

function IsJREInstalled: Boolean;
var
  JREVersion: string;
begin
  // read JRE version
  Result := RegQueryStringValue(HKLM32, 'Software\JavaSoft\Java Runtime Environment',
    'CurrentVersion', JREVersion);
  // if the previous reading failed and we're on 64-bit Windows, try to read 
  // the JRE version from WOW node
  if not Result and IsWin64 then
    Result := RegQueryStringValue(HKLM64, 'Software\JavaSoft\Java Runtime Environment',
      'CurrentVersion', JREVersion);
  // if the JRE version was read, check if it's at least the minimum one
  if Result then
    Result := CompareStr(JREVersion, '{#MinJRE}') >= 0;
end;

function InitializeSetup: Boolean;
var
  ErrorCode: Integer;
begin
  Result := True;
  // check if JRE is installed; if not, then...
  if not IsJREInstalled then
  begin
    // show a message box and let user to choose if they want to download JRE;
    // if so, go to its download site and exit setup; continue otherwise
    if MsgBox('Java is required. Do you want to download it now ?',
      mbConfirmation, MB_YESNO) = IDYES then
    begin
      Result := False;
      ShellExec('', '{#WebJRE}', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode);
    end;
  end;
end;