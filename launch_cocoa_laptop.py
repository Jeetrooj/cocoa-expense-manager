#!/usr/bin/env python3
"""
Cocoa — Managing Expenses by Cocoa (Laptop & Desktop Launcher)
Starts a local offline server and opens the Cocoa Desktop & Mobile Studio in your default browser.
"""
import http.server
import os
import socketserver
import webbrowser
import threading

PORT = 8085
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "docs")

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

def open_browser():
    webbrowser.open(f"http://localhost:{PORT}/index.html")

if __name__ == "__main__":
    print("=================================================================")
    print("  Cocoa — Managing Expenses by Cocoa (100% Offline Desktop App)  ")
    print(f"  Running locally at: http://localhost:{PORT}/index.html")
    print("  Press Ctrl+C to stop.")
    print("=================================================================")
    threading.Timer(0.8, open_browser).start()
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down Cocoa local server.")
