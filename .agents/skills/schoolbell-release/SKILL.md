---
name: schoolbell-release
description: Publishes a new release of the School Bell application. Automates version bumping, compiling, packaging, updating README.md, updates.json, creating GitHub release, and pushing changes.
---

# School Bell Release & Publication Skill

## Overview
This skill guides the AI agent through publishing a new version of the School Bell application. It wraps the execution of `publish_release.ps1` and ensures all checks, version bumps, and documentation updates are performed correctly.

## Pre-requisites
1. **GitHub CLI (`gh`):** Must be installed and authorized (`gh auth status`).
2. **Clean Git Status:** Make sure all code features are committed before starting the release process.

## Step-by-Step Release Workflow

1. **Verify Git Status:**
   - Run `git status` to ensure all main code changes (except configuration files updated during release) are committed.

2. **Align on Version and Changelog:**
   - Identify the release version (e.g., `1.2.0`).
   - Draft a comprehensive changelog in Ukrainian, summarizing all new features, improvements, and bug fixes since the last release.

3. **Run the Release Script:**
   - Execute the publishing script from the project root:
     ```powershell
     ./publish_release.ps1 -Version "<version>" -Changelog "<changelog_text>"
     ```

4. **Verify Automated Changes:**
   - The script automatically updates versions in the following files:
     - `src/main/java/com/schoolbell/MainApp.java` (VERSION variable)
     - `pom.xml` (Maven version)
     - `build_dist.ps1` ($VERSION and $MAIN_JAR variables)
     - `schoolbell_installer.iss` (Inno Setup version definition)
     - `README.md` (updates installer name and download links)
     - `updates.json` (updates OTA update manifest with version, checksum, and date)
   - It also compiles the project, runs Inno Setup, computes the SHA-256 hash, creates the GitHub release, uploads the executable, and commits/pushes the changes to the current git branch.

5. **Verify GitHub Release:**
   - Run `gh release list` or view the repository releases page on GitHub to confirm the release is published and the installer is attached.
   - Verify that the OTA update manifest `updates.json` is updated and pushed.
