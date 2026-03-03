# Kitchen Board Stable Polling

## Purpose
TBD - Ensure the kitchen board polling remains stable and does not clear visible orders during refresh.

## Requirements

### Requirement: Kitchen board keeps orders visible during polling
The kitchen board SHALL show a blocking loading state only on the initial load (or when filters change) and SHALL keep the last successful list of orders visible during subsequent refresh polls.

#### Scenario: Initial load shows loading
- **WHEN** the kitchen board is opened (or filters change)
- **THEN** the UI shows a loading state until the first successful response

#### Scenario: Polling does not clear orders
- **WHEN** a polling refresh is triggered
- **THEN** the UI keeps displaying the last successful list of orders without switching to a blocking loading screen

### Requirement: Polling requests do not overlap
The kitchen board SHALL avoid overlapping polling requests by cancelling or skipping a new poll when a previous request is still in flight.

#### Scenario: In-flight request prevents overlap
- **WHEN** a polling request is already in progress
- **THEN** the next scheduled poll is skipped or delayed until the current request completes

### Requirement: Polling errors do not wipe the UI
The kitchen board SHALL preserve the last successful orders list if a polling request fails and provide a non-blocking error indicator with a retry option.

#### Scenario: Error during polling
- **WHEN** a polling request fails
- **THEN** the UI keeps the existing orders and surfaces an error state without clearing the list

### Requirement: Polling cadence remains active while screen is mounted
The kitchen board SHALL refresh active orders on a recurring interval (about every 3 seconds) while the screen is mounted.

#### Scenario: Periodic refresh
- **WHEN** the kitchen board remains open
- **THEN** the system issues periodic refresh requests at the configured interval
