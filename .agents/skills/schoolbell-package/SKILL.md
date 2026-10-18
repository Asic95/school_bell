---
name: schoolbell-package
description: Rebuilds and packages the School Bell project into a Windows EXE (app-image). Use when asked to "rebuild", "package", or "update distribution".
---

# School Bell Packaging Skill

## Overview
This skill automates the multi-step process of compiling the School Bell application and packaging it into a self-contained Windows executable (EXE) with its own Java runtime.

## Workflow
When triggered, follow these steps:
1. Run the `scripts/package.ps1` PowerShell script.
2. Report the outcome to the user.
3. Remind the user that the distribution is located in the `dist/SchoolBell` folder.

## Resources
- **scripts/package.ps1**: The primary automation script.
