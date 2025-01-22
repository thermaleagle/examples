import groovy.json.JsonSlurper
import jenkins.model.*
import hudson.util.Secret
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials

// Bitbucket Data Center URL
def BITBUCKET_URL = "https://your-bitbucket-server-url"  // Replace with your Bitbucket Data Center URL

// List of Bitbucket projects
def PROJECTS = ["PROJECT1", "PROJECT2", "PROJECT3"]  // Replace with actual project keys

// Get credentials securely from Jenkins
def credentialsId = "bitbucket-creds"  // Use your Jenkins credential ID
def credentials = CredentialsProvider.lookupCredentials(
    StandardUsernamePasswordCredentials.class,
    Jenkins.instance,
    null,
    null
).find { it.id == credentialsId }

if (credentials == null) {
    throw new Exception("Bitbucket credentials not found in Jenkins")
}

// Declare global variables
def USERNAME = credentials.username
def PASSWORD = credentials.password.getPlainText()

// Counters
def totalMainBranches = 0
def totalOtherBranches = 0

// Function to make paginated API calls
def makePaginatedApiCall(baseUrl) {
    def allResults = []
    def start = 0
    def nextPageExists = true

    while (nextPageExists) {
        def url = "${baseUrl}?start=${start}"
        try {
            def connection = new URL(url).openConnection()
            String auth = "${USERNAME}:${PASSWORD}".bytes.encodeBase64().toString()
            connection.setRequestProperty("Authorization", "Basic ${auth}")
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()

            if (connection.responseCode == 200) {
                def jsonResponse = new JsonSlurper().parse(connection.inputStream)
                allResults.addAll(jsonResponse.values)

                // Handle pagination
                if (jsonResponse.isLastPage) {
                    nextPageExists = false
                } else {
                    start = jsonResponse.nextPageStart
                }
            } else {
                println "ERROR: Failed to fetch data from ${url}, Response Code: ${connection.responseCode}"
                nextPageExists = false
            }
        } catch (Exception e) {
            println "Exception while calling API: ${e.message}"
            nextPageExists = false
        }
    }
    return allResults
}

// Loop through each project
PROJECTS.each { project ->
    println "Fetching repositories in project: ${project}"

    // Get all repositories in the project (paginated)
    def reposUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
    def repos = makePaginatedApiCall(reposUrl)

    if (!repos) {
        println "WARNING: No repositories found in project ${project} or failed to fetch data."
        return
    }

    repos.each { repo ->
        def repoName = repo.slug
        println "  Processing repository: ${repoName}"

        // Get all branches (paginated)
        def branchesUrl = "${BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
        def branches = makePaginatedApiCall(branchesUrl)

        def mainBranches = 0
        def otherBranches = 0

        // Categorize branches
        branches.each { branch ->
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
}

// Print final summary
println "======================================"
println "Total Main Branches Across All Projects: ${totalMainBranches}"
println "Total Other Branches Across All Projects: ${totalOtherBranches}"
println "======================================"