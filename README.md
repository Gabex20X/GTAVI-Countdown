# GTA VI Countdown

A simple Java desktop countdown widget for GTA VI's release on November 19th, 2026 (00:00 GMT-3).

It renders as a borderless, transparent, always-on-top overlay you can drag anywhere on your desktop, so it blends in like part of your wallpaper. Closes from a system tray icon.

## Requirements

- JDK 21+ (needs `jpackage`, bundled with the JDK since version 14)

`build.ps1` expects the JDK at `C:\Program Files\Java\jdk-21`. If yours is installed elsewhere, edit the `$jdk` variable at the top of that script.

## Setup

### 1. Get the Pricedown font

The countdown text uses the **Pricedown** font (the GTA logo typeface), which isn't bundled in this repo due to its license (see the EULA included in the font's download).

1. Go to [dafont.com](https://www.dafont.com) and search for **"Pricedown"**.
2. Click **Download**.
3. Unzip it and find the `.otf` file (usually named `Pricedown Bl.otf`).
4. Rename it to `Pricedown.otf` and place it at:
   ```
   src/fonts/Pricedown.otf
   ```

If you skip this step, the app still works — it falls back to a bold system font automatically.

### 2. Build

```powershell
.\build.ps1
```

This compiles the app and packages it into a standalone `.exe` (no Java install required to run it) at:
```
dist\GTA6Countdown\GTA6Countdown.exe
```

### 3. Run

```powershell
.\dist\GTA6Countdown\GTA6Countdown.exe
```

Drag the widget with the left mouse button to position it. Right-click the tray icon to exit.

### 4. (Optional) Start with Windows

```powershell
.\enable-autostart.ps1
```

Creates a shortcut in your Startup folder. To undo:

```powershell
.\disable-autostart.ps1
```
