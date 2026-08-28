#!/bin/bash
# Bulk-fetch all developer.gnome.org subpages + convert to Markdown.
#
# Source domains:
#   - developer.gnome.org/hig/*              (Human Interface Guidelines, ~56 pages)
#   - developer.gnome.org/documentation/*    (tutorials, guidelines, ~81 pages)
#
# Output: ~/gtk4kt/docs/gnome-dev-docs/<path>.md
#
# Tools used:
#   - curl       (HTML fetch with Mozilla UA — required, GNOME blocks default curl UA)
#   - python3+BeautifulSoup  (HTML → Markdown; pandoc is heavier and may not be installed)
set -uo pipefail

REPO=~/gtk4kt
OUT="$REPO/docs/gnome-dev-docs"
mkdir -p "$OUT"

UA="Mozilla/5.0 (X11; Linux x86_64; rv:115.0) Gecko/20100101 Firefox/115.0"

collect_links() {
  local index_url=$1
  # Resolve relative URLs against the index URL.
  python3 - "$index_url" << 'PYEOF'
import sys, re
from urllib.parse import urljoin, urlparse

index_url = sys.argv[1]
try:
    from bs4 import BeautifulSoup
except ImportError:
    sys.exit(2)
import urllib.request
req = urllib.request.Request(index_url, headers={"User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:115.0) Gecko/20100101 Firefox/115.0"})
html = urllib.request.urlopen(req, timeout=15).read().decode("utf-8", "replace")
soup = BeautifulSoup(html, "html.parser")
seen = set()
for a in soup.select("a[href]"):
    href = a.get("href", "")
    if href.startswith(("#", "mailto:", "tel:", "javascript:")):
        continue
    if "_static" in href or "genindex" in href or "search.html" in href:
        continue
    # Resolve relative
    abs_url = urljoin(index_url, href)
    parsed = urlparse(abs_url)
    if parsed.netloc != "developer.gnome.org":
        continue
    path = parsed.path.lstrip("/")
    if not path.endswith(".html"):
        continue
    if path not in seen:
        seen.add(path)
        print(path)
PYEOF
}

fetch_index() {
  local url=$1
  curl -fsSL -A "$UA" "$url" 2>/dev/null
}

# ─── Collect link lists from index pages ─────────────────────────────────────
echo "=== Collecting subpage links ==="
{
  collect_links "https://developer.gnome.org/hig/"
  collect_links "https://developer.gnome.org/documentation/"
  # Also fetch index.html and tutorials.html to cover any subpages not linked from main
  collect_links "https://developer.gnome.org/hig/index.html"
  collect_links "https://developer.gnome.org/documentation/tutorials.html"
  collect_links "https://developer.gnome.org/documentation/introduction/builder.html"
} | sort -u > /tmp/gnome-doc-links.txt

# Add the entry-point pages themselves
{
  echo "hig/index.html"
  echo "documentation/index.html"
  echo "documentation/tutorials.html"
  echo "documentation/introduction/builder.html"
} >> /tmp/gnome-doc-links.txt
sort -u -o /tmp/gnome-doc-links.txt /tmp/gnome-doc-links.txt

TOTAL=$(wc -l < /tmp/gnome-doc-links.txt)
echo "Found $TOTAL unique subpages"
echo "First 10:"
head -10 /tmp/gnome-doc-links.txt
echo "Last 5:"
tail -5 /tmp/gnome-doc-links.txt

# ─── HTML → Markdown converter ──────────────────────────────────────────────
cat > /tmp/html_to_md.py << 'PYEOF'
#!/usr/bin/env python3
"""Minimal HTML → Markdown converter for Sphinx-style docs.gtk.org pages.

Handles:
  - title, h1-h6, p, ul/ol/li, code/pre, strong/em, a, blockquote, table
  - strips nav/footer/sidebar, fixes relative URLs
"""
import sys, re
from html.parser import HTMLParser

try:
    from bs4 import BeautifulSoup
    HAVE_BS4 = True
except ImportError:
    HAVE_BS4 = False

if not HAVE_BS4:
    sys.stderr.write("ERROR: python -m pip install beautifulsoup4 needed\n")
    sys.exit(2)

url = sys.argv[1] if len(sys.argv) > 1 else ""
html = sys.stdin.read()
soup = BeautifulSoup(html, "html.parser")

# Strip non-content
for sel in ["nav", "footer", "script", "style", "header.navigation",
            "div[role='navigation']", "div.sidebar", "div.related",
            "form.search", "div.sphinxsidebar", "div.right-sidebar"]:
    for el in soup.select(sel):
        el.decompose()

# Find main content
main = soup.select_one("main") or soup.select_one("div[role='main']") \
       or soup.select_one("article") or soup.select_one("div.body") \
       or soup.body or soup

def fix_url(href):
    if not href or href.startswith(("http://", "https://", "mailto:", "#", "tel:")):
        return href
    # Resolve relative to base URL
    from urllib.parse import urljoin
    return urljoin(url, href)

# Title
title_el = soup.select_one("title") or soup.select_one("h1")
title = title_el.get_text(strip=True) if title_el else ""

# Make relative links absolute
for a in main.select("a[href]"):
    a["href"] = fix_url(a.get("href", ""))
for img in main.select("img[src]"):
    img["src"] = fix_url(img.get("src", ""))

# Convert simple table to MD
def table_to_md(table):
    rows = []
    for tr in table.find_all("tr"):
        cells = [c.get_text(" ", strip=True).replace("\n", " ").replace("|", "\\|")
                 for c in tr.find_all(["th", "td"])]
        rows.append("| " + " | ".join(cells) + " |")
    if not rows: return ""
    # Build alignment from first row if it uses <th>
    first = rows[0]
    n = first.count("|") - 1
    align = "|".join(["---"] * n)
    out = [first, "|" + align + "|"] + rows[1:]
    return "\n\n" + "\n".join(out) + "\n\n"

# Walk top-level children of main, emit MD
out = []
if title:
    out.append(f"# {title}\n\n")

def emit(node):
    if not node: return
    name = getattr(node, "name", None)
    if name is None:
        return
    if name in ("script", "style", "nav", "footer", "form"):
        return
    txt = node.get_text(" ", strip=True) if hasattr(node, "get_text") else ""

    if name in ("h1", "h2", "h3", "h4", "h5", "h6"):
        level = int(name[1])
        out.append("\n\n" + "#" * level + " " + txt + "\n\n")
    elif name == "p":
        out.append("\n\n" + txt + "\n\n")
    elif name == "pre":
        code = node.get_text("\n", strip=False).rstrip()
        out.append("\n\n```\n" + code + "\n```\n\n")
    elif name in ("ul", "ol"):
        for li in node.find_all("li", recursive=False):
            out.append("\n- " + li.get_text(" ", strip=True))
        out.append("\n")
    elif name == "table":
        out.append(table_to_md(node))
    elif name == "blockquote":
        out.append("\n\n> " + txt.replace("\n", "\n> ") + "\n\n")
    elif name == "code":
        out.append("`" + txt + "`")
    elif name == "strong" or name == "b":
        out.append("**" + txt + "**")
    elif name == "em" or name == "i":
        out.append("*" + txt + "*")
    elif name == "a":
        href = node.get("href", "")
        out.append(f"[{txt}]({href})")
    elif name == "br":
        out.append("\n")
    elif name == "img":
        alt = node.get("alt", "")
        src = node.get("src", "")
        out.append(f"![{alt}]({src})")
    elif name in ("div", "section", "article"):
        for c in node.children:
            emit(c)
    else:
        # Fallback: recurse
        for c in node.children:
            emit(c)

for c in main.children:
    emit(c)

md = "".join(out)
# Collapse 3+ blank lines
md = re.sub(r"\n{3,}", "\n\n", md)
sys.stdout.write(md.strip() + "\n")
PYEOF

# ─── Fetch + convert each page ──────────────────────────────────────────────
echo ""
echo "=== Fetching + converting pages ==="
OK=0; FAIL=0
while IFS= read -r path; do
  url="https://developer.gnome.org/$path"
  out_path="$OUT/${path%.html}.md"
  mkdir -p "$(dirname "$out_path")"
  if curl -fsSL -A "$UA" "$url" 2>/dev/null \
     | python3 /tmp/html_to_md.py "$url" > "$out_path" 2>/dev/null; then
    OK=$((OK+1))
  else
    FAIL=$((FAIL+1))
    echo "  FAIL: $url"
  fi
done < /tmp/gnome-doc-links.txt

echo ""
echo "=== Summary ==="
echo "OK:    $OK"
echo "FAIL:  $FAIL"
echo "Total: $((OK+FAIL))"
echo "Output: $OUT"
echo "Markdown file count:"
find "$OUT" -name '*.md' | wc -l