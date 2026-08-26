# TARUMT Resort Data-Structure Project

Compile every Java source from the project root in PowerShell:

```powershell
$sources = Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -d bin $sources
java -cp bin Main
```

## Main portals

- **Staff Login**: reservation queue/allocation/reports, housekeeping, front
  desk, and loyalty/reward administration (including the redemption queue).
- **Member / Guest**: existing members enter their Member ID; guests register
  as members before opening the personal reservation page.

Set `RESORT_STAFF_USERNAME` and `RESORT_STAFF_PASSWORD` before starting the
application. For a local demonstration only, set `RESORT_DEMO_MODE=true` to
explicitly enable the demo login `staff` / `staff123` when custom credentials
are not configured.
