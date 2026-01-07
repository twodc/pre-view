---
name: codebase-analysis-web-report
description: Generates a self-contained interactive web report that analyzes a codebase: summarizes purpose, documents architecture and call/flow graphs, produces interactive diagrams and search, and suggests improvements. Triggered by requests to analyze a repository, specific project directories, or uploaded source files.
---

# Skill purpose

This Skill analyzes a codebase or selected project files and produces a self-contained HTML web report that documents: a high-level summary, module/class/function inventories, control and data flow diagrams, an interactive navigable diagram (click to view details), a searchable index, and a concise list of recommended improvements.

It is intended for developers who want a quick, navigable overview of how a program works (especially web apps / frontend-backend services) and an actionable set of notes for maintenance or refactoring.

# Step-by-step instructions (what Claude must do)

1. Input intake
   - If the user supplies a repository URL, clone or fetch files as permitted by environment. If user supplies a set of files or a directory path, read those files.
   - If the user specifies languages or frameworks (e.g., React, Node, Flask), prioritize extracting framework-specific structure (routes, components, services).
   - If the codebase is large, ask the user to scope (directories, languages, or target entry points). Use a reasonable default scope: top-level src/ or app/ directories.

2. Static analysis and metadata extraction
   - Parse files by language with heuristic parsing (AST where available) to extract: modules/packages, classes, functions, public APIs, route endpoints, components, configuration files, and key resource files.
   - Extract call relationships and dependencies: function A calls B, module imports, HTTP endpoints calling services, DB access points, and configuration-driven wiring.
   - Collect metrics for each file/module: lines of code (LOC), number of functions/classes, cyclomatic complexity estimate (basic heuristics), and last modification time if available.
   - Identify entry points (e.g., main server file, index.html, React root) and external integrations (DBs, queues, third-party APIs).

3. Control / data-flow graph construction
   - Build a directed graph representing major flows: frontend user event -> component -> API call -> backend handler -> DB. Represent nodes for modules/classes/functions and edges for calls or data flows.
   - Group nodes by logical layers (frontend, backend, services, data-storage) and apply clustering for readability.
   - Generate both a coarse-grained graph (modules/services) and a fine-grained graph (functions/call graph) where feasible.

4. Textual report generation
   - Produce a concise high-level summary of purpose: inferred application domain, main features, and primary modules.
   - For each major module/group include: short description, list of exported functions/classes with one-line docstrings (or inferred summary), key callers/callees, and metrics.
   - Create a findings section that lists potential issues and recommended improvements (e.g., complex functions, tightly coupled modules, missing tests, insecure patterns, large files to refactor). Prioritize recommendations by likely impact and estimated effort.

5. Interactive web report assembly
   - Generate a single self-contained HTML (or HTML+assets) report that includes:
     - Landing summary with quick stats and search bar.
     - Navigable file/module index with previews and clickable items.
     - Interactive diagrams (SVG or embedded graph library like Mermaid or Cytoscape.js) that allow clicking a node to show details and source excerpts.
     - Search and filter by filename, symbol name, or text.
     - For each item, include quick links to the original source file content and highlighted code excerpts.
     - Export/download buttons (e.g., download JSON analysis or the HTML report).
   - Ensure generated HTML is static and self-contained wherever possible (embed CSS/JS) so it can be opened locally without a server.

6. Output and follow-up
   - Provide the user with the generated report file(s) and a short summary of key findings in plain text.
   - Offer follow-up actions: narrow analysis scope, run deeper dynamic analysis suggestions, or generate a prioritized refactor plan.

# Implementation notes and heuristics

- When parsing languages with available AST libraries, prefer AST extraction for higher accuracy; for others, use robust regex heuristics.
- If repository is large (>1000 files), automatically summarize by sampling or request user to restrict scope; provide an option to produce only the coarse-grained module-level report first.
- When detecting React frontends, surface component trees, props flow, and common routing files (react-router). For Node/Flask backends, identify route handlers, middleware, and DB access layers.
- For interactive diagrams, prefer a simple embeddable library (Mermaid or Cytoscape.js). If embedding a library is not permitted, generate SVG diagrams with clickable regions and embedded metadata.

# Usage examples

- Example 1: "Analyze my repository at https://... and produce an interactive HTML report focusing on src/backend and src/frontend."
  - The Skill clones the repo, extracts structure for those directories, builds module-level and function-level graphs, and returns report.zip containing report.html and assets.

- Example 2: "Upload these files from my Flask app — I want an overview of routes, DB calls, and recommended refactors."
  - The Skill parses Python files, lists route endpoints and their handlers, surfaces DB interactions, computes complexity hotspots, and produces the interactive report.

- Example 3: "Scan this React project and give an interactive component tree with click-to-view source and suggestions to reduce bundle size."
  - The Skill extracts component import graph, flags large components and duplicated code, and provides interactive visualization and suggestions.

# Best practices for users

- Provide a clear scope for large repositories (specific directories, languages, or entry points) to get faster, more focused results.
- Provide credentials or sample env configs if analysis must detect runtime wiring (e.g., env-based service URLs). Do not provide secrets; prefer sanitized samples.
- For better accuracy, include tests or example input that reveals runtime behavior, or request dynamic analysis as a follow-up.
- If you need continuous integration, run the Skill regularly on PRs or commits and store the generated report artifacts.

# Output files and links

- Primary output: report.html (self-contained interactive web report) and optional report.zip if multiple asset files are required.
- Optional outputs: analysis.json (machine-readable analysis), findings.txt (concise recommendations).

# When to ask clarifying questions

- If the repository is very large or contains multiple unrelated projects, ask the user to select directories or services to analyze.
- If the user expects runtime-only behavior (e.g., dynamic code generation), inform them static analysis has limits and offer dynamic analysis suggestions.

# Related scripts and integration tips

- If automation is desired, integrate this Skill into CI by running it against the checked-out repo and storing report.html as a build artifact.
- For embedding in documentation sites, host the generated report or link to the static HTML in the docs portal.

