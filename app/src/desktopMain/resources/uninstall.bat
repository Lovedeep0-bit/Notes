@echo off
echo ============================================
echo   Notes App - Uninstaller
echo ============================================
echo.
echo This will uninstall Notes from your computer.
echo.
set /p confirm=Are you sure? (Y/N): 
if /i not "%confirm%"=="Y" (
    echo Uninstall cancelled.
    pause
    exit /b
)

echo.
echo Uninstalling Notes...

:: Try to find and run the MSI uninstall via registry
for /f "tokens=*" %%a in ('reg query "HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "Notes" /d 2^>nul ^| findstr "HKEY_"') do (
    for /f "tokens=2*" %%b in ('reg query "%%a" /v "UninstallString" 2^>nul ^| findstr "UninstallString"') do (
        echo Found uninstall entry, running...
        %%c
        goto :done
    )
)

:: Fallback: try HKLM
for /f "tokens=*" %%a in ('reg query "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" /s /f "Notes" /d 2^>nul ^| findstr "HKEY_"') do (
    for /f "tokens=2*" %%b in ('reg query "%%a" /v "UninstallString" 2^>nul ^| findstr "UninstallString"') do (
        echo Found uninstall entry, running...
        %%c
        goto :done
    )
)

:: Fallback: manual removal
echo Registry entry not found. Performing manual cleanup...
set "INSTALL_DIR=%LOCALAPPDATA%\Notes"
if exist "%INSTALL_DIR%" (
    rmdir /s /q "%INSTALL_DIR%"
    echo Removed installation directory.
)

:: Remove desktop shortcut
if exist "%USERPROFILE%\Desktop\Notes.lnk" (
    del "%USERPROFILE%\Desktop\Notes.lnk"
    echo Removed desktop shortcut.
)

:: Remove start menu shortcut
if exist "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Notes" (
    rmdir /s /q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Notes"
    echo Removed start menu entry.
)

:: Remove user data (optional)
echo.
set /p removeData=Remove user data (notes database)? (Y/N): 
if /i "%removeData%"=="Y" (
    if exist "%USERPROFILE%\NotesApp" (
        rmdir /s /q "%USERPROFILE%\NotesApp"
        echo Removed user data.
    )
)

:done
echo.
echo Notes has been uninstalled.
pause
