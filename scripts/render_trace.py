#!/usr/bin/env python3
"""Render a live NexusOps trace endpoint as a terminal-style PNG."""

import argparse
import json
import textwrap
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def font(size: int):
    candidates = [
        "C:/Windows/Fonts/consola.ttf",
        "C:/Windows/Fonts/msyh.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--request-id", required=True)
    parser.add_argument("--output", default="docs/assets/agent-trace-example.png")
    args = parser.parse_args()

    url = f"{args.base_url.rstrip('/')}/trace/tool/{args.request_id}"
    with urllib.request.urlopen(url, timeout=10) as response:
        status = response.status
        payload = json.load(response)

    raw_lines = json.dumps(payload, ensure_ascii=False, indent=2).splitlines()
    lines = []
    for line in raw_lines:
        lines.extend(textwrap.wrap(line, width=100, subsequent_indent="    ") or [""])

    body_font = font(22)
    title_font = font(23)
    line_height = 34
    width = 1500
    height = 92 + line_height * len(lines) + 42
    image = Image.new("RGB", (width, height), "#0b1220")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, width - 1, height - 1), radius=18, outline="#334155", width=2)
    draw.rectangle((0, 0, width, 72), fill="#111c2f")
    for x, color in ((34, "#fb7185"), (60, "#fbbf24"), (86, "#4ade80")):
        draw.ellipse((x - 8, 28, x + 8, 44), fill=color)
    draw.text((120, 22), f"NexusOps · GET /trace/tool/{args.request_id} · HTTP {status}", font=title_font, fill="#93c5fd")
    y = 92
    for line in lines:
        draw.text((42, y), line, font=body_font, fill="#d7e0ee")
        y += line_height

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, optimize=True)
    print(f"Rendered {url} -> {output} ({width}x{height})")


if __name__ == "__main__":
    main()
