# Recovery Playbook (APK/DEX/XML Only)

Use this when a ROM update changes SystemUI internals.

## 1. Pull SystemUI APK from Device

```powershell
adb devices -l
adb shell pm path com.android.systemui
adb pull /system_ext/priv-app/SystemUI/SystemUI.apk .\SystemUI.apk
```

## 2. Extract DEX

```powershell
Copy-Item .\SystemUI.apk .\_systemui_dex\SystemUI.zip -Force
Expand-Archive -Path .\_systemui_dex\SystemUI.zip -DestinationPath .\_systemui_dex\unz -Force
Get-ChildItem .\_systemui_dex\unz -Filter "classes*.dex"
```

## 3. Locate Relevant Classes/Methods

```powershell
rg -a -n "OplusConnectingDisplayExImpl|ConnectedDisplayInteractor|setOnSelectedListener|showDialog|onSelected" .\_systemui_dex\unz -g "classes*.dex"
```

## 4. Dump Candidate DEX and Parse

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\dexdump.exe" -d .\_systemui_dex\unz\classes6.dex > .\_systemui_dex\classes6.dump.txt
rg -n "OplusConnectingDisplayExImpl\\.showDialog|setOnSelectedListener|showDialog\\$|onSelected" .\_systemui_dex\classes6.dump.txt
```

## 5. Parse UI Dump (Optional)

```powershell
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml .\window_dump.xml
python dev\scripts\parse_uiautomator_dump.py .\window_dump.xml --package com.android.systemui --contains "display"
```

## 6. Cleanup Temporary Files

```powershell
cmd /c "del /f /q SystemUI.apk window_dump.xml 2>nul & rmdir /s /q _systemui_dex 2>nul"
```
