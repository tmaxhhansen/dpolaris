# dPolaris Java App (Cross-Platform)

This is a Java desktop client for dPolaris that runs on macOS and Windows.

It communicates with the existing Python backend (`dpolaris_ai`) over HTTP.

## What it includes

- Backend connection check (`/health`)
- Backend lifecycle control from Java UI:
  - start/stop AI backend process (`python -m cli.main server`)
  - start/stop scheduler daemon (`/api/scheduler/start`, `/api/scheduler/stop`)
  - live selectable backend logs
- Stable model training (XGBoost)
- Deep learning training jobs (LSTM / Transformer) with live polling logs
- Selectable/copyable training logs
- Dashboard for:
  - model prediction direction/confidence
  - trade setup with entry zone, stop, targets, and suggested sizing
- Backend data views for:
  - `/api/status`
  - `/api/models`
  - `/api/memories`
  - memory distribution summary by category

## Prerequisites

- Java 17+ installed
- Python backend project available locally at `~/my-git/dpolaris_ai`

## Quick Start (macOS)

### Run Full App
```bash
cd ~/my-git/dpolaris
./gradlew run
```

### Run Control Center (Backend + Deep Learning focused)
```bash
cd ~/my-git/dpolaris
./gradlew run -PmainClass=com.dpolaris.javaapp.ControlCenterLauncher
```

Or use the shell scripts:
```bash
./run.sh
```

## Features

### Backend Control (One-Click)
- **Start Backend**: Launches `python -m cli.main server --host 127.0.0.1 --port 8420`
- **Stop Backend**: Terminates the process Java started
- **Reset & Restart (Clean)**: Stops, waits for port free, restarts, verifies `/health` within 30s
- Device selection: `auto` / `cpu` / `mps` (Apple Silicon) / `cuda`

### Deep Learning Training
- Universe tabs (NASDAQ 300 / WSB 100 / Combined) backed by:
  - `nasdaq300`
  - `wsb100`
  - `combined`
- Ticker table with filter (calls `/api/universe/{name}`)
- Train button: POST `/api/jobs/deep-learning/train` with symbol, model, epochs
- Job monitor: polls GET `/api/jobs/{id}` every 2s while running
- Log viewer: shows last N lines from job logs
- Predict button: calls `/api/deep-learning/predict/{symbol}`

### Universe Tab Verification (macOS)

1. Start backend:
   - `cd ~/my-git/dPolaris_ai && ./.venv/bin/python -m cli.main server --host 127.0.0.1 --port 8420`
2. Verify backend universes:
   - `curl http://127.0.0.1:8420/api/universe/list`
   - `curl http://127.0.0.1:8420/api/universe/nasdaq300`
3. Open Deep Learning view in Java app:
   - NASDAQ tab should populate immediately.
   - Switching tabs should be instant and keep checkbox selection per tab.

### Diagnostics
- Backend process status (state, PID, uptime)
- API health check
- Deep learning device info
- System information (OS, Java, memory)

## Run in IntelliJ

1. Open `~/my-git/dpolaris` as a project.
2. IntelliJ will detect Gradle (`build.gradle`). Click **Load Gradle Project**.
3. Run either:
   - Gradle task: `application > run`, or
   - Main class: `com.dpolaris.javaapp.DPolarisJavaApp` (full app), or
   - Main class: `com.dpolaris.javaapp.ControlCenterLauncher` (control center)

Then in the app:

1. Open **AI Management** from the side menu.
2. In **Backend Control**, confirm **AI Path** and click **Start AI Backend**.
3. In **Training**, train your models and watch logs.
4. Open **Dashboard** from the side menu to view prediction + entry/stop/targets.

## Run (Windows)

```bat
run.bat
```

## Build runnable JAR

### macOS/Linux

```bash
./build.sh
java -jar build/dpolaris-java-app.jar
```

### Windows

```bat
build.bat
java -jar build\dpolaris-java-app.jar
```

## Package As Native macOS App

Create a `.app` bundle:

```bash
./package-mac-app.sh
```

Output:
- `build/macos-app/dPolarisJava.app`

Create a `.dmg` installer:

```bash
./package-mac-dmg.sh
```

Output:
- `build/macos-dmg/dPolarisJava-1.0.0.dmg`

## Architecture

```
src/main/java/com/dpolaris/javaapp/
├── DPolarisJavaApp.java          # Full application main class
├── ControlCenterLauncher.java    # Standalone control center launcher
├── BackendProcessManager.java    # Direct process lifecycle management
├── BackendControlPanel.java      # Backend control UI panel
├── DeepLearningPanel.java        # Deep learning training UI
├── ApiClient.java                # HTTP client for backend API
└── ...
```
