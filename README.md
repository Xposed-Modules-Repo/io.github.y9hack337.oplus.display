<div align="center">
  <img src="https://raw.githubusercontent.com/Xposed-Modules-Repo/io.github.y9hack337.oplus.display/master/app/src/main/res/mipmap/ic_launcher.png" width="220" height="220" />

# Skip Display Confirmation

[![LSPosed](https://img.shields.io/badge/LSPosed-Module-red.svg)](https://github.com/Xposed-Modules-Repo/)
[![Repository](https://img.shields.io/badge/GitHub-io.github.y9hack337.oplus.display-black.svg)](https://github.com/Xposed-Modules-Repo/io.github.y9hack337.oplus.display)
[![Releases](https://img.shields.io/badge/Download-Latest-blue.svg)](https://github.com/Xposed-Modules-Repo/io.github.y9hack337.oplus.display/releases)

**LSPosed module for Oplus/OxygenOS that auto-confirms the USB external display connection dialog in SystemUI.**
</div>

## Features

- Hooks `com.android.systemui` display connection flow.
- Automatically confirms the "Cast/Project Screen" dialog.
- Includes a module settings action to restart SystemUI.

## Tested

- Device: OnePlus 13 (`CPH2653`)
- ROM: OxygenOS `16.0.3.501`
- LSPosed scope: `com.android.systemui`

## Download

Get the latest build from:

- https://github.com/Xposed-Modules-Repo/io.github.y9hack337.oplus.display/releases

## Restart SystemUI

- The in-module **Restart SystemUI** button requires root (`su`) permission.
- If you do not want to grant root, simply reboot the phone.

Manual restart command (root required):

```bash
su -c "pkill -9 -f com.android.systemui || killall com.android.systemui"
```

From PC with ADB:

```bash
adb shell su -c "pkill -9 -f com.android.systemui || killall com.android.systemui"
```

## Credits

Huge thanks to the original author and repository:

- https://github.com/Xposed-Modules-Repo/io.github.dixtdf.oplus.display

This fork is based on the original implementation with compatibility improvements for newer OxygenOS builds.

## Disclaimer

This project is provided for educational and research purposes only.
Use it at your own risk and in compliance with local laws.
