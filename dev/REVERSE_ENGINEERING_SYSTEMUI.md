# Reverse Engineering New SystemUI Builds

Use this guide when vendor updates change internals and you need fresh class/method info.

## 1. Pull SystemUI APK

```powershell
adb shell pm path com.android.systemui
# Example output:
# package:/system_ext/priv-app/SystemUI/SystemUI.apk

adb pull /system_ext/priv-app/SystemUI/SystemUI.apk .\SystemUI.apk
```

## 2. Extract DEX Files

```powershell
Copy-Item .\SystemUI.apk .\_systemui_dex\SystemUI.zip -Force
Expand-Archive -Path .\_systemui_dex\SystemUI.zip -DestinationPath .\_systemui_dex\unz -Force
Get-ChildItem .\_systemui_dex\unz -Filter "classes*.dex"
```

## 3. Find Target Class Quickly

```powershell
rg -a -n "OplusConnectingDisplayExImpl|ConnectedDisplayInteractor|setOnSelectedListener|onSelected|showDialog" .\_systemui_dex\unz -g "classes*.dex"
```

## 4. Dump Candidate DEX

Pick the DEX that contains `OplusConnectingDisplayExImpl`.

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\dexdump.exe" -d .\_systemui_dex\unz\classes6.dex > .\_systemui_dex\classes6.dump.txt
```

## 5. Inspect Method and Callback Shape

```powershell
rg -n "Class descriptor  : 'Lcom/oplus/systemui/oplusstatusbar/display/OplusConnectingDisplayExImpl;'" .\_systemui_dex\classes6.dump.txt
rg -n "OplusConnectingDisplayExImpl\\.showDialog|setOnSelectedListener|showDialog\\$|onSelected" .\_systemui_dex\classes6.dump.txt
```

What to verify:

- `showDialog(PendingDisplay, boolean)` still exists.
- Which callback class is bound in `setOnSelectedListener(...)`.
- Whether callback naming changed from `$showDialog$1` to `$showDialog$2` (or another index).
- Whether `onSelected(int, boolean)` still exists.

## 6. Parse Dialog UI Dump (Optional)

```powershell
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml .\window_dump.xml
python dev\scripts\parse_uiautomator_dump.py .\window_dump.xml --package com.android.systemui --only-clickable
```

## 7. Cleanup Temporary Artifacts

```powershell
cmd /c "del /f /q SystemUI.apk window_dump.xml 2>nul & rmdir /s /q _systemui_dex 2>nul"
```
