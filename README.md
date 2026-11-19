# Aqua Core – Water Intelligence

Native Android app (Kotlin, Jetpack Compose, Material 3, MVVM) that reads a **real water level sensor**
from an **Arduino Uno over USB OTG**. No internet, no demo data.

## Hardware
| Sensor pin | Arduino |
|-----------|---------|
| S         | A0      |
| +         | D7 (powered only while measuring → less corrosion) |
| −         | GND     |

1. Upload `arduino/AquaCoreSensor/AquaCoreSensor.ino` (9600 baud).
2. Connect the Uno to the phone with a USB OTG cable, open Aqua Core, tap **CONNECT DEVICE**, allow USB access.
3. Dashboard → **CALIBRATE SENSOR**: sensor dry → *SET EMPTY*; sensor at full level → *SET FULL*.
   The % / litres appear only after calibration (they are computed from your two real points).

Accepted serial lines: `{"raw":312.4}`, `312`, `raw=312`, `A0: 312`.

## Build
GitHub Actions (`.github/workflows/build.yml`) builds a debug APK on every push to `main`
(JDK 17, Gradle 8.9) and uploads it as the `AquaCore-debug-apk` artifact.
Locally: open in Android Studio (Ladybug or newer) and run.

## Structure
`model/` data classes · `data/network/` USB serial + line parser · `data/local/` stored config, calibration, history ·
`data/repository/` WaterRepository · `viewmodel/` AquaViewModel + UiState · `ui/` Compose theme, components, screens.
