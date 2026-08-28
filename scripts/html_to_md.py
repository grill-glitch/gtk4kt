#!/usr/bin/env python3
"""HTML → Markdown converter for Sphinx-style docs.gtk.org / developer.gnome.org pages.

Handles:
  - title, h1-h6, p, ul/ol/li, code/pre, strong/em, a, blockquote, table
  - strips nav/footer/sidebar, fixes relative URLs, drops Sphinx ¶ permalinks
"""
import sys, re
from urllib.parse import urljoin

try:
    from bs4 import BeautifulSoup
except ImportError:
    sys.stderr.write("ERROR: pip install beautifulsoup4\n")
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

def fix_url(href):
    if not href or href.startswith(("http://", "https://", "mailto:", "#", "tel:")):
        return href
    return urljoin(url, href)

# Title (strip " - GNOME Developer Documentation" suffix and the Sphinx ¶ marker)
title = ""
title_el = soup.select_one("title") or soup.select_one("h1")
if title_el:
    title = title_el.get_text(strip=True)
    title = re.sub(r"\s*[-–—]\s*GNOME\s+(Developer\s+Documentation|Human\s+Interface\s+Guidelines).*$", "", title)
    title = title.replace("¶", "").strip()

# Find main content
main = soup.select_one("main") or soup.select_one("div[role='main']") \
       or soup.select_one("article") or soup.select_one("div.body") \
       or soup.body or soup

# Strip Sphinx heading permalink markers
for a in main.select("a.headerlink"):
    a.decompose()
for h in main.find_all(["h1", "h2", "h3", "h4", "h5", "h6"]):
    for c in list(h.children):
        if getattr(c, "name", None) is None and "¶" in c:
            c.replace_with(c.replace("¶", ""))

# Drop the document's own H1 (we already emit a top-level heading from <title>)
first_h1 = main.find("h1")
if first_h1:
    first_h1.decompose()

# Resolve relative URLs
for a in main.select("a[href]"):
    a["href"] = fix_url(a.get("href", ""))
for img in main.select("img[src]"):
    img["src"] = fix_url(img.get("src", ""))

def table_to_md(table):
    rows = []
    for tr in table.find_all("tr"):
        cells = [c.get_text(" ", strip=True).replace("\n", " ").replace("|", "\\|")
                 for c in tr.find_all(["th", "td"])]
        rows.append("| " + " | ".join(cells) + " |")
    if not rows: return ""
    n = rows[0].count("|") - 1
    align = "|" + "|".join(["---"] * n) + "|"
    return "\n\n" + "\n".join([rows[0], align] + rows[1:]) + "\n\n"

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
        for c in node.children:
            emit(c)

for c in main.children:
    emit(c)

md = "".join(out)
md = re.sub(r"\n{3,}", "\n\n", md)
sys.stdout.write(md.strip() + "\n")