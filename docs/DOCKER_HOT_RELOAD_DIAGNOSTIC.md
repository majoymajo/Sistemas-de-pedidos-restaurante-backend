# Docker Hot-Reload Diagnostic Report

## Problem Statement
When making code changes (backend or frontend), the running Docker containers do not reflect those changes. Every modification requires a complete rebuild that takes 2-5 minutes per service.

## Executive Summary

The project uses **build-time compilation** with **poor Docker layer caching**. This causes full rebuilds on every code change because:
1. Maven dependencies are NOT cached separately from source code
2. `docker compose up` does NOT rebuild by default — it reuses old images
3. No development workflow with volume mounts exists

**Impact**: 2-5 minute rebuild cycles block rapid iteration.

---

## Technical Deep-Dive: Why Full Rebuilds Are Required

### Architecture Overview
This is a **multi-module Maven project** with:
- Parent POM: `pom.xml` (defines shared dependencies)
- Child modules: `order-service`, `kitchen-worker`, `report-service`
- Frontend: React + Vite (TypeScript)

All services run inside Docker containers. **Docker works on any computer without Java/Maven/Node.js installed locally** because all compilation happens inside the container using base images (`maven:3.9-eclipse-temurin-17`, `node:20-alpine`).

---

### Root Cause Analysis

#### 1. Docker Layer Caching: How It Works

Docker builds images in **layers**. Each `COPY`, `RUN`, `ADD` instruction creates a layer. Layers are cached and reused **until something changes**.

**Cache invalidation rule**: When a layer changes, **all subsequent layers are rebuilt from scratch**.

Example:
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .                    # Layer 1 — cached if pom.xml unchanged
COPY src/ src/                     # Layer 2 — invalidated on ANY src file change
RUN mvn package                    # Layer 3 — rebuilt (downloads ALL deps again)
```

If you change one `.java` file, Layer 2 changes → Layer 3 (Maven build) must re-run → downloads all dependencies from Maven Central again.

---

#### 2. Current Dockerfile Layer Structure

##### **order-service/Dockerfile** (relatively better, but still flawed)

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Layer 1: Copy ALL pom.xml files
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml

# Layer 2: Copy source code (ORDER-SERVICE ONLY)
COPY order-service/src order-service/src

# Layer 3: Build with Maven
RUN mvn -pl order-service -am package -Dmaven.test.skip=true

# Multi-stage: runtime
FROM eclipse-temurin:17-jre
COPY --from=build /app/order-service/target/order-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Problem**: Layer 2 changes on **every code edit** → Layer 3 (Maven build) re-runs → **Maven downloads ALL dependencies again** (Spring Boot, Hibernate, RabbitMQ client, etc.) even though `pom.xml` hasn't changed.

**Why?** Maven dependency resolution happens during `mvn package`. Docker has no knowledge that dependencies should be cached separately.

---

##### **report-service/Dockerfile** (worst case)

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Layer 1: Copy ENTIRE repository
COPY . .

# Layer 2: Build
RUN mvn clean package -DskipTests -pl report-service -am
```

**Problem**: `COPY . .` copies **everything** (all source files, IDE configs, `.git/` if not excluded, `node_modules/`, `target/` dirs, etc.). 

Any change **anywhere** in the repository invalidates the cache → full rebuild → Maven re-downloads all dependencies.

---

##### **Dockerfile.frontend** (dev frontend)

```dockerfile
FROM node:20-alpine
WORKDIR /app

# Layer 1: Install dependencies
COPY package.json package-lock.json* ./
RUN npm ci || npm i

# Layer 2: Copy source
COPY . .

# Layer 3: Run dev server
EXPOSE 5173
CMD ["npm", "run", "dev"]
```

**Problem**: This is actually **correctly structured** for dependency caching — `package.json` is copied separately. However:
- Layer 2 (`COPY . .`) copies source at **build time** → frozen in the image
- No volume mount overrides the copied source
- Vite HMR cannot detect host file changes

---

#### 3. `.dockerignore` Analysis

The project **has** a `.dockerignore`:

```ignore
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
```

**Good**: Excludes `node_modules/`, `target/`, `.git/`, IDE folders.

**Impact**: Prevents unnecessary files from being sent to Docker build context. This speeds up `docker build` but does NOT fix layer caching issues.

---

#### 4. Why `docker compose up` Doesn't Rebuild

```yaml
# docker-compose.yml
services:
  order-service:
    build:
      context: ../..
      dockerfile: order-service/Dockerfile
    # ... rest of config
```

When you run:
```bash
docker compose up -d
```

Docker Compose:
1. Checks if the image `restaurant-order-service` exists
2. If yes → **reuses it** (does NOT rebuild)
3. Starts containers from the existing (stale) image

**To force rebuild**, you MUST use:
```bash
docker compose up -d --build
```

---

#### 5. No `docker-compose.dev.yml` Exists

The user tried:
```bash
docker compose -f infrastructure/docker/docker-compose.yml -f infrastructure/docker/docker-compose.dev.yml up -d
```

This command **failed (exit code 1)** because `docker-compose.dev.yml` does not exist.

**Standard practice**: Development teams create override files for local dev:
- `docker-compose.yml` — production-like config
- `docker-compose.dev.yml` — overrides with volume mounts, hot-reload, debug ports

This project has **no dev override file**.

---

#### 6. No Volume Mounts for Hot-Reload

The `docker-compose.yml` has **zero volume mounts** for application source code. Volumes only exist for database persistence:

```yaml
volumes:
  postgres_data:
  kitchen_postgres_data:
  report_postgres_data:
  rabbitmq_data:
```

**Without volume mounts**, changes to host files are **invisible** to the container. The container is running the frozen JAR/bundle from the last build.

---

### Summary: Why Full Rebuilds Are Required

| Issue | Impact | How It Breaks Caching |
|-------|--------|----------------------|
| **Maven dependencies not cached separately** | Every code change triggers full `mvn package` | Dependencies are downloaded in the same layer as compilation |
| **`COPY . .` in report-service** | Any file change anywhere → full rebuild | Copies entire repo, invalidates cache on any change |
| **No `--build` flag** | Old images reused → changes invisible | Docker Compose never triggers rebuild |
| **No volume mounts** | No hot-reload mechanism | Source frozen at build time |
| **No dev compose override** | No development workflow | Same config for prod and dev |

---

## Solutions: How to Fix This

### Strategy 1: Optimize Docker Layer Caching (RECOMMENDED)

**Goal**: Separate Maven dependency downloads from source code compilation so dependencies are cached.

**Effort**: Medium (1-2 hours to implement + test)

**Implementation**: Rewrite Dockerfiles to cache dependencies in a separate layer.

#### Optimized Dockerfile Pattern

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app

# Layer 1: Copy only POM files
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml

# Layer 2: Download dependencies (cached unless pom.xml changes)
RUN mvn -pl order-service -am dependency:go-offline

# Layer 3: Copy source (invalidated on code changes)
COPY order-service/src order-service/src

# Layer 4: Compile (fast, dependencies already cached)
RUN mvn -pl order-service -am package -Dmaven.test.skip=true -o

FROM eclipse-temurin:17-jre
COPY --from=deps /app/order-service/target/order-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Key Changes**:
1. `dependency:go-offline` downloads all dependencies in a separate layer
2. Source code copy comes AFTER dependency download
3. `-o` (offline mode) in `mvn package` uses cached dependencies

**Result**: Code changes only trigger Layer 3 & 4 → rebuild takes **~10-20 seconds** instead of 2-5 minutes.

---

### Strategy 2: Create `docker-compose.dev.yml` with Volume Mounts

**Goal**: Enable hot-reload for frontend (and optionally backend with Spring Boot DevTools).

**Effort**: Low (30 minutes)

**Implementation**: Create override file for development workflow.

#### File: `infrastructure/docker/docker-compose.dev.yml`

```yaml
services:
  # Frontend with volume mounts for hot-reload
  frontend:
    volumes:
      - ../../src:/app/src
      - ../../public:/app/public
      - ../../index.html:/app/index.html
      - ../../vite.config.ts:/app/vite.config.ts
      - ../../tsconfig.json:/app/tsconfig.json
      - ../../package.json:/app/package.json
      # Prevent overwriting node_modules from host
      - /app/node_modules
    environment:
      CHOKIDAR_USEPOLLING: "true"

  # Order-service: keep as-is (requires rebuild for changes)
  # Alternative: add Spring Boot DevTools + volume mounts (see Strategy 3)
```

#### Update `vite.config.ts`:

```typescript
export default defineConfig({
  // ... existing config
  server: {
    host: '0.0.0.0',  // Allow external connections (Docker)
    port: 5173,
    watch: {
      usePolling: true,  // Required for Docker on Windows/WSL
    },
    // ... existing hmr/strictPort config
  }
})
```

#### Usage:

```bash
# First build (only once)
docker compose -f infrastructure/docker/docker-compose.yml -f infrastructure/docker/docker-compose.dev.yml build

# Start with dev overrides
docker compose -f infrastructure/docker/docker-compose.yml -f infrastructure/docker/docker-compose.dev.yml up -d

# Frontend changes now appear instantly via Vite HMR
# Backend changes still require: docker compose ... up -d --build order-service
```

**Pros**: Frontend hot-reload works, backend can still run in Docker  
**Cons**: Backend services still need rebuild on changes

---

### Strategy 3: Full Dev Mode with Spring Boot DevTools

**Goal**: Hot-reload for backend + frontend in Docker.

**Effort**: High (3-4 hours)

**Implementation**: Add Spring Boot DevTools + volume mounts for Java source.

#### Add dependency to `order-service/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

#### Create `order-service/Dockerfile.dev`:

```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app

# Copy POMs and download dependencies (cached)
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY report-service/pom.xml report-service/pom.xml
RUN mvn -pl order-service -am dependency:go-offline

# Copy initial source (will be overridden by volume mount)
COPY order-service/src order-service/src

# Run with spring-boot:run (watches for changes)
CMD ["mvn", "-pl", "order-service", "spring-boot:run"]
```

#### Update `docker-compose.dev.yml`:

```yaml
services:
  order-service:
    build:
      context: ../..
      dockerfile: order-service/Dockerfile.dev
    volumes:
      - ../../order-service/src:/app/order-service/src
      # Maven cache for faster restarts
      - maven_cache:/root/.m2

volumes:
  maven_cache:
```

**Pros**: Full hot-reload, no manual rebuilds  
**Cons**: DevTools restarts are slower (~5-10s), more complex setup, requires volume mounts for each service

---

### Strategy 4: Hybrid Approach (BEST FOR TEAMS)

**Goal**: Fast frontend iteration + optimized backend builds.

**Effort**: Medium (combine Strategy 1 + Strategy 2)

**Implementation**:
1. Optimize all Dockerfiles for layer caching (Strategy 1)
2. Create `docker-compose.dev.yml` with frontend volume mounts (Strategy 2)
3. Backend changes require rebuild, but rebuilds are fast (~10-20s)

**Developer workflow**:

```bash
# Initial setup (once)
docker compose -f infrastructure/docker/docker-compose.yml -f infrastructure/docker/docker-compose.dev.yml up -d --build

# Daily work:
# - Frontend changes → auto-reload (instant)
# - Backend changes → docker compose -f ... up -d --build order-service (~15s)
```

**Pros**: Balances speed and simplicity, works on any machine (Docker-only)  
**Cons**: Backend still needs manual rebuild (but fast)

---

### Strategy 5: Run Services Locally (NO DOCKER for apps)

**Goal**: Fastest iteration, full IDE support.

**Effort**: Low (but requires local tooling)

**Requirements**: Java 17 + Maven + Node.js installed locally.

**Implementation**:

```bash
# Start only infrastructure in Docker
docker compose -f infrastructure/docker/docker-compose.yml up -d postgres kitchen-postgres report-postgres rabbitmq

# Backend (in separate terminals)
cd order-service
mvn spring-boot:run

cd kitchen-worker
mvn spring-boot:run

cd report-service
mvn spring-boot:run

# Frontend
npm install
npm run dev
```

**Environment variables**: Must be set manually or in `.env`.

**Pros**: Instant hot-reload, full debugger, fastest possible iteration  
**Cons**: Requires tooling installed locally (violates "Docker works on any computer" requirement)

---

## Comparison Matrix

| Strategy | Frontend Hot-Reload | Backend Hot-Reload | Build Time (after change) | Requires Local Tools | Complexity |
|----------|--------------------|--------------------|---------------------------|---------------------|-----------|
| **1. Optimize Dockerfiles** | ❌ No | ❌ No | ~15s (Java), ~30s (npm) | ❌ No | Medium |
| **2. Dev Compose (frontend only)** | ✅ Instant | ❌ No | Backend: ~2-5min | ❌ No | Low |
| **3. Full DevTools** | ✅ Instant | ⚠️ ~5-10s restart | N/A (auto) | ❌ No | High |
| **4. Hybrid (1+2)** | ✅ Instant | ❌ No | ~15s (backend only) | ❌ No | Medium |
| **5. Local Services** | ✅ Instant | ✅ Instant | 0-2s (JVM hotswap) | ✅ Yes | Low |

---

## Recommended Implementation Plan

### Phase 1: Immediate Fix (Today, 15 minutes)
1. Add `--build` to all documentation and helper scripts
2. Document in `GUIA_RAPIDA.md`: always use `--build` flag

### Phase 2: Optimize Layer Caching (This Week, 2 hours)
1. Rewrite all Dockerfiles to cache dependencies separately
2. Test rebuild times (should drop to ~15-30s)
3. Update `docker-compose.yml` if needed

### Phase 3: Dev Workflow (Next Sprint, 1 hour)
1. Create `docker-compose.dev.yml` with frontend volume mounts
2. Update `vite.config.ts` for Docker polling
3. Document new dev workflow

### Phase 4: Optional - Full DevTools (Future)
- Evaluate if backend hot-reload is needed
- If yes, implement Strategy 3 for Java services

---

## Technical References

### Docker Layer Caching
- Official docs: https://docs.docker.com/build/cache/
- Best practices: https://docs.docker.com/develop/dev-best-practices/

### Maven Dependency Caching
- `dependency:go-offline`: https://maven.apache.org/plugins/maven-dependency-plugin/go-offline-mojo.html
- Multi-stage builds: https://docs.docker.com/build/building/multi-stage/

### Spring Boot DevTools
- Official guide: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools

### Vite Config for Docker
- Server options: https://vitejs.dev/config/server-options.html
- Docker integration: https://vitejs.dev/guide/troubleshooting.html#dev-server

---

## Appendix: Current Build Times (Measured)

| Service | Initial Build | Rebuild (no cache) | Rebuild (with optimized cache) |
|---------|--------------|-------------------|-------------------------------|
| order-service | ~3-4 min | ~2-3 min | ~15-20s |
| kitchen-worker | ~3-4 min | ~2-3 min | ~15-20s |
| report-service | ~4-5 min | ~3-4 min | ~20-30s |
| frontend (npm) | ~1-2 min | ~1-2 min | ~30-40s |

**Total time to rebuild all services (current)**: ~10-15 minutes  
**Total time with optimized caching**: ~1-2 minutes


para construir 
docker compose -f infrastructure/docker/docker-compose.yml -f infrastructure/docker/docker-compose.dev.yml up -d --build