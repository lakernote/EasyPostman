; EasyPostman Inno Setup 脚本
; 详细文档请参考：https://jrsoftware.org/ishelp/

; ==================== 外部传入的变量 ====================
; 这些变量可以通过命令行传入：iscc /DMyAppVersion=4.0.9 easy-postman.iss
; 注意：路径是相对于 ISS 脚本位置（build 目录），所以需要 ..\ 返回上级
#ifndef MyAppVersion
  #define MyAppVersion "4.0.9"
#endif
#ifndef MyAppSourceDir
  #define MyAppSourceDir "..\target\EasyPostman"
#endif
#ifndef MyOutputDir
  #define MyOutputDir "..\dist"
#endif
#ifndef MyArch
  #define MyArch "x64"
#endif

; ==================== 应用常量定义 ====================
#define MyAppName "EasyPostman"
#define MyAppPublisher "Laker"
#define MyAppURL "https://github.com/lakernote/easy-postman"
#define MyAppSupportURL "https://github.com/lakernote/easy-postman/issues"
#define MyAppUpdatesURL "https://github.com/lakernote/easy-postman/releases"
#define MyAppExeName "EasyPostman.exe"

[Setup]
; AppId 是应用的唯一标识符（GUID），不要修改！用于支持升级安装
; 注意：AppId 直接使用 GUID（不带花括号），Inno Setup 会自动添加花括号和 _is1 后缀生成注册表键
; 最终注册表路径: HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E}_is1
AppId=8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E
; 应用基本信息
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppSupportURL}
AppUpdatesURL={#MyAppUpdatesURL}
; 默认安装路径：C:\Program Files\EasyPostman
DefaultDirName={autopf}\{#MyAppName}
; 卸载程序显示的图标
UninstallDisplayIcon={app}\{#MyAppExeName}
; 支持的架构：x64 和 ARM64（Windows 11 on ARM）
ArchitecturesAllowed=x64compatible arm64
ArchitecturesInstallIn64BitMode=x64compatible arm64
; 不显示"选择开始菜单文件夹"页面
DisableProgramGroupPage=yes
; 输出配置
OutputDir={#MyOutputDir}
OutputBaseFilename={#MyAppName}-{#MyAppVersion}-windows-{#MyArch}
; 压缩配置：使用最高级别的压缩
Compression=lzma2/max
SolidCompression=yes
; 安装向导界面风格：modern（现代扁平化）
WizardStyle=modern
; 安装程序的图标（相对于 ISS 脚本的路径）
SetupIconFile=..\assets\win\EasyPostman.ico

[Languages]
; 安装向导语言
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
; 可选任务：创建桌面快捷方式（默认选中）
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; 复制 jpackage 生成的所有文件到安装目录
Source: "{#MyAppSourceDir}\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#MyAppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; 创建快捷方式
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[InstallDelete]
; 升级安装时清理旧版本的文件（避免文件冗余和版本冲突）
Type: files; Name: "{app}\app\*.jar"
Type: filesandordirs; Name: "{app}\runtime\*"
Type: filesandordirs; Name: "{app}\plugins\*"

[Run]
; 安装完成后的启动选项
; 交互式安装：提示用户是否启动应用
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent; Check: ShouldPromptStart
; 静默安装：如果指定了 /AUTOSTART 参数，则后台自动启动（用于自动更新场景）
Filename: "{app}\{#MyAppExeName}"; Flags: nowait runhidden; Check: ShouldAutoStart

[Code]
{ 检查命令行参数是否存在 }
function CmdLineParamExists(const Value: string): Boolean;
var
  I: Integer;
begin
  Result := False;
  for I := 1 to ParamCount do
    if CompareText(ParamStr(I), Value) = 0 then
    begin
      Result := True;
      Exit;
    end;
end;

{ 判断是否应该自动启动应用 }
{ 使用场景：EasyPostman-Setup.exe /VERYSILENT /AUTOSTART }
function ShouldAutoStart: Boolean;
begin
  Result := WizardSilent and CmdLineParamExists('/AUTOSTART');
end;

{ 判断是否应该提示用户启动应用（交互式安装时） }
function ShouldPromptStart: Boolean;
begin
  Result := not WizardSilent;
end;
