# HRMS — Human Resource Management System

A full-featured HRMS built with Spring Boot 3, PostgreSQL, and React 19.

## 🚀 Running with Docker (Recommended)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- That's it — no Java, Node, or PostgreSQL needed on your machine

### Setup

**1. Clone the repository**
```bash
git clone <repo-url>
cd HRMS
```

**2. Create your environment file**
```bash
cp .env.example .env
```

**3. Generate a JWT secret and paste it in `.env`**
```bash
# Option A — if you have OpenSSL:
openssl rand -base64 64

# Option B — if you have Node.js:
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```
Open `.env` and set `JWT_SECRET=<your generated value>`.

**4. Start everything**
```bash
docker compose up --build
```

> First build takes ~3–5 minutes (downloads Java 21, Node 22, builds both apps).  
> Subsequent starts take ~30 seconds.

**5. Open the app**

| Service | URL |
|---|---|
| 🌐 HRMS App | http://localhost |
| 🔧 API (direct) | http://localhost:8085/api/v1 |
| 📖 Swagger UI | http://localhost:8085/swagger-ui |

**Default login credentials:**
| Role | Username | Password | Notes |
|---|---|---|---|
| 👑 Admin | `admin` | `Admin@123` | Full system access & role provisioning |
| 💼 HR | `hr` | `Hr@123` | Manage employees, org structure & leaves |
| 👔 Manager | `manager` | `Manager@123` | Team approvals (leave & regularizations) |
| 👤 Employee | `employee` | `Employee@123` | Self-service punch in/out & leave requests |

---

### Useful Commands

```bash
# Start (background)
docker compose up -d --build

# Stop
docker compose down

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Start with pgAdmin (DB inspection tool at http://localhost:5050)
docker compose --profile tools up -d

# Completely reset (⚠️ deletes all data)
docker compose down -v
```

---

## 🛠️ Local Development (without Docker)

### Backend
```bash
cd backend
# Requires: Java 21, PostgreSQL running locally
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs at http://localhost:5173

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Spring Security, JPA/Hibernate |
| Database | PostgreSQL 16 + Flyway migrations |
| Frontend | React 19, Ant Design, Vite, TanStack Query |
| Auth | JWT (15 min access token) + httpOnly cookie refresh token |
| Packaging | Docker + Nginx |
