# Quick Start Script for Windows
docker compose up -d

cd be
.\mvnw.cmd spring-boot:run

cd fe
npm run dev
```
