# Hostel Management System

A console-based Hostel Management System built in Java that handles member registration, room allocation, payment processing, and reporting — all persisted to flat CSV files with no external dependencies.

---

## Features

### Authentication
- Admin login with username and password
- **Forgot username** recovery using registered phone number
- **OTP-based password reset** — a 6-digit OTP is generated and must be verified (3 attempts) before setting a new password

### Member Management
- Add members with full validation (name, email, phone, address)
- Update member details (phone, email, address) — blank fields are skipped
- Soft-delete members (marked inactive, not erased)
- Search by Member ID or partial name match
- List all active members in a formatted table

### Room Management
- Add rooms with type (`STANDARD` or `LUXURY`), capacity, and rent
- Update rent and status (`FREE` / `OCCUPIED` / `MAINTENANCE`)
- View all rooms or only rooms with vacancies
- Real-time occupancy tracking (`filled / capacity`)

### Allocation Management
- Allocate a room to a member — triggers the payment flow before confirming
- Vacate by **Member ID** or by **Room Number** (clears all occupants)
- **Reallocate** — vacates current room, processes new payment, allocates new room
- Search allocation history by member or by room

### Payment Processing
- Integrated into the allocation flow (no allocation without payment)
- Supports **CASH** and **UPI** payment methods
- Generates a printed receipt with transaction ID (`TRX0001`, `TRX0002`, ...)
- Payment records are appended to file immediately (no data loss on crash)

### Reports
| Report | Description |
|---|---|
| Members With Rooms | All members currently allocated to a room |
| Members Without Rooms | Members registered but not yet allocated |
| Occupied Rooms | Rooms with at least one occupant |
| Available Rooms | Rooms with remaining vacancy |
| Room Type Summary | Count of Standard vs Luxury rooms and free slots |
| All Payments | Full payment ledger |
| Payments by Phone | Filter payment history by member phone |
| Payments by Room | Filter payment history by room number |
| Revenue Summary | Total revenue broken down by CASH and UPI |

### Data Persistence
- All data is saved to CSV files in the `data/` directory
- **Save Data** menu option saves members, rooms, allocations, and admin config
- Payments are written to file immediately on transaction
- Data is loaded on startup and IDs are restored to avoid duplicates

---

## Project Structure

```
JavaProject/
├── HostelManagementSystem.java   # Entire application in one file
└── data/                         # Auto-created on first run
    ├── admin.txt                 # Admin credentials (username, password, phone)
    ├── members.txt               # Member records (CSV)
    ├── rooms.txt                 # Room records (CSV)
    ├── allocations.txt           # Allocation records (CSV)
    └── payments.txt              # Payment records (CSV)
```

---

## Setup & Running

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- A terminal that supports ANSI color codes (Linux/macOS Terminal, Windows Terminal, VS Code terminal)

### Compile

```bash
javac HostelManagementSystem.java
```

### Run

```bash
java HostelManagementSystem
```

### Default Admin Credentials

| Field    | Value        |
|----------|--------------|
| Username | `admin`      |
| Password | `Admin@123`  |
| Phone    | `9876543210` |

> Change the password after first login using the **Reset Password** flow.

---

## Usage Walkthrough

```
MAIN MENU
=========
1) Login with default credentials (admin / Admin@123)

ADMIN DASHBOARD
===============
1) Member Management     → Register and manage hostel members
2) Room Management       → Add and configure rooms
3) Allocation Management → Assign and release rooms
4) Reports               → View occupancy and revenue data
5) Save Data             → Persist all changes to disk
6) Exit                  → Save and quit
```

### Typical Workflow

1. **Add a Room** — Room Management → Add Room (e.g., `101`, `STANDARD`, capacity `2`, rent `5000`)
2. **Add a Member** — Member Management → Add Member (name, valid email, 10-digit phone starting 6–9)
3. **Allocate Room** — Allocation Management → Allocate Room → enter Member ID and Room Number → select payment method → receipt is printed
4. **Save Data** — Dashboard → Save Data (or it auto-saves on Exit)

---

## System Limits

| Resource    | Limit |
|-------------|-------|
| Members     | 200   |
| Rooms       | 50    |
| Allocations | 400   |
| Payments    | 1000  |

---

## Java Concepts Demonstrated

### Object-Oriented Programming
- **Encapsulation** — all fields are package-private within static nested classes; access is through service methods
- **Static nested classes** — `Member`, `Room`, `Allocation`, `Payment` are model classes; `MemberService`, `RoomService`, `AllocationService`, `PaymentService`, `ReportService` are service classes — all nested inside the top-level class
- **Separation of concerns** — utility classes (`FileHandler`, `InputValidator`, `DateUtils`) are independent of business logic

### File I/O
- `BufferedReader` / `BufferedWriter` with `FileReader` / `FileWriter` for efficient line-by-line file access
- Append mode (`FileWriter(f, true)`) used for payments to avoid full rewrites on every transaction
- Custom CSV serialization with comma escaping (`\,`) to handle names containing commas

### Java Standard Library
- `java.time.LocalDate` and `DateTimeFormatter` for date handling (Java 8+ time API)
- `java.util.Random` for OTP generation
- Regex via `String.matches()` for email and phone validation (`^[6-9][0-9]{9}$`)

### Data Structures
- Fixed-size arrays instead of `ArrayList` — demonstrates manual index tracking and bounds checking
- Array copying with `System.arraycopy` and `Arrays.copyOf`

### Control Flow
- `switch` statements for menu navigation
- Loop-based interactive menus that return on back/exit selection

### Input Validation
- Email validated with regex
- Indian phone numbers validated (must start with 6–9, exactly 10 digits)
- Blank-skip pattern for optional update fields

### Terminal UI
- ANSI escape codes (`\u001B[31m` etc.) for colored output (RED, GREEN, YELLOW, CYAN, BOLD/RESET)
- `String.format` with fixed-width specifiers (`%-20s`, `%-10s`) for aligned table output

---

## Data File Format

**members.txt**
```
MB001,John Doe,john@example.com,9876543210,Chennai,2024-01-15,true
```

**rooms.txt**
```
101,STANDARD,2,5000.0,1/2
```

**allocations.txt**
```
AL0001,MB001,101,2024-01-16,,ACTIVE
```

**payments.txt**
```
TRX0001,John Doe,9876543210,101,2024-01-16,5000.0,CASH
```

**admin.txt**
```
admin
Admin@123
9876543210
```
