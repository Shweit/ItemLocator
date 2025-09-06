#!/bin/bash

# Script to rebase all release/* branches on master and create GitHub releases
# Author: MinecraftServerAPI Team
# Date: 2025-09-06

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions - using printf instead of echo -e to avoid -e issues
log_info() {
    printf "${BLUE}[INFO]${NC} %s\n" "$1"
}

log_success() {
    printf "${GREEN}[SUCCESS]${NC} %s\n" "$1"
}

log_warning() {
    printf "${YELLOW}[WARNING]${NC} %s\n" "$1"
}

log_error() {
    printf "${RED}[ERROR]${NC} %s\n" "$1"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ] || [ ! -d ".git" ]; then
    log_error "This script must be run from the MinecraftServerAPI project root directory!"
    exit 1
fi

# Check if gh (GitHub CLI) is installed
if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI (gh) is not installed. Please install it with: brew install gh"
    exit 1
fi

# Check if we're authenticated with GitHub
if ! gh auth status &> /dev/null; then
    log_error "Not authenticated with GitHub. Please run 'gh auth login'."
    exit 1
fi

# Save current branch
CURRENT_BRANCH=$(git branch --show-current)
log_info "Current branch: $CURRENT_BRANCH"

# Ensure working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    log_error "Working directory is not clean. Please commit or stash your changes."
    exit 1
fi

# Fetch latest changes
log_info "Fetching latest changes from remote..."
git fetch --all --prune > /dev/null 2>&1

# Switch to master and update
log_info "Switching to master branch..."
git checkout master > /dev/null 2>&1
git pull origin master > /dev/null 2>&1

# Get all release/* branches
RELEASE_BRANCHES=$(git branch -r | grep 'origin/release/' | sed 's/origin\///' | sort -V)

if [ -z "$RELEASE_BRANCHES" ]; then
    log_warning "No release/* branches found!"
    git checkout "$CURRENT_BRANCH" > /dev/null 2>&1
    exit 0
fi

log_info "Found release branches:"
echo "$RELEASE_BRANCHES" | while read -r branch; do
    printf "  - %s\n" "$branch"
done

# Arrays for success and failure tracking
SUCCESSFUL_REBASES=()
FAILED_REBASES=()
SUCCESSFUL_RELEASES=()
FAILED_RELEASES=()

# Rebase all release branches
printf "\n"
log_info "=== PHASE 1: Rebase all release branches on master ==="
printf "\n"

for branch in $RELEASE_BRANCHES; do
    log_info "Processing branch: $branch"
    
    # Checkout the branch
    if git checkout "$branch" > /dev/null 2>&1 || git checkout -b "$branch" "origin/$branch" > /dev/null 2>&1; then
        
        # Try rebase
        log_info "Rebasing $branch on master..."
        if git rebase master > /dev/null 2>&1; then
            log_success "Rebase of $branch successful!"
            
            # Force push (since we rebased)
            log_info "Pushing $branch to remote..."
            if git push --force-with-lease origin "$branch" > /dev/null 2>&1; then
                log_success "Push of $branch successful!"
                SUCCESSFUL_REBASES+=("$branch")
            else
                # Show error output when push fails
                log_error "Push of $branch failed!"
                git push --force-with-lease origin "$branch" 2>&1 | sed 's/^/  /'
                FAILED_REBASES+=("$branch")
                git rebase --abort > /dev/null 2>&1 || true
            fi
        else
            # Show error output when rebase fails
            log_error "Rebase of $branch failed! Skipping..."
            git rebase master 2>&1 | head -10 | sed 's/^/  /'
            FAILED_REBASES+=("$branch")
            git rebase --abort > /dev/null 2>&1 || true
        fi
    else
        log_error "Could not checkout branch $branch!"
        FAILED_REBASES+=("$branch")
    fi
    
    printf "\n"
done

# Switch back to master for release creation
git checkout master > /dev/null 2>&1

printf "\n"
log_info "=== PHASE 2: Create GitHub Releases ==="
printf "\n"

# Create releases for successfully rebased branches
for branch in "${SUCCESSFUL_REBASES[@]}"; do
    # Extract version from branch name (e.g. release/1.19.4 -> 1.19.4)
    VERSION=$(echo "$branch" | sed 's/release\///')
    TAG_NAME="v$VERSION"
    
    log_info "Creating release for $branch (Tag: $TAG_NAME)..."
    
    # Checkout branch for release
    git checkout "$branch" > /dev/null 2>&1
    
    # Get latest commit message for release notes
    LAST_COMMIT=$(git log -1 --pretty=format:"%s")
    
    # Generate release notes
    RELEASE_NOTES="## MinecraftServerAPI v$VERSION

### 🎮 Minecraft Version
Supports Minecraft $VERSION

### 📦 Changes
- Rebased on latest master branch
- Includes all current features and bugfixes

### 🔄 Latest Change
$LAST_COMMIT

### 📥 Installation
Download the JAR file and place it in your Minecraft server's \`plugins\` folder.

### 📖 Documentation
See [README.md](https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/blob/$branch/README.md) for full documentation.

---
*Automatically generated on $(date '+%Y-%m-%d %H:%M:%S')*"
    
    # Create tag if not exists
    if ! git tag | grep -q "^$TAG_NAME$"; then
        log_info "Creating tag $TAG_NAME..."
        git tag "$TAG_NAME" > /dev/null 2>&1
        git push origin "$TAG_NAME" > /dev/null 2>&1
    else
        log_warning "Tag $TAG_NAME already exists, updating..."
        git tag -f "$TAG_NAME" > /dev/null 2>&1
        git push origin -f "$TAG_NAME" > /dev/null 2>&1
    fi
    
    # Build the project
    log_info "Building JAR file for $VERSION..."
    if mvn clean package -DskipTests > /dev/null 2>&1; then
        JAR_FILE=$(find target -name "MinecraftServerAPI-*.jar" | head -1)
        
        if [ -f "$JAR_FILE" ]; then
            # Create or update release
            log_info "Creating GitHub Release..."
            
            # Check if release already exists
            if gh release view "$TAG_NAME" > /dev/null 2>&1; then
                log_warning "Release $TAG_NAME already exists, deleting and recreating..."
                gh release delete "$TAG_NAME" --yes > /dev/null 2>&1
            fi
            
            # Create new release (don't use --draft to ensure workflow triggers)
            if gh release create "$TAG_NAME" \
                --title "MinecraftServerAPI v$VERSION" \
                --notes "$RELEASE_NOTES" \
                --target "$branch" \
                --latest=false \
                "$JAR_FILE#MinecraftServerAPI-$VERSION.jar" > /dev/null 2>&1; then
                
                log_success "Release $TAG_NAME successfully created!"
                
                # Trigger the release workflow manually since gh CLI doesn't trigger it automatically
                log_info "Triggering release workflow for $TAG_NAME..."
                if gh workflow run release.yml -f release_tag="$TAG_NAME" --ref master > /dev/null 2>&1; then
                    log_success "Release workflow triggered for $TAG_NAME"
                else
                    log_warning "Could not trigger release workflow for $TAG_NAME (manual trigger may be needed)"
                fi
                
                SUCCESSFUL_RELEASES+=("$VERSION")
            else
                log_error "Failed to create release $TAG_NAME!"
                # Show error output when release creation fails
                gh release create "$TAG_NAME" \
                    --title "MinecraftServerAPI v$VERSION" \
                    --notes "$RELEASE_NOTES" \
                    --target "$branch" \
                    --latest=false \
                    "$JAR_FILE#MinecraftServerAPI-$VERSION.jar" 2>&1 | sed 's/^/  /'
                FAILED_RELEASES+=("$VERSION")
            fi
        else
            log_error "JAR file not found for $VERSION!"
            FAILED_RELEASES+=("$VERSION")
        fi
    else
        log_error "Build failed for $VERSION!"
        # Show Maven error output
        mvn clean package -DskipTests 2>&1 | tail -20 | sed 's/^/  /'
        FAILED_RELEASES+=("$VERSION")
    fi
    
    printf "\n"
done

# Back to original branch
log_info "Switching back to $CURRENT_BRANCH..."
git checkout "$CURRENT_BRANCH" > /dev/null 2>&1

# Summary
printf "\n"
printf "========================================\n"
printf "                SUMMARY                 \n"
printf "========================================\n"
printf "\n"

if [ ${#SUCCESSFUL_REBASES[@]} -gt 0 ]; then
    log_success "Successfully rebased branches (${#SUCCESSFUL_REBASES[@]}):"
    for branch in "${SUCCESSFUL_REBASES[@]}"; do
        printf "  ✅ %s\n" "$branch"
    done
fi

if [ ${#FAILED_REBASES[@]} -gt 0 ]; then
    printf "\n"
    log_error "Failed rebases (${#FAILED_REBASES[@]}):"
    for branch in "${FAILED_REBASES[@]}"; do
        printf "  ❌ %s\n" "$branch"
    done
fi

if [ ${#SUCCESSFUL_RELEASES[@]} -gt 0 ]; then
    printf "\n"
    log_success "Successfully created releases (${#SUCCESSFUL_RELEASES[@]}):"
    for version in "${SUCCESSFUL_RELEASES[@]}"; do
        printf "  ✅ v%s\n" "$version"
    done
fi

if [ ${#FAILED_RELEASES[@]} -gt 0 ]; then
    printf "\n"
    log_error "Failed releases (${#FAILED_RELEASES[@]}):"
    for version in "${FAILED_RELEASES[@]}"; do
        printf "  ❌ v%s\n" "$version"
    done
fi

printf "\n"
printf "========================================\n"

# Exit code based on success
if [ ${#FAILED_REBASES[@]} -eq 0 ] && [ ${#FAILED_RELEASES[@]} -eq 0 ]; then
    log_success "All operations completed successfully! 🎉"
    exit 0
else
    log_warning "Some operations failed. Please check the errors above."
    exit 1
fi