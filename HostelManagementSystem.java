import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

class HostelManagementSystem {

    // Configurations
    static final int MAX_MEMBERS = 200;
    static final int MAX_ROOMS = 50;
    static final int MAX_ALLOCS = 400;
    static final String DATA_DIR = "data";
    static final String MEMBERS_FILE = "members.txt";
    static final String ROOMS_FILE = "rooms.txt";
    static final String ALLOCS_FILE = "allocations.txt";
    static final String ADMIN_FILE = "admin.txt";
    static final String PAYMENTS_FILE = "payments.txt";

    // Utilities
    static class FileHandler {
        private String basePath;

        public FileHandler(String basePath) {
            this.basePath = basePath;
            File d = new File(basePath);
            if (!d.exists())
                d.mkdirs();
        }

        public String[] readAllLines(String fileName) {
            File f = new File(basePath, fileName);
            if (!f.exists())
                return new String[0];
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                java.util.List<String> list = new java.util.ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    list.add(line);
                }
                br.close();
                return list.toArray(new String[0]);
            } catch (IOException e) {
                System.out.println("Error reading file " + fileName);
                return new String[0];
            }
        }

        public boolean writeLines(String fileName, String[] lines) {
            File f = new File(basePath, fileName);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));
                for (String l : lines) {
                    if (l != null)
                        bw.write(l);
                    bw.newLine();
                }
                bw.close();
                return true;
            } catch (IOException e) {
                System.out.println("Error writing file " + fileName);
                return false;
            }
        }

        public boolean appendLine(String fileName, String line) {
            File f = new File(basePath, fileName);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
                bw.write(line);
                bw.newLine();
                bw.close();
                return true;
            } catch (IOException e) {
                System.out.println("Error appending to file " + fileName);
                return false;
            }
        }
    }

    static class InputValidator {
        static boolean isNonEmpty(String s) {
            return s != null && s.trim().length() > 0;
        }

        static boolean isValidEmail(String e) {
            if (!isNonEmpty(e))
                return false;
            return e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        }

        static boolean isValidPhone(String p) {
            if (!isNonEmpty(p)) {
                return false;
            }
            return p.matches("^[6-9][0-9]{9}$");
        }
    }

    static class DateUtils {
        static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        static String today() {
            return LocalDate.now().format(F);
        }

        static boolean isValidDate(String d) {
            try {
                LocalDate.parse(d, F);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    // Models
    static class Member {
        String memberId, name, email, phone, address, joinDate;
        boolean active;

        Member(String memberId, String name, String email, String phone, String address, String joinDate) {
            this.memberId = memberId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.joinDate = joinDate;
            this.active = true;
        }

        String toCsv() {
            return memberId + "," + escape(name) + "," + escape(email) + "," + escape(phone) + "," + escape(address)
                    + "," + joinDate + "," + active;
        }

        static Member fromCsv(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 7) {
                return null;
            }
            Member m = new Member(p[0], unescape(p[1]), unescape(p[2]), unescape(p[3]), unescape(p[4]), p[5]);
            m.active = Boolean.parseBoolean(p[6]);
            return m;
        }

        static String escape(String s) {
            return s == null ? "" : s.replace(",", "\\,");
        }

        static String unescape(String s) {
            return s == null ? "" : s.replace("\\,", ",");
        }
    }

    static class Room {
        String roomNumber;
        String roomType;
        int capacity;
        int filled;
        double rent;

        Room(String rn, String rt, int cap, int filled, double rent) {
            this.roomNumber = rn;
            this.roomType = rt;
            this.capacity = cap;
            this.filled = filled;
            this.rent = rent;
        }

        boolean hasVacancy() {
            return filled < capacity && capacity > 0;
        }

        void inc() {
            if (filled < capacity)
                filled++;
        }

        void dec() {
            if (filled > 0)
                filled--;
        }

        String toCsvForSave() {
            return roomNumber + "," + roomType + "," + capacity + "," + rent + "," + filled + "/" + capacity;
        }

        String toPretty() {
            return roomNumber + " | " + roomType + " | cap:" + capacity + " | occupied:" + filled + "/" + capacity
                    + " | rent:₹" + String.format("%.2f", rent);
        }
    }

    static class Allocation {
        String allocId, memberId, roomNumber, allocDate, vacateDate, status;

        Allocation(String allocId, String memberId, String roomNumber, String allocDate) {
            this.allocId = allocId;
            this.memberId = memberId;
            this.roomNumber = roomNumber;
            this.allocDate = allocDate;
            this.vacateDate = "";
            this.status = "ACTIVE";
        }

        boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
        }

        void vacate(String date) {
            this.vacateDate = date;
            this.status = "VACATED";
        }

        String toCsv() {
            return allocId + "," + memberId + "," + roomNumber + "," + allocDate + "," + vacateDate + "," + status;
        }

        static Allocation fromCsv(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 6)
                return null;
            Allocation a = new Allocation(p[0], p[1], p[2], p[3]);
            a.vacateDate = p[4];
            a.status = p[5];
            return a;
        }
    }

    static class Payment {
        String trxId, name, phone, roomNumber, date, method;
        double amount;

        Payment(String trxId, String name, String phone, String roomNumber, String date, double amount, String method) {
            this.trxId = trxId;
            this.name = name;
            this.phone = phone;
            this.roomNumber = roomNumber;
            this.date = date;
            this.amount = amount;
            this.method = method;
        }

        String toCsv() {
            return trxId + "," + escape(name) + "," + phone + "," + roomNumber + "," + date + "," + amount + ","
                    + method;
        }

        static String escape(String s) {
            return s == null ? "" : s.replace(",", "\\,");
        }
    }

    /* ========== SERVICES ========== */
    static class AdminAuth {
        String username = "admin";
        String password = "Admin@123";
        String phone = "9876543210";

        void load(FileHandler fh) {
            String[] lines = fh.readAllLines(ADMIN_FILE);
            if (lines.length >= 3) {
                username = lines[0];
                password = lines[1];
                phone = lines[2];
            } else {
                save(fh);
            }
        }

        void save(FileHandler fh) {
            String[] lines = { username, password, phone };
            fh.writeLines(ADMIN_FILE, lines);
        }

        boolean authenticate(String u, String p) {
            return u != null && p != null && u.equals(username) && p.equals(password);
        }

        boolean verifyPhone(String ph) {
            return ph != null && ph.equals(phone);
        }

        int generateOTP() {
            java.util.Random r = new java.util.Random();
            return 100000 + r.nextInt(900000);
        }
    }

    static class MemberService {
        Member[] members = new Member[MAX_MEMBERS];
        int memberCount = 0;
        int idCounter = 0;
        FileHandler fh;

        MemberService(FileHandler fh) {
            this.fh = fh;
        }

        void reset() {
            members = new Member[MAX_MEMBERS];
            memberCount = 0;
            idCounter = 0;
        }

        String nextId() {
            idCounter++;
            return String.format("MB%03d", idCounter);
        }

        Member addMember(String name, String email, String phone, String address) {
            if (!InputValidator.isNonEmpty(name)) {
                System.out.println("Name cannot be empty");
                return null;
            }
            if (!InputValidator.isValidEmail(email)) {
                System.out.println("Invalid email");
                return null;
            }
            if (!InputValidator.isValidPhone(phone)) {
                System.out.println("Invalid phone number must starts with (6-9), 10 digits.");
                return null;
            }
            if (memberCount >= MAX_MEMBERS) {
                System.out.println("Max members reached");
                return null;
            }
            String id = nextId();
            Member m = new Member(id, name, email, phone, address, DateUtils.today());
            members[memberCount++] = m;
            System.out.println("Member added: " + id);
            return m;
        }

        Member findById(String id) {
            if (id == null)
                return null;
            for (int i = 0; i < memberCount; i++) {
                Member m = members[i];
                if (m != null && m.memberId.equalsIgnoreCase(id) && m.active) {
                    return m;
                }
            }
            return null;
        }

        boolean updateMember(String id, String phone, String email, String address) {
            Member m = findById(id);
            if (m == null) {
                System.out.println("Member Not Found");
                return false;
            }
            if (InputValidator.isNonEmpty(phone)) {
                if (!InputValidator.isValidPhone(phone)) {
                    System.out.println("Invalid Phone Number");
                    return false;
                }
                m.phone = phone;
            }
            if (InputValidator.isNonEmpty(email)) {
                if (!InputValidator.isValidEmail(email)) {
                    System.out.println("Invalid Email");
                    return false;
                }
                m.email = email;
            }
            if (InputValidator.isNonEmpty(address)) {
                m.address = address;
            }
            System.out.println("Member Details Updated");
            return true;
        }

        boolean deleteMember(String id) {
            Member m = findById(id);
            if (m == null) {
                System.out.println("Member not found");
                return false;
            }
            m.active = false;
            System.out.println("Member Deleted Successfully");
            return true;
        }

        Member[] searchMembersByName(String namePart) {
            if (!InputValidator.isNonEmpty(namePart)) {
                return new Member[0];
            }
            Member[] temp = new Member[memberCount];
            int c = 0;
            for (int i = 0; i < memberCount; i++) {
                Member m = members[i];
                if (m != null && m.active && m.name.toLowerCase().contains(namePart.toLowerCase())) {
                    temp[c++] = m;
                }
            }
            Member[] out = new Member[c];
            System.arraycopy(temp, 0, out, 0, c);
            return out;
        }

        void displayAllMembers() {
            boolean any = false;
            for (int i = 0; i < memberCount; i++) {
                Member m = members[i];
                if (m != null && m.active) {
                    System.out.println(
                            m.memberId + " | " + m.name + " | " + m.email + " | " + m.phone + " | " + m.joinDate);
                    any = true;
                }
            }
            if (!any) {
                System.out.println("No Details Found");
            }
        }

        void loadFromFile() {
            reset();
            String[] lines = fh.readAllLines(MEMBERS_FILE);
            for (String l : lines) {
                if (l == null || l.trim().isEmpty()) {
                    continue;
                }
                Member m = Member.fromCsv(l);
                if (m != null) {
                    members[memberCount++] = m;
                    String num = m.memberId.replaceAll("[^0-9]", "");
                    try {
                        int v = Integer.parseInt(num);
                        if (v > idCounter) {
                            idCounter = v;
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }

        void saveToFile() {
            String[] lines = new String[memberCount];
            int c = 0;
            for (int i = 0; i < memberCount; i++) {
                Member m = members[i];
                if (m != null) {
                    lines[c++] = m.toCsv();
                }
            }
            fh.writeLines(MEMBERS_FILE, java.util.Arrays.copyOf(lines, c));
        }
    }

    static class RoomService {
        Room[] rooms = new Room[MAX_ROOMS];
        int roomCount = 0;
        FileHandler fh;

        RoomService(FileHandler fh) {
            this.fh = fh;
        }

        void reset() {
            rooms = new Room[MAX_ROOMS];
            roomCount = 0;
        }

        Room findRoomByNumber(String rn) {
            if (rn == null) {
                return null;
            }
            for (int i = 0; i < roomCount; i++) {
                Room r = rooms[i];
                if (r != null && r.roomNumber.equalsIgnoreCase(rn)) {
                    return r;
                }
            }
            return null;
        }

        // Loading Details from file
        void loadFromFile() {
            reset();
            String[] lines = fh.readAllLines(ROOMS_FILE);
            for (String l : lines) {
                if (l == null || l.trim().isEmpty()) {
                    continue;
                }
                String[] p = l.split(",", -1);
                try {
                    if (p.length >= 5 && p[4].contains("/")) {
                        String rn = p[0].trim();
                        String type = p[1].trim();
                        int cap = Integer.parseInt(p[2].trim());
                        double rent = Double.parseDouble(p[3].trim());
                        String[] f = p[4].trim().split("/", -1);
                        int filled = 0;
                        try {
                            filled = Integer.parseInt(f[0].trim());
                        } catch (Exception ex) {
                            filled = 0;
                        }
                        rooms[roomCount++] = new Room(rn, type, cap, filled, rent);
                    } else if (p.length >= 5) {
                        // fallback
                        String rn = p[0].trim();
                        String type = p[1].trim();
                        int cap = Integer.parseInt(p[2].trim());
                        int filled = Integer.parseInt(p[3].trim());
                        double rent = Double.parseDouble(p[4].trim());
                        rooms[roomCount++] = new Room(rn, type, cap, filled, rent);
                    } else {
                        System.out.println("Skipping malformed rooms line: " + l);
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing room line, skipping: " + l);
                }
            }
        }

        void saveToFile() {
            String[] lines = new String[roomCount];
            int c = 0;
            for (int i = 0; i < roomCount; i++) {
                Room r = rooms[i];
                if (r != null) {
                    lines[c++] = r.toCsvForSave();
                }
            }
            fh.writeLines(ROOMS_FILE, java.util.Arrays.copyOf(lines, c));
        }

        void displayAllRooms() {
            if (roomCount == 0) {
                System.out.println("No Rooms Found.");
                return;
            }
            for (int i = 0; i < roomCount; i++) {
                Room r = rooms[i];
                if (r != null) {
                    System.out.println(r.toPretty());
                }
            }
        }

        void displayAvailableRooms() {
            boolean any = false;
            for (int i = 0; i < roomCount; i++) {
                Room r = rooms[i];
                if (r != null && r.hasVacancy()) {
                    System.out.println(r.toPretty());
                    any = true;
                }
            }
            if (!any) {
                System.out.println("No available rooms.");
            }
        }
    }

    static class AllocationService {
        Allocation[] allocations = new Allocation[MAX_ALLOCS];
        int allocCount = 0;
        int allocIdCounter = 0;
        FileHandler fh;
        RoomService rs;
        MemberService ms;

        AllocationService(FileHandler fh, RoomService rs, MemberService ms) {
            this.fh = fh;
            this.rs = rs;
            this.ms = ms;
        }

        void reset() {
            allocations = new Allocation[MAX_ALLOCS];
            allocCount = 0;
            allocIdCounter = 0;
        }

        String nextAllocId() {
            allocIdCounter++;
            return String.format("AL%04d", allocIdCounter);
        }

        Allocation allocateRoom(String memberId, String roomNumber, String allocDate) {
            Member m = ms.findById(memberId);
            if (m == null) {
                System.out.println("Member not found");
                return null;
            }
            Room r = rs.findRoomByNumber(roomNumber);
            if (r == null) {
                System.out.println("Room not found");
                return null;
            }
            if (!r.hasVacancy()) {
                System.out.println("Room is full or under maintenance");
                return null;
            }
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.isActive() && a.memberId.equalsIgnoreCase(memberId)) {
                    System.out.println("Member already allocated to room " + a.roomNumber);
                    return null;
                }
            }
            String aid = nextAllocId();
            Allocation a = new Allocation(aid, memberId, roomNumber, allocDate);
            if (allocCount < MAX_ALLOCS)
                allocations[allocCount++] = a;
            r.inc();
            System.out.println("Allocated: " + aid);
            return a;
        }

        boolean vacateRoomByMember(String memberId, String vacateDate) {
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.isActive() && a.memberId.equalsIgnoreCase(memberId)) {
                    a.vacate(vacateDate);
                    Room r = rs.findRoomByNumber(a.roomNumber);
                    if (r != null) {
                        r.dec();
                    }
                    System.out.println("Vacated allocation: " + a.allocId);
                    return true;
                }
            }
            System.out.println("No active allocation found for this member: " + memberId);
            return false;
        }

        boolean vacateRoomByRoom(String roomNumber, String vacateDate) {
            boolean foundAny = false;
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.isActive() && a.roomNumber.equalsIgnoreCase(roomNumber)) {
                    a.vacate(vacateDate);
                    foundAny = true;
                }
            }
            if (!foundAny) {
                System.out.println("No active allocations found for room: " + roomNumber);
                return false;
            }

            Room r = rs.findRoomByNumber(roomNumber);
            if (r != null) {
                int occ = 0;
                for (int i = 0; i < allocCount; i++) {
                    Allocation a = allocations[i];
                    if (a != null && a.isActive() && a.roomNumber.equalsIgnoreCase(roomNumber)) {
                        occ++;
                    }
                }
                r.filled = occ;
            }
            System.out.println("Vacated allocations for room: " + roomNumber);
            return true;
        }

        Allocation[] getAllocationsByMember(String memberId) {
            Allocation[] tmp = new Allocation[allocCount];
            int c = 0;
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.memberId.equalsIgnoreCase(memberId)) {
                    tmp[c++] = a;
                }
            }
            Allocation[] out = new Allocation[c];
            System.arraycopy(tmp, 0, out, 0, c);
            return out;
        }

        Allocation[] getAllocationsByRoom(String roomNumber) {
            Allocation[] tmp = new Allocation[allocCount];
            int c = 0;
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.roomNumber.equalsIgnoreCase(roomNumber)) {
                    tmp[c++] = a;
                }
            }
            Allocation[] out = new Allocation[c];
            System.arraycopy(tmp, 0, out, 0, c);
            return out;
        }

        void displayAllAllocations() {
            if (allocCount == 0) {
                System.out.println("No allocations.");
                return;
            }
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null) {
                    System.out.println(a.allocId + " | " + a.memberId + " -> " + a.roomNumber + " | " + a.status + " | "
                            + a.allocDate);
                }
            }
        }

        void loadFromFile() {
            reset();
            String[] lines = fh.readAllLines(ALLOCS_FILE);
            for (String l : lines) {
                if (l == null || l.trim().isEmpty())
                    continue;
                Allocation a = Allocation.fromCsv(l);
                if (a != null) {
                    allocations[allocCount++] = a;
                    String num = a.allocId.replaceAll("[^0-9]", "");
                    try {
                        int v = Integer.parseInt(num);
                        if (v > allocIdCounter) {
                            allocIdCounter = v;
                        }
                    } catch (Exception e) {

                    }
                }
            }

            for (int i = 0; i < rs.roomCount; i++) {
                Room r = rs.rooms[i];
                if (r != null) {
                    r.filled = 0;
                }
            }
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.isActive()) {
                    Room rr = rs.findRoomByNumber(a.roomNumber);
                    if (rr != null) {
                        rr.inc();
                    }
                }
            }
        }

        void saveToFile() {
            String[] lines = new String[allocCount];
            int c = 0;
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null) {
                    lines[c++] = a.toCsv();
                }
            }
            fh.writeLines(ALLOCS_FILE, java.util.Arrays.copyOf(lines, c));
        }

        boolean memberHasActiveAllocation(String memberId) {
            for (int i = 0; i < allocCount; i++) {
                Allocation a = allocations[i];
                if (a != null && a.memberId.equalsIgnoreCase(memberId) && a.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    static class ReportService {
        MemberService ms;
        RoomService rs;
        AllocationService as;

        ReportService(MemberService ms, RoomService rs, AllocationService as) {
            this.ms = ms;
            this.rs = rs;
            this.as = as;
        }

        void membersWithRooms() {
            boolean any = false;
            for (int i = 0; i < as.allocCount; i++) {
                Allocation a = as.allocations[i];
                if (a != null && a.isActive()) {
                    Member m = ms.findById(a.memberId);
                    if (m != null) {
                        System.out.println(m.memberId + " | " + m.name + " | Room: " + a.roomNumber + " | AllocDate: "
                                + a.allocDate);
                        any = true;
                    }
                }
            }
            if (!any) {
                System.out.println("No active allocations.");
            }
        }

        void membersWithoutRooms() {
            boolean any = false;
            for (int i = 0; i < ms.memberCount; i++) {
                Member m = ms.members[i];
                if (m == null || !m.active) {
                    continue;
                }
                boolean has = as.memberHasActiveAllocation(m.memberId);
                if (!has) {
                    System.out.println(m.memberId + " | " + m.name + " | " + m.phone);
                    any = true;
                }
            }
            if (!any) {
                System.out.println("All members have rooms or no members exist.");
            }
        }

        void occupiedRooms() {
            boolean any = false;
            for (int i = 0; i < rs.roomCount; i++) {
                Room r = rs.rooms[i];
                if (r != null && r.filled > 0) {
                    System.out.println(r.roomNumber + " | Type: " + r.roomType + " | Occupied: " + r.filled + "/"
                            + r.capacity + " | Rent:₹" + String.format("%.2f", r.rent));
                    any = true;
                }
            }
            if (!any) {
                System.out.println("No occupied rooms.");
            }
        }

        void availableRooms() {
            boolean any = false;
            for (int i = 0; i < rs.roomCount; i++) {
                Room r = rs.rooms[i];
                if (r != null && r.hasVacancy()) {
                    System.out.println(r.roomNumber + " | Type: " + r.roomType + " | Vacant: " + (r.capacity - r.filled)
                            + "/" + r.capacity + " | Rent:₹" + String.format("%.2f", r.rent));
                    any = true;
                }
            }
            if (!any) {
                System.out.println("No available rooms.");
            }
        }

        void roomTypeSummary() {
            int std = 0, lux = 0, stdFree = 0, luxFree = 0;
            for (int i = 0; i < rs.roomCount; i++) {
                Room r = rs.rooms[i];
                if (r == null) {
                    continue;
                }
                if ("STANDARD".equalsIgnoreCase(r.roomType)) {
                    std++;
                    if (r.hasVacancy()) {
                        stdFree++;
                    }
                } else {
                    lux++;
                    if (r.hasVacancy())
                        luxFree++;
                }
            }
            System.out.println("Standard Rooms: " + std + " (Free: " + stdFree + ")");
            System.out.println("Luxury Rooms  : " + lux + " (Free: " + luxFree + ")");
        }
    }

    /* ========== MAIN APPLICATION ========== */
    static Scanner sc = new Scanner(System.in);
    static FileHandler fh;
    static AdminAuth adminAuth;
    static MemberService memberService;
    static RoomService roomService;
    static AllocationService allocationService;
    static ReportService reportService;
    static int paymentCounter = 0;

    public static void main(String[] args) {
        fh = new FileHandler(DATA_DIR);
        adminAuth = new AdminAuth();
        memberService = new MemberService(fh);
        roomService = new RoomService(fh);
        allocationService = new AllocationService(fh, roomService, memberService);
        reportService = new ReportService(memberService, roomService, allocationService);

        // load data
        adminAuth.load(fh);
        roomService.loadFromFile();
        memberService.loadFromFile();
        allocationService.loadFromFile();
        loadPaymentCounter();

        System.out.println("MAIN MENU");
        System.out.println("---------");

        if (!loginFlow()) {
            System.out.println("Authentication failed. Exiting.....");
            return;
        }

        while (true) {
            showMainMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    memberManagementMenu();
                    break;
                case "2":
                    roomManagementMenu();
                    break;
                case "3":
                    allocationManagementMenu();
                    break;
                case "4":
                    reportsMenu();
                    break;
                case "5":
                    saveAll();
                    break;
                case "6":
                    saveAll();
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    /* ========== LOGIN FLOW ========== */
    static boolean loginFlow() {
        while (true) {
            System.out.print("Enter Username: ");
            String user = sc.nextLine();
            System.out.print("Enter Password: ");
            String pass = sc.nextLine();
            if (adminAuth.authenticate(user, pass)) {
                System.out.println("Login successful!");
                return true;
            }

            // determine which field incorrect
            boolean userOk = adminAuth.username.equals(user);
            boolean passOk = adminAuth.password.equals(pass);

            if (!userOk) {
                System.out.println("You have entered wrong usename");
                System.out.println("1) Try again  2) Forgot username  3) Exit");
                System.out.print("Enter Your Choice : ");
                String ch = sc.nextLine().trim();
                if ("1".equals(ch)) {
                    continue;
                }
                if ("2".equals(ch)) {
                    System.out.print("Enter your registered phone number : ");
                    String ph = sc.nextLine().trim();
                    if (adminAuth.verifyPhone(ph)) {
                        System.out.println("Your username is " + adminAuth.username);
                    } else {
                        System.out.println("Phone does not match");
                    }
                    continue;
                }
                return false;
            } else if (!passOk) {
                System.out.println("You have entered wrong password");
                System.out.println("1) Try again  2) Reset password  3) Exit");
                System.out.print("Enter Your Choice : ");
                String ch = sc.nextLine().trim();
                if ("1".equals(ch)) {
                    continue;
                }
                if ("2".equals(ch)) {
                    System.out.print("Enter your registered phone number : ");
                    String ph = sc.nextLine().trim();
                    if (!adminAuth.verifyPhone(ph)) {
                        System.out.println("Phone number does not match");
                        continue;
                    }
                    int otp = adminAuth.generateOTP();
                    System.out.println(otp + " is your one time password (OTP) to proceed.");
                    int tries = 3;
                    boolean ok = false;
                    while (tries-- > 0) {
                        System.out.print("Enter OTP : ");
                        String entered = sc.nextLine().trim();
                        if (entered.equals(String.valueOf(otp))) {
                            ok = true;
                            break;
                        } else {
                            System.out.println("Entered OTP is Invalid, attempts left: " + tries);
                        }
                    }
                    if (!ok) {
                        System.out.println("OTP verification failed");
                        continue;
                    }
                    System.out.print("Enter new password : ");
                    String np = sc.nextLine();
                    System.out.print("Re-enter new password: ");
                    String np2 = sc.nextLine();
                    if (!np.equals(np2)) {
                        System.out.println("Passwords do not match");
                        continue;
                    }
                    adminAuth.password = np;
                    adminAuth.save(fh);
                    System.out.println("Password updated successfully- please login with new credentials");
                    continue;
                }
                return false;
            } else {
                continue;
            }
        }
    }

    /* ========== MENUS ========== */
    static void showMainMenu() {
        System.out.println();
        System.out.println("ADMIN DASHBOARD");
        System.out.println("---------------");
        System.out.println("1) Member Management");
        System.out.println("2) Room Management");
        System.out.println("3) Allocation Management");
        System.out.println("4) Reports");
        System.out.println("5) Save Data");
        System.out.println("6) Exit");
        System.out.print("Enter Your Choice : ");
    }

    static void memberManagementMenu() {
        while (true) {
            System.out.println();
            System.out.println("MEMBER MANAGEMENT");
            System.out.println("-----------------");
            System.out.println("1) Add Member");
            System.out.println("2) Update Member");
            System.out.println("3) Delete Member");
            System.out.println("4) Search Member by ID");
            System.out.println("5) Search Member by Name");
            System.out.println("6) Show All Members");
            System.out.println("7) Back");
            System.out.print("Enter Your Choice : ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": {
                    System.out.print("Enter Full Name         : ");
                    String name = sc.nextLine();
                    System.out.print("Enter Your Email ID     : ");
                    String email = sc.nextLine();
                    System.out.print("Enter Your Phone Number : ");
                    String phone = sc.nextLine();
                    System.out.print("Enter Your Address      : ");
                    String addr = sc.nextLine();
                    memberService.addMember(name, email, phone, addr);
                    break;
                }
                case "2": {
                    System.out.print("Enter Member ID: ");
                    String id = sc.nextLine();
                    Member found = memberService.findById(id);
                    if (found == null) {
                        System.out.println("Member ID not found");
                        break;
                    }
                    System.out.println("Member found: " + found.name + " | " + found.email + " | " + found.phone);
                    System.out.print("New Phone Number (blank to skip) : ");
                    String ph = sc.nextLine();
                    System.out.print("New Email ID (blank to skip) : ");
                    String em = sc.nextLine();
                    System.out.print("New Address (blank to skip) : ");
                    String ad = sc.nextLine();
                    memberService.updateMember(id, ph, em, ad);
                    break;
                }
                case "3": {
                    System.out.print("Enter Member ID to delete: ");
                    String id = sc.nextLine();
                    memberService.deleteMember(id);
                    break;
                }
                case "4": {
                    System.out.print("Enter Member ID: ");
                    String id = sc.nextLine();
                    Member m = memberService.findById(id);
                    if (m != null) {
                        System.out.println(m.memberId + " | " + m.name + " | " + m.email + " | " + m.phone);
                    } else {
                        System.out.println("Member not found");
                    }
                    break;
                }
                case "5": {
                    System.out.print("Enter name search: ");
                    String nm = sc.nextLine();
                    Member[] res = memberService.searchMembersByName(nm);
                    if (res.length == 0) {
                        System.out.println("No matching members");
                    } else {
                        for (Member mm : res) {
                            System.out.println(mm.memberId + " | " + mm.name + " | " + mm.phone);
                        }
                    }
                    break;
                }
                case "6":
                    memberService.displayAllMembers();
                    break;
                case "7":
                    return;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    static void roomManagementMenu() {
        while (true) {
            System.out.println();
            System.out.println("ROOM MANAGEMENT");
            System.out.println("---------------");
            System.out.println("1) Show All Rooms");
            System.out.println("2) Show Available Rooms");
            System.out.println("3) Update Room (rent/status)");
            System.out.println("4) Back");
            System.out.print("Enter Your Choice : ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1":
                    roomService.displayAllRooms();
                    break;
                case "2":
                    roomService.displayAvailableRooms();
                    break;
                case "3": {
                    System.out.print("Enter Room Number: ");
                    String rn = sc.nextLine();
                    Room r = roomService.findRoomByNumber(rn);
                    if (r == null) {
                        System.out.println("Room not found");
                        break;
                    }
                    System.out.print("New Rent (blank for skip): ");
                    String rentS = sc.nextLine();
                    System.out.print("New Status (FREE/OCCUPIED/MAINTENANCE) (blank for skip): ");
                    String st = sc.nextLine();
                    if (InputValidator.isNonEmpty(rentS)) {
                        try {
                            r.rent = Double.parseDouble(rentS);
                        } catch (Exception e) {
                            System.out.println("Invalid rent");
                        }
                    }
                    if (InputValidator.isNonEmpty(st)) {
                        String up = st.toUpperCase();
                        if (!up.equals("FREE") && !up.equals("OCCUPIED") && !up.equals("MAINTENANCE")) {
                            System.out.println("Invalid status");
                        } else {
                            if (up.equals("FREE")) {
                                r.filled = 0;
                            }
                            r.roomType = r.roomType;
                        }
                    }
                    System.out.println("Room updated");
                    break;
                }
                case "4":
                    return;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    static void allocationManagementMenu() {
        while (true) {
            System.out.println();
            System.out.println("ALLOCATION MANAGEMENT");
            System.out.println("---------------------");
            System.out.println("1) Allocate Room");
            System.out.println("2) Vacate Room by Member");
            System.out.println("3) Vacate Room by Room");
            System.out.println("4) Reallocate Room");
            System.out.println("5) Search Allocation by Member");
            System.out.println("6) Search Allocation by Room");
            System.out.println("7) Show All Allocations");
            System.out.println("8) Back");
            System.out.print("Enter Your Choice : ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1":
                    allocateWithPaymentFlow();
                    break;
                case "2": {
                    System.out.print("Enter Member ID: ");
                    String id = sc.nextLine();
                    allocationService.vacateRoomByMember(id, DateUtils.today());
                    break;
                }
                case "3": {
                    System.out.print("Enter Room Number: ");
                    String rn = sc.nextLine();
                    allocationService.vacateRoomByRoom(rn, DateUtils.today());
                    break;
                }
                case "4": {
                    System.out.print("Enter Member ID to reallocate: ");
                    String mid = sc.nextLine();
                    System.out.print("Enter New Room Number: ");
                    String newRn = sc.nextLine();
                    boolean vac = allocationService.vacateRoomByMember(mid, DateUtils.today());
                    if (vac) {
                        boolean paid = paymentFlowForMember(mid, newRn);
                        if (paid) {
                            allocationService.allocateRoom(mid, newRn, DateUtils.today());
                        } else {
                            System.out.println("Reallocation cancelled (payment failed)");
                        }
                    } else {
                        System.out.println("Reallocation aborted (no active allocation)");
                    }
                    break;
                }
                case "5": {
                    System.out.print("Enter Member ID: ");
                    String mid = sc.nextLine();
                    Allocation[] arr = allocationService.getAllocationsByMember(mid);
                    if (arr.length == 0) {
                        System.out.println("No allocation available associated with the ID : " + mid);
                    } else {
                        for (Allocation a : arr) {
                            System.out.println(a.allocId + " | " + a.roomNumber + " | " + a.status);
                        }
                    }
                    break;
                }
                case "6": {
                    System.out.print("Enter Room Number: ");
                    String rn = sc.nextLine();
                    Allocation[] arr = allocationService.getAllocationsByRoom(rn);
                    if (arr.length == 0) {
                        System.out.println("No allocations");
                    } else {
                        for (Allocation a : arr) {
                            System.out.println(a.allocId + " | " + a.memberId + " | " + a.status);
                        }
                    }
                    break;
                }
                case "7":
                    allocationService.displayAllAllocations();
                    break;
                case "8":
                    return;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    // PAYMENT & ALLOCATION HELPERS
    static void allocateWithPaymentFlow() {
        System.out.print("Enter Member ID: ");
        String mid = sc.nextLine();
        System.out.print("Enter Room Number: ");
        String rn = sc.nextLine();
        Member m = memberService.findById(mid);
        if (m == null) {
            System.out.println("Member Details Not Found");
            return;
        }
        Room r = roomService.findRoomByNumber(rn);
        if (r == null) {
            System.out.println("Room Details Not Found");
            return;
        }
        if (!r.hasVacancy()) {
            System.out.println("Room Fully Occupied, Please allot another room!");
            return;
        }
        if (allocationService.memberHasActiveAllocation(mid)) {
            System.out.println("Member already has an active allocation");
            return;
        }
        System.out.println("Room Rent is : " + String.format("%.2f", r.rent));
        boolean paid = paymentFlowForMember(mid, rn);
        if (!paid) {
            System.out.println("Payment failed or cancelled");
            return;
        }
        allocationService.allocateRoom(mid, rn, DateUtils.today());
    }

    static boolean paymentFlowForMember(String memberId, String roomNumber) {
        Member m = memberService.findById(memberId);
        Room r = roomService.findRoomByNumber(roomNumber);
        if (m == null || r == null) {
            return false;
        }
        System.out.println("Select payment method: 1) CASH  2) UPI");
        System.out.print("Enter Your Choice: ");
        String ch = sc.nextLine().trim();
        String method = null;
        if ("1".equals(ch)) {
            method = "CASH";
        } else if ("2".equals(ch)) {
            method = "UPI";
        } else {
            System.out.println("Invalid Payment Method");
            return false;
        }
        String trx = nextPaymentId();
        Payment p = new Payment(trx, m.name, m.phone, roomNumber, DateUtils.today(), r.rent, method);
        boolean ok = fh.appendLine(PAYMENTS_FILE, p.toCsv());
        if (!ok) {
            System.out.println("Failed to save payment record");
            return false;
        }
        System.out.println("Payment successful");
        System.out.println("Tranasction ID : " + trx);
        return true;
    }

    static int loadPaymentCounter() {
        String[] lines = fh.readAllLines(PAYMENTS_FILE);
        int max = 0;
        for (String l : lines) {
            if (l == null || l.trim().isEmpty()) {
                continue;
            }
            String[] p = l.split(",", -1);
            if (p.length > 0) {
                String num = p[0].replaceAll("[^0-9]", "");
                try {
                    int v = Integer.parseInt(num);
                    if (v > max)
                        max = v;
                } catch (Exception e) {

                }
            }
        }
        paymentCounter = max;
        return max;
    }

    static String nextPaymentId() {
        paymentCounter++;
        return String.format("TRX%04d", paymentCounter);
    }

    // REPORTS
    static void reportsMenu() {
        while (true) {
            System.out.println();
            System.out.println("REPORTS");
            System.out.println("-------");
            System.out.println("1) Members With Rooms");
            System.out.println("2) Members Without Rooms");
            System.out.println("3) Occupied Rooms");
            System.out.println("4) Available Rooms");
            System.out.println("5) Room Type Summary");
            System.out.println("6) Back");
            System.out.print("Enter Your Choice : ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1":
                    reportService.membersWithRooms();
                    break;
                case "2":
                    reportService.membersWithoutRooms();
                    break;
                case "3":
                    reportService.occupiedRooms();
                    break;
                case "4":
                    reportService.availableRooms();
                    break;
                case "5":
                    reportService.roomTypeSummary();
                    break;
                case "6":
                    return;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    // SAVE THE FILES
    static void saveAll() {
        memberService.saveToFile();
        roomService.saveToFile();
        allocationService.saveToFile();
        adminAuth.save(fh);
        System.out.println("All Data Saved Successfully.");
    }
}