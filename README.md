# TARUMT Resort Data-Structure Project

## Requirements

- JDK 11 or later (verified with JDK 11.0.21)
- PowerShell

Compile every Java source from the project root in PowerShell:

```powershell
$sources = Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac --release 11 -d bin $sources
java -cp bin Main
```

## Main application flow

The application has one functional main menu:

- **VIP Priority Room Allocation**: make and manage reservations, view the
  priority queue, allocate rooms, and submit member-points payment requests.
- **Loyalty & Rewards**: register members, view loyalty information, process
  redemption requests, and view member/tier/report data.
- **Front Desk Service**: check guests in or out and view billing reports.
- **Housekeeping & Task Log**: manage room-cleaning tasks and reports.

Use the same member ID issued by Loyalty & Rewards when making or viewing a
reservation in the VIP module.
