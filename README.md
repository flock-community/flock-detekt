# Flock Detekt Rules

Custom very opinionated [Detekt](https://detekt.dev/) rules for enforcing hexagonal architecture and typed error handling in Kotlin projects.

These rules are particularly useful for guiding **AI coding agents** (Copilot, Cursor, Claude, etc.) toward consistent architectural patterns. When AI-generated code violates the architecture, Detekt catches it immediately — providing automated guardrails that keep both human and AI contributions aligned with your codebase conventions.

## Modules

| Module | RuleSets | Rules | Purpose |
|--------|----------|-------|---------|
| `hexagonal-detekt-rules` | 5 | 20 | Enforce hexagonal/ports & adapters architecture |
| `arrow-detekt-rules` | 1 | 6 | Enforce typed error handling with Arrow |
| `wirespec-detekt-rules` | 1 | 4 | Enforce [Wirespec](https://wirespec.io)-generated interfaces in Spring controllers and Ktor endpoints |

Rules marked **opt-in** are disabled by default; see [Opt-in rules](#opt-in-rules) for how to enable them.

## Installation

### Compatibility

Detekt 2.0 is pre-release software: its alpha releases break binary compatibility of the
rule-provider API between versions (for example, 2.0.0-alpha.2 renamed `RuleSet.Id` to
`RuleSetId`, which is why rules 1.1.0 fails with `NoClassDefFoundError: dev/detekt/api/RuleSet$Id`
on alpha.2+ engines). Each release of these rules is therefore compiled against a specific
`detekt-api` and only works on detekt engines that are binary-compatible with it:

| Rules version | Detekt engine (`toolVersion`) | Engine's embedded Kotlin | Min JVM |
|---------------|-------------------------------|--------------------------|---------|
| 1.2.0 | 2.0.0-alpha.4 – 2.0.0-alpha.5 | 2.4.0 | 11 |
| 1.1.0 | 2.0.0-alpha.1 | 2.2.20 | 17 |
| 1.0.x | 2.0.0-alpha.1 | 2.2.20 | 17 |

Notes:

- The detekt engine analyzes sources with its own embedded Kotlin compiler, so the engine
  version — not your project's Kotlin version — determines which Kotlin language features
  it can parse (a 2.4.0 engine happily analyzes Kotlin 2.3 code).
- Rules 1.2.0 uses only API that is unchanged since 2.0.0-alpha.2, so it may also load on
  alpha.2/alpha.3 engines, but it is built with Kotlin 2.4.0 and only tested on
  alpha.4/alpha.5 — treat older engines as unsupported.
- See the [Detekt Compatibility Table](https://detekt.dev/docs/introduction/compatibility/)
  for which Gradle and JVM versions each detekt release supports.

### 1. Add Detekt plugin to root build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"  // Your project's Kotlin — need not match the engine's
    id("dev.detekt") version "2.0.0-alpha.5" apply false
}
```

### 2. Configure module build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    id("dev.detekt")
}

// Force detekt's classpath onto the Kotlin version its engine was compiled with.
// This must match the *engine's* embedded Kotlin (see the compatibility matrix above),
// NOT your project's Kotlin version — the engine hard-refuses any other version.
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.4.0")
        }
    }
}

dependencies {
    detektPlugins("community.flock:hexagonal-detekt-rules:1.2.0")
    detektPlugins("community.flock:arrow-detekt-rules:1.2.0")  // optional
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/detekt.yml"))
}
```

### 3. Create detekt.yml in your module

See [Configuration](#configuration) below for examples by module type.

## Using with Maven

The rule sets are published to Maven Central as plain JVM artifacts, so they can be
consumed from a Maven build too. Detekt 2.0 has no first-party Maven plugin, so the
supported path is to run the [Detekt CLI](https://detekt.dev/docs/gettingstarted/cli/)
via the [Maven Ant Task](https://detekt.dev/docs/gettingstarted/mavenanttask/), loading
the custom rules through the CLI's `--plugins` flag.

Two plugins are needed: `maven-dependency-plugin` resolves the rule jars into a known
directory, and `maven-antrun-plugin` runs the detekt CLI against them.

```xml
<build>
  <plugins>
    <!-- 1. Resolve the Flock rule jars into target/detekt-plugins -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-dependency-plugin</artifactId>
      <version>3.8.1</version>
      <executions>
        <execution>
          <id>copy-detekt-plugins</id>
          <phase>process-test-classes</phase>
          <goals><goal>copy</goal></goals>
          <configuration>
            <stripVersion>true</stripVersion>
            <outputDirectory>${project.build.directory}/detekt-plugins</outputDirectory>
            <artifactItems>
              <artifactItem>
                <groupId>community.flock</groupId>
                <artifactId>hexagonal-detekt-rules</artifactId>
                <version>1.2.0</version>
              </artifactItem>
              <artifactItem>
                <groupId>community.flock</groupId>
                <artifactId>arrow-detekt-rules</artifactId>
                <version>1.2.0</version>
              </artifactItem>
            </artifactItems>
          </configuration>
        </execution>
      </executions>
    </plugin>

    <!-- 2. Run the detekt CLI with the rule jars passed via --plugins -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-antrun-plugin</artifactId>
      <version>3.1.0</version>
      <executions>
        <execution>
          <id>detekt</id>
          <phase>verify</phase>
          <goals><goal>run</goal></goals>
          <configuration>
            <target name="detekt">
              <java taskname="detekt" dir="${basedir}"
                    fork="true"
                    failonerror="true"
                    classname="dev.detekt.cli.Main"
                    classpathref="maven.plugin.classpath">
                <arg value="--input"/>
                <arg value="${basedir}/src/main/kotlin"/>
                <arg value="--config"/>
                <arg value="${basedir}/detekt.yml"/>
                <arg value="--build-upon-default-config"/>
                <!-- Separate multiple jars with ':' (use ';' on Windows) -->
                <arg value="--plugins"/>
                <arg value="${project.build.directory}/detekt-plugins/hexagonal-detekt-rules.jar:${project.build.directory}/detekt-plugins/arrow-detekt-rules.jar"/>
              </java>
            </target>
          </configuration>
        </execution>
      </executions>
      <dependencies>
        <dependency>
          <groupId>dev.detekt</groupId>
          <artifactId>detekt-cli</artifactId>
          <version>2.0.0-alpha.5</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</build>
```

Run it with:

```bash
mvn verify
```

The `detekt.yml` and the per-layer rule set configuration are identical to the Gradle
setup — see [Configuration](#configuration) below.

> **Note:** keep the `detekt-cli` version within the engine range these rules support
> (see the [compatibility matrix](#compatibility)). Because Detekt 2.0 alphas break the
> rule-provider API between releases, a mismatched CLI can fail to load the rule jars.

## Hexagonal Architecture RuleSets

The hexagonal rules are organized into **5 layer-specific rulesets** that can be enabled/disabled as a group. This allows each module to enable only the rules relevant to its architectural layer.

| RuleSet | Rules | Target Modules |
|---------|-------|----------------|
| `hexagonal-domain` | 7 | domain |
| `hexagonal-port` | 3 | domain |
| `hexagonal-adapter` | 4 | adapters/* |
| `hexagonal-dependency` | 4 | varies |
| `hexagonal-layering` | 2 | all |

### hexagonal-domain

| Rule | Description |
|------|-------------|
| `DomainNoPrimitiveObsession` | Prevents primitive types in domain data classes — use value classes |
| `DomainMustBeImmutable` | Enforces `val` over `var` in domain classes |
| `DomainNoFrameworkImports` | Blocks Spring, Ktor, Jakarta, etc. imports in domain |
| `DomainModelMustBeStandalone` | Allow-lists domain imports so foreign models can't leak in (opt-in) |
| `ValueClassMustHaveJvmInline` | Requires `@JvmInline` on value classes |
| `SmartConstructorMustHaveConsistentCopyVisibility` | Data classes with a non-public constructor need `@ConsistentCopyVisibility`, so `copy()` can't bypass the smart constructor (opt-in) |
| `IdentifierMustBeValueClass` | Identifiers (`id`, `*Id`) must not be raw `String`/`Int`/`Long`/`UUID` — a low-noise alternative to `DomainNoPrimitiveObsession` (opt-in) |

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
| `AdapterCannotDependOnDomainServices` | Adapters may import domain ports and models, not domain services (opt-in) |

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
| `NoExceptionTypesInDomain` | No exception classes in the domain — expected failures are typed errors (opt-in) |
| `NoEitherInDomainSignatures` | Public domain signatures use `context(_: Raise<E>)`, not `Either` (opt-in) |
| `CatchAllMustRethrowCancellation` | Catch-alls in suspend functions must rethrow `CancellationException` (opt-in) |

`CatchAllMustRethrowCancellation` is a syntax-only counterpart of detekt's
`coroutines>SuspendFunSwallowedCancellation`. The built-in rule needs type resolution, so it only
runs in the type-resolving tasks (`detektMain`, `detektTest`), but it knows whether a `try` block
really calls suspend functions and also covers `runCatching`. This rule runs in the plain
`detekt` task too and assumes every `try` in a suspend function can be cancelled.

## Wirespec Rules

| Rule | Description |
|------|-------------|
| `ControllerMustImplementWirespecHandler` | Spring `@RestController`s must implement a Wirespec `Handler` |
| `NoManualSpringMappingAnnotations` | Spring mapping annotations only on overridden Wirespec `Handler` methods |
| `NoManualKtorRoutes` | No hand-written Ktor routes (`get("/path") { }`) — mount Wirespec handlers with `wirespec(handler)` (opt-in) |
| `EndpointsMustImplementWirespecHandler` | Ktor `*Endpoints` classes must implement a Wirespec `Handler` (opt-in) |

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
  # Opt-in: enforce a fully self-contained domain model (allow-list instead of deny-list).
  DomainModelMustBeStandalone:
    active: true
    domainPackages: ['domain']
    allowedPackages:
      - 'kotlin'
      - 'kotlinx'
      - 'java'
      - 'javax.annotation'
    additionalAllowedPackages:
      - 'com.example.shared'  # intentionally shared value-object module

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

### Opt-in rules

The rules added in 1.3.0 are disabled by default, so upgrading does not fail existing builds.
Detekt only runs a rule whose own configuration sets `active: true` (enabling the rule set
alone is not enough), so enable each rule you want explicitly. Package options accept bare
segment names (`domain`) as well as fully qualified prefixes (`com.example.platform.domain`):

```yaml
hexagonal-domain:
  active: true
  SmartConstructorMustHaveConsistentCopyVisibility:
    active: true
    domainPackages: ['com.example.platform.domain']
  IdentifierMustBeValueClass:
    active: true
    domainPackages: ['com.example.platform.domain']

hexagonal-adapter:
  active: true
  AdapterCannotDependOnDomainServices:
    active: true
    adapterPackages: ['com.example.database', 'com.example.integrations']
    domainPackages: ['com.example.platform.domain']

arrow:
  active: true
  NoExceptionTypesInDomain:
    active: true
    domainPackages: ['com.example.platform.domain']
  NoEitherInDomainSignatures:
    active: true
    domainPackages: ['com.example.platform.domain']
  CatchAllMustRethrowCancellation:
    active: true

wirespec:
  active: true
  NoManualKtorRoutes:
    active: true
    apiPackages: ['com.example.platform.api']
    allowedPaths: ['/orders/{orderId}/events']  # e.g. an SSE stream Wirespec cannot model
  EndpointsMustImplementWirespecHandler:
    active: true
    apiPackages: ['com.example.platform.api']
```

All options and their defaults are listed in each module's `src/main/resources/config/*.yml`.

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
