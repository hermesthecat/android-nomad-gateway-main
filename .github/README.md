# 🔄 GitHub Actions CI/CD

This repository uses GitHub Actions for automated building, testing, and releasing.

## 📋 Workflows

### 1. 🔍 CI - Build & Test (`ci.yml`)

**Triggers:**

- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**Jobs:**

- ✅ **Lint Check** - Code quality analysis
- ✅ **Unit Tests** - Run all unit tests
- ✅ **Debug Build** - Ensure app builds correctly
- ✅ **Release Build** - Build release APK
- 📊 **Test Reports** - Upload test results as artifacts

### 2. 🚀 Automated Release Build (`release.yml`)

**Triggers:**

- New tags matching `v*.*.*` pattern (e.g., `v1.4.0`, `v2.0.1`)

**Jobs:**

- 🏗️ **Build APKs** - Both debug and release variants
- 🧪 **Run Tests** - Unit tests and Android instrumentation tests
- 📝 **Extract Release Notes** - From `CHANGELOG.md`
- 🏷️ **Create GitHub Release** - Automated release creation
- 📎 **Upload Assets**:
  - Debug APK (`hermes-gateway-vX.X.X-debug.apk`)
  - Release APK (`hermes-gateway-vX.X.X-release.apk`)
  - Checksums (`checksums.txt`)
  - Build information (`build-info.md`)

## 🔧 How to Trigger a Release

1. **Create and push a version tag:**

   ```bash
   git tag -a v1.5.0 -m "Release v1.5.0 - New features"
   git push origin v1.5.0
   ```

2. **GitHub Actions will automatically:**
   - Build debug and release APKs
   - Run all tests
   - Extract release notes from `CHANGELOG.md`
   - Create a GitHub release
   - Upload APK files and metadata

## 📊 Build Status

You can check the build status in the **Actions** tab of the repository or by looking at the badges in the main README.

## 🔐 Permissions

The workflows use the default `GITHUB_TOKEN` which has the necessary permissions to:

- Read repository contents
- Create releases
- Upload release assets
- Write to the repository (for artifacts)

## 🐛 Troubleshooting

### Build Fails

- Check the **Actions** tab for detailed logs
- Ensure all tests pass locally before pushing
- Verify the `CHANGELOG.md` format is correct

### Release Not Created

- Ensure the tag follows the `v*.*.*` pattern
- Check that the tag was pushed to the repository
- Verify the `GITHUB_TOKEN` has sufficient permissions

### APK Upload Fails

- Check that the APK was built successfully
- Verify the file paths in the workflow are correct
- Ensure the release was created before attempting uploads

## 📝 Release Notes Format

Release notes are automatically extracted from `CHANGELOG.md`. Ensure your changelog follows this format:

```markdown
## [1.5.0] - 2025-07-03

### Added
- New feature descriptions

### Changed
- Modified functionality

### Fixed
- Bug fixes

### Security
- Security improvements
```

## 🔄 Continuous Integration

Every push and pull request triggers:

- Code quality checks (lint)
- Unit test execution
- Build verification
- Artifact generation for manual testing

This ensures code quality and prevents broken builds from reaching the main branch.
