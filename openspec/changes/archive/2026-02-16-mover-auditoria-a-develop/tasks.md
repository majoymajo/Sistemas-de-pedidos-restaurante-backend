## 1. Git preparation and verification

- [x] 1.1 Confirm that commit `e1f09b0` only adds `AUDITORIA.md`.
- [x] 1.2 Ensure local branches `main` and `develop` are available and up to date.

## 2. Move audit file to develop

- [x] 2.1 Checkout `develop`.
- [x] 2.2 Cherry-pick commit `e1f09b0` into `develop`.
- [x] 2.3 Verify `AUDITORIA.md` exists in `develop`.

## 3. Remove audit file from main safely

- [x] 3.1 Checkout `main`.
- [x] 3.2 Revert commit `e1f09b0` on `main`.
- [x] 3.3 Verify `AUDITORIA.md` is absent in `main`.

## 4. Final validation and reporting

- [x] 4.1 Compare file presence across both branches with `git ls-tree`.
- [x] 4.2 Summarize resulting commits and next push actions for the team.
