# Skip Display Confirmation (LSPosed)

An LSPosed module for Oplus/OxygenOS that automatically confirms the USB external display connection dialog in SystemUI.

## Features

- Hooks `com.android.systemui` display connection flow.
- Automatically confirms the "Cast/Project screen" dialog.
- Removes the need for manual confirmation every time.

## Tested

- Device: OnePlus 13 (`CPH2653`)
- ROM: OxygenOS `16.0.3.501`
- LSPosed scope: `com.android.systemui`

If it does not work right after enabling, restart SystemUI or reboot the device.

## Credits

Huge thanks to the original author and repository:

- https://github.com/Xposed-Modules-Repo/io.github.dixtdf.oplus.display

This fork is based on the original implementation with compatibility adjustments for newer OxygenOS builds.

## Disclaimer

This project is provided for educational and research purposes only. Use it at your own risk and in compliance with local laws.
