# FieldMind

**Observe. Question. Discover.**

FieldMind is a phone-first, offline-friendly Android research notebook built with Kotlin and Jetpack Compose. It transforms the former Rhythm app shell into a local-first workspace for observations, questions, hypotheses, projects, sources, data collection, analysis, reports, and archive search.

## Core flow

> Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive

Every active feature is designed to connect to this chain.

## Current FieldMind scope

- Home dashboard for current project, goal, streak, stats, and recent activity
- Capture tab for factual observations with category, certainty, evidence, location, tags, and context
- Research tab for questions, hypotheses, projects, and simple data tools
- Library tab for articles, papers, books, videos, websites, notes, and guided paper-reading prompts
- Archive tab for universal search across research records
- FieldMind settings for observation defaults, location/media planning, Gemini assistant planning, export, and data ownership
- Offline-first Room database for observations, questions, hypotheses, projects, sources, data records, reports, tags, and relationship tables

## Design principles

- Minimal, calm, research-focused UI
- Reuses the existing Material 3 Compose theme and component foundation
- Supports light and dark mode through theme tokens
- Keeps spacing consistent and interactions clear
- Uses subtle navigation and content feedback

## Asset policy

Existing image, vector, font, and media assets are intentionally kept in the repository. The active app flow no longer depends on music-player behavior, but assets are not deleted unless a future task explicitly approves that cleanup.

## Android

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Offline-first local storage

## Future milestones

- Contextual camera/audio/location attachment flows
- More data tools: measurement log, checklist, event log, weather log, site log, species tracker, comparison table
- Markdown/CSV/JSON export
- Report builder templates
- Mature skill-tree progress tracking
- Optional Gemini assistant that helps with observation review, question quality, hypotheses, summarization, flashcards, and research writing without inventing evidence
