# Changelog
All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
## [Unreleased]

## [3.0.0] - -07-2025

### Added
- feat: Implement SMS Remote Control feature in Settings

## [2.0.0] - -07-2025
### Added
- feat: Implement SMS Remote Control feature in Settings
## [1.6.3] - 2025-07-03
### Fixed
- **CI/CD**: Complete GitHub Actions workflow fixes for all environments
- Added debug keystore creation to both ci.yml and release.yml workflows
- Fixed signing configuration compatibility with CI environments
- Ensured proper APK generation in automated builds
### Technical
- Version code incremented to 18
- All GitHub Actions workflows now operational
- Automated release pipeline with proper signing
- Enhanced CI stability and reliability
## [1.6.2] - 2025-07-03
### Fixed
- **CI/CD**: GitHub Actions build failure with keystore configuration
- Fixed 'Keystore file not found' error in CI environment
- Added automatic debug keystore generation for CI builds
- Improved signing reliability across different environments
### Technical
- Version code incremented to 17
- Enhanced CI workflow with proper keystore handling
- Maintained security for development builds
## [1.6.1] - 2025-07-03
### Fixed
- **Build**: Widget drawable color resource for successful compilation
- Fixed stats_item_background.xml color references
- Resolved Android resource linking issues
- Ensured successful APK generation (~8.2 MB)
### Technical
- Version code incremented to 16
- Build compatibility improvements
- Resource reference corrections
## [1.6.0] - 2025-07-03
### Added
- **Widget**: Home screen widget for delivery statistics
- Real-time display of webhook, SMS, and email success counts
- Color-coded statistics with modern Material Design
- Tap-to-open main app functionality
- Auto-updates on successful deliveries
- Compact 2x1 widget size (180dp x 110dp)
### Enhanced
- **Statistics**: Comprehensive delivery tracking system
- Success rate calculations for each delivery method
- Last activity timestamp tracking
- Overall performance metrics display
- Widget integration with statistics updates
### Technical
- Version code incremented to 15
- StatisticsWidgetProvider implementation
- Efficient broadcast system for widget updates
- Modern widget layout with proper theming
## [1.5.0] - 2025-07-03
### Added
- **Statistics**: Comprehensive delivery statistics tracking
- Real-time success/failure counts for webhook, SMS, and email
- Success rate calculations and visual indicators
- Statistics dashboard in main activity
- Last activity timestamp tracking
- Statistics reset and export capabilities
### Enhanced
- **Delivery Tracking**: Complete integration across all delivery methods
- DeliveryStatistics class for centralized tracking
- Updated DeliveryRouter with statistics integration
- RequestWorker webhook statistics tracking
- Auto-refresh statistics on activity resume
### UI Improvements
- Modern statistics card with Material Design
- Color-coded delivery method indicators
- Visual success rate displays
- Enhanced main activity layout
### Technical
- Version code incremented to 14
- JSON export functionality for statistics
- Efficient SharedPreferences-based storage
- Background statistics updating
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
