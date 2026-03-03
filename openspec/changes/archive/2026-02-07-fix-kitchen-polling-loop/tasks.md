## 1. Polling Logic

- [x] 1.1 Refactor `KitchenBoardPage` to separate initial loading vs refresh state
- [x] 1.2 Prevent overlapping polling requests (in-flight guard or scheduled polling)

## 2. UI Behavior

- [x] 2.1 Keep last successful orders visible during refresh polling
- [x] 2.2 Show non-blocking error banner on polling failures with retry

## 3. Validation

- [x] 3.1 Manual verify: kitchen board no longer flickers (orders remain visible)
- [x] 3.2 Manual verify: error during polling keeps orders and shows banner
