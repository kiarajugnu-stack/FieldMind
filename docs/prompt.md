# FieldMind — Product Redesign Prompt

> **For AI assistants, contributors, and collaborators working on this repo.**
> Read this before touching any screen, component, or workflow.

---

## What FieldMind Should Feel Like

A **field journal that grows into a research workspace** — not a database that stores records.

The user should feel:
> *"I discovered something."*

Not:
> *"I completed a form."*

---

## Primary Design Goals

- **Reduce** form-first thinking
- **Increase** observation-first thinking
- **Increase** question-first thinking
- **Increase** connection-first thinking

---

## Critical Problem to Fix

Every screen currently starts with **classification**.

> Bird → Animal → Rock → Weather → Biology → Ecology → Wildlife…

Users are forced to categorize **before** thinking. This must be reversed.

**Capture thoughts first. Organize later.**

---

## Home Screen

### Current Problem
Navigation icons (Capture, Notes, Questions, Projects, etc.) are too small and feel like utility shortcuts rather than core research actions.

### Required Changes
- **Increase icon size** for all primary nav items — Capture, Notes, Questions, Projects, Hypotheses, Data, Library
- **Increase label size** and weight so each action reads as a primary destination, not a footnote
- **Increase tap target size** to at least 64×64dp minimum
- Add a subtle **active state / ripple** to reinforce interactivity
- The home screen should feel like an **open field journal**, not a dashboard toolbar

---

## CameraX — In-App Camera Redesign

### Current Problem
The camera feels like a basic photo picker bolted on. It does not serve field research.

### Required Changes

#### Go Full Screen
- Camera preview must be **true full screen** — edge to edge, no system bars visible
- Use `WindowCompat.setDecorFitsSystemWindows(window, false)` and immersive sticky mode
- Remove any surrounding card/container chrome

#### Add the Following Features

| Feature | Details |
|---|---|
| **Zoom** | Pinch-to-zoom gesture + optional on-screen zoom slider |
| **Tap to Focus** | Tap anywhere on preview to set focus point; show animated focus ring |
| **Flash Toggle** | Auto / On / Off cycle button, visible on screen |
| **Front/Rear Switch** | Flip camera button always visible |
| **Grid Overlay** | Optional rule-of-thirds grid toggle for field composition |
| **Capture Timer** | 3s / 5s / 10s countdown option for hands-free shots |
| **Aspect Ratio Toggle** | 4:3 / 16:9 / 1:1 options |
| **Quick Annotation** | After capture, allow user to immediately draw/annotate on photo before saving |
| **Observation Tag** | After capture, one-tap option to tag the photo to an existing or new Observation |
| **Auto Metadata** | Automatically attach GPS coordinates, timestamp, and weather snapshot on capture |

#### Post-Capture Flow
After a photo is taken, show a minimal bottom sheet:

```
[Thumbnail] Photo saved.
──────────────────────────
[ Add to Observation ]  [ Add Question ]  [ Just Save ]
```

Do not return to a form. Keep the user in the field.

---

## Full Product Restructure

### Capture Screen

**Current:** Long form → multiple chip groups → metadata-first

**Required flow:**
```
Take evidence (camera / audio / sketch)
     ↓
Write facts (single text field, no required fields)
     ↓
Save
     ↓
"What do you want to do next?"
   [ Add Question ]  [ Add Hypothesis ]  [ Continue Observing ]  [ Add To Project ]
```

- Advanced metadata (species, location tags, category) → **optional, collapsed by default**
- Capture must take **under 15 seconds**

---

### Notes Screen

**Current:** Starts with category selection — feels like a database entry.

**Required flow:**
```
Title (optional)
──────────────────
Body (start writing immediately, no barriers)
──────────────────
[ Tags ]  [ Link to... ]  [ Save ]
```

- Category becomes **optional metadata**, not a required first step
- Not visually dominant

---

### Projects Screen

**Current:** Giant creation form.

**Required creation flow:**
```
Project Name
Research Question
[ Create ]
```

Done. Everything else added after creation. Projects are **containers for investigations**, not forms.

---

### Questions

- Questions are currently buried inside Workspace — **make them first-class objects**
- Every observation must be able to **generate a question** with one tap
- Question screen focuses on:
  - The question itself
  - Why it matters
  - Linked observations
  - Linked hypotheses
  - Open investigations
- Remove category chips from question screens

---

### Hypotheses

Hypothesis should feel like a **prediction**, not a structured record.

Required format:
```
If...
Then...
Because...
```

Support:
- Linked observations
- Linked evidence
- Linked data

Make hypotheses easy to **compare side by side**.

---

### Data Tools

**Current:** Counter, Measurement Log, Checklist, Event Log, Weather Log, Site Log, Species Tracker, Comparison Table — all feel disconnected.

**Required:** Unified entry point:
```
[ + Add Data ]
     ↓
Choose tool:
  Counter / Measurement / Checklist / Species Tracker / Event Log / Weather / Site / Comparison
```

Do **not** expose all tools immediately. Progressively reveal advanced tools.

---

### Library (Knowledge Hub)

Rename and restructure:

```
Library → Knowledge Hub
──────────────────────────
Reading
Notes
Sources
Learning
```

Everything should feel **connected**, not like five separate apps.

---

### Insights Screen

**Current:** Empty widgets — Field Map, Knowledge Graph, Tags, Questions, Projects, Achievements.

**Required:** Answer the question: *"What am I discovering?"*

Show:
- Patterns
- Repeated observations
- Frequently observed subjects
- Open questions
- Emerging themes
- Research progress

Hide advanced analytics until **meaningful data exists**. Replace empty widgets with guided prompts.

---

## Global Rules

### Remove Category Chip Overload
- Almost every screen has large chip groups → **reduce by at least 70%**
- Only keep chips where they add genuine value, not just because the data model has a field

### Fix Visual Hierarchy

| Screen | Priority order |
|---|---|
| Observation | Evidence > Facts > Questions > Metadata |
| Note | Content > Connections > Metadata |
| Project | Research Question > Progress > Resources |
| Insights | Patterns > Statistics |

### Connections as Core Feature
Every item must support linking to:
- Observations
- Notes
- Questions
- Hypotheses
- Projects
- Sources

The system should feel **interconnected**, not siloed.

### Empty States — Replace Passive with Guided

| Instead of | Show |
|---|---|
| "No projects yet" | **Start a research project** → Create a question → Capture observations → Collect evidence → Build conclusions |
| "No observations yet" | **Go outside. Notice something.** → Tap Capture to begin |
| "No questions yet" | **Curiosity is where research starts** → Add your first question |

---

## Information Architecture Audit Checklist

Before implementation, produce:

- [ ] Full UX audit — every screen documented
- [ ] Screen-by-screen redesign report
- [ ] Component inventory (identify duplicates)
- [ ] Navigation restructuring proposal
- [ ] Workflow simplification proposal
- [ ] Form reduction proposal (target: 70% fewer required fields)
- [ ] Implementation roadmap with priorities

---

## Implementation Order (Recommended)

1. **Home screen** — bigger icons, bigger labels, stronger hierarchy
2. **CameraX full screen + features** — full screen, zoom, focus, post-capture flow
3. **Capture redesign** — remove form-first structure
4. **Questions as first-class objects**
5. **Notes redesign**
6. **Hypothesis redesign**
7. **Projects redesign**
8. **Data tools unification**
9. **Library → Knowledge Hub**
10. **Insights redesign**
11. **Empty states**
12. **Global chip reduction pass**

---

*FieldMind is a curiosity system. Build it like one.*