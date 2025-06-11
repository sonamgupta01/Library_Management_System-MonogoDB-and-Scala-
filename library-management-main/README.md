# Library Management System

A modern web-based library management system built with Scala, HTTP4s, and MongoDB.

## Features

- Book management (add, list, track availability)
- User management
- Book loan and return functionality
- Modern web UI with Tailwind CSS
- RESTful API endpoints
- MongoDB integration
- Real-time updates

## Prerequisites

- Scala 2.13.12
- SBT (Scala Build Tool)
- MongoDB 4.10.2 or later
- Java 11 or later

## Getting Started

1. Clone the repository:
```bash
git clone https://github.com/yourusername/library-management.git
cd library-management
```

2. Make sure MongoDB is running on localhost:27017

3. Run the application:
```bash
sbt clean compile run
```

4. Open your browser and navigate to http://localhost:8080

## API Endpoints

### Books
- `GET /books` - List all books
- `POST /books` - Add a new book

### Users
- `POST /users` - Add a new user

### Loans
- `POST /loans` - Issue a book
- `POST /returns` - Return a book

## Project Structure

```
library-management/
├── src/
│   └── main/
│       ├── scala/
│       │   └── example/
│       │       └── LibraryManagement.scala
│       └── resources/
│           ├── application.conf
│           └── index.html
├── build.sbt
└── README.md
```

## Technologies Used

- Scala 2.13.12
- HTTP4s 0.23.25
- MongoDB Scala Driver 4.10.2
- Circe 0.14.6
- PureConfig 0.17.5
- Tailwind CSS
- SBT

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

![image](https://github.com/user-attachments/assets/6648ac15-9062-4334-8ef6-5d0bbcb6de0b)
