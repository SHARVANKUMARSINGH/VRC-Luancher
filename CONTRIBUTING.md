# Contributing to Cubiq Launcher

Thank you for your interest in contributing to **Cubiq Launcher**! As an open-source project driven by community feedback, we welcome code contributions, issue reports, translations, and ideas.

---

## 📋 Table of Contents
1. [Reporting Bugs](#reporting-bugs)
2. [Suggesting Features & Enhancements](#suggesting-features--enhancements)
3. [Setting Up Local Development](#setting-up-local-development)
4. [Pull Request Guidelines](#pull-request-guidelines)
5. [Code Style & Standards](#code-style--standards)

---

## 🐛 Reporting Bugs

Before creating an issue, please check the [existing issues](https://github.com/VivxKumar07/Cubiq-Launcher/issues) to see if someone else has already reported it.

When filing a bug report, please use our **[Bug Report Template](https://github.com/VivxKumar07/Cubiq-Launcher/issues/new?template=bug_report.yml)** and include:
- **Device Model & Android Version** (e.g. Poco X3 Pro, Android 13)
- **Cubiq Launcher Version** (e.g. v1.0.0 Debug/Release)
- **Renderer Selected** (Krypton, Kopper Zink, GL4ES, etc.)
- **Minecraft Version & Loader** (e.g. 1.21.4 Fabric)
- **Detailed Log File**: Exported crash or game log from the launcher.
- **Steps to Reproduce**: Clear step-by-step instructions.

---

## 💡 Suggesting Features & Enhancements

Have an idea to make Cubiq faster, more lightweight, or easier to use?
- Open a **[Feature Request](https://github.com/VivxKumar07/Cubiq-Launcher/issues/new?template=feature_request.yml)**.
- Clearly describe the problem your feature solves and how it enhances the user experience.

---

## 🛠️ Setting Up Local Development

1. **Prerequisites**:
   - Android Studio (Ladybug | 2024.2.1+)
   - JDK 17 or 21 configured as Gradle JDK
   - Android SDK with NDK 25.2+ installed

2. **Clone & Compile**:
   ```bash
   git clone --recursive https://github.com/VivxKumar07/Cubiq-Launcher.git
   cd Cubiq-Launcher
   ./gradlew CubiqLauncher:assembleDebug
   ```

3. **Deploy to Device**:
   ```bash
   ./gradlew CubiqLauncher:installDebug
   ```

---

## 🔄 Pull Request Guidelines

1. **Fork & Branch**: Create a feature branch with a descriptive name (e.g., `git checkout -b fix/touch-layout-gap` or `feat/mobileglues-renderer`).
2. **Atomic Commits**: Keep commits focused and provide clear, conventional commit messages:
   - `fix(ui): ...`
   - `feat(renderer): ...`
   - `perf(compose): ...`
   - `docs(readme): ...`
3. **No Unrelated Changes**: Do not bundle refactors or reformatting into bug-fix PRs.
4. **Preserve Licenses & Attribution**: Respect upstream GPL-3.0 licensing and never remove existing copyright headers.
5. **Verify Compilation**: Ensure `./gradlew CubiqLauncher:assembleDebug` builds cleanly without regressions before pushing.

---

## 🎨 Code Style & Standards

- **UI Framework**: Modern declarative UI is built with **Jetpack Compose** (`androidx.compose.*`).
- **Aesthetic**: Maintain the authentic Minecraft desktop launcher pixel aesthetic (custom pixel button styles, dark background gradients, authentic click sounds).
- **Architecture**: Keep UI components isolated from launch-engine JNI native code.

Thank you for helping make Cubiq Launcher the best Minecraft Java launcher on mobile!
