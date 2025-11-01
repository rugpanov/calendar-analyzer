# GitHub Setup Instructions

## Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `calendar-analyzer`
3. Description: `AI-powered calendar analysis tool using Koog SDK for focus time optimization`
4. Choose visibility: Public or Private
5. **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

## Push to GitHub

After creating the repository on GitHub, run these commands:

```bash
# Add GitHub remote
git remote add origin https://github.com/YOUR_USERNAME/calendar-analyzer.git

# Or using SSH (recommended if you have SSH keys set up)
git remote add origin git@github.com:YOUR_USERNAME/calendar-analyzer.git

# Push to GitHub
git push -u origin main
```

## Verify

Check your repository at:
```
https://github.com/YOUR_USERNAME/calendar-analyzer
```

## Optional: Add Topics/Tags

On GitHub repository page, click "⚙️" (settings icon) next to "About" and add topics:
- `kotlin`
- `calendar`
- `ai`
- `productivity`
- `google-calendar`
- `koog`
- `cli-tool`
- `focus-time`

## Repository Structure

```
calendar-analyzer/
├── src/
│   └── main/kotlin/dev/grigri/
│       ├── analysis/         # Meeting analysis logic
│       ├── calendar/         # Google Calendar integration
│       ├── cli/             # CLI commands and output formatting
│       ├── config/          # Configuration management
│       └── llm/             # Koog AI integration
├── build.gradle.kts         # Build configuration
├── calendar-analyzer        # Wrapper script for easy execution
├── README.md               # Main documentation
├── USAGE.md                # Detailed usage guide
├── LICENSE                 # MIT License
└── .gitignore             # Git ignore rules
```

## Next Steps

After pushing to GitHub, you may want to:

1. **Add GitHub Actions** for CI/CD (optional)
2. **Create Releases** with pre-built JARs
3. **Add Contributing Guidelines** if accepting contributions
4. **Set up GitHub Issues** templates
5. **Add badges** to README (build status, license, etc.)

## Example README Badges

Add these to your README.md if desired:

```markdown
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue?logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-green)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
```
