# Flock Detekt Rules

Custom very opinionated [Detekt](https://detekt.dev/) rules for enforcing hexagonal architecture and typed error handling in Kotlin projects.

These rules are particularly useful for guiding **AI coding agents** (Copilot, Cursor, Claude, etc.) toward consistent architectural patterns. When AI-generated code violates the architecture, Detekt catches it immediately — providing automated guardrails that keep both human and AI contributions aligned with your codebase conventions.

## Modules

| Module | RuleSets | Rules | Purpose |
|--------|----------|-------|---------|
| `hexagonal-detekt-rules` | 5 | 16 | Enforce hexagonal/ports & adapters architecture |
| `arrow-detekt-rules` | 1 | 3 | Enforce typed error handling with Arrow |

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
    detektPlugins("community.flock:hexagonal-detekt-rules:1.1.0")
    detektPlugins("community.flock:arrow-detekt-rules:1.1.0")  // optional
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/detekt.yml"))
}
```

### 3. Create detekt.yml in your module

See [Configuration](#configuration) below for examples by module type.

## Hexagonal Architecture RuleSets

The hexagonal rules are organized into **5 layer-specific rulesets** that can be enabled/disabled as a group. This allows each module to enable only the rules relevant to its architectural layer.

| RuleSet | Rules | Target Modules |
|---------|-------|----------------|
| `hexagonal-domain` | 4 | domain |
| `hexagonal-port` | 3 | domain |
| `hexagonal-adapter` | 3 | adapters/* |
| `hexagonal-dependency` | 4 | varies |
| `hexagonal-layering` | 2 | all |

### hexagonal-domain

| Rule | Description |
|------|-------------|
| `DomainNoPrimitiveObsession` | Prevents primitive types in domain data classes — use value classes |
| `DomainMustBeImmutable` | Enforces `val` over `var` in domain classes |
| `DomainNoFrameworkImports` | Blocks Spring, Ktor, Jakarta, etc. imports in domain |
| `ValueClassMustHaveJvmInline` | Requires `@JvmInline` on value classes |

### hexagonal-port

| Rule | Description |
|------|-------------|
| `PortMustBeInterface` | Ports must be interfaces, not classes |
| `PortNamingConvention` | Ports must end with `Port`, `Repository`, `Gateway`, or `Client` |
| `PortsInDomainOnly` | Ports can only be defined in domain packages |

### hexagonal-adapter

| Rule | Description |
|------|-------------|
| `AdapterMustImplementPort` | Adapters must implement a port interface |
| `AdapterNamingConvention` | Adapters must follow naming patterns (`*Adapter`, `*Impl`, `Mock*`) |
| `AdapterCannotDependOnAdapter` | Prevents cross-adapter dependencies |

### hexagonal-dependency

| Rule | Description |
|------|-------------|
| `DomainCannotDependOnAdapters` | Domain cannot import adapter code |
| `DomainCannotDependOnApi` | Domain cannot import API layer code |
| `ApiCannotDependOnAdapters` | API layer cannot import adapter implementations |
| `ApiCannotDependOnPorts` | API layer should use domain services, not ports directly |

### hexagonal-layering

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

### Domain Module

```yaml
# domain/detekt.yml
hexagonal-domain:
  active: true
  DomainNoPrimitiveObsession:
    domainPackages: ['domain']
  DomainNoFrameworkImports:
    domainPackages: ['domain']
    forbiddenImports:
      - 'io.ktor'
      - 'jakarta.persistence'

hexagonal-port:
  active: true
  PortsInDomainOnly:
    domainPackages: ['domain']

hexagonal-adapter:
  active: false

hexagonal-dependency:
  active: true
  ApiCannotDependOnAdapters:
    active: false
  ApiCannotDependOnPorts:
    active: false

hexagonal-layering:
  active: true
```

### API Module

```yaml
# api/detekt.yml
hexagonal-domain:
  active: false

hexagonal-port:
  active: false

hexagonal-adapter:
  active: false

hexagonal-dependency:
  active: true
  DomainCannotDependOnAdapters:
    active: false
  DomainCannotDependOnApi:
    active: false
  ApiCannotDependOnAdapters:
    apiPackages: ['api']
  ApiCannotDependOnPorts:
    apiPackages: ['api']

hexagonal-layering:
  active: true
```

### Adapter Module

```yaml
# adapters/*/detekt.yml
hexagonal-domain:
  active: false

hexagonal-port:
  active: false

hexagonal-adapter:
  active: true
  AdapterMustImplementPort:
    adapterPatterns: ['.*Adapter', 'Mock.*', '.*Impl', '.*Client', '.*Repository']

hexagonal-dependency:
  active: false

hexagonal-layering:
  active: false
```

### App Module (Composition Root)

```yaml
# app/detekt.yml - disable all hexagonal rules
hexagonal-domain:
  active: false

hexagonal-port:
  active: false

hexagonal-adapter:
  active: false

hexagonal-dependency:
  active: false

hexagonal-layering:
  active: false
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

## Migration from v1.0.x

Version 1.1.0 replaces the single `hexagonal` ruleset with 5 layer-specific rulesets. Update your `detekt.yml` files:

**Before (v1.0.x):**
```yaml
hexagonal:
  DomainNoPrimitiveObsession:
    active: false
  DomainNoFrameworkImports:
    active: false
  # ... 10+ more disabled rules
```

**After (v1.1.0):**
```yaml
hexagonal-domain:
  active: false
```

## Local Development

To build and publish to Maven Local without GPG signing:

```bash
export ENABLE_GRADLE_SIGNING=false
./gradlew publishToMavenLocal
```

Or add to your shell profile (`~/.zshrc` or `~/.bashrc`):

```bash
export ENABLE_GRADLE_SIGNING=false
```

## License

MIT
