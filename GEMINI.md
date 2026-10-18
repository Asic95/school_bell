# Project Instructions: School Bell

## Architecture & Conventions
- This is a Java-based school bell management system with a JavaFX desktop UI and a web-based dashboard.
- UI styles are centralized in `com.schoolbell.ui.UIStyles` for JavaFX and `dashboard.html` for the web view.

## Design Language: "Modern Academic Soft UI v2 (Premium Productivity)"
Strictly adhere to the following design principles for all UI modifications and new features.
Primary visual goal: a clean, premium, work-focused desktop SaaS interface with strong structure and calm visual hierarchy.

### 1. Surface System (Premium Floating UI)
- **Background:** Use complex gradients for page backgrounds: `linear-gradient(to bottom right, #f8fbff 0%, #f5f7fb 45%, #edf4ff 100%)`.
- **Surfaces (Floating Cards):**
  - Background: Use `GLASS_WHITE` constant (`rgba(255,255,255,0.96)`).
  - Border: Use `BORDER_SLATE_70` constant (`rgba(226,232,240,0.7)`), width `1px`.
  - Radius: `28px` (Main cards, Dialogs), `18px` (Buttons, Inputs, Small cards).
  - Shadow: Use `SHADOW_NAVY_08` (`dropshadow(three-pass-box, rgba(15,23,42,0.08), 30, 0, 0, 10)`).
- **Text:** 
  - Primary: `COLOR_NAVY` (#0f172a).
  - Secondary/Body: `COLOR_SLATE` (#64748b).
  - Accent/Label: `COLOR_INDIGO` (#4f46e5).

### 2. Modern SaaS Composition
- **Header Architecture:** 
  - **Badge:** A `54x54` VBox with `18px` radius, `ICON_BADGE_STYLE` background, and colored icon.
  - **Eyebrow:** Small bold label above title (11px, `COLOR_SLATE`, uppercase, weight 900, 1.5px spacing).
  - **Title:** Large bold text (32px, `COLOR_NAVY`, weight 900).
  - **Subtitle:** Descriptive body text (14px, wrap enabled).
- **Navigation:** Use `FadeTransition` (120ms/180ms) for all view switches. Ensure new node opacity is set to 0.0 before adding to parent.

### 3. Component Language
- **Inputs/Combos:** 
  - Style: `PREMIUM_FIELD_STYLE` (18px radius, white background).
  - Validation: Use `PREMIUM_FIELD_ERROR_STYLE` (red border/pale red bg) for empty required fields.
- **Primary Buttons (Premium):** 
  - Style: `PREMIUM_BTN_STYLE` (Indigo/Primary gradient, 18px radius).
  - Shadow: `SHADOW_INDIGO_28`.
- **Toggle Switches:** Use `createToggleSwitch`. It handles smart state transitions (instant placement on load, smooth animation on user click).

### 4. Data-Editing Screens
For schedule/config editors:
- Prefer grid/table-like editing layouts over decorative cards per row.
- Use stable column widths (e.g., 170px for labels in dialogs) to prevent text clipping.
- Empty States: ALWAYS show an `ICON_INFO` (color: `COLOR_WHITE_MUTED_BORDER`) with a helpful sublabel when lists are empty.

## UI Implementation Details
- **JavaFX:** Use `UIStyles.java` constants exclusively for styling.
- **FORBIDDEN:** Never use hardcoded `rgba()` strings or hex colors in `.java` files.
    - `PREMIUM_FIELD_STYLE`: For all text fields and areas.
    - `PREMIUM_SELECT_STYLE`: For all ComboBoxes.
    - `SOFT_CARD`: Base surface for floating cards and dialogs (28px radius).

- **Standard Radii:**
    - `28px`: Main cards, Page headers, Dialog roots.
    - `18px`: Buttons, Inputs, Lesson cards, Sub-sections.
    - `14px`: Avatars, Small badges, Tooltips.

- **Dialog Architecture:**
    - Use `StageStyle.TRANSPARENT`.
    - Scene fill: `Color.TRANSPARENT`.
    - Root: `VBox` with `SOFT_CARD` style.

- **Icons:** Use SVG icons from `UIStyles.java`.
    - **Source:** Material Design Icons (MDI) for 24x24 grid.
    - **Scaling:** Always ensure compatibility with `UIStyles.createSVGIcon`.

## Common UI Patterns

### 1. Action Button Pattern (Edit/Delete)
For small functional buttons inside cards:
- **Base Style:** White background, `12px` border radius, `BORDER_SLATE_85`, and `SHADOW_NAVY_06`.
- **Icons:** Use `18px` size, `COLOR_NEUTRAL` color.
- **Interactivity:**
    - **Edit:** Hover background `COLOR_SURFACE_SUBTLE`, icon `COLOR_PRIMARY`.
    - **Delete:** Hover background `COLOR_DANGER_LIGHT`, icon `COLOR_DANGER`.

### 2. Form Inputs & Primary Buttons
- **Corner Radius:** Always use `18px`.
- **Primary Action:** Use `COLOR_PRIMARY` with `font-weight: 900` and `SHADOW_INDIGO_28`.

## Build & Distribution
- **Automated Packaging:** To rebuild the JAR and update the EXE, run:
```powershell
./build_dist.ps1
```
