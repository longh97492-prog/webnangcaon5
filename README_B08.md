# B08 - V4 security verification package

This ZIP only adds files. It does not overwrite any of the 40 previously passing tests.

Run from repository root (branch `feature/b08-tv4`):

```powershell
Expand-Archive -Path "$env:USERPROFILE\Downloads\B08_V4_NEXT_TESTS.zip" -DestinationPath . -Force
mvn "-Dtest=B08SecurityRegressionTest" test
mvn clean test
powershell -ExecutionPolicy Bypass -File .\scripts\check-b08-repository.ps1
```

Expected new count if original 40 remain: 45 tests. These new five tests have NOT been executed with Maven in this environment.

Review `/verification/B08_BM1_BM12_STATUS.csv`. None of the 12 risks may be marked fully verified from automated test counts alone. The coursework requires live validation and evidence screenshots 16-26. For production review, verify HTTPS, security response headers, permissions and historical secrets without publishing confidential values.

Before pushing, inspect `git diff --cached`, ensure no secrets and no files other than intended changes are staged. Use `git push group feature/b08-tv4` after committing to the team remote.
