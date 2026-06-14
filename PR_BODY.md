Phase 3-12 Complete Redesign: UI Refinements, Evidence Hub Cleanup, and What's New Updates

## Summary

This PR contains the complete Phase 3-12 redesign implementation with critical UI refinements, evidence hub cleanup, and comprehensive What's New documentation.

## Major Changes

### 1. Evidence Hub Redesign (Phase 6)
- Redesigned EvidenceFilterBar with collapsible Material Design filter panel
- New expandable filter sections for Type and Category with smooth animations
- Proper filter hierarchy with section headers and descriptions
- Added BulkSelectionToolbar, CompletenessIndicator, and EvidenceGridCard utilities
- Filters now collapse under 'Filter & sort' header to reduce visual clutter

**Before:** Flat layout with multiple chip rows
**After:** Collapsible, organized filter sections with clear hierarchy

### 2. UI Cleanup & Deduplication
- Removed duplicate FlowRow chips from ProjectWorkspaceCard
- ResearchRelationshipStrip now serves as the single source of truth for project metrics
- Eliminated duplicate information display (was showing metrics twice)
- Added Select icon (check_box) to FieldMindIcons for bulk operations

### 3. What's New Complete Redesign
- Beautiful Material Design changelog with 80+ features
- All 12 phases documented with detailed feature lists
- Latest version (v1.2.0) prominently featured
- Intro card explaining redesign scope
- Material icons throughout (Sparkle, Check, Calendar)
- Semantic color coding for importance levels

### 4. Database & Component Structure
- Added 5 new database fields for Phase 3-5 features
- ObservationEntity: qualityScore, parentObservationId, followUpScheduledAt
- ProjectEntity: projectType, selectedMethods
- 10 new component files created (~2,365 lines of code)
- 4 comprehensive documentation files

## Design Improvements

### Filter Bar Hierarchy
Filter & sort (collapsible header)
├─ Type (section with chips)
├─ Category (section with lazy scroll)
└─ Bulk Management (button)

### What's New Card Layout
[Icon] Version Title          [Badge]
  Date • Version info
─────────────────
[Bullet] Section Header
  ✓ Feature 1
  ✓ Feature 2
  ✓ Feature 3

## Testing Checklist

- [ ] Evidence tab filters work correctly
- [ ] Bulk selection toolbar appears when items selected
- [ ] Filter sections expand/collapse smoothly
- [ ] ProjectWorkspaceCard shows unified metrics (no duplicates)
- [ ] What's New screen displays all phases correctly
- [ ] All Material icons render properly
- [ ] Changelog cards show proper styling for versions

## Related Issues

- Removed duplicate Evidence Hub component file (was unused)
- Consolidated Evidence Tab into ProjectsScreen (single source of truth)
- Eliminated duplicate workspace details display
- Redesigned filter UI for better UX with 3+ options

## Type

Feature + Refactor + UI Polish

## Impact

High (affects Evidence Hub UX, removes duplicate code, improves visual hierarchy)
