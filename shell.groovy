import groovy.json.JsonSlurper
import hudson.util.Secret

// Bitbucket Credentials
def BITBUCKET_URL = "https://your-bitbucket-server-url"  // Replace with your Bitbucket Data Center URL
def USERNAME = "your-username"  // Replace with your Bitbucket username
def PASSWORD = "your-password"  // Replace with your Bitbucket password (Consider using Secret.fromString)

// List of Bitbucket projects
def PROJECTS = ["PROJECT1", "PROJECT2", "PROJECT3"]  // Replace with actual project keys

// Counters
def totalMainBranches = 0
def totalOtherBranches = 0

// Function to make API calls
def makeApiCall(url) {
    def connection = new URL(url).openConnection()
    String auth = "${USERNAME}:${PASSWORD}".bytes.encodeBase64().toString()
    connection.setRequestProperty("Authorization", "Basic ${auth}")
    connection.setRequestProperty("Accept", "application/json")
    connection.connect()

    if (connection.responseCode == 200) {
        return new JsonSlurper().parse(connection.inputStream)
    } else {
        println "ERROR: Failed to fetch data from ${url}, Response Code: ${connection.responseCode}"
        return null
    }
}

// Loop through each project
PROJECTS.each { project ->
    println "Fetching repositories in project: ${project}"

    // Get all repositories in the project
    def reposUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
    def reposResponse = makeApiCall(reposUrl)

    if (reposResponse?.values) {
        reposResponse.values.each { repo ->
            def repoName = repo.slug
            println "  Processing repository: ${repoName}"

            // Get all branches
            def branchesUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
            def branchesResponse = makeApiCall(branchesUrl)

            def mainBranches = 0
            def otherBranches = 0

            // Categorize branches
            branchesResponse?.values?.each { branch ->
                def branchName = branch.displayId
                if (branchName == "main" || branchName == "master" || branchName.startsWith("release/") || branchName.startsWith("hotfix/")) {
                    mainBranches++
                } else {
                    otherBranches++
                }
            }

            println "    Main Branches (main, master, release/*, hotfix/*): ${mainBranches}"
            println "    Other Branches: ${otherBranches}"

            // Update total counts
            totalMainBranches += mainBranches
            totalOtherBranches += otherBranches
        }
    } else {
        println "WARNING: No repositories found in project ${project} or failed to fetch data."
    }
}

// Print final summary
println "======================================"
println "Total Main Branches Across All Projects: ${totalMainBranches}"
println "Total Other Branches Across All Projects: ${totalOtherBranches}"
println "======================================"