package org.librelab.gtk4kt.runtime

/**
 * Design tokens — Phase 8.
 *
 * Single source of truth for spacing, typography, and surface roles.
 * Mirrors the CSS class names in `gtk_bridge_load_theme`'s default theme
 * (Rust side: `src/lib.rs` `DEFAULT_THEME_CSS`).
 *
 * Naming convention:
 *   - Spacing: `s1`, `s2`, ... `s8` (4px .. 32px, ×n). Multiples of 4.
 *   - Radius: `rSm`, `rMd`, `rLg`.
 *   - Typography class: `textTitle`, `textSubtitle`, `textMetadata`.
 *
 * Use these instead of raw integers / magic numbers:
 *   Modifier.s(Design.s4)   // 16px
 *   Modifier.r(Design.rMd)   // 9px
 */
object Design {
    // ─── Spacing scale (4px base, 8 steps) ──────────────────────────────────
    const val s1 = 4
    const val s2 = 8
    const val s3 = 12
    const val s4 = 16
    const val s5 = 20
    const val s6 = 24
    const val s8 = 32
    const val s10 = 40
    const val s12 = 48

    // ─── Radius ────────────────────────────────────────────────────────────
    const val rSm = 6
    const val rMd = 9
    const val rLg = 12

    // ─── Component sizes ───────────────────────────────────────────────────
    const val sidebarWidth = 240
    const val thumbSize = 48
    const val rowHeight = 64
    const val headerHeight = 44
    const val maxContentWidth = 960

    // ─── CSS class names (match DEFAULT_THEME_CSS) ──────────────────────────
    /** Sidebar root: transparent, no gray slab. */
    const val CLS_SIDEBAR = "sidebar"
    /** Sidebar header (small caps title above nav). */
    const val CLS_SIDEBAR_HEADER = "sidebar-header"
    /** Selected sidebar item (accent pill background). */
    const val CLS_SIDEBAR_SELECTED = "sidebar-selected"

    /** Content list (ListBox root). */
    const val CLS_CONTENT_LIST = "content-list"
    /** Game cover thumbnail. */
    const val CLS_THUMB = "thumb"

    /** Page title (large, 22px). */
    const val CLS_PAGE_TITLE = "page-title"
    /** Page subtitle (small, dim). */
    const val CLS_PAGE_SUBTITLE = "page-subtitle"
    /** Section title (small caps). */
    const val CLS_SECTION_TITLE = "section-title"

    /** List row primary text (game name, 14px medium). */
    const val CLS_TEXT_TITLE = "text-title"
    /** List row secondary text (type label, 12px dim). */
    const val CLS_TEXT_SUBTITLE = "text-subtitle"
    /** List row metadata (last played, 11px very dim). */
    const val CLS_TEXT_METADATA = "text-metadata"

    /** Settings row container. */
    const val CLS_ACTION_ROW = "action-row"
    /** Settings row primary text. */
    const val CLS_ACTION_TITLE = "action-title"
    /** Settings row secondary text. */
    const val CLS_ACTION_DESCRIPTION = "action-description"

    /** Horizontal hairline between preference groups. */
    const val CLS_GROUP_SEPARATOR = "group-separator"

    /** Empty state. */
    const val CLS_STATUS_PAGE = "status-page"
    const val CLS_STATUS_PAGE_ICON = "status-page-icon"
    const val CLS_STATUS_PAGE_TITLE = "status-page-title"

    /** Flat button (secondary action). */
    const val CLS_BUTTON_FLAT = "flat"
    /** Suggested-action button (primary accent). */
    const val CLS_BUTTON_SUGGESTED = "suggested-action"

    /** Search entry (used in top-bar search). */
    const val CLS_SEARCH_ENTRY = "search-entry"
}

/**
 * Modifier extensions for design tokens.
 *
 * Usage:
 *   Modifier.s(Design.s4)         // padding 16
 *   Modifier.r(Design.rMd)         // radius 9
 *   Modifier.classes("sidebar-selected", "text-title")
 */
fun Modifier.s(pixels: Int): Modifier = padding(pixels, pixels)
fun Modifier.h(pixels: Int): Modifier = padding(pixels, 0)
fun Modifier.v(pixels: Int): Modifier = padding(0, pixels)
