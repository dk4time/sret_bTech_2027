# Task Manager API (Node.js + Express + MongoDB)

A production-style REST API for managing **Users, Projects, and Tasks** with **assignment workflows, pagination, search, and analytics**.

---

## Features

- **Users**
  - Create user
  - List users

- **Projects**
  - Create project with owner & members
  - List / get project (with populate)
  - Update / delete

- **Tasks**
  - Full CRUD
  - **Assignment & Unassignment APIs**
  - **Pagination & Search**
  - **Advanced Filters**
  - **Stats (Aggregation)**
  - **Due-date queries (overdue, due soon, by date)**

- **Relationships**
  - Task → Project
  - Task → User (assignedTo)
  - Project → User (owner, members)

---

## 🧱 Tech Stack

- Node.js
- Express.js
- MongoDB + Mongoose
- Postman (API testing)

---

## Project Structure

```
project-root/
│
├── models/
│   ├── User.js
│   ├── Project.js
│   └── Task.js
│
├── routes/
│   ├── userRoutes.js
│   ├── projectRoutes.js
│   └── taskRoutes.js
│
├── server.js
└── README.md
```

---

## Setup Instructions

1. Clone repo
2. Install dependencies

   ```bash
   npm install
   ```

3. Add `.env`

   ```env
   MONGO_URI=your_mongodb_uri
   PORT=3000
   ```

4. Run server

   ```bash
   npm run dev
   ```

---

## 🔗 Base URL

```
http://localhost:5050/api

```

---

# USER APIs

### Create User

```
POST /users
```

```json
{
  "name": "Dinesh",
  "email": "dinesh@test.com"
}
```

---

### Get Users

```
GET /users
```

---

# PROJECT APIs

### Create Project

```
POST /projects
```

```json
{
  "name": "API Project",
  "owner": "USER_ID",
  "members": ["USER_ID"]
}
```

---

### Get Projects

```
GET /projects
```

---

### Get Project by ID

```
GET /projects/:id
```

---

### Update Project

```
PATCH /projects/:id
```

---

### Delete Project

```
DELETE /projects/:id
```

---

# TASK APIs

---

## Create Task

```
POST /tasks
```

```json
{
  "title": "Fix bug",
  "project": "PROJECT_ID",
  "assignedTo": "USER_ID"
}
```

---

## Get Tasks (Pagination + Filters)

```
GET /tasks?page=1&limit=10&status=todo&search=api
```

### Query Params

| Param     | Description               |
| --------- | ------------------------- |
| page      | Page number               |
| limit     | Items per page            |
| status    | todo / in-progress / done |
| userId    | Filter by user            |
| projectId | Filter by project         |
| overdue   | true                      |
| search    | text search               |

---

## Response Format

```json
{
  "total": 20,
  "page": 1,
  "limit": 10,
  "totalPages": 2,
  "data": []
}
```

---

## Get Task by ID

```
GET /tasks/:id
```

---

## Update Task (PATCH)

```
PATCH /tasks/:id
```

```json
{
  "status": "done"
}
```

---

## Replace Task (PUT)

```
PUT /tasks/:id
```

```json
{
  "title": "Replaced Task"
}
```

> ⚠️ This replaces the entire document.

---

## Delete Task

```
DELETE /tasks/:id
```

---

# ASSIGNMENT APIs

---

## Assign Task

```
PATCH /tasks/:id/assign
```

```json
{
  "userId": "USER_ID"
}
```

### Validation

- User must be **project owner or member**

---

## Unassign Task

```
PATCH /tasks/:id/unassign
```

---

# ANALYTICS APIs

---

## Task Stats

```
GET /tasks/stats
```

Response:

```json
[
  { "_id": "todo", "count": 5 },
  { "_id": "done", "count": 3 }
]
```

---

## Stats Per User

```
GET /tasks/stats/user/:userId
```

---

## Recent Tasks

```
GET /tasks/recent
```

---

## Tasks Due Soon

```
GET /tasks/due-soon
```

---

## Tasks Due on Date

```
GET /tasks/due/:date
```

---

## Overdue Tasks

```
GET /tasks/overdue/list
```

---

# Design Decisions

### Why relationships are stored in Project (not User)

- Avoid duplication
- Single source of truth
- Easier consistency

---

### Why PATCH is preferred over PUT

- Prevents accidental data loss
- Safer updates

---

### Why assignment is a separate API

- Business logic validation
- Controlled workflow

---

### Why pagination is needed

- Scales with large data
- Improves performance

---

# Important Notes

- Route order matters (`/:id` must be last)
- Always validate ObjectId
- Use indexes for performance
- Avoid duplicate indexes

---

# Future Improvements

- Authentication (JWT)
- Role-based access (admin/member)
- Activity logs
- Notifications
- File attachments

---

# Author

Dineshkumar

---

# Summary

This project demonstrates:

- REST API design
- Data modeling
- Relationships
- Query optimization
- Real-world backend patterns

---
