"""
build_site.py – BoxViewer website build script
================================================
Generates static HTML pages from Markdown source files and HTML templates.
Zero dependencies – pure Python 3.8+ stdlib.

Local development
-----------------
    python scripts/build_site.py
    python -m http.server 8080 --directory site/dist

Then open http://localhost:8080 in a browser.
Edit a template or source file, re-run the script, and refresh.

Output
------
    site/dist/
        index.html
        changelog.html
        credits.html
        privacy.html
        styles.css
        theme.js
        assets/
            logo.webp
            badge_google_play.png
            badge_github.png
            badge_obtainium.png
"""

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).parent.parent
SITE = ROOT / "site"
DIST = SITE / "dist"


# ── Markdown → HTML converter ──────────────────────────────────────────────

def md_inline(text: str) -> str:
    """Convert inline Markdown (bold, code, links) to HTML."""
    # Escape HTML entities first (before we inject tags)
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # Re-unescape only known safe HTML we want to keep from source
    text = text.replace("&lt;br&gt;", "<br>")
    # Links: [text](url)
    text = re.sub(
        r'\[([^\]]+)\]\((https?://[^\)]+)\)',
        lambda m: f'<a href="{m.group(2)}" target="_blank" rel="noopener noreferrer">{m.group(1)}</a>',
        text,
    )
    # Bold: **text**
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    # Inline code: `code`
    text = re.sub(r'`([^`]+)`', r'<code>\1</code>', text)
    return text


def md_to_html(block: str) -> str:
    """
    Convert a Markdown block to HTML.
    Handles: paragraphs, bullet lists (* or -), ordered lists, headings (##, ###).
    """
    lines = block.strip().splitlines()
    html_parts: list[str] = []
    in_ul = False
    in_ol = False

    def close_lists():
        nonlocal in_ul, in_ol
        if in_ul:
            html_parts.append("</ul>")
            in_ul = False
        if in_ol:
            html_parts.append("</ol>")
            in_ol = False

    for line in lines:
        stripped = line.strip()

        # Skip comment markers (section boundary markers)
        if stripped.startswith("<!--") and stripped.endswith("-->"):
            continue

        # Heading ##
        if stripped.startswith("### "):
            close_lists()
            html_parts.append(f"<h3>{md_inline(stripped[4:])}</h3>")
            continue
        if stripped.startswith("## "):
            close_lists()
            html_parts.append(f"<h2>{md_inline(stripped[3:])}</h2>")
            continue
        if stripped.startswith("# "):
            close_lists()
            html_parts.append(f"<h1>{md_inline(stripped[2:])}</h1>")
            continue

        # Horizontal rule
        if stripped in ("---", "***", "___"):
            close_lists()
            html_parts.append("<hr>")
            continue

        # Ordered list item: "1. text"
        ol_match = re.match(r'^\d+\.\s+(.*)', stripped)
        if ol_match:
            if not in_ol:
                close_lists()
                html_parts.append("<ol>")
                in_ol = True
            html_parts.append(f"<li>{md_inline(ol_match.group(1))}</li>")
            continue

        # Unordered list item: "* text" or "- text" or "*   **Bold**: desc"
        ul_match = re.match(r'^[\*\-]\s+(.*)', stripped)
        if ul_match:
            if not in_ul:
                close_lists()
                html_parts.append("<ul>")
                in_ul = True
            html_parts.append(f"<li>{md_inline(ul_match.group(1))}</li>")
            continue

        # Empty line → close any list, then paragraph break
        if not stripped:
            close_lists()
            html_parts.append("")
            continue

        # Regular paragraph line
        close_lists()
        html_parts.append(f"<p>{md_inline(stripped)}</p>")

    close_lists()

    # Collapse consecutive empty strings (blank lines between blocks)
    result = []
    prev_empty = False
    for part in html_parts:
        if part == "":
            if not prev_empty:
                result.append("")
            prev_empty = True
        else:
            result.append(part)
            prev_empty = False

    return "\n".join(p for p in result if p != "")


# ── Section extraction (README markers) ────────────────────────────────────

def extract_section(text: str, name: str) -> str:
    """Extract content between <!-- web:name-start --> and <!-- web:name-end -->."""
    pattern = rf'<!--\s*web:{re.escape(name)}-start\s*-->(.*?)<!--\s*web:{re.escape(name)}-end\s*-->'
    match = re.search(pattern, text, re.DOTALL)
    if not match:
        raise ValueError(f"Section '{name}' not found in README.md")
    return match.group(1).strip()


# ── Feature cards ──────────────────────────────────────────────────────────

def build_feature_cards(features_md: str) -> str:
    """Convert README features bullet list into feature card HTML."""
    cards = []
    for line in features_md.splitlines():
        stripped = line.strip()
        # Match: *   **emoji Title**: description
        m = re.match(r'^[\*\-]\s+\*\*(.+?)\*\*[:\s]+(.*)', stripped)
        if not m:
            continue
        title_raw = m.group(1)
        desc_raw = m.group(2)
        # Extract leading emoji (first char if it's a multi-byte emoji sequence)
        emoji_m = re.match(r'^([\U00010000-\U0010ffff]|[\U00002600-\U000027BF])\s*(.*)', title_raw)
        if emoji_m:
            emoji = emoji_m.group(1)
            title = emoji_m.group(2)
        else:
            emoji = "•"
            title = title_raw
        desc = md_inline(desc_raw)
        cards.append(
            f'<div class="feature-card">\n'
            f'  <span class="feature-emoji">{emoji}</span>\n'
            f'  <div class="feature-title">{md_inline(title)}</div>\n'
            f'  <div class="feature-desc">{desc}</div>\n'
            f'</div>'
        )
    return "\n".join(cards)


# ── Download cards ─────────────────────────────────────────────────────────

def build_download_cards(download_md: str) -> str:
    """Build the three download cards (Play Store, GitHub, Obtainium)."""
    return """\
<div class="download-card">
    <a class="badge-link" href="https://play.google.com/store/apps/details?id=de.nichu42.boxviewer" target="_blank" rel="noopener noreferrer">
        <img src="assets/badge_google_play.png" alt="Get it on Google Play">
    </a>
    <h3>Google Play Store</h3>
    <p><strong>Official Release</strong> — Download stable production releases directly from the <a href="https://play.google.com/store/apps/details?id=de.nichu42.boxviewer" target="_blank" rel="noopener noreferrer">Google Play Store</a>.</p>
    <p class="mt-xs"><strong>Open Testing (Beta)</strong> — Opt-in on Google Play to test upcoming pre-release builds ahead of general release.</p>
</div>
<div class="download-card">
    <a class="badge-link" href="https://github.com/nichu42/boxviewer/releases" target="_blank" rel="noopener noreferrer">
        <img src="assets/badge_github.png" alt="Get it on GitHub">
    </a>
    <h3>GitHub Releases</h3>
    <p>Download APK packages directly from the <a href="https://github.com/nichu42/boxviewer/releases" target="_blank" rel="noopener noreferrer">GitHub Releases</a> page.</p>
    <p class="mt-xs"><strong>Pre-Releases</strong> — Early test builds published in parallel with the Google Play Open Testing track.</p>
</div>
<div class="download-card">
    <a class="badge-link" href="obtainium://add/https%3A%2F%2Fgithub.com%2Fnichu42%2Fboxviewer">
        <img src="assets/badge_obtainium.png" alt="Get it on Obtainium">
    </a>
    <h3>Obtainium</h3>
    <p>Tap the button to add BoxViewer directly to your existing <a href="https://obtainium.imranr.dev/" target="_blank" rel="noopener noreferrer">Obtainium</a> installation — it opens the <em>Add App</em> screen pre-filled and ready to confirm.</p>
    <p class="mt-xs">Don't have Obtainium yet? Download it first from <a href="https://obtainium.imranr.dev/" target="_blank" rel="noopener noreferrer">obtainium.imranr.dev</a>. Once added, Obtainium tracks GitHub releases automatically (including pre-releases if enabled).</p>
</div>"""


# ── Attribution cards ──────────────────────────────────────────────────────

def build_attribution_cards(attribution_md: str) -> str:
    """Build attribution cards for openSenseMap, Photon, Nominatim."""
    return """\
<div class="card attribution-card">
    <h4>📡 openSenseMap</h4>
    <p>Operated by <strong>openSenseLab gGmbH</strong> (Münster, Germany). An open-source platform for collecting and sharing environmental sensor data worldwide.</p>
    <p class="mt-xs"><a href="https://opensensemap.org" target="_blank" rel="noopener noreferrer">opensensemap.org</a> · <a href="https://www.betterplace.org/en/projects/89947" target="_blank" rel="noopener noreferrer">Support via Betterplace</a></p>
</div>
<div class="card attribution-card">
    <h4>📍 Photon Geocoder</h4>
    <p>OpenStreetMap-powered search and reverse-geocoding, operated by <strong>komoot GmbH</strong> (Berlin, Germany). Used as the primary geocoding fallback.</p>
    <p class="mt-xs"><a href="https://photon.komoot.io" target="_blank" rel="noopener noreferrer">photon.komoot.io</a></p>
</div>
<div class="card attribution-card">
    <h4>🗺️ Nominatim / OpenStreetMap</h4>
    <p>Open-source geocoding engine operated by the <strong>OpenStreetMap Foundation</strong>. Used as secondary geocoding fallback. Data © OpenStreetMap contributors.</p>
    <p class="mt-xs"><a href="https://nominatim.org" target="_blank" rel="noopener noreferrer">nominatim.org</a> · <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">ODbL License</a></p>
</div>"""


# ── Support / Translation sections ────────────────────────────────────────

def build_support_html(support_md: str) -> str:
    return "<p>BoxViewer is developed with love as an open-source project. If you find it useful, please consider supporting its ongoing development:</p>"


def build_translation_html(translation_md: str) -> str:
    return "<p>Help make BoxViewer accessible to everyone! We collaboratively translate the app using <strong>POEditor</strong>. You can submit translations, corrections, or suggest new languages:</p>"


def build_disclaimer_html(disclaimer_md: str) -> str:
    text = disclaimer_md.strip().strip("*")
    return md_inline(text)


# ── Changelog page ─────────────────────────────────────────────────────────

def build_changelog_page(changelog_text: str, template: str) -> str:
    """Parse CHANGELOG.md into a timeline of version cards."""
    sections: list[dict] = []
    current: dict | None = None
    current_cat: str | None = None

    for line in changelog_text.splitlines():
        stripped = line.strip()

        # Version heading: ## [0.52] - 2026-07-23
        vm = re.match(r'^##\s+\[(.+?)\]\s*[-–]\s*(.+)', stripped)
        if vm:
            if current:
                sections.append(current)
            current = {"version": vm.group(1), "date": vm.group(2).strip(), "cats": {}}
            current_cat = None
            continue

        if current is None:
            continue

        # Category: ### Added / Fixed / Changed / Removed
        hm = re.match(r'^###\s+(Added|Fixed|Changed|Removed|Updated)', stripped, re.IGNORECASE)
        if hm:
            current_cat = hm.group(1).capitalize()
            current["cats"].setdefault(current_cat, [])
            continue

        # Bullet item
        bm = re.match(r'^[\*\-]\s+(.*)', stripped)
        if bm and current_cat:
            current["cats"][current_cat].append(bm.group(1))
            continue

        # Continuation line (indented sub-bullets or plain text)
        if stripped and current_cat and current["cats"].get(current_cat):
            current["cats"][current_cat][-1] += " " + stripped

    if current:
        sections.append(current)

    # Build HTML
    cards_html = []
    for sec in sections:
        cats_html = []
        cat_order = ["Added", "Changed", "Fixed", "Updated", "Removed"]
        for cat in cat_order:
            items = sec["cats"].get(cat, [])
            if not items:
                continue
            css_class = cat.lower()
            items_html = "\n".join(f"<li>{md_inline(i)}</li>" for i in items)
            cats_html.append(
                f'<div class="version-section">\n'
                f'  <span class="category-badge {css_class}">{cat}</span>\n'
                f'  <ul>{items_html}</ul>\n'
                f'</div>'
            )
        card = (
            f'<div class="version-card">\n'
            f'  <div class="version-header">\n'
            f'    <span class="version-badge">v{sec["version"]}</span>\n'
            f'    <span class="version-date">{sec["date"]}</span>\n'
            f'  </div>\n'
            + "\n".join(cats_html) +
            f'\n</div>'
        )
        cards_html.append(card)

    content = (
        '<div class="subpage-content">\n'
        '<h1 class="subpage-title">Changelog</h1>\n'
        '<div class="changelog-list">\n'
        + "\n".join(cards_html) +
        '\n</div>\n</div>'
    )

    return (
        template
        .replace("{{page_title}}", "Changelog")
        .replace("{{page_id}}", "changelog")
        .replace("{{page_description}}", "Full version history and release notes for BoxViewer, the openSenseMap Android client.")
        .replace("{{page_canonical}}", "https://boxviewer.app/changelog.html")
        .replace("{{nav_active_changelog}}", ' class="active"')
        .replace("{{nav_active_credits}}", "")
        .replace("{{nav_active_privacy}}", "")
        .replace("{{page_content}}", content)
    )


# ── Credits page ──────────────────────────────────────────────────────────

LICENSE_PILLS = {
    "Apache License 2.0": ("Apache 2.0", "apache"),
    "MIT License": ("MIT", "mit"),
    "MIT": ("MIT", "mit"),
    "GNU General Public License": ("GPL", "gpl"),
    "GPLv3": ("GPLv3", "gpl"),
    "Creative Commons Attribution-ShareAlike": ("CC-BY-SA", "cc"),
    "CC-BY-SA": ("CC-BY-SA", "cc"),
    "Open Database License": ("ODbL", "other"),
    "GPL v2": ("GPL v2", "gpl"),
}


def license_pill(license_text: str) -> str:
    for key, (label, css) in LICENSE_PILLS.items():
        if key in license_text:
            return f'<span class="license-pill {css}">{label}</span>'
    return f'<span class="license-pill other">{license_text[:18]}</span>'


def build_credits_page(credits_text: str, template: str) -> str:
    """Parse CREDITS.md into credit cards grouped by section.

    CREDITS.md format:
      ## Section heading
      ### Optional sub-group
      * **Entry Name**            ← entry start: bold closes at end of line
        * **Field:** value        ← field line: colon inside bold, value follows
    """
    sections_html: list[str] = []
    current_section: str | None = None
    current_group: str | None = None
    current_entry: dict | None = None
    group_entries: list[dict] = []
    groups: list[tuple] = []   # (group_title, [entry_dicts])

    FIELD_MAP = {
        "Operator": "operator",
        "Author": "operator",
        "Original Creator": "operator",
        "Description": "desc",
        "License": "license",
    }

    def flush_entry() -> None:
        nonlocal current_entry
        if current_entry is not None:
            group_entries.append(current_entry)
            current_entry = None

    def flush_group() -> None:
        if group_entries:
            groups.append((current_group, list(group_entries)))
            group_entries.clear()

    def entry_to_card(e: dict) -> str:
        pill = license_pill(e.get("license", ""))
        card = f'<div class="credit-card">\n  <span class="credit-name">{md_inline(e["name"])}</span>{pill}\n'
        if e.get("operator"):
            card += f'  <span class="credit-author">{md_inline(e["operator"])}</span>\n'
        if e.get("desc"):
            card += f'  <span class="credit-desc">{md_inline(e["desc"])}</span>\n'
        if e.get("url"):
            card += f'  <a class="credit-link" href="{e["url"]}" target="_blank" rel="noopener noreferrer">{e["url"]}</a>\n'
        card += '</div>'
        return card

    def flush_section() -> None:
        nonlocal current_section
        flush_entry()
        flush_group()
        if current_section and groups:
            parts = []
            for group_title, entries in groups:
                if group_title:
                    parts.append(f'<div class="credits-group-title">{group_title}</div>')
                cards_html = "\n".join(entry_to_card(e) for e in entries)
                parts.append(f'<div class="credit-cards">{cards_html}</div>')
            sections_html.append(
                f'<div class="credits-section">\n'
                f'<div class="credits-section-title">{current_section}</div>\n'
                + "\n".join(parts) +
                "\n</div>"
            )
            groups.clear()

    for line in credits_text.splitlines():
        stripped = line.strip()
        if not stripped or stripped == "---":
            continue

        if stripped.startswith("## "):
            flush_section()
            current_section = stripped[3:].strip()
            current_group = None
            continue

        if stripped.startswith("### "):
            flush_entry()
            flush_group()
            current_group = stripped[4:].strip()
            continue

        # Field line FIRST (more specific): * **FieldName:** value
        # The colon is the last char before the closing **, e.g. "**Operator:**"
        field_m = re.match(r'^\*\s+\*\*([^*:]+):\*\*\s*(.*)', stripped)
        if field_m and current_entry is not None:
            field_name = field_m.group(1).strip()
            field_value = field_m.group(2).strip()
            mapped = FIELD_MAP.get(field_name)
            if mapped:
                current_entry[mapped] = field_value
                if mapped == "license" and not current_entry.get("url"):
                    url_m = re.search(r'\[([^\]]*)\]\((https?://[^\)]+)\)', field_value)
                    if url_m:
                        current_entry["url"] = url_m.group(2)
            continue

        # Entry start: * **Name**  (greedy match; nothing significant after closing **)
        entry_m = re.match(r'^\*\s+\*\*(.+)\*\*\s*$', stripped)
        if entry_m:
            flush_entry()
            name = entry_m.group(1)
            # Pull URL from name itself if present (some entries embed a link)
            url_m = re.search(r'\[([^\]]*)\]\((https?://[^\)]+)\)', name)
            current_entry = {
                "name": name,
                "operator": "",
                "desc": "",
                "license": "",
                "url": url_m.group(2) if url_m else "",
            }
            continue

    flush_section()

    content = (
        '<div class="subpage-content">\n'
        '<h1 class="subpage-title">Credits &amp; Attributions</h1>\n'
        + "\n".join(sections_html) +
        '\n</div>'
    )

    return (
        template
        .replace("{{page_title}}", "Credits")
        .replace("{{page_id}}", "credits")
        .replace("{{page_description}}", "Third-party libraries, data services, geocoding backends, and graphic assets used by BoxViewer.")
        .replace("{{page_canonical}}", "https://boxviewer.app/credits.html")
        .replace("{{nav_active_changelog}}", "")
        .replace("{{nav_active_credits}}", ' class="active"')
        .replace("{{nav_active_privacy}}", "")
        .replace("{{page_content}}", content)
    )


# ── Privacy page ──────────────────────────────────────────────────────────

def build_privacy_page(privacy_text: str, template: str) -> str:
    """Render PRIVACY.md preserving existing anchor IDs, both EN + DE sections."""
    # Swap GitHub LICENSE link → local rendered page
    privacy_text = privacy_text.replace(
        "https://github.com/nichu42/boxviewer/blob/main/LICENSE",
        "license.html"
    )
    # Convert the whole document to HTML
    lines = privacy_text.splitlines()
    html_lines: list[str] = []
    in_ul = False

    def close_ul():
        nonlocal in_ul
        if in_ul:
            html_lines.append("</ul>")
            in_ul = False

    for line in lines:
        stripped = line.strip()

        # Preserve <a id="..."> anchors as-is (they come through as raw HTML in MD)
        if re.match(r'^<a id="[^"]+"></a>$', stripped):
            close_ul()
            html_lines.append(stripped)
            continue

        if stripped == "---":
            close_ul()
            html_lines.append('<hr class="privacy-lang-divider">')
            continue

        if stripped.startswith("# "):
            close_ul()
            html_lines.append(f"<h1>{md_inline(stripped[2:])}</h1>")
            continue
        if stripped.startswith("## "):
            close_ul()
            html_lines.append(f"<h2>{md_inline(stripped[3:])}</h2>")
            continue
        if stripped.startswith("### "):
            close_ul()
            html_lines.append(f"<h3>{md_inline(stripped[4:])}</h3>")
            continue
        if stripped.startswith("#### "):
            close_ul()
            html_lines.append(f"<h4>{md_inline(stripped[5:])}</h4>")
            continue

        bm = re.match(r'^\*\s+(.*)', stripped)
        if bm:
            if not in_ul:
                html_lines.append("<ul>")
                in_ul = True
            html_lines.append(f"<li>{md_inline(bm.group(1))}</li>")
            continue

        if not stripped:
            close_ul()
            continue

        close_ul()
        html_lines.append(f"<p>{md_inline(stripped)}</p>")

    close_ul()

    lang_jump = (
        '<div class="lang-jump">'
        '<a href="#english-version">🇬🇧 English</a>'
        '<a href="#deutsche-version">🇩🇪 Deutsch</a>'
        '</div>'
    )

    content = (
        '<div class="subpage-content">\n'
        '<h1 class="subpage-title">Legal</h1>\n'
        + lang_jump + '\n'
        '<div class="privacy-content">\n'
        + "\n".join(html_lines) +
        '\n</div>\n</div>'
    )

    return (
        template
        .replace("{{page_title}}", "Legal")
        .replace("{{page_id}}", "privacy")
        .replace("{{page_description}}", "Privacy policy and imprint for BoxViewer. Available in English and German.")
        .replace("{{page_canonical}}", "https://boxviewer.app/privacy.html")
        .replace("{{nav_active_changelog}}", "")
        .replace("{{nav_active_credits}}", "")
        .replace("{{nav_active_privacy}}", ' class="active"')
        .replace("{{page_content}}", content)
    )


# ── License page ──────────────────────────────────────────────────────────

def build_license_page(license_text: str, template: str) -> str:
    """Render the plain-text LICENSE file as a preformatted page."""
    import html as html_lib
    escaped = html_lib.escape(license_text)
    content = (
        '<div class="subpage-content">\n'
        '<h1 class="subpage-title">License</h1>\n'
        '<div class="license-content">'
        f'<pre>{escaped}</pre>'
        '</div>\n</div>'
    )
    return (
        template
        .replace("{{page_title}}", "License")
        .replace("{{page_id}}", "license")
        .replace("{{page_description}}", "GNU General Public License v3 — the open-source license under which BoxViewer is distributed.")
        .replace("{{page_canonical}}", "https://boxviewer.app/license.html")
        .replace("{{nav_active_changelog}}", "")
        .replace("{{nav_active_credits}}", "")
        .replace("{{nav_active_privacy}}", "")
        .replace("{{page_content}}", content)
    )


# ── Main build ─────────────────────────────────────────────────────────────

def build():
    print("BoxViewer site builder")
    print("=" * 40)

    # Clean and recreate dist
    if DIST.exists():
        shutil.rmtree(DIST)
    DIST.mkdir(parents=True)

    # Copy static assets
    shutil.copy(SITE / "styles.css", DIST / "styles.css")
    shutil.copy(SITE / "theme.js", DIST / "theme.js")
    shutil.copytree(SITE / "assets", DIST / "assets")
    print("[ok] Copied static assets")

    # Compute cache-busting hash from CSS + JS content
    import hashlib
    css_content = (SITE / "styles.css").read_bytes()
    js_content = (SITE / "theme.js").read_bytes()
    cache_v = hashlib.md5(css_content + js_content).hexdigest()[:8]

    # Read source files
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    changelog_text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    credits_text = (ROOT / "CREDITS.md").read_text(encoding="utf-8")
    privacy_text = (ROOT / "PRIVACY.md").read_text(encoding="utf-8")
    license_text = (ROOT / "LICENSE").read_text(encoding="utf-8")
    index_tpl = (SITE / "template_index.html").read_text(encoding="utf-8").replace(
        'href="styles.css"', f'href="styles.css?v={cache_v}"'
    ).replace(
        'src="theme.js"', f'src="theme.js?v={cache_v}"'
    )
    page_tpl = (SITE / "template_page.html").read_text(encoding="utf-8").replace(
        'href="styles.css"', f'href="styles.css?v={cache_v}"'
    ).replace(
        'src="theme.js"', f'src="theme.js?v={cache_v}"'
    )
    print(f"[ok] Read source files (cache bust: {cache_v})")

    # ── index.html ──
    intro_md = extract_section(readme, "intro")
    features_md = extract_section(readme, "features")
    download_md = extract_section(readme, "download")
    support_md = extract_section(readme, "support")
    attribution_md = extract_section(readme, "attribution")
    translation_md = extract_section(readme, "translation")
    disclaimer_md = extract_section(readme, "disclaimer")

    index_html = (
        index_tpl
        .replace("{{intro}}", md_inline(intro_md.replace("\n", " ").strip()))
        .replace("{{features}}", build_feature_cards(features_md))
        .replace("{{download}}", build_download_cards(download_md))
        .replace("{{support}}", build_support_html(support_md))
        .replace("{{attribution}}", build_attribution_cards(attribution_md))
        .replace("{{translation}}", build_translation_html(translation_md))
        .replace("{{disclaimer}}", build_disclaimer_html(disclaimer_md))
    )
    (DIST / "index.html").write_text(index_html, encoding="utf-8")
    print("[ok] Built index.html")

    # ── changelog.html ──
    changelog_html = build_changelog_page(changelog_text, page_tpl)
    (DIST / "changelog.html").write_text(changelog_html, encoding="utf-8")
    print("[ok] Built changelog.html")

    # ── credits.html ──
    credits_html = build_credits_page(credits_text, page_tpl)
    (DIST / "credits.html").write_text(credits_html, encoding="utf-8")
    print("[ok] Built credits.html")

    # ── privacy.html ──
    privacy_html = build_privacy_page(privacy_text, page_tpl)
    (DIST / "privacy.html").write_text(privacy_html, encoding="utf-8")
    print("[ok] Built privacy.html")

    # ── license.html ──
    license_html = build_license_page(license_text, page_tpl)
    (DIST / "license.html").write_text(license_html, encoding="utf-8")
    print("[ok] Built license.html")

    # ── robots.txt + sitemap.xml ──
    robots = "User-agent: *\nAllow: /\nSitemap: https://boxviewer.app/sitemap.xml\n"
    (DIST / "robots.txt").write_text(robots, encoding="utf-8")
    from datetime import date
    today = date.today().isoformat()
    pages = ["", "changelog.html", "credits.html", "privacy.html", "license.html"]
    urls = "\n".join(
        f"  <url><loc>https://boxviewer.app/{p}</loc><lastmod>{today}</lastmod></url>"
        for p in pages
    )
    sitemap = f'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{urls}\n</urlset>\n'
    (DIST / "sitemap.xml").write_text(sitemap, encoding="utf-8")
    print("[ok] Built robots.txt + sitemap.xml")

    print("=" * 40)
    print(f"Done! Output: {DIST}")
    print("Preview: python -m http.server 8080 --directory site/dist")


if __name__ == "__main__":
    build()
