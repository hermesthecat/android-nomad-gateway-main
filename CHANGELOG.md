# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2025-07-03

### Changed

- **BREAKING**: Package name changed from `tech.wdg.incomingactivitygateway` to `com.keremgok.hermesgateway`
- Application name updated to "Hermes Gateway"
- Complete rebranding of the project
- All Java classes and imports updated to new package structure
- AndroidManifest.xml and build configuration updated
- Maintained full backward compatibility for user data and settings

### Fixed

- Layout XML namespace issues with CardView attributes
- Exception handling in EmailDeliveryService and SmtpEmailDeliveryService
- JavaMail packaging conflicts in build configuration

### Technical

- Version code incremented to 13
- Build system validated and tested
- All Android instrumentation tests updated and passing

## [1.3.0] - 2025-06-04

### Added

- Improved background stability operation; Added app webhooks (start, stop, sim status change). Added extended data optional sending with forwarding rules (configurable for each rule); Added WIFI permission;

## [1.2.1] - 2025-05-31

### Added

- Release version bump (patch)

## [1.2.0] - 2025-05-31

### Added

- Add comprehensive release automation and GitHub integration

## [1.1.7] - 2025-05-31

### Added

- Test unified release workflow with single commit

## [1.1.6] - 2025-05-31

### Added

- Test enhanced release workflow with git commits

## [1.1.5] - 2025-05-31

### Added

- Test README update functionality

## [1.1.4] - 2025-05-31

### Added

- Fix APK signing for physical device installation

## [1.1.3] - 2025-05-31

### Added

- Test complete release workflow

## [1.1.2] - 2025-05-31

### Added

- Final testing and cleanup

## [1.1.1] - 2025-05-31

### Added

- Test version bump

## [1.1.0] - 2025-05-31

### Added

- Add new features and improvements
