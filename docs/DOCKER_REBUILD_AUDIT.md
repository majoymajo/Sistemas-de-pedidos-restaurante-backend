# Docker Rebuild Audit — Why Every Change Requires a Full Rebuild

**Generated:** February 24, 2026  
**System:** Restaurant Ordering System  
**Scope:** All Dockerfiles, docker-compose files, build scripts, and related configuration

---

## Executive Summary

The project requires a full Docker rebuild on every code change because **none of the services use source-code volume mounts** in development, and all Dockerfiles bake source code at build time with no hot-reload support. This affects all 4 application services (frontend, order-service, kitchen-worker, report-service).

**Estimated time per rebuild cycle:** 2–5 minutes (backend), 30–60 seconds (frontend)

---

## Inventory of Docker Files

| File | Purpose | Multi-Stage | Hot-Reload |
|---|---|:---:|:---:|
| `Dockerfile` | Frontend production build (`vite preview`) | Yes (3 stages) | No |
| `Dockerfile.frontend` | Frontend dev build (`vite dev`) | No | Broken (no volume) |
| `order-service/Dockerfile` | Order service (Maven build → JRE) | Yes (2 stages) | No |
| `kitchen-worker/Dockerfile` | Kitchen worker (Maven build → JRE) | Yes (2 stages) | No |
| `report-service/Dockerfile` | Report service (Maven build → JRE) | Yes (2 stages) | No |
| `infrastructure/docker/docker-compose.yml` | Full-stack orchestration | N/A | No volumes |
| `infrastructure/docker/docker-compose.frontend.yml` | Frontend standalone | N/A | No volumes |
| `.dockerignore` | Build context exclusions | N/A | N/A |

---

## Root Causes

### Cause 1 — No source-code volume mounts (ALL services)

**Impact:** CRITICAL — This is the #1 reason for mandatory rebuilds.

`docker-compose.yml` defines **zero volume mounts** for source code. Every service copies files at build time via `COPY`. The only way to pick up changes is:

```
docker-compose build <service> && docker-compose up <service>
```

**Evidence:** In `infrastructure/docker/docker-compose.yml`, no application service has a `volumes:` section mounting host source code.

---

### Cause 2 — Backend Dockerfiles compile inside the container with no dev mode

**Impact:** HIGH — Each backend change triggers the full cycle: send context → download deps → compile → package → build image → restart container.

All three Java Dockerfiles (`order-service`, `kitchen-worker`, `report-service`) run `mvn package` inside the container, producing a fat JAR. There is no alternative `docker-compose` profile that runs `mvn spring-boot:run` with source mounts instead.

No `spring-boot-devtools` dependency exists in any service `pom.xml`, so even with volume mounts, automatic restart would not work.

---

### Cause 3 — No Maven dependency caching layer

**Impact:** HIGH — A single `.java` change re-downloads/re-resolves ALL Maven dependencies.

The `order-service/Dockerfile` and `kitchen-worker/Dockerfile` copy POM files first (good), but then run `mvn package` in a single `RUN` command that downloads dependencies AND compiles source together. There is **no `mvn dependency:go-offline`** step to create a separate cached layer.

**Current pattern:**
```dockerfile
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src       # ← source + deps in same layer
RUN mvn -pl order-service -am package -Dmaven.test.skip=true
```

**Better pattern:**
```dockerfile
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -pl order-service -am dependency:go-offline -B  # ← cached layer
COPY order-service/src order-service/src                 # ← only this changes
RUN mvn -pl order-service -am package -Dmaven.test.skip=true -o
```

---

### Cause 4 — report-service copies the ENTIRE monorepo

**Impact:** HIGH — Changing ANY file in ANY service invalidates report-service's Docker cache.

**Current `report-service/Dockerfile`:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY . .                    # ← copies EVERYTHING: frontend, docs, other services
RUN mvn clean package -DskipTests -pl report-service -am
```

This means editing a frontend `.tsx` file, a documentation `.md` file, or any file in `order-service/` triggers a full report-service rebuild.

---

### Cause 5 — Frontend dev container has HMR but no bind mount

**Impact:** HIGH — `Dockerfile.frontend` runs Vite dev server with HMR, but it's useless.

```dockerfile
# Dockerfile.frontend
COPY . .                    # ← static copy, baked at build time
CMD ["npm", "run", "dev"]   # ← Vite starts but watches unchanging files
```

The `docker-compose.yml` does NOT mount the `src/` directory as a volume into the frontend container. Vite's HMR watches the baked copy, which never changes at runtime.

---

### Cause 6 — No dev vs production compose profile

**Impact:** MEDIUM — There's no way to switch between fast dev iteration and production-like builds.

Only one `docker-compose.yml` exists. It always uses `build:` directives for all services. There is no `docker-compose.dev.yml` override with volume mounts for development, nor a `docker-compose.prod.yml` using pre-built images.

---

### Cause 7 — Large build context sent every time

**Impact:** MEDIUM — The `.dockerignore` is incomplete.

All services use `context: ../..` (project root) as build context. The `.dockerignore` excludes `node_modules`, `target`, `.git`, etc. but does NOT exclude:

- `docs/` (~100+ KB of markdown)
- `openspec/` (spec files)
- `scripts/` 
- `infrastructure/`
- `*.md` files in root

These are sent to the Docker daemon on every build, slowing context transfer.

---

### Cause 8 — Root Dockerfile reinstalls all deps in runner stage

**Impact:** LOW — Wastes time and image size.

```dockerfile
FROM node:20-alpine AS runner
# ...
RUN npm i --omit=dev=false     # ← reinstalls ALL deps including devDeps
CMD ["npm", "run", "preview"]
```

The runner stage reinstalls all dependencies (including devDependencies) just to run `vite preview`. This defeats the purpose of the multi-stage build.

---

### Cause 9 — No Vite polling configuration for Docker

**Impact:** LOW (only relevant once volume mounts are added)

On Windows/macOS with Docker bind mounts, filesystem events (inotify) don't propagate into the container. Vite needs `usePolling: true` in its `server.watch` config to detect file changes.

Currently missing from `vite.config.ts`.

---

## Solutions Overview

### Solution A — Dev Compose Override (Fastest path to hot-reload)

Create `infrastructure/docker/docker-compose.dev.yml` with volume mounts for all services:

- **Frontend:** Mount `src/`, `index.html`, `public/` → Vite HMR works instantly
- **Backend services:** Mount full project + Maven cache volume → `spring-boot:run` with DevTools
- **Usage:** `docker-compose -f docker-compose.yml -f docker-compose.dev.yml up`

**Pros:** Fastest iteration, minimal changes, keeps production Dockerfiles untouched  
**Cons:** Requires `spring-boot-devtools` dependency added to each service

**Estimated effort:** 2–4 hours  
**Rebuild time after:** Frontend: 0s (HMR), Backend: 5–15s (auto-restart)

---

### Solution B — Fix Docker Layer Caching (Faster production builds)

Optimize all Dockerfiles to separate dependency resolution from source compilation:

1. Add `mvn dependency:go-offline` as a separate step in all Java Dockerfiles
2. Fix `report-service/Dockerfile` to copy only its own source (not entire monorepo)
3. Expand `.dockerignore` to exclude docs, scripts, infrastructure, etc.
4. Fix root `Dockerfile` runner stage to use nginx or minimal deps

**Pros:** Production builds go from ~5 min to ~30s on cache hit  
**Cons:** Still requires `docker-compose build` (no hot-reload)

**Estimated effort:** 1–2 hours  
**Rebuild time after:** 20–40s (cached) vs 2–5 min (current)

---

### Solution C — Hybrid (Recommended)

Combine Solution A + Solution B:

1. Create `docker-compose.dev.yml` for daily development (hot-reload everything)
2. Fix Dockerfiles for faster CI/CD and production builds (layer caching)
3. Add `spring-boot-devtools` to all Java services
4. Add Vite polling config for Windows Docker compatibility
5. Expand `.dockerignore`

**Pros:** Best of both worlds — instant dev, fast prod builds  
**Cons:** Most work upfront

**Estimated effort:** 4–6 hours  
**Rebuild time after:** Dev: 0–15s, Prod: 20–40s

---

## Detailed Implementation — Solution C (Hybrid)

### Step 1: Create `docker-compose.dev.yml`

```yaml
# infrastructure/docker/docker-compose.dev.yml
# Usage: docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
services:
  frontend:
    build:
      context: ../..
      dockerfile: Dockerfile.frontend
    volumes:
      - ../../src:/app/src
      - ../../index.html:/app/index.html
      - ../../public:/app/public
    environment:
      - CHOKIDAR_USEPOLLING=true

  order-service:
    build: !reset null
    image: eclipse-temurin:17-jdk
    working_dir: /app
    volumes:
      - ../..:/app
      - maven-cache:/root/.m2
    command: >
      ./mvnw spring-boot:run
      -pl order-service
      -Dspring-boot.run.profiles=dev
      -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "8080:8080"
      - "5005:5005"

  kitchen-worker:
    build: !reset null
    image: eclipse-temurin:17-jdk
    working_dir: /app
    volumes:
      - ../..:/app
      - maven-cache:/root/.m2
    command: >
      ./mvnw spring-boot:run
      -pl kitchen-worker
      -Dspring-boot.run.profiles=dev

  report-service:
    build: !reset null
    image: eclipse-temurin:17-jdk
    working_dir: /app
    volumes:
      - ../..:/app
      - maven-cache:/root/.m2
    command: >
      ./mvnw spring-boot:run
      -pl report-service
      -Dspring-boot.run.profiles=dev
    ports:
      - "8082:8082"

volumes:
  maven-cache:
```

### Step 2: Fix `report-service/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml

RUN mvn -pl report-service -am dependency:go-offline -B

COPY report-service/src report-service/src

RUN mvn -pl report-service -am package -DskipTests -o

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/report-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 3: Add dependency caching to `order-service/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml

RUN mvn -pl order-service -am dependency:go-offline -B

COPY order-service/src order-service/src

RUN mvn -pl order-service -am package -Dmaven.test.skip=true -o

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/order-service/target/order-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Step 4: Same pattern for `kitchen-worker/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml

RUN mvn -pl kitchen-worker -am dependency:go-offline -B

COPY kitchen-worker/src kitchen-worker/src

RUN mvn -pl kitchen-worker -am package -Dmaven.test.skip=true -o

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/kitchen-worker/target/kitchen-worker-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Step 5: Add `spring-boot-devtools` to each service `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
    <scope>runtime</scope>
</dependency>
```

### Step 6: Add Vite polling to `vite.config.ts`

```typescript
server: {
  host: '0.0.0.0',
  port: 5173,
  watch: {
    usePolling: true,
    interval: 1000,
  },
},
```

### Step 7: Expand `.dockerignore`

```
.git
.gitignore
node_modules
dist
coverage
**/target
**/.idea
**/.vscode
**/.DS_Store
**/tsconfig*.tsbuildinfo
docs/
openspec/
scripts/
infrastructure/
*.md
.github/
```

### Step 8: Fix root `Dockerfile` runner stage

Replace the current runner with nginx for production:

```dockerfile
FROM nginx:alpine AS runner
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
```

---

## Summary Matrix

| Root Cause | Severity | Solution | Effort |
|---|:---:|---|:---:|
| No source volume mounts | CRITICAL | docker-compose.dev.yml | 1h |
| No Maven dependency caching | HIGH | dependency:go-offline step | 1h |
| report-service copies entire repo | HIGH | Fix Dockerfile to copy only its source | 30min |
| Frontend HMR broken (no volume) | HIGH | Volume mounts + Vite polling | 30min |
| No dev/prod profile split | MEDIUM | docker-compose.dev.yml override | 30min |
| No spring-boot-devtools | MEDIUM | Add dependency to each pom.xml | 30min |
| Incomplete .dockerignore | MEDIUM | Expand exclusions | 15min |
| Runner stage reinstalls deps | LOW | Use nginx for production | 30min |
| No Vite polling config | LOW | Add usePolling to vite.config.ts | 15min |

---

**Recommendation:** Go with **Solution C (Hybrid)** for maximum impact. Total estimated effort: 4–6 hours. After implementation, dev rebuild time drops from 2–5 minutes to **0–15 seconds**.
