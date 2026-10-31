@echo off
title Cocoa — Managing Expenses by Cocoa (Desktop Launcher)
echo Launching Cocoa Offline Financial Vault on your laptop...
where python >nul 2>nul
if %errorlevel%==0 (
    python launch_cocoa_laptop.py
) else (
    start "" "%~dp0docs\index.html"
)
