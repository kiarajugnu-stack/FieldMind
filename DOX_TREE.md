# DOX Hierarchy — AGENTS.md Tree

## Mermaid Diagram

```mermaid
graph TB
    MASTER["📐 master.md
    DOX Framework"] --> ROOT["📋 AGENTS.md (root)
    DOX Rail — Project-wide rules"]

    ROOT --> APP["📱 app/AGENTS.md
    Android App Module"]

    APP --> FEATURES["🧩 features/field/AGENTS.md
    Field Feature Module"]

    FEATURES --> DATA["🗄️ features/field/data/AGENTS.md
    Data Layer"]

    FEATURES --> UI["🎨 features/field/presentation/AGENTS.md
    Presentation Layer"]

    APP --> SHARED["🔗 shared/AGENTS.md
    Shared Code Layer"]

    APP --> INFRA["⚙️ infrastructure/AGENTS.md
    Infrastructure"]

    APP --> RES["🎯 res/AGENTS.md
    Android Resources"]

    ROOT --> WEB["🌐 web/AGENTS.md
    Web Landing Page"]

    ROOT --> GRADLE["🏗️ gradle/AGENTS.md
    Gradle Build System"]

    ROOT --> WIKI["📖 wiki/AGENTS.md
    Wiki Documentation"]

    ROOT --> FASTLANE["🚀 fastlane/AGENTS.md
    App Store Deployment"]

    ROOT --> GITHUB["🐙 .github/AGENTS.md
    GitHub CI/CD"]

    style MASTER fill:#e1d5e7,stroke:#9673a6,color:#1a1a2e
    style ROOT fill:#d4e6f1,stroke:#5d8aa8,color:#1a1a2e
    style APP fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    style FEATURES fill:#fdebd0,stroke:#e67e22,color:#1a1a2e
    style DATA fill:#fadbd8,stroke:#c0392b,color:#1a1a2e
    style UI fill:#fadbd8,stroke:#c0392b,color:#1a1a2e
    style SHARED fill:#fdebd0,stroke:#e67e22,color:#1a1a2e
    style INFRA fill:#fdebd0,stroke:#e67e22,color:#1a1a2e
    style RES fill:#fdebd0,stroke:#e67e22,color:#1a1a2e
    style WEB fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    style GRADLE fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    style WIKI fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    style FASTLANE fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    style GITHUB fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e

    classDef level0 fill:#e1d5e7,stroke:#9673a6,color:#1a1a2e
    classDef level1 fill:#d4e6f1,stroke:#5d8aa8,color:#1a1a2e
    classDef level2 fill:#d5f5e3,stroke:#27ae60,color:#1a1a2e
    classDef level3 fill:#fdebd0,stroke:#e67e22,color:#1a1a2e
    classDef level4 fill:#fadbd8,stroke:#c0392b,color:#1a1a2e
```

## ASCII Tree

```
📐 master.md                          ← DOX Framework (the rules)
 └─📋 AGENTS.md (root)               ← DOX Rail (project-wide rules)
    ├─📱 app/AGENTS.md                ← Android App Module
    │  ├─🧩 features/field/AGENTS.md  ← Field Feature (core product)
    │  │  ├─🗄️ data/AGENTS.md        ← Data layer (weather, vision, AI, DB, etc.)
    │  │  └─🎨 presentation/AGENTS.md ← UI layer (screens, components, nav)
    │  ├─🔗 shared/AGENTS.md          ← Shared code (theme, icons, settings)
    │  ├─⚙️ infrastructure/AGENTS.md  ← Workers, widgets
    │  └─🎯 res/AGENTS.md             ← Android resources
    ├─🌐 web/AGENTS.md                ← Web landing page (Next.js)
    ├─🏗️ gradle/AGENTS.md             ← Build system (version catalog)
    ├─📖 wiki/AGENTS.md               ← Wiki documentation
    ├─🚀 fastlane/AGENTS.md           ← App store deployment
    └─🐙 .github/AGENTS.md            ← GitHub CI/CD, templates
```

## Legend

| Level | Role | Files |
|-------|------|-------|
| 📐 Framework | DOX rules | `master.md` |
| 📋 DOX Rail | Project-wide | `AGENTS.md` (root) |
| 📱 App Module | Android app | `app/AGENTS.md` + 5 children |
| 🌐 Web | Landing site | `web/AGENTS.md` |
| 🏗️ Build | Gradle | `gradle/AGENTS.md` |
| 📖 Docs | Wiki | `wiki/AGENTS.md` |
| 🚀 Deploy | Fastlane | `fastlane/AGENTS.md` |
| 🐙 CI/CD | GitHub | `.github/AGENTS.md` |

## Reading Order

When editing any file, read the DOX chain from top to bottom:

1. `master.md` — Understand the DOX framework itself
2. `AGENTS.md` (root) — Get project-wide rules (environment, workflow, Prompt.md)
3. Walk from root to target path, reading every AGENTS.md found along the route
4. Use the nearest AGENTS.md as the local contract for detailed guidance
