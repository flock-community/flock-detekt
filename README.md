# Flock Detekt Rules

Custom very opinionated [Detekt](https://detekt.dev/) rules for enforcing hexagonal architecture and typed error handling in Kotlin projects.

These rules are particularly useful for guiding **AI coding agents** (Copilot, Cursor, Claude, etc.) toward consistent architectural patterns. When AI-generated code violates the architecture, Detekt catches it immediately — providing automated guardrails that keep both human and AI contributions aligned with your codebase conventions.

## Modules

| Module | Rules | Purpose |
|--------|-------|---------|
| `hexagonal-detekt-rules` | 15 | Enforce hexagonal/ports & adapters architecture |
| `arrow-detekt-rules` | 3 | Enforce typed error handling with Arrow |

## Installation

### Prerequisites

| Detekt Version | Kotlin Version | Gradle |
|----------------|----------------|--------|
| 2.0.0-alpha.2 | 2.3.0 | 9.3.0 |
| 2.0.0-alpha.1 | 2.2.20 | 9.1.0 |
| 2.0.0-alpha.0 | 2.2.10 | 8.13.0 |

See the [Detekt Compatibility Table](https://detekt.dev/docs/introduction/compatibility/) for the full matrix.

### 1. Add Detekt plugin to root build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.2.20"  // Must match Detekt's Kotlin version
    id("dev.detekt") version "2.0.0-alpha.1" apply false
}
```

### 2. Configure module build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    id("dev.detekt")
}

// Force detekt to use the Kotlin version it was compiled with
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.2.20")
        }
    }
}

dependencies {
    detektPlugins("community.flock:hexagonal-detekt-rules:1.0.0")
    detektPlugins("community.flock:arrow-detekt-rules:1.0.0")  // optional
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/detekt.yml"))
}
```

### 3. Create detekt.yml in your module

See [Configuration](#configuration) below for a full example.

## Hexagonal Architecture Rules

### Domain Layer
| Rule | Description |
|------|-------------|
| `DomainNoPrimitiveObsession` | Prevents primitive types in domain data classes — use value classes |
| `DomainMustBeImmutable` | Enforces `val` over `var` in domain classes |
| `DomainNoFrameworkImports` | Blocks Spring, Ktor, Jakarta, etc. imports in domain |
| `ValueClassMustHaveJvmInline` | Requires `@JvmInline` on value classes |

### Ports
| Rule | Description |
|------|-------------|
| `PortMustBeInterface` | Ports must be interfaces, not classes |
| `PortNamingConvention` | Ports must end with `Port`, `Repository`, `Gateway`, or `Client` |
| `PortsInDomainOnly` | Ports can only be defined in domain packages |

### Adapters
| Rule | Description |
|------|-------------|
| `AdapterMustImplementPort` | Adapters must implement a port interface |
| `AdapterNamingConvention` | Adapters must follow naming patterns (`*Adapter`, `*Impl`, `Mock*`) |
| `AdapterCannotDependOnAdapter` | Prevents cross-adapter dependencies |

### Dependencies
| Rule | Description |
|------|-------------|
| `DomainCannotDependOnAdapters` | Domain cannot import adapter code |
| `DomainCannotDependOnApi` | Domain cannot import API layer code |
| `ApiCannotDependOnAdapters` | API layer cannot import adapter implementations |

### Layering
| Rule | Description |
|------|-------------|
| `DtoOnlyInAdaptersOrApi` | DTOs/Request/Response classes only in adapters or API |
| `NoServiceInApiOrAdapter` | Service classes must be in domain layer |

## Arrow Error Handling Rules

| Rule | Description |
|------|-------------|
| `NoThrowInDomainOrAdapters` | No throwing exceptions — use Arrow's `Raise` |
| `DomainServiceMustUseRaise` | Public service functions must have `context(Raise<E>)` |
| `ErrorTypeMustBeSealedInterface` | Error types must be sealed interfaces |

## Configuration

```yaml
# detekt.yml
hexagonal:
  DomainNoPrimitiveObsession:
    active: true
    domainPackages: ['domain', 'core']
  DomainNoFrameworkImports:
    active: true
    forbiddenImports:
      - 'org.springframework'
      - 'io.ktor'
      - 'jakarta'
  AdapterNamingConvention:
    active: true
    adapterPatterns: ['.*Adapter', 'Mock.*', '.*Impl', '.*Client']

arrow:
  NoThrowInDomainOrAdapters:
    active: true
  DomainServiceMustUseRaise:
    active: true
    serviceSuffixes: ['Service', 'UseCase', 'Handler']
```

## Architecture Enforced

```
                    ┌───────────────────┐
                    │  API / Controllers │
                    │   (Driving Side)   │
                    └─────────┬─────────┘
                              │ calls directly
                              ▼
              ┌───────────────────────────────┐
             ╱                                 ╲
            ╱      ┌─────────────────────┐      ╲
           │       │       DOMAIN        │       │
           │       │  ┌───────────────┐  │       │
           │       │  │   Services    │  │       │
           │       │  │    Models     │  │       │
           │       │  └───────┬───────┘  │       │
           │       │          │ uses     │       │
           │       │  ┌───────┴───────┐  │       │
           │       │  │   «ports»     │  │       │
           │       │  │ (interfaces)  │  │       │
            ╲      └──┴───────┬───────┴──┘      ╱
             ╲                │                ╱
              └───────────────────────────────┘
                              │ implement
                              ▼
                    ┌───────────────────┐
                    │      Adapters     │
                    │   (Driven Side)   │
                    │  DB, HTTP, Queue  │
                    └───────────────────┘

        ─────────────────────────────────────────
        • Driving adapters (API, events, CLI, jobs) invoke
          domain services — the service interface is the port
        • Domain defines ports for external dependencies
        • Driven adapters (DB, HTTP clients, queues) implement ports
        • Dependencies always point inward
```

## License

MIT
