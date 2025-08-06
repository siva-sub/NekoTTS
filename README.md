# NekoTTS - Local Text-to-Speech for Android

NekoTTS is a fully local, privacy-focused Text-to-Speech (TTS) application for Android that provides system-wide TTS service integration. It runs completely offline using ONNX models for inference.

## Features

- 🔒 **100% Local & Private** - All processing happens on your device
- 🎯 **System-wide Integration** - Works with any app that supports Android TTS
- 📖 **"Read Aloud" Support** - Select text in any app and use "Read Aloud" option
- 🌍 **Multi-language Support** - Supports 54 languages and dialects
- 🎨 **Dual Engine Support** - Kokoro TTS (310MB, 54 voices) and Kitten TTS (25MB, 8 voices)
- 🚀 **Android 15 Ready** - Full support for latest Android features
- 🔋 **Battery Optimized** - Smart power management with background service support

## Supported Languages

NekoTTS supports the following languages:
- English (US, UK, Australian, Canadian, Indian, Irish, Scottish)
- Spanish (Spain, Mexican)
- French (France, Canadian)
- German, Italian, Portuguese (Brazil, Portugal)
- Russian, Polish, Turkish, Swedish, Norwegian, Danish, Finnish, Dutch
- Arabic, Hebrew, Hindi, Japanese, Korean, Chinese (Simplified, Traditional)
- And many more!

## Installation

### Requirements
- Android 7.0 (API 24) or higher
- ARM v7, ARM64 v8, or x86/x86_64 processor

### Download

Download the appropriate APK for your device architecture:

- **Universal APK** (62MB) - Works on all devices
- **ARM64-v8a** (18MB) - For modern 64-bit ARM devices (recommended for most phones)
- **ARMv7** (13MB) - For older 32-bit ARM devices
- **x86/x86_64** (20MB) - For Intel/AMD based devices

### Setup

1. Install the APK
2. Go to Android Settings → Accessibility → Text-to-speech output
3. Select "NekoTTS" as your preferred engine
4. Configure voice and speed settings as desired

## Usage

### System TTS
Once set as the default TTS engine, NekoTTS will automatically handle all system TTS requests from apps like:
- E-book readers (Moon Reader, etc.)
- Web browsers
- Accessibility tools
- Navigation apps

### "Read Aloud" Feature
1. Select any text in any app
2. Look for "Read Aloud" in the context menu
3. Tap to have NekoTTS speak the selected text

## Technical Details

- **Framework**: Jetpack Compose with Material Design 3
- **Inference**: ONNX Runtime for Android
- **Architecture**: MVVM with Coroutines
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/NekoTTS.git
cd NekoTTS

# Build debug APK
./gradlew assembleDebug

# Build release APKs with ABI splits
./gradlew assembleRelease
```

## Models

NekoTTS uses open-source TTS models:
- **Kokoro TTS**: High-quality 82M parameter model with 54 voices
- **Kitten TTS**: Lightweight 15M parameter model with 8 voices

Models are loaded dynamically based on available memory and user preference.

## Privacy

NekoTTS is designed with privacy in mind:
- ✅ No internet permission required
- ✅ All processing happens locally
- ✅ No data collection or telemetry
- ✅ No ads or tracking
- ✅ Open source for transparency

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

NekoTTS is released under the MIT License. See LICENSE file for details.

## Acknowledgments

- Kokoro TTS model by Hex
- Kitten TTS model by the open-source community
- ONNX Runtime by Microsoft
- Android Jetpack libraries by Google

---

**Note**: This is an early release. Some features may be incomplete or have bugs. Please report any issues you encounter.