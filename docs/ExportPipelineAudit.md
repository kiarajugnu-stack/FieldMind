# Export Pipeline Audit — Missing Data Analysis

## Overview

The export/import pipeline has two paths:
1. **JSON archive** (`archiveJson()` → `parseArchiveJson()`) — Used by Share, Save-as, and Settings export
2. **`.fieldmind` package** (`MediaPacker.buildPackage()` → `extractPackage()`) — Used by Backup tab & auto-backup; adds media file packing

Both paths share the same `archiveJson()`/`parseArchiveJson()` functions for entity data.

---

## 1. ObservationEntity — 24 of 37 fields MISSING from export

| Field | In Export? | In Import? |
|-------|-----------|-----------|
| id | ✅ | ✅ |
| date | ✅ | ✅ |
| time | ✅ | ✅ |
| subject | ✅ | ✅ |
| category | ✅ | ✅ |
| factsOnlyNotes | ✅ | ✅ |
| evidenceSummary | ✅ | ✅ |
| tags | ✅ | ✅ |
| projectId | ✅ | ✅ |
| **manualLocation** | ❌ | ❌ |
| **latitude** | ❌ | ❌ |
| **longitude** | ❌ | ❌ |
| **confidenceLevel** | ❌ | ❌ |
| **moodOrContext** | ❌ | ❌ |
| **structuredDetailsJson** | ❌ | ❌ |
| **startedAt** | ❌ | ❌ |
| **endedAt** | ❌ | ❌ |
| **durationMs** | ❌ | ❌ |
| **changeObservedAt** | ❌ | ❌ |
| **changeDurationMs** | ❌ | ❌ |
| **timeNote** | ❌ | ❌ |
| **weatherTemperature** | ❌ | ❌ |
| **weatherCondition** | ❌ | ❌ |
| **weatherHumidity** | ❌ | ❌ |
| **weatherWindSpeed** | ❌ | ❌ |
| **weatherCloudCover** | ❌ | ❌ |
| **weatherPressure** | ❌ | ❌ |
| **weatherSnapshotAt** | ❌ | ❌ |
| **qualityScore** | ❌ | ❌ |
| **parentObservationId** | ❌ | ❌ |
| **followUpScheduledAt** | ❌ | ❌ |
| **timestamp** | ❌ | ❌ |
| **status** | ❌ | ❌ |

**Impact:** Locations, weather attached to observations, confidence, structured details, timing, and status are all lost on export/import.

---

## 2. EvidenceAttachmentEntity — COMPLETELY MISSING from archive JSON

**EvidenceAttachmentEntity** (photos, audio, video, files attached to observations) is:
- ❌ NOT in `archiveJson()` at all
- ❌ NOT in `parseArchiveJson()` at all
- ✅ **Only** handled by `.fieldmind` media packer (separate binary files)
- ❌ JSON-only export has no attachments whatsoever

**Fields lost:** observationId, type, uri, localPath, caption, status, createdAt

**Impact:** User's #1 complaint — audio and images are lost in JSON/Share export. Only the `.fieldmind` package preserves them.

---

## 3. ProjectEntity — 11 of 17 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, title, topicType, objective, researchQuestion, status | ✅ |
| **backgroundNotes** | ❌ |
| **hypothesisSummary** | ❌ |
| **methods** | ❌ |
| **dataSummary** | ❌ |
| **analysis** | ❌ |
| **conclusion** | ❌ |
| **futureQuestions** | ❌ |
| **connectionMap** | ❌ |
| **attachmentUris** | ❌ |
| **projectType** | ❌ |
| **selectedMethods** | ❌ |

---

## 4. QuestionEntity — 5 of 10 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, questionText, category, status, priority | ✅ |
| **answer** | ❌ |
| **answeredAt** | ❌ |
| **relatedObservationIds** | ❌ |
| **relatedSourceIds** | ❌ |
| **relatedProjectId** | ❌ |

---

## 5. HypothesisEntity — 6 of 10 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, prediction, resultStatus, confidencePercent | ✅ |
| **reasoning** | ❌ |
| **evidenceNeeded** | ❌ |
| **supportCriteria** | ❌ |
| **weakeningCriteria** | ❌ |
| **testMethod** | ❌ |
| **linkedQuestionId** | ❌ |

---

## 6. DataRecordEntity — 8 of 13 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, toolType, label, value, unit | ✅ |
| **projectId** | ❌ |
| **observationId** | ❌ |
| **timestamp** | ❌ |
| **location** | ❌ |
| **notes** | ❌ |
| **datasetKind** | ❌ |
| **chartPreference** | ❌ |
| **linkedSessionId** | ❌ |

---

## 7. ReportEntity — 12 of 17 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, type, title, status, markdownDraft | ✅ |
| **projectId** | ❌ |
| **background** | ❌ |
| **question** | ❌ |
| **methods** | ❌ |
| **observations** | ❌ |
| **results** | ❌ |
| **interpretation** | ❌ |
| **conclusion** | ❌ |
| **limitations** | ❌ |
| **nextSteps** | ❌ |
| **templateId** | ❌ |
| **preset** | ❌ |

---

## 8. FlashcardEntity — 9 of 13 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, front, back, type | ✅ |
| **sourceId** | ❌ |
| **projectId** | ❌ |
| **reviewCount** | ❌ |
| **lastReviewedAt** | ❌ |
| **nextReviewAt** | ❌ |
| **easeFactor** | ❌ |
| **intervalDays** | ❌ |
| **repetitionCount** | ❌ |
| **deckMode** | ❌ |

---

## 9. SpeciesEntity — 1 field MISSING

| Field | In Export? |
|-------|-----------|
| id, commonName, scientificName, kingdom, phylum, classs, order, family, genus, species, conservationStatus, targetCount, observationCount, projectId, notes | ✅ |
| **autoCountTracking** | ❌ |

**Note:** There is no `gender` or `sex` field on SpeciesEntity. If such data exists, it would be in observation notes or a separate entity.

---

## 10. ResearchSessionEntity — 2 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, name, projectId, startedAt, endedAt, totalDurationMs, observationCount, location, status, notes | ✅ |
| **latitude** | ❌ |
| **longitude** | ❌ |

---

## 11. TaskEntity — 2 fields MISSING

| Field | In Export? |
|-------|-----------|
| id, title, description, taskType, priority, dueDate, assignedTo, status, linkedQuestionId, linkedObservationId, linkedSpeciesId, projectId, parentTaskId, sortOrder | ✅ |
| **linkedEvidenceId** | ❌ |
| **linkedSessionId** | ❌ |

---

## 12. COMPLETELY MISSING Entity Types from archive JSON

These entity types exist in the database but are NOT exported or imported:

1. **EvidenceAttachmentEntity** — Photos, audio, video, files attached to observations
2. **TagEntity** — Custom tag definitions with colors
3. **ObservationTagCrossRef** — Tag-to-observation relationships
4. **TeamMemberEntity** — Team members with roles
5. All **CrossRef tables** — Relationships between entities are lost (project-observation links, hypothesis-evidence links, session-observation links, etc.)

---

## 13. Auto-Backup Worker — Missing entity types

The `FieldMindAutoBackupWorker` calls `archiveJson()` WITHOUT these entity types that the UI export includes:
- **species** ❌
- **weatherCatalog** ❌
- **researchSessions** ❌
- **tasks** ❌

---

## Summary of Issues by User Complaint

| User Complaint | Finding |
|---------------|---------|
| Weather data not saved | **Confirmed.** All 7 weather fields on ObservationEntity are missing from export. WeatherCatalog IS exported, but the per-observation weather is lost. |
| Location data not saved | **Confirmed.** manualLocation, latitude, longitude on observations are missing. ResearchSession lat/lon also missing. |
| Species gender & data | **Gender**: Not a field on SpeciesEntity. **autoCountTracking**: Missing from export. |
| Sources PDFs not exported | SourceEntity fields are fully exported. But `fileUri` files are only packed in `.fieldmind`, not in JSON export. |
| Audio/images not exported | **Confirmed.** EvidenceAttachmentEntity is NOT in archiveJson at all. Only `.fieldmind` package handles media files. JSON-only export has zero attachments. |
| Audio/images not imported | **Confirmed.** parseArchiveJson() has no EvidenceAttachmentEntity handling. Import only works via `.fieldmind` package. |

---

## Recommended Implementation Plan

### Phase A: Fix archiveJson() — Add all missing fields
Edit `FieldMindExport.archiveJson()` to include all missing entity fields. This is the highest impact change.

### Phase B: Add EvidenceAttachmentEntity to archive JSON
Add a new `"evidenceAttachments"` array to `archiveJson()`. This makes attachments exportable in ALL formats, not just `.fieldmind`.

### Phase C: Fix parseArchiveJson() — Import all missing fields
Edit `FieldMindExport.parseArchiveJson()` to parse the new fields added in Phase A+B.

### Phase D: Fix restoreArchiveJson() — Import EvidenceAttachmentEntity + cross-refs
Edit `FieldMindViewModel.restoreArchiveJson()` to process EvidenceAttachmentEntity from the archive JSON.

### Phase E: Add TagEntity + CrossRefs to export/import
Export TagEntity, ObservationTagCrossRef, and key cross-ref tables so relationships are preserved.

### Phase F: Fix auto-backup worker
Add species, weatherCatalog, researchSessions, tasks to the auto-backup worker's archiveJson call.

---

## Files to Modify

| File | Changes |
|------|---------|
| `FieldMindExport.kt` | `archiveJson()` — add missing fields. `parseArchiveJson()` — parse missing fields. Add EvidenceAttachmentEntity array. |
| `FieldMindViewModel.kt` | `restoreArchiveJson()` — import EvidenceAttachmentEntity from JSON, restore cross-refs |
| `FieldMindAutoBackupWorker.kt` | Add missing entity types to archiveJson call |
