# Calendar Analyzer - Usage Guide

## Building the Application

### Build the fat JAR (includes all dependencies):
```bash
./gradlew shadowJar
```

This creates: `build/libs/calendar-analyzer-1.0-SNAPSHOT-all.jar` (~37MB)

### Alternative: Build distribution with shell scripts:
```bash
./gradlew installDist
```

This creates: `build/install/calendar-analyzer/` with executable scripts

## Running the Application

### Option 1: Using the wrapper script (recommended):
```bash
./calendar-analyzer --help
./calendar-analyzer list
./calendar-analyzer analyze
./calendar-analyzer config show
```

### Option 2: Using Java directly:
```bash
java -jar build/libs/calendar-analyzer-1.0-SNAPSHOT-all.jar --help
```

**Note:** Requires Java 21. If you get a version error, ensure Java 21 is installed:
```bash
java -version  # Check current version
/usr/libexec/java_home -V  # List all installed versions (macOS)
```

## Available Commands

### 1. List Events
Display all events from your Google Calendar.

```bash
# List current week (default)
./calendar-analyzer list

# List tomorrow's events
./calendar-analyzer list -r TOMORROW

# List next week's events
./calendar-analyzer list -r NEXT_WEEK
```

**Output includes:**
- Event title
- Date and time
- Duration
- Number of attendees
- Location
- Description preview

### 2. Analyze Calendar
Analyze your calendar for meeting patterns and focus time opportunities.

```bash
# Basic analysis for current week
./calendar-analyzer analyze

# Analyze tomorrow
./calendar-analyzer analyze -r TOMORROW

# Analyze with AI optimization suggestions
./calendar-analyzer analyze -o

# Show reschedulable meetings
./calendar-analyzer analyze -s

# Combine options
./calendar-analyzer analyze -r NEXT_WEEK -o -s
```

**Output includes:**
- Total meetings count
- Average and median duration
- Fragmentation score
- Meetings per day
- Longest free blocks
- Reschedulable vs fixed meetings
- AI optimization suggestions (with `-o` flag)

### 3. Configuration Management

```bash
# Show current configuration
./calendar-analyzer config show

# Mark a meeting as "never move"
./calendar-analyzer config never-move -e <event-id>

# Remove "never move" restriction
./calendar-analyzer config allow-move -e <event-id>
```

## Configuration

Config file location: `~/.calendar-analyzer/config.json`

```json
{
  "neverMoveEventIds": [],
  "workDayStart": 9,
  "workDayEnd": 18,
  "reschedulableThreshold": 3,
  "minFocusBlockMinutes": 180,
  "maxFocusBlockMinutes": 240,
  "openAIApiKey": "your-api-key-here"
}
```

## First-Time Setup

### 1. Google Calendar Authentication

Place your Google OAuth credentials at:
```
~/.calendar-analyzer/credentials.json
```

Get credentials from: https://console.cloud.google.com/

On first run, the app will:
1. Open your browser for OAuth authentication
2. Save tokens to `~/.calendar-analyzer/tokens/`
3. Subsequent runs will use the saved tokens

### 2. Koog/OpenAI API Key

Either:
- Add to config file: `~/.calendar-analyzer/config.json`
- Or set environment variable: `export KOOG_API_KEY=your-key`

## Examples

```bash
# Quick check of today's schedule
./calendar-analyzer list -r TOMORROW

# Weekly planning with AI suggestions
./calendar-analyzer analyze -r NEXT_WEEK -o

# Find which meetings can be rescheduled
./calendar-analyzer analyze -s

# Full analysis with all options
./calendar-analyzer analyze -r CURRENT_WEEK -o -s
```

## Troubleshooting

### "Java version error"
The app requires Java 21. Install from: https://adoptium.net/

### "Missing credentials.json"
Get OAuth credentials from Google Cloud Console and place in:
`~/.calendar-analyzer/credentials.json`

### "KOOG_API_KEY not found"
Add your API key to the config file or set as environment variable.

### Config file parse error
Ensure `~/.calendar-analyzer/config.json` is valid JSON format.

## Distribution

To distribute the application:

```bash
# Create a ZIP distribution
./gradlew distZip

# The ZIP file will be at:
# build/distributions/calendar-analyzer-1.0-SNAPSHOT.zip
```

Unzip and run:
```bash
unzip calendar-analyzer-1.0-SNAPSHOT.zip
./calendar-analyzer-1.0-SNAPSHOT/bin/calendar-analyzer list
```
