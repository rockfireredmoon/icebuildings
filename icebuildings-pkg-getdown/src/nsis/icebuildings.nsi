;Icebuildings

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"
  !include ..\..\target\project.nsh
  !include "InstallOptions.nsh"

;--------------------------------
;General

  ;Name and file
  Name "Icebuildings"
  
  ; Handled by plugin
  ;OutFile "${PROJECT_BUILD_DIR}\SetupIcebuildings.exe"

  ;Default installation folder
  InstallDir "$LOCALAPPDATA\Icebuildings"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\Icebuildings" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel user

  !define JRE_VERSION "1.8.0"
  !define JRE_SETUP "jre-8u45-windows-i586.exe"

;--------------------------------
;Variables

  Var StartMenuFolder
  Var InstallJRE
  Var JREPath
  
;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
  !define MUI_HEADERIMAGE
  !define MUI_HEADERIMAGE_BITMAP "icebuildingsbanner.bmp"

;--------------------------------
;Pages

  ; This page checks for JRE. It displays a dialog based on JRE.ini if it needs to install JRE
  ; Otherwise you won't see it.
  Page custom CheckInstalledJRE
 
  ; Define headers for the 'Java installation successfully' page
  !define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Java installation complete"
  !define MUI_PAGE_HEADER_TEXT "Installing Java runtime"
  !define MUI_PAGE_HEADER_SUBTEXT "Please wait while we install the Java runtime"
  
  !define MUI_PAGE_CUSTOMFUNCTION_PRE myPreInstfiles
  !define MUI_PAGE_CUSTOMFUNCTION_LEAVE RestoreSections
  
  !insertmacro MUI_PAGE_WELCOME
  !define MUI_PAGE_CUSTOMFUNCTION_SHOW licpageshow
  !insertmacro MUI_PAGE_LICENSE "License.txt"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY

;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\Icebuildings" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Icebuildings"
  
  !insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder

  !insertmacro MUI_PAGE_INSTFILES
  !define MUI_FINISHPAGE_NOAUTOCLOSE
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_NOTCHECKED
  !define MUI_FINISHPAGE_RUN_TEXT "Start Icebuildings Now"
  !define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"
  !define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
  !define MUI_FINISHPAGE_SHOWREADME $INSTDIR\readme.txt
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  !insertmacro MUI_UNPAGE_FINISH
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "Icebuildings" Icebuildings

  SetOutPath "$INSTDIR"
  
  ;ADD YOUR OWN FILES HERE...
  File /r ..\..\target\getdown-stub\*.*
  File icebuildings.ico
  File icebuildingssmall.ico
  File icebuildingsbanner.bmp
  File Readme.txt
  File ..\..\..\src\main\installers\configuration\logging.properties
  
  ;Store installation folder
  WriteRegStr HKCU "Software\Icebuildings" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Menu Shortcuts" SecShortcuts
	SectionIn 1

  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Icebuildings.lnk" "$INSTDIR\getdown.jar" \
	"." \
	$INSTDIR\icebuildings.ico 0 SW_SHOWNORMAL ALT|CONTROL|SHIFT|U "Icebuildings"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
SectionEnd

Section installjre jre
  Push $0
  Push $1
 
;  MessageBox MB_OK "Inside JRE Section"
  Strcmp $InstallJRE "yes" InstallJRE JREPathStorage
  DetailPrint "Starting the JRE installation"
InstallJRE:
  File /oname=$TEMP\${JRE_SETUP} ${JRE_SETUP}
  MessageBox MB_OK "Installing JRE"
  DetailPrint "Launching JRE setup"
  ;ExecWait "$TEMP\j${JRE_SETUP} /S" $0
  ; The silent install /S does not work for installing the JRE, sun has documentation on the 
  ; parameters needed.  I spent about 2 hours hammering my head against the table until it worked
  ; ExecWait '"$TEMP\${JRE_SETUP}" /s /v\"/qn REBOOT=0 INSTALL_SILENT=1 WEB_ANALYTICS=0 NOSTARTMENU=1 AUTO_UPDATE=0\"' $0
  ExecWait '"$TEMP\${JRE_SETUP}" /s /L C:\jresetup.log"' $0
  DetailPrint "Setup finished"
  Delete "$TEMP\${JRE_SETUP}"
  StrCmp $0 "0" InstallVerif 0
  Push "The JRE setup has been abnormally interrupted."
  Goto ExitInstallJRE
 
InstallVerif:
  DetailPrint "Checking the JRE Setup's outcome"
;  MessageBox MB_OK "Checking JRE outcome"
  Push "${JRE_VERSION}"
  Call DetectJRE  
  Pop $0	  ; DetectJRE's return value
  StrCmp $0 "0" ExitInstallJRE 0
  StrCmp $0 "-1" ExitInstallJRE 0
  Goto JavaExeVerif
  Push "The JRE setup failed"
  Goto ExitInstallJRE
 
JavaExeVerif:
  IfFileExists $0 JREPathStorage 0
  Push "The following file : $0, cannot be found."
  Goto ExitInstallJRE
 
JREPathStorage:
;  MessageBox MB_OK "Path Storage"
  !insertmacro INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $1
  StrCpy $JREPath $0
  Goto End
 
ExitInstallJRE:
  Pop $1
  MessageBox MB_OK "The setup is about to be interrupted for the following reason : $1"
  Pop $1 	; Restore $1
  Pop $0 	; Restore $0
  Abort
End:
  Pop $1	; Restore $1
  Pop $0	; Restore $0
SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_Icebuildings ${LANG_ENGLISH} "Icebuildings."
  
  ;Header
  LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "Java Runtime Environment"
  LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "Installation"

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${Icebuildings} $(DESC_Icebuildings)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
  
;--------------------------------
;Reserve Files
 
  ;Only useful for BZIP2 compression
 
  ReserveFile "jre.ini"
  ReserveFile "${NSISDIR}\Plugins\InstallOptions.dll"

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ;ADD YOUR OWN FILES HERE...

  Delete "$INSTDIR\Uninstall.exe" ; delete self
  Delete "$INSTDIR\*"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\assets"

  RMDir /REBOOTOK "$INSTDIR"

  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
    
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Icebuildings.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"

  DeleteRegKey /ifempty HKCU "Software\Icebuildings"

SectionEnd

Function .onInit
 
  ;Extract InstallOptions INI Files
  !insertmacro INSTALLOPTIONS_EXTRACT "jre.ini"
  Call SetupSections
 
FunctionEnd
 
Function myPreInstfiles
 
  Call RestoreSections
  SetAutoClose true
 
FunctionEnd

Function RestoreSections
  !insertmacro UnselectSection ${jre}
  !insertmacro SelectSection ${Icebuildings}
  !insertmacro SelectSection ${SecShortcuts}
FunctionEnd
 
Function SetupSections
  !insertmacro SelectSection ${jre}
  !insertmacro UnselectSection ${Icebuildings}
  !insertmacro UnselectSection ${SecShortcuts}
FunctionEnd

Function licpageshow
    FindWindow $0 "#32770" "" $HWNDPARENT
    CreateFont $1 "Courier New" "$(^FontSize)"
    GetDlgItem $0 $0 1000
    SendMessage $0 ${WM_SETFONT} $1 1
FunctionEnd

Function LaunchLink
  ExecShell "" "$SMPROGRAMS\$StartMenuFolder\Icebuildings.lnk"
FunctionEnd

Function CheckInstalledJRE
  MessageBox MB_OK "Checking Installed JRE Version"
  Push "${JRE_VERSION}"
  Call DetectJRE
  Messagebox MB_OK "Done checking JRE version"
  Exch $0	; Get return value from stack
  StrCmp $0 "0" NoFound
  StrCmp $0 "-1" FoundOld
  Goto JREAlreadyInstalled
 
FoundOld:
  MessageBox MB_OK "Old JRE found"
  !insertmacro INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "Icebuildings requires a more recent version of the Java Runtime Environment than the one found on your computer. The installation of JRE ${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
NoFound:
  MessageBox MB_OK "JRE not found"
  !insertmacro INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "No Java Runtime Environment could be found on your computer.\nThe installation of JRE v${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
MustInstallJRE:
  Exch $0	; $0 now has the installoptions page return value
  ; Do something with return value here
  Pop $0	; Restore $0
  StrCpy $InstallJRE "yes"
  Return
 
JREAlreadyInstalled:
;  MessageBox MB_OK "No download: ${TEMP2}"
  MessageBox MB_OK "JRE already installed"
  StrCpy $InstallJRE "no"
  !insertmacro INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $JREPATH
  Pop $0		; Restore $0
  Return
 
FunctionEnd
 
; Returns: 0 - JRE not found. -1 - JRE found but too old. Otherwise - Path to JAVA EXE
 
; DetectJRE. Version requested is on the stack.
; Returns (on stack)	"0" on failure (java too old or not installed), otherwise path to java interpreter
; Stack value will be overwritten!
 
Function DetectJRE
  Exch $0	; Get version requested  
		; Now the previous value of $0 is on the stack, and the asked for version of JDK is in $0
  Push $1	; $1 = Java version string (ie 1.5.0)
  Push $2	; $2 = Javahome
  Push $3	; $3 and $4 are used for checking the major/minor version of java
  Push $4
  MessageBox MB_OK "Detecting JRE"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  MessageBox MB_OK "Read : $1"
  StrCmp $1 "" DetectTry2
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
  MessageBox MB_OK "Read 3: $2"
  StrCmp $2 "" DetectTry2
  Goto GetJRE
 
DetectTry2:
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  MessageBox MB_OK "Detect Read : $1"
  StrCmp $1 "" NoFound
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$1" "JavaHome"
  MessageBox MB_OK "Detect Read 3: $2"
  StrCmp $2 "" NoFound
 
GetJRE:
; $0 = version requested. $1 = version found. $2 = javaHome
  MessageBox MB_OK "Getting JRE"
  IfFileExists "$2\bin\java.exe" 0 NoFound
  StrCpy $3 $0 1			; Get major version. Example: $1 = 1.5.0, now $3 = 1
  StrCpy $4 $1 1			; $3 = major version requested, $4 = major version found
  MessageBox MB_OK "Want $3 , found $4"
  IntCmp $4 $3 0 FoundOld FoundNew
  StrCpy $3 $0 1 2
  StrCpy $4 $1 1 2			; Same as above. $3 is minor version requested, $4 is minor version installed
  MessageBox MB_OK "Want $3 , found $4" 
  IntCmp $4 $3 FoundNew FoundOld FoundNew
 
NoFound:
  MessageBox MB_OK "JRE not found"
  Push "0"
  Goto DetectJREEnd
 
FoundOld:
  MessageBox MB_OK "JRE too old: $3 is older than $4"
;  Push ${TEMP2}
  Push "-1"
  Goto DetectJREEnd  
FoundNew:
  MessageBox MB_OK "JRE is new: $3 is newer than $4"
 
  Push "$2\bin\java.exe"
;  Push "OK"
;  Return
   Goto DetectJREEnd
DetectJREEnd:
	; Top of stack is return value, then r4,r3,r2,r1
	Exch	; => r4,rv,r3,r2,r1,r0
	Pop $4	; => rv,r3,r2,r1r,r0
	Exch	; => r3,rv,r2,r1,r0
	Pop $3	; => rv,r2,r1,r0
	Exch 	; => r2,rv,r1,r0
	Pop $2	; => rv,r1,r0
	Exch	; => r1,rv,r0
	Pop $1	; => rv,r0
	Exch	; => r0,rv
	Pop $0	; => rv 
FunctionEnd