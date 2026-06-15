# Observation UI Gap Analysis

## Compare: Current Code ↔ Detailed Spec

### ObserveScreen (Observation Creation)

#### Layout Structure
| Spec Section | Current Status |
|---|---|
| Camera preview at top with floating timer pill | ❌ Currently buttons-only; no live preview strip |
| Quick Classification grid (Bird/Mammal/Reptile/Amphibian/Insect/etc.) | 🟡 Categories are collapsible chips, not a prominent grid |
| Full Observation Form (subject, species, category, confidence, etc.) | ✅ Present but layout differs |
| Auto Metadata section (GPS/Weather/Timestamp status) | ❌ GPS/weather in separate sections |
| Species Identification live chip | 🟡 SpeciesIdButton exists but is manual-tap only |
| Recent Captures | ✅ Present |
| No observations list | ❌ Currently shows full observations list |

#### Missing Dropdowns & Fields
| Field | Status |
|---|---|
| Species search dropdown with recent/nearby/taxonomy | ❌ |
| Category dropdown (Bird/Mammal/Reptile/Amphibian/Insect/Arachnid/Fish/Plant/Fungus/Habitat/Track/Nest/Other) | 🟡 Partially — 12 categories exist but not 15 |
| Confidence (Certain/Very Likely/Probable/Unsure/Needs Review) | 🟡 Current: Certain/Likely/Unsure — missing Very Likely, Probable, Needs Review |
| Behavior dropdown (Feeding/Hunting/Nesting/Resting etc.) | ❌ |
| Life Stage (Egg/Juvenile/Subadult/Adult/Unknown) | ❌ |
| Conservation Status auto-fill | ❌ |
| Sex (Male/Female/Unknown) | ❌ |
| Weather auto/manual toggle | ❌ |
| Habitat Type (Forest/Wetland/Grassland etc.) | ❌ |
| Observation Quality (Excellent/Good/Fair/Poor) | 🟡 Uses score % instead |
| Save Draft + Save Obs two-button row | ❌ Single button only |
| Autosave every few seconds | ❌ |
| Bulk evidence attachment | ❌ |
| Duplicate detection | ❌ |
| Species autocomplete | ❌ |

### DetailScreen (Observation View)

#### Layout Structure
| Spec Section | Current Status |
|---|---|
| ← Back | ✏ Edit | ↗ Share | 🗑 Delete top bar | ❌ Currently Edit/Delete below header |
| Swipeable Media Gallery | ✅ Present (HorizontalPager) |
| Subject + Badges | ✅ Present |
| Species Information (scientific name, taxonomy, conservation) | ❌ Not shown as dedicated card |
| Observation Notes | ✅ Present (Facts) |
| Behavior & Context | 🟡 Context exists but behavior as structured section is missing |
| Evidence section with counts | ✅ Present (via attachments panel) |
| Weather & Location section | ✅ Present (combined with chips) |
| Map Preview | ✅ Present |
| AI Species Analysis card | ❌ Not shown as standalone card |
| Provenance | ✅ Present (collapsible) |
| Related Observations | ✅ Present |
| Export: PDF / CSV / JSON | ❌ Markdown only |
| Full-screen media viewer | ❌ Opens WebView/reader instead |

#### Missing Display Fields
| Field | Status |
|---|---|
| Sex display | ❌ |
| Life Stage display | ❌ |
| Conservation Status display | ❌ |
| Habitat Type display | ❌ |
| GPS accuracy indicator | ❌ |
| Compass direction capture | ❌ |
| Altitude recording | ❌ |
| Observation quality score display | ❌ |
| Cloud sync status | ❌ |
| Research-grade badge | ❌ |
