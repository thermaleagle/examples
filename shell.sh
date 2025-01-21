#!/bin/bash

# Set your Bitbucket Data Center credentials
USERNAME="your-username"  # Replace with your Bitbucket username
PASSWORD="your-password"  # Replace with your Bitbucket password
BITBUCKET_URL="https://your-bitbucket-server-url"  # Replace with your Bitbucket Data Center URL

# List of projects (space-separated)
PROJECTS=("PROJECT1" "PROJECT2" "PROJECT3")  # Replace with your actual project keys

# Logging settings
LOG_FILE="bitbucket_branch_count.log"
DEBUG=true  # Set to false to disable detailed logging

# Initialize total branch counts
TOTAL_MAIN_BRANCHES=0
TOTAL_OTHER_BRANCHES=0

# Function to log messages
log() {
    local MESSAGE="$1"
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $MESSAGE" | tee -a "$LOG_FILE"
}

# Function to log API responses for debugging
log_api_response() {
    local RESPONSE="$1"
    local ENDPOINT="$2"
    echo -e "\n--- API CALL: $ENDPOINT ---\n$RESPONSE\n--- END ---\n" >> "$LOG_FILE"
}

# Loop through each project
for PROJECT in "${PROJECTS[@]}"; do
    log "Fetching repositories in project: $PROJECT"

    # Get all repositories in the project
    REPOS_JSON=$(curl -s -u "$USERNAME:$PASSWORD" "$BITBUCKET_URL/rest/api/1.0/projects/$PROJECT/repos")
    if [[ -z "$REPOS_JSON" ]]; then
        log "ERROR: Failed to fetch repositories for project $PROJECT"
        continue
    fi

    log_api_response "$REPOS_JSON" "/rest/api/1.0/projects/$PROJECT/repos"

    REPOS=$(echo "$REPOS_JSON" | grep -o '"slug":"[^"]*' | sed 's/"slug":"//')

    if [[ -z "$REPOS" ]]; then
        log "WARNING: No repositories found in project $PROJECT"
        continue
    fi

    # Loop through each repository and count branches
    for REPO in $REPOS; do
        log "Processing repository: $REPO"

        # Fetch all branch names
        BRANCHES_JSON=$(curl -s -u "$USERNAME:$PASSWORD" "$BITBUCKET_URL/rest/api/1.0/projects/$PROJECT/repos/$REPO/branches")
        if [[ -z "$BRANCHES_JSON" ]]; then
            log "ERROR: Failed to fetch branches for repository $REPO"
            continue
        fi

        log_api_response "$BRANCHES_JSON" "/rest/api/1.0/projects/$PROJECT/repos/$REPO/branches"

        BRANCHES=$(echo "$BRANCHES_JSON" | grep -o '"displayId":"[^"]*' | sed 's/"displayId":"//')

        MAIN_BRANCHES=0
        OTHER_BRANCHES=0

        # Categorize branches
        for BRANCH in $BRANCHES; do
            if [[ "$BRANCH" == "main" || "$BRANCH" == "master" || "$BRANCH" == release/* || "$BRANCH" == hotfix/* ]]; then
                ((MAIN_BRANCHES++))
            else
                ((OTHER_BRANCHES++))
            fi
        done

        log "    Main Branches (main, master, release/*, hotfix/*): $MAIN_BRANCHES"
        log "    Other Branches: $OTHER_BRANCHES"

        # Update total counts
        TOTAL_MAIN_BRANCHES=$((TOTAL_MAIN_BRANCHES + MAIN_BRANCHES))
        TOTAL_OTHER_BRANCHES=$((TOTAL_OTHER_BRANCHES + OTHER_BRANCHES))
    done
done

# Print the final summary
log "======================================"
log "Total Main Branches Across All Projects: $TOTAL_MAIN_BRANCHES"
log "Total Other Branches Across All Projects: $TOTAL_OTHER_BRANCHES"
log "======================================"

echo "Execution complete. Check the log file '$LOG_FILE'