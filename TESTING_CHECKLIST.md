# Testing Checklist for PR #47

## Evidence Hub Filter Redesign
- [ ] Open Projects screen
- [ ] Navigate to Evidence tab
- [ ] Click "Filter & sort" to expand filter panel
- [ ] Verify "Type" section shows: All, Observation, Note, Question, Source
- [ ] Select different types and verify filtering works
- [ ] Verify "Category" section appears with project categories
- [ ] Test lazy scroll if more than 8 categories
- [ ] Verify smooth expand/collapse animation
- [ ] Test bulk selection checkbox appears
- [ ] Toggle bulk mode and verify toolbar shows

## What's New Changelog Redesign
- [ ] Open Settings
- [ ] Tap About or Info
- [ ] Open "What's new"
- [ ] Verify intro card explains Phases 1-12
- [ ] Check v1.2.0 latest version is highlighted
- [ ] Verify Material icons render (Sparkle, Check, Calendar)
- [ ] Check importance badges show "Major" or "Minor"
- [ ] Verify all 12 phases are listed
- [ ] Scroll through all features (80+ should be visible)
- [ ] Check section headers have colored bullets
- [ ] Verify tags display as pills
- [ ] Check "Latest version" badge at bottom of v1.2.0 card

## ProjectWorkspaceCard Duplication Fix
- [ ] Open any project
- [ ] View project details/workspace
- [ ] Verify metrics show ONCE (not duplicated)
- [ ] Check ResearchRelationshipStrip displays correctly
- [ ] Verify no duplicate AssistChips appear

## UI Elements & Icons
- [ ] Filter icon (filter_list) renders
- [ ] Select icon (check_box) renders for bulk mode
- [ ] Sparkle icon appears in What's New
- [ ] Check icon appears in feature lists
- [ ] Calendar icon appears in date info
- [ ] All icons have proper colors and sizing

## Performance & Animations
- [ ] Filter expand/collapse animation is smooth
- [ ] No jank when scrolling through What's New
- [ ] Bulk selection changes don't freeze UI
- [ ] Animations work on lower-end devices
- [ ] Dark mode renders correctly

## Data Integrity
- [ ] Selected filters persist across navigation
- [ ] Bulk selection state clears on back
- [ ] No data loss on filter changes
- [ ] Project metrics are accurate
- [ ] Feature list is complete and accurate

## Responsive Design
- [ ] Test on phone (portrait)
- [ ] Test on phone (landscape)
- [ ] Test on tablet
- [ ] Verify text doesn't overflow
- [ ] Check spacing is consistent

## Error Handling
- [ ] No console errors in Evidence tab
- [ ] No console errors in What's New
- [ ] Handle empty categories gracefully
- [ ] Handle no projects case
- [ ] Network errors don't crash app

## Accessibility
- [ ] Filter buttons are tappable (min 48dp)
- [ ] Icons have descriptions
- [ ] Colors have sufficient contrast
- [ ] Text is readable at all sizes
- [ ] Touch targets are appropriate

---

**Test Date:** ________________
**Device:** ________________
**OS Version:** ________________
**Build:** ________________

**Issues Found:**
1. _________________________________________
2. _________________________________________
3. _________________________________________

**Notes:**
_________________________________________
_________________________________________

