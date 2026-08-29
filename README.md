# TARUMT Resort Hotel Management System

A Java console application that demonstrates the use of abstract data types and file-based persistence in a small hotel management system. The application combines reservation allocation, loyalty and rewards, front-desk operations, housekeeping, and report generation behind one main menu.

## Features

The application starts with a unified hotel management menu:

1. **VIP Priority Room Allocation**
2. **Loyalty & Rewards**
3. **Front Desk Service**
4. **Housekeeping & Task Log**

### VIP Priority Room Allocation

- Register reservation requests for existing loyalty members.
- Look up members by member ID or phone number.
- View a member's reservations, search reservations, or list all reservations.
- Place pending reservation requests in a priority queue based on loyalty tier.
- Allocate available Standard Rooms by priority.
- Confirm or reject pending requests when room capacity is insufficient.
- Submit a confirmed, unpaid reservation for member-points payment approval.
- View monthly reservation and room-allocation reports.
- Export reports as chart PDFs.

New reservations are initially saved as `PENDING`. Room allocation is a separate operation so staff can review the waiting queue before assigning rooms.

### Loyalty & Rewards

- Register members with unique passport and phone number values.
- View member profiles, available points, total expenses, current tier, and benefits.
- Display point-expiry alerts and history-based booking promotions.
- Process the next pending redemption or member-points payment request.
- View pending request history, the member list, and tier progression.
- Generate expiring-points and point-transaction reports.
- Export loyalty reports as chart PDFs.

The current tier thresholds are based on total expenses:

| Tier | Total-expense range | Reservation priority |
| --- | ---: | ---: |
| Classic | RM0–RM199 | Lowest |
| Silver | RM200–RM499 | 2 |
| Gold | RM500–RM700 | 3 |
| Platinum | RM701 and above | Highest |

### Front Desk Service

- Search for a reservation by confirmation number, member ID, or guest name.
- Check in confirmed reservations when the check-in date has been reached.
- Record payment using Cash, Credit / Debit Card, Touch n Go, or Online Banking.
- Block check-in while a member-points payment request is pending approval.
- View billing details and calculate the stay charge from room rate and nights.
- Check out checked-in guests and update room status.
- Award loyalty points after eligible completed stays.
- Record a future late check-out time and notify housekeeping.
- Generate outstanding-balance and payment-method reports.
- Export front-desk reports as chart PDFs.

### Housekeeping & Task Log

- Add a cleaning task for an existing room.
- Prevent more than one unfinished cleaning task for the same room.
- Update a task through `DIRTY`, `CLEANING_IN_PROGRESS`, `INSPECTED`, and `READY_FOR_CHECK_IN`.
- Roll back the most recent task-status change.
- Search tasks by room.
- List all tasks, summarize tasks by status, or filter tasks by creation date.
- Automatically create a cleaning task when a reservation is checked out.
- Mark a room available again when its task reaches `READY_FOR_CHECK_IN`.
- Export housekeeping reports as chart PDFs.

## Requirements

- JDK 11 or later
- A terminal that can run the Java compiler and runtime
- Windows PowerShell for the commands below

The program uses only the Java standard library. No external JAR files or database server are required.

## Getting started

Run all commands from the repository root:

```powershell
cd "C:\path\to\Data-Structure"
```

Check that Java is available:

```powershell
java -version
javac -version
```

Compile every Java source file into `bin`:

```powershell
$sources = Get-ChildItem -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName
javac --release 11 -d bin $sources
```

Start the unified application:

```powershell
java -cp bin Main
```

The application should be started from the repository root because the DAOs use root-relative paths such as `VIPPriorityRoomAllocation/src/rooms.csv`.

## Data persistence

The system loads data when a module starts and rewrites the relevant CSV file after changes. The default files are:

| File | Purpose |
| --- | --- |
| `VIPPriorityRoomAllocation/src/rooms.csv` | Room number, nightly rate, and room status |
| `VIPPriorityRoomAllocation/src/reservations.csv` | Guest, reservation, room assignment, payment, and reservation status |
| `LoyaltyAndRewardsService/src/member.csv` | Member identity, points, and total expenses |
| `LoyaltyAndRewardsService/src/transaction.csv` | Earned points, remaining points, dates, and source references |
| `LoyaltyAndRewardsService/src/requests.csv` | Redemption and member-points payment requests |
| `FrontDeskService/src/late_checkout_extensions.csv` | Active late check-out times and reasons |
| `HousekeepingAndTaskLog/src/housekeeping_tasks.csv` | Cleaning tasks, status changes, timestamps, and remarks |

CSV files are runtime state. They may contain member or guest identity fields and operational records, so back them up before manually editing or resetting them. Keep the header row and the existing column order intact.

Missing data files are created with their expected headers when the relevant DAO first needs them. Invalid rows may be skipped or reported depending on the file being loaded; malformed room or reservation values can stop loading with an error.

## Typical cross-module workflow

1. Register a member in **Loyalty & Rewards**.
2. Use that member ID or phone number in **VIP Priority Room Allocation** to submit a reservation.
3. Review the priority waiting queue and allocate available rooms.
4. If required, submit a member-points payment request and approve it in **Loyalty & Rewards**.
5. Check the guest in through **Front Desk Service** after the check-in date is reached.
6. Check the guest out after the stay. The room becomes `NEEDS_CLEANING` and a housekeeping task can be created automatically.
7. Complete the housekeeping task to return the room to `AVAILABLE`.

The shared member ID and confirmation number are the main identifiers connecting the modules.

## Data structures demonstrated

The project includes custom implementations in the `adt` package rather than relying exclusively on Java collection classes:

| ADT | Usage in the system |
| --- | --- |
| `MaxHeapPriorityQueue` | Orders pending reservations by loyalty priority during room allocation |
| `LinkedQueue` | Processes loyalty and member-points requests in queue order |
| `ArrayStack` | Stores housekeeping status changes for last-change rollback |
| `BinarySearchTree` | Indexes reservations by confirmation number for front-desk lookup |
| `SortedArrayList` | Produces ordered transaction, reservation, and report results |
| `ArrayList` and `LinkedList` | Store module records and filtered results |

The main application follows a simple boundary-control-DAO structure:

- **Boundary** classes handle menus, input, tables, and user-facing messages.
- **Control** classes implement business rules and coordinate module workflows.
- **DAO** classes load and save CSV data.
- **Entity** classes represent members, reservations, rooms, requests, and tasks.
- **Reporting** classes prepare text reports and chart data for PDF export.
- **Common** classes provide shared validation, UI formatting, animation, and the dependency-free PDF writer.

## Project structure

```text
.
├── Main.java
├── MainUI.java
├── adt/                         # Custom ADT interfaces and implementations
├── common/                      # Shared validation, UI, and PDF utilities
├── VIPPriorityRoomAllocation/   # Reservations and priority room allocation
├── LoyaltyAndRewardsService/   # Members, points, tiers, and requests
├── FrontDeskService/            # Check-in, check-out, billing, and late checkout
├── HousekeepingAndTaskLog/      # Cleaning tasks and rollback history
├── output/pdf/                  # Generated report PDFs
└── bin/                         # Compiled .class files (generated)
```

Each service directory contains `boundary`, `control`, `dao`, `entity`, and, where needed, `reporting`, `utility`, and `src` data directories.

## Reports and PDF output

Reports are first displayed in the console. When a report menu offers chart export, choose `Y` to create a PDF in:

```text
output/pdf/
```

The generated absolute path is printed. The application attempts to open the PDF with the desktop's default PDF viewer; on systems without desktop integration, open the printed path manually.

## Validation and status rules

- Dates use `yyyy-MM-dd` unless a prompt explicitly requests `yyyy-MM` or `yyyy-MM-dd HH:mm`.
- Reservation check-out must be after check-in.
- Only confirmed reservations with assigned rooms can be checked in.
- Only checked-in reservations can be checked out or extended.
- A late check-out time must be in the future and must include a reason.
- A room must be `AVAILABLE` before allocation and becomes `RESERVED`, `OCCUPIED`, and then `NEEDS_CLEANING` as the guest lifecycle progresses.
- Member-points payment requests require a confirmed reservation with an assigned room and remain subject to Loyalty approval.

## Troubleshooting

### `javac` or `java` is not recognized

Install JDK 11 or later and ensure the JDK `bin` directory is on `PATH`. Confirm with `java -version` and `javac -version`.

### Data files cannot be found

Run the program from the repository root. The application uses relative paths and is not intended to be launched from `bin` or an individual module directory.

### A PDF is generated but does not open

The PDF is still saved in `output/pdf/`. Open the path printed by the application manually.

### The application shows unexpected existing records

The CSV files preserve data between runs. Exit the application, back up the relevant CSV files, and edit or replace them only if you intentionally want to reset the data.

## Authors

- Chee Weng — application entry point, loyalty and rewards, shared utilities
- Wan Yin — VIP priority room allocation
- Yi Ren — front-desk service
- Zhe Sheng — housekeeping and task log
