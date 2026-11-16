# Kinnect: How the Code Works

Kinnect is a role-based group project coordination and progress monitoring system with three roles: professor, group leader, and member. This guide explains every part of the code, where it sits, and how the parts work together.

Author: Ariel Soriano

| Fact | Detail |
|---|---|
| Language | Pure Java (Java 21), with JavaFX for the screens. No FXML, no Scene Builder. |
| Size | 21 `.java` files, about 5,900 lines |
| Package | `com.mycompany.mavenproject2` |
| Main class | `Mavenproject2` |
| Database | MySQL. Online on Aiven, or XAMPP on your own computer |
| Build tool | Maven (inside NetBeans), set up by `pom.xml` |

---

## 1. The big picture: four layers

Kinnect is built as four layers stacked on top of each other. Each layer only talks to the one below it.

```
 1. START         Mavenproject2.java  -->  KinnectApp.java (opens the window)
       |
       v
 2. SCREENS       AuthScreens      ProfessorScreens    StudentScreens
  (what you see)  GroupScreen      GroupPages          Dialogs (pop-up forms)
                  Ui (buttons, cards)                  Theme (colors, fonts)
       |   screens never touch the database; they ask a service
       v
 3. SERVICES      AuthService   ClassService    GroupService   TaskService
  (the rules)     CollabService BudgetService   Security (passwords)   Session (who is logged in)
       |   a service checks the rules first, then saves
       v
 4. DATA          Models (User, Task, ProjectGroup... matches the class diagram)
                  Db (connection, tables, SQL)          DemoData (sample data)
       |   JDBC (MySQL Connector/J)
       v
          MySQL database on Aiven (17 tables)
```

A simple way to picture it is a restaurant. The **screens** are the waiter taking your order. The **services** are the kitchen, which checks that the order is allowed and then cooks it. **Db** is the pantry where everything is stored.

### Every file and its job

| Layer | File | Lines | What it does |
|---|---|---|---|
| Start | `Mavenproject2.java` | 16 | The `main` method; it only launches `KinnectApp` |
| Start | `KinnectApp.java` | 241 | The one window; connects to the database, then shows screens in it |
| Screens | `AuthScreens.java` | 341 | Welcome, log in, sign up, join a class |
| Screens | `ProfessorScreens.java` | 190 | Professor's classes and class workspace |
| Screens | `StudentScreens.java` | 99 | Student's home: their classes, groups, tasks |
| Screens | `GroupScreen.java` | 225 | Group window with the sidebar; different pages per role |
| Screens | `GroupPages.java` | 1,077 | Pages inside a group: workspace, tasks, files, calendar, messages, documents, progress, budget |
| Screens | `Dialogs.java` | 681 | Every pop-up form (create task, record payment, manage group, and more) |
| Screens | `Ui.java` | 593 | Shared building blocks: text, buttons, cards, icons, pills, progress bars |
| Screens | `Theme.java` | 277 | Colors, fonts, and style rules from the Figma design |
| Services | `AuthService.java` | 72 | Sign up and log in |
| Services | `ClassService.java` | 134 | Professor creates classes; students join with the code |
| Services | `GroupService.java` | 276 | Groups, leaders, members, renaming, the name lock, permission checks |
| Services | `TaskService.java` | 109 | Leader creates and assigns tasks; the assigned member updates the status |
| Services | `CollabService.java` | 278 | Files, calendar events, messages, shared document links |
| Services | `BudgetService.java` | 297 | Ambagan: even split, custom shares, payments |
| Services | `Security.java` | 52 | Password hashing |
| Services | `Session.java` | 33 | Who is logged in right now |
| Data | `Models.java` | 145 | The data classes, following the class diagram |
| Data | `Db.java` | 511 | Database connection, CREATE TABLE statements, SQL helpers |
| Data | `DemoData.java` | 249 | Sample classes and groups on an empty database |

---

## 2. What happens when you open Kinnect

```
Mavenproject2.main()
   -> KinnectApp.start()          makes the window, loads the icon and Theme's styles
      -> KinnectApp.connect()
         -> Db.configure()        reads kinnect.properties (where the database is)
         -> Db.init()             creates missing tables, upgrades old ones, adds demo data if empty
         -> AuthScreens.welcome() shows the welcome screen
            (if any step fails -> the "Kinnect can't reach the database" screen with Try again)
```

### `Mavenproject2.java`

It holds only the `main` method, and its only job is to start JavaFX:

```java
public static void main(String[] args) {
    Application.launch(KinnectApp.class, args);
}
```

### `KinnectApp.java`

This is the one and only window. Every screen is swapped into it with `show(page)`, instead of opening new windows.

| Method | What it does |
|---|---|
| `start` | Creates the window, sets the Kinnect icon, loads the styles, then calls `connect` |
| `connect` | The startup sequence above |
| `show` | Replaces the current screen with a new one |
| `openModal`, `closeModal` | Show and hide pop-up forms on top of the current screen |
| `toast` | The small note that slides in after a save, like "Task created..." |
| `home` | Sends you to the right home page for your role |
| `logOut` | Ends the session and returns to the welcome screen |

---

## 3. Data layer: where everything is stored

### `Models.java`

These are the "nouns" of the system, written as Java `record`s. A record is a small class that just holds data. They match the class diagram:

| Record / enum | Holds |
|---|---|
| `User` and `AccountRole` (PROFESSOR, STUDENT) | An account |
| `ClassRoom`, `Enrollment` | A class, and a student's membership in it |
| `ProjectGroup`, `Member` and `GroupRole` (LEADER, MEMBER) | A group and the people in it |
| `Task` and `TaskStatus` (PENDING, ONGOING, COMPLETED) | A task |
| `GroupFile`, `CalendarEvent`, `Message`, `SharedDocument` | Collaboration items |
| `Expense`, `Contribution`, `BudgetSummary` | The ambagan |
| `Progress` | Completed, ongoing, and pending counts. `percent()` is completed divided by total, times 100 |

### `Db.java`

Everything that talks to MySQL is in here.

| Part | Methods | What it does |
|---|---|---|
| Settings | `configure`, `settingsFile` | Reads `kinnect.properties` for the database address, user, and password. Without it, Kinnect uses XAMPP on localhost |
| Connection | `connect`, `disconnect`, `run` | Keeps one connection open and reuses it. If the internet drops, `run` reconnects and tries once more |
| Helpers | `query`, `one`, `count`, `update`, `insert` | Short ways to run SQL. They all use `PreparedStatement` with `?` placeholders, which blocks SQL injection |
| Transactions | `transaction` | Groups several saves so they all succeed or all cancel (used for uploads and creating groups) |
| Setup | `SCHEMA`, `init`, `upgrade`, `moveLocalFiles` | The 17 CREATE TABLE statements, run on every start (they skip tables that already exist), plus updates for older Kinnect databases |

### `DemoData.java`

On an empty database with `demo.data=true`, it adds a sample professor, class, groups, tasks, and files, so Kinnect can be shown right away. Every demo password is `kinnect123`.

---

## 4. Service layer: the rules

Every service follows the same pattern: **check who you are, check the input, then save.** If a check fails, it throws an `AppException` with a friendly message, and the screen shows that message in red.

### `Security.java`

This handles password hashing. `hash()` makes a random 16-byte salt and runs PBKDF2 (HMAC SHA-256, 120,000 rounds). It saves the result as `pbkdf2$120000$salt$hash`, which is the scrambled text you see in the database. `matches()` hashes the typed password the same way and compares the two. The real password is never stored, so it can't be read back.

### `Session.java`

This remembers who is logged in right now: `start(user)` at login, `end()` at logout, and `user()` to ask who it is.

### `AuthService.java`

This handles accounts. `signUp` checks the name, the email format, that the password is at least 8 characters, that both passwords match, and that the email isn't already used. Then it saves the user with a hashed password. `logIn` looks up the email and checks the password with `Security.matches`. It gives the same "Incorrect email or password" message either way, so nobody can find out which emails have accounts.

### `ClassService.java`

This handles classes. `createClass` makes a class with a random code like `KQ7-4MX`; `newCode` keeps generating until the code is unused. `joinClass` cleans up what the student typed (it adds the dash if they skipped it), checks the code exists and that the student hasn't joined already, then enrolls them. `studentsWithoutGroup` feeds the "add members" lists.

### `GroupService.java`

This handles groups and who may do what.

| Method | Rule |
|---|---|
| `createGroup` | Only the class's professor. The name must be unique in the class, and the leader can't already be in a group. Saves the group and its leader together in a transaction |
| `requireMember`, `requireLeader`, `requireProfessor`, `requireLeaderOrProfessor` | The permission checks every other service calls |
| `addMembers` | Leader or professor adds students who have no group yet |
| `setLeader`, `removeMember` | Professor only. Can't remove someone who already has work in the group |
| `rename`, `setNameLocked` | The leader can rename unless the professor locked the name. A professor rename locks it |
| `progress` | Counts tasks by status with one SQL query |

### `TaskService.java`

This handles tasks. `create` is leader only, and checks the title, that the deadline isn't in the past, and that the assignee is in the group. New tasks start as PENDING. `updateStatus` lets **only the assigned member** change a status. `isOverdue` means the deadline has passed and the task isn't completed.

```java
Member leader = GroupService.requireLeader(groupId, leaderUserId);   // only the leader may do this
if (name.isEmpty()) {
    throw new AppException("Enter a task title.");
}
if (deadline.isBefore(LocalDate.now())) {
    throw new AppException("The deadline cannot be in the past.");
}
```

### `CollabService.java`

This covers files, calendar, messages, and links. `uploadFile` checks the type and that the file is 25 MB or less, then saves its details plus 128 KB pieces (`storeChunks`), and `copyOut` joins the pieces back together. `addEvent` checks that the end time is after the start. `sendMessage` and `messagesAfter` power the chat. `addDocument` only accepts full http or https links. `calendarMonth` mixes events and task deadlines for the calendar.

### `BudgetService.java`

This is the ambagan, and everything in it is leader only. Here is the core of the split:

```java
int evenCount = members.size() - custom.size();     // people without a custom share
BigDecimal rest = total.subtract(customSum);        // what's left after custom shares
even = rest.divide(BigDecimal.valueOf(evenCount), 0, RoundingMode.CEILING);  // round UP to the peso
```

For example, a ₱1,000 total with 4 members where Mika agrees to pay ₱400: the other 3 split ₱600, so each pays ₱200. `recordPayment` allows partial payments but never more than what's owed, and `undoLastPayment` removes a mistake. Money uses `BigDecimal` instead of `double`, so there are no rounding errors like 0.1 + 0.2 = 0.30000000000000004.

---

## 5. Screen layer: what you see

| File | What it shows |
|---|---|
| `Theme.java` | The colors from Figma (`PINE #1E6B52`, `AMBER #E3A23B`, and the rest) and the fonts. Its CSS is written as a Java string and loaded into the window |
| `Ui.java` | Building blocks every screen reuses: text, buttons (`primary`, `secondary`, `link`), cards, icons, avatars, status pills, progress bars, the Kinnect logo, and peso and date formats. Because of this, screens stay short and all look the same |
| `AuthScreens.java` | Welcome, log in, sign up, join a class. Each has the form on the left and the green brand panel on the right |
| `ProfessorScreens.java` | "My classes", plus each class's workspace with its groups (each with a **Manage** link) and the students list |
| `StudentScreens.java` | The student's home: each joined class, their group in it, and their tasks |
| `GroupScreen.java` | The group window, with the sidebar on the left and the chosen page on the right. **The sidebar depends on your role:** the leader sees Tasks and Budget, a member sees My tasks, and the professor can view everything |
| `GroupPages.java` | Every page inside a group. The Messages page refreshes on its own every 4 seconds with a JavaFX `Timeline`, which calls `messagesAfter` to fetch only new messages. That's how chat feels live without a chat server |
| `Dialogs.java` | Every pop-up form. They all share one rule: **the form stays open and shows the problem in red if a rule fails, and closes only when the save works** |

### How a screen is built in pure Java

There are no FXML files. Each screen is a Java method that creates its parts and returns them. For example, the "can't reach the database" screen in `KinnectApp.java`:

```java
Button retry = Ui.primary("Try again");
retry.setOnAction(e -> connect());
Label error = Ui.wrap(detail, "error-text");
VBox box = Ui.card(14, Ui.logo(24), Ui.text("Kinnect can't reach the database", "h2"), hint, settings, error, retry);
StackPane page = new StackPane(box);
```

In FXML you would describe those same parts in a separate `.fxml` file and connect them to a controller class.

---

## 6. How one click travels through all the layers

Example: the leader creates a task.

```
SCREEN    Dialogs.createTask        you fill in the form and click "Create task"
   |
SERVICE   TaskService.create        is this the leader? title filled? deadline not past?
   |         (rule broken -> AppException -> red message, form stays open)
DATA      Db.insert                 INSERT INTO tasks ... VALUES (?, ?, ...)
   |
MySQL     Aiven                     the row is saved; every groupmate sees it
```

**Step 1. Screen.** The leader clicks Create task on the Tasks page (`GroupPages.java`). That opens the form in `Dialogs.createTask`, which is built from a `TextField`, a `DatePicker`, and a `ComboBox` of members.

**Step 2. Service.** Clicking the form's button calls `TaskService.create(...)`, which checks the rules before saving anything.

**Step 3. Data.** If every rule passes, it saves through `Db`:

```java
long id = Db.insert("""
        INSERT INTO tasks (group_id, title, description, deadline, status, assignee_id, creator_id)
        VALUES (?, ?, ?, ?, 'PENDING', ?, ?)""", ...);
```

**Step 4. Back on screen.** The form closes, a note says "Task created and assigned to ...", and the page refreshes.

Every feature follows this same path, so once you understand one, you understand them all.

---

## 7. Where uploaded files and links go

Files are saved in the online database, not in a folder on someone's laptop. Since every copy of Kinnect connects to that same database, groupmates see a file as soon as their Files page refreshes.

```
 YOUR LAPTOP                          AIVEN (online MySQL)                 GROUPMATE'S LAPTOP
 Files page, Upload                   group_files table                    Files page
 report.pdf (300 KB)  ------------>   file_id 7 | report.pdf | pdf |  ---> sees "report.pdf"
                                                  300 KB | Ariel | 10:48     in the list
                                      file_chunks table
                                      file 7, piece 0 | 128 KB  --+
                                      file 7, piece 1 | 128 KB    +-----> clicks Open: Kinnect joins
                                      file 7, piece 2 |  44 KB  --+       the pieces back into
                                                                          report.pdf on their laptop
```

**Uploading (`CollabService.uploadFile`).** Kinnect checks the file type (PDF, Word, PowerPoint, Excel, image, text, or ZIP) and that it's no bigger than 25 MB. Then, in one transaction, it saves a row in **`group_files`** (name, type, size, uploader, group, time) and the content in **`file_chunks`**, cut into 128 KB pieces:

```java
Db.transaction(() -> {
    id[0] = Db.insert("""
            INSERT INTO group_files (group_id, file_name, storage_path, file_type, file_size, uploader_id)
            VALUES (?, ?, ?, ?, ?, ?)""", groupId, name, IN_DATABASE, type, size, me.id());
    storeChunks(id[0], bytes);    // INSERT INTO file_chunks, one row per 128 KB piece
});
```

**Opening (`CollabService.copyOut`).** Kinnect reads the pieces back in order and joins them into the original file on that person's computer.

**Why pieces?** Databases limit how much data one command can send at a time; XAMPP's limit is only 1 MB. Small 128 KB pieces always fit, whether the file is 50 KB or 25 MB.

**Links (`CollabService.addDocument`).** A link is just text, so it's one row in `shared_documents`:

```java
long id = Db.insert("""
        INSERT INTO shared_documents (group_id, document_title, document_url, contributor_id)
        VALUES (?, ?, ?, ?)""", groupId, name, url, me.id());
```

### How Kinnect knows to save to Aiven

No single file decides "this goes to Aiven." `CollabService` decides **what** to save and **which table** it goes into. `Db` decides **where** the database is, using `kinnect.properties`. Every save goes through the same connection.

```
kinnect.properties          db.url=jdbc:mysql://...aivencloud.com:28675/kinnect...
        |
        v  read once when Kinnect starts
Db.configure()              stores the url, user, password
        |
        v
Db.connect()                DriverManager.getConnection(url, user, password)  -->  opens the line to Aiven
        ^
        |  every save uses this one connection
        |
Db.insert(sql, values)      sends the SQL through that line
        ^
        |
CollabService.uploadFile()     INSERT INTO group_files + file_chunks       (a file)
CollabService.addDocument()    INSERT INTO shared_documents                (a link)
        ^
        |
GroupPages.java / Dialogs.java      the Upload button and the Add link form
```

Change the `db.url` line to `localhost` and the exact same code saves to XAMPP instead. None of the Java changes.

---

## 8. What `pom.xml` is

`pom.xml` is the only XML file in the project, and it is not part of the program. It is the settings file for **Maven**, the build tool NetBeans uses. It lists the Java version, the libraries to download (JavaFX and the MySQL driver), and how to build the shareable `Kinnect.exe`.

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.1.0</version>
</dependency>
```

| | `.java` files | `pom.xml` |
|---|---|---|
| Language | Java | XML |
| Who reads it | The Java compiler, which turns it into the running app | Maven (inside NetBeans), when you click Build |
| What it holds | All screens, logic, and database code | Java version, libraries, and the build settings |
| Inside the running app? | Yes | No, it's only used while building |

Every Maven project in NetBeans has a `pom.xml`, even a simple Hello World.

---

## 9. Why Aiven and not Supabase

Supabase uses PostgreSQL, while Kinnect is built on MySQL (the same database family as XAMPP). Aiven offers a free plan running real MySQL 8, so Kinnect went online with no Java changes; only `kinnect.properties` changed. Moving to Supabase would mean swapping the driver and rewriting all 17 CREATE TABLE statements, because PostgreSQL uses different wording:

```sql
-- MySQL (what Kinnect uses)
task_id BIGINT AUTO_INCREMENT PRIMARY KEY
data MEDIUMBLOB NOT NULL

-- PostgreSQL would need
task_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
data BYTEA NOT NULL
```

With Aiven, the same code also still runs offline on XAMPP.

---

## 10. Questions a professor might ask

| Question | Answer |
|---|---|
| Where is the main method? | `Mavenproject2.java`. It only starts `KinnectApp`. |
| Did you use FXML? | No. Each screen is a Java method that builds the layout in code with JavaFX. |
| Where are the rules checked? | In the Service files, before anything is saved. |
| How do you stop a member from creating tasks? | Members don't see the button, and `TaskService` also calls `GroupService.requireLeader`, so the rule holds even if the screen is bypassed. |
| How are passwords stored? | `Security.java` hashes them with PBKDF2 and a random salt. The plain password is never saved. |
| Where is the database connection? | `Db.java`. It reads `kinnect.properties` and uses JDBC with `?` prepared statements, which prevents SQL injection. |
| Where do the tables come from? | `Db.java` holds the CREATE TABLE statements and runs them on the first start. |
| Where are uploaded files kept? | Inside the database, in 128 KB pieces (the `file_chunks` table), so every computer sees the same files. |
| How does chat update without refreshing? | The Messages page checks for new messages every 4 seconds with a JavaFX `Timeline`. |
| How is the ambagan split? | Custom shares first, then the rest is split evenly and rounded up to the next peso, using `BigDecimal` for exact money math. |
| What is `pom.xml`? | Maven's build file. It lists the libraries and how to build the app; it isn't the program. |

---

## 11. A speaking order for explaining the code

**Step 1.** Say in one sentence what Kinnect is: group project coordination with three roles (professor, leader, member).

**Step 2.** Show the four-layer picture from section 1.

**Step 3.** Walk "create a task" through the layers in NetBeans (section 6), opening each file as you talk.

**Step 4.** Show `Models.java` next to the class diagram in `docs/diagrams`, and point out that they match.

**Step 5.** Finish with security: role checks in the services, and hashed passwords.
