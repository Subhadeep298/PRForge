# PRForge

A full-stack application for streamlining Pull Requests, featuring GitHub OAuth integration and Jira ticket visualization.

## Prerequisites
- **Java 17** or higher
- **Node.js** (v18+ recommended) & **npm**
- **Docker** & **Docker Compose** (for the database)

## Getting Started

Follow these instructions to run the project locally.

### 1. Database Setup
Start the PostgreSQL database using Docker Compose:

```bash
docker-compose up -d
```
This will start a Postgres container on port `5432` with the credentials defined in `docker-compose.yml`.

### 2. Backend Setup (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd be
   ```

2. Configure Environment Variables:
   Create a `.env` file in the `be` directory.

   > **Tip**: If you received a `.env` file from the project maintainer, simply place it in the `be/` directory.

   If you are setting this up from scratch, create the file with the following content:
   ```env
   GITHUB_CLIENT_ID=your_github_client_id
   GITHUB_CLIENT_SECRET=your_github_client_secret

3. Run the Backend:
   ```bash
   # On Windows
   ./mvnw spring-boot:run

   # On macOS/Linux
   ./mvnw spring-boot:run
   ```
   The backend will start on `http://localhost:8080`.

### 3. Frontend Setup (React + Vite)
1. Navigate to the frontend directory:
   ```bash
   cd ../fe
   ```

2. Install Dependencies:
   ```bash
   npm install
   ```

3. Start the Development Server:
   ```bash
   npm run dev
   ```
   The frontend will typically start on `http://localhost:5173`.

## Usage
1. Open your browser and go to the frontend URL (e.g., `http://localhost:5173`).
2. Click "Get Started" to log in with GitHub.
