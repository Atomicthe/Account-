# Implementation Plan - UI & Feature Refinements

This plan addresses five specific user requirements: enabling transaction editing on click, repositioning the date/title header on the transaction screen, displaying category emojis with auto-wrap, restricting the FAB "+" button exclusively to the Journal tab, and fixing font size scaling.

## User Review & Critical Decisions

> [!IMPORTANT]
> - **Transaction Editing**: Tapping a transaction item will open the `AddTransactionSheet` pre-populated with existing transaction data for editing/updating.
> - **Header Layout**: On the Journal/Transaction screen, the title and selected date will be on the top row, with the date right-aligned.
> - **Category Emojis**: Categories will display distinct emojis and be laid out with proper wrapping so they fit cleanly.
> - **FAB Visibility**: The floating action button (`+`) will only be rendered when the Journal tab is selected (`selectedTab == 0`).
> - **Font Scaling**: Fix font size offset calculation in `ThemedApp` so Small, Medium, and Large settings actually scale text sizes noticeably (e.g. +0sp, +3sp, +6sp).

---

## 1. Overview & Core Concept

- **What It Does**: Refines the core UX to match user expectations for editing records, clean header layout, emoji-based categorization, contextual FAB, and functional font scaling.
- **Key Value**: Delivers a polished, highly responsive, and accessible Japanese retro-fresh ledger experience.

## 2. User Experience & Visual Design

- **Transaction List**: Tapping any transaction row opens the edit sheet.
- **Journal Header**: Clean row with app title on the left and selected date (with quick picker or month indicator) on the right.
- **Category Grid/Row**: Emojis clearly visible, wrapping nicely.
- **Font Scaling**: Dynamic font sizing based on `SettingsManager` offset applied cleanly to text styles.

## 3. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity / ThemedApp                 │
│              (Applies font scale & dark theme)               │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌───────────────────────────┐  ┌──────────────────────────────┐
│       MainAppScreen       │  │       SettingsManager        │
│  - FAB shown only on tab 0│  │  - Font size offset (0, 3, 6)│
└──────────────┬────────────┘  └──────────────────────────────┘
               │
               ▼
┌───────────────────────────┐
│       LedgerScreen        │
│  - Right-aligned date     │
│  - Clickable tx items     │
│  - Emoji categories       │
└───────────────────────────┘
```

- **State Updates**: Ensure `LedgerViewModel` supports updating existing transactions when an ID is provided.
- **Font Scaling Integration**: Update `ThemedApp` or typography definition to scale base font sizes by the offset (`0`, `3`, `6` sp).
