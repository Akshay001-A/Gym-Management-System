\# 🏋️ Gym Management System



!\[Java](https://img.shields.io/badge/Java-17-blue)

!\[MySQL](https://img.shields.io/badge/MySQL-Database-orange)

!\[Swing](https://img.shields.io/badge/Java-Swing-green)

!\[License](https://img.shields.io/badge/License-MIT-green)



A \*\*Java Swing desktop application\*\* designed to manage gym members, payments, schedules, and notifications using a \*\*MySQL database\*\*.



This project was developed as part of a \*\*Database Management System (DBMS) academic project\*\*.



---



\# 📌 Overview



The \*\*Gym Management System\*\* automates daily gym management tasks and provides an easy-to-use graphical interface for administrators and members.



Main functionalities include:



\* Member login system

\* Admin dashboard

\* Member management

\* Payment tracking

\* Schedule management

\* Notification system



The application uses \*\*Java Swing for GUI\*\* and \*\*MySQL with JDBC for database connectivity\*\*.



---



\# ⚙️ Technologies Used



| Technology   | Purpose                   |

| ------------ | ------------------------- |

| Java         | Core programming language |

| Java Swing   | Desktop GUI               |

| MySQL        | Database                  |

| JDBC         | Database connectivity     |

| Eclipse IDE  | Development               |

| Git \& GitHub | Version control           |



---



\# 📂 Project Structure



```text

Gym\_management\_System

│

├── src

│   └── gymmanagement

│       ├── model        # Data models

│       ├── ui           # User interface pages

│       ├── util         # Database connection utilities

│       └── main.java    # Application entry point

│

├── assets               # Images and icons used in UI

│

├── database.sql         # Database creation script

│

├── README.md

└── LICENSE

```



---



\# ✨ Features



\## 👤 Member Features



\* Member login

\* View member dashboard

\* View notifications

\* View gym schedule

\* View payment details



\## 🛠 Admin Features



\* Add new members

\* Update member details

\* Delete members

\* List all members

\* Manage member payments



---



\# 🗄 Database Setup



Start the MySQL server and open MySQL shell.



\### Connect to MySQL



```

\\sql

\\connect root@localhost

```



\### Show databases



```

SHOW DATABASES;

```



\### Select project database



```

USE gym\_management;

```



\### Show tables



```

SHOW TABLES;

```



\### Example query



```

SELECT \* FROM members;

```



---



\# ▶️ How to Run the Project



1\. Clone the repository



```

git clone https://github.com/Akshay001-A/Gym-Management-System.git

```



2\. Open the project in \*\*Eclipse IDE\*\*



3\. Start the \*\*MySQL server\*\*



4\. Update database credentials in



```

src/gymmanagement/util/dbconnection.java

```



5\. Run the application



```

main.java

```



---



\# 👨‍💻 Developers



\* \*\*Akshay R\*\*

\* \*\*Charan Kumar R\*\*



---



\# 📄 License



This project is licensed under the \*\*MIT License\*\*.



You are free to use, modify, and distribute this software for educational or personal purposes.



See the full license details in the \*\*LICENSE\*\* file included in this repository.



© 2026 Akshay R and Charan Kumar R



