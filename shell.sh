#!/bin/bash

# Set your Bitbucket Data Center credentials
USERNAME="your-username"  # Replace with your Bitbucket username
PASSWORD="your-password"  # Replace with your Bitbucket password
BITBUCKET_URL="https://your-bitbucket-server-url"  # Replace with your Bitbucket Data Center URL

# List of projects (space-separated)
PROJECTS=("PROJECT1" "PROJECT2" "PROJECT3")  # Replace with your actual project keys

# Initialize total branch counts
TOTAL_MAIN_BRANCHES=0
TOTAL_OTHER_BRANCHES=0

# Loop through each project
for PROJECT in "${PROJECTS[@]}"; do
    echo "Fetching repositories in project: $PROJECT"

    # Get all repositories in the project (without jq)
    REPOS=$(curl -s -u "$USERNAME:$PASSWORD" "$BITBUCKET_URL/rest/api/1.0/projects/$PROJECT/repos" | grep -o '"slug":"[^"]*' | sed 's/"slug":"//')

    # Loop through each repository and count branches
    for REPO in $REPOS; do
        echo "  Repository: $REPO"

        # Fetch all branch names (without jq)
        BRANCHES=$(curl -s -u "$USERNAME:$PASSWORD" "$BITBUCKET_URL/rest/api/1.0/projects/$PROJECT/repos/$REPO/branches" | grep -o '"displayId":"[^"]*' | sed 's/"displayId":"//')

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

        echo "    Main Branches (main, master, release/*, hotfix/*): $MAIN_BRANCHES"
        echo "    Other Branches: $OTHER_BRANCHES"

        # Update total counts
        TOTAL_MAIN_BRANCHES=$((TOTAL_MAIN_BRANCHES + MAIN_BRANCHES))
        TOTAL_OTHER_BRANCHES=$((TOTAL_OTHER_BRANCHES + OTHER_BRANCHES))
    done
done

# Print the final summary
echo "======================================"
echo "Total Main Branches Across All Projects: $TOTAL_MAIN_BRANCHES"
echo "Total Other Branches Across All Projects: $TOTAL_OTHER_BRANCHES"
echo "======================================"