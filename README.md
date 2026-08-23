# TARUMT Resort Data Structures Prototype

BMCS2063 Data Structures and Algorithms assignment prototype for the TARUMT
Resort reservation, housekeeping, front-desk, and loyalty services.

## Requirements

- JDK 11 or newer
- Run commands from the repository root so the CSV data paths resolve correctly

## Compile

PowerShell:

```powershell
$sources = Get-ChildItem -Recurse -File -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $sources
```

## Run

```powershell
java -cp bin Main
```

The application opens the main menu. Select a module by entering its menu
number and follow the prompts.

## Modules

- VIP and Loyalty Tier Priority Room Allocation
- Housekeeping and Task Log
- Front Desk Service
- Loyalty and Rewards Service

The application reads and writes the CSV files inside each module's `src`
directory. Keep the working directory at the repository root when running the
program.

## Data structures used

The project uses the custom ADTs in the `adt` directory, including list,
queue, stack, priority queue, sorted list, and binary search tree structures.
It does not use Java collection container classes such as `java.util.List` or
`java.util.ArrayList`.
