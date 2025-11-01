# Calendar Analyzer

A CLI tool to analyze your Google Calendar and optimize your schedule for focus time.

## Features

- Fetch events from Google Calendar (where you're organizer or participant)
- Analyze multiple time ranges: tomorrow, current week, next week
- Calculate meeting statistics: count, duration, distribution
- Find longest free blocks and measure schedule fragmentation
- Identify reschedulable meetings (≤ 3 participants)
- AI-powered focus time recommendations via Koog SDK (OpenAI-compatible)
- Mark meetings as "never move"
- Plain text terminal output

## Prerequisites

1. **Java 21** or higher
2. **Google Calendar API credentials**
3. **Koog API key** (for optimization features)

## Setup

### 1. Google Calendar API

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google Calendar API
4. Create OAuth 2.0 credentials (Desktop application)
5. Download credentials JSON file
6. Place it at: `~/.calendar-analyzer/credentials.json`

### 2. Koog/OpenAI API Key

Either add to config file or set as environment variable:

**Option 1: Config file**
```json
{
  "openAIApiKey": "your-api-key-here"
}
```

**Option 2: Environment variable**
```bash
export KOOG_API_KEY="your-api-key-here"
```

## Build

Build the fat JAR with all dependencies:

```bash
./gradlew shadowJar
```

This creates `build/libs/calendar-analyzer-1.0-SNAPSHOT-all.jar` (~37MB)

## Usage

### Option 1: Using the wrapper script (recommended)

```bash
# Make script executable (first time only)
chmod +x calendar-analyzer

# List events
./calendar-analyzer list
./calendar-analyzer list -r TOMORROW
./calendar-analyzer list -r NEXT_WEEK

# Analyze calendar
./calendar-analyzer analyze
./calendar-analyzer analyze -r NEXT_WEEK
./calendar-analyzer analyze -o  # With AI optimization
./calendar-analyzer analyze -s  # Show reschedulable meetings

# Sign out from Google Calendar
./calendar-analyzer signout

# Configuration
./calendar-analyzer config show
./calendar-analyzer config never-move -e <event-id>
./calendar-analyzer config allow-move -e <event-id>
```

### Option 2: Using Java directly

```bash
java -jar build/libs/calendar-analyzer-1.0-SNAPSHOT-all.jar list
java -jar build/libs/calendar-analyzer-1.0-SNAPSHOT-all.jar analyze -o
```

### Option 3: Using Gradle

```bash
./gradlew run --args="list"
./gradlew run --args="analyze --optimize"
```

## Commands

### List Events
Display all events from your Google Calendar with details.

```bash
./calendar-analyzer list [-r TOMORROW|CURRENT_WEEK|NEXT_WEEK]
```

Shows: event title, time, duration, attendees, location, and description.

### Analyze Calendar
Analyze your calendar for meeting patterns and focus time opportunities.

```bash
./calendar-analyzer analyze [options]

Options:
  -r, --range           Time range (TOMORROW|CURRENT_WEEK|NEXT_WEEK)
  -o, --optimize        Generate AI optimization suggestions
  -s, --show-reschedulable  Show list of reschedulable meetings
```

### Sign Out
Sign out from Google Calendar to switch accounts.

```bash
./calendar-analyzer signout
```

Removes stored OAuth credentials. Next time you run a command, you'll be prompted to sign in with a different account.

### Configuration
Manage application settings and meeting restrictions.

```bash
./calendar-analyzer config show
./calendar-analyzer config never-move -e <event-id>
./calendar-analyzer config allow-move -e <event-id>
```

## Configuration

Configuration is stored at `~/.calendar-analyzer/config.json`:

```json
{
  "neverMoveEventIds": [],
  "workDayStart": 9,
  "workDayEnd": 18,
  "reschedulableThreshold": 3,
  "minFocusBlockMinutes": 180,
  "maxFocusBlockMinutes": 240,
  "openAIApiKey": "sk-..."
}
```

**Settings:**
- `neverMoveEventIds`: Array of event IDs that should never be rescheduled
- `workDayStart`/`workDayEnd`: Work hours (24-hour format)
- `reschedulableThreshold`: Max participants for a meeting to be considered reschedulable
- `minFocusBlockMinutes`/`maxFocusBlockMinutes`: Target focus time duration
- `openAIApiKey`: Your Koog/OpenAI API key (optional, can use env var instead)

## How It Works

1. **Fetches events** from your primary Google Calendar
2. **Categorizes meetings**:
   - Reschedulable: ≤ 3 participants, not marked as "never move"
   - Fixed: > 3 participants or marked as "never move"
3. **Analyzes schedule**:
   - Total meetings, average/median duration
   - Meetings per day distribution
   - Longest free blocks (sorted)
   - Fragmentation score (% of short gaps)
4. **AI Optimization** (optional):
   - Uses Koog to suggest specific meeting reschedules
   - Targets 3-4 hour focus blocks
   - Provides actionable time slot recommendations

## Example Output

```
======================================================================
CALENDAR ANALYSIS: CURRENT WEEK
======================================================================

OVERVIEW
----------------------------------------------------------------------
Total meetings:        12
Average duration:      45 minutes
Median duration:       30 minutes
Fragmentation score:   42.5%

MEETINGS PER DAY
----------------------------------------------------------------------
Mon Nov 04: 3 meetings
Tue Nov 05: 4 meetings
Wed Nov 06: 2 meetings

LONGEST FREE BLOCKS
----------------------------------------------------------------------
Tue Nov 05 09:00 - 12:30 (3h 30m)
Wed Nov 06 13:00 - 17:00 (4h 0m)

MEETING CATEGORIZATION
----------------------------------------------------------------------
Reschedulable (≤ 3 participants): 8
Fixed (> 3 participants):         4

FOCUS TIME RECOMMENDATIONS
======================================================================

1. Move "Team Sync" from Tuesday 10:00 to Tuesday 15:00 to create a 10:00-13:30 focus block
2. Combine "1:1 with Alice" and "Code Review" to Monday 16:00-17:00 to create a 09:00-12:00 focus block
3. Move "Planning Session" from Wednesday 14:00 to Friday 10:00 to extend Wednesday 13:00-17:00 focus block
```

## License

MIT
