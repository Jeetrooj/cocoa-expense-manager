#!/usr/bin/env bash
cd "$(dirname "$0")"
if command -v python3 >/dev/null 2>&1; then
    python3 launch_cocoa_laptop.py
else
    open docs/index.html || xdg-open docs/index.html
fi
