# **Bitbucket Branch Count Jenkins Pipeline**

This Jenkins pipeline automates the process of counting branches in Bitbucket repositories across multiple projects. The pipeline retrieves the number of branches under specific categories (`main`, `master`, `release/*`, `hotfix/*`, and `feature/*`) and exports the results to a CSV file, which is archived as a Jenkins build artifact.

## **Features**
- ✅ **Sequential execution**: Processes repositories sequentially (no parallel execution).
- ✅ **Handles multiple projects**: Supports multiple Bitbucket projects by fetching project-specific tokens from Jenkins credentials.
- ✅ **Counts branch types**: Counts `main`, `master`, `release/*`, `hotfix/*`, and all other branches as `feature/*`.
- ✅ **Generates CSV output**: Results are written to a CSV file, which can be downloaded from the Jenkins build artifacts.
- ✅ **Uses secure access tokens**: Fetches Bitbucket **access tokens** from Jenkins credentials instead of using username/password.

---

## **Prerequisites**

### **1. Add Bitbucket Access Tokens to Jenkins**
1. Go to **Manage Jenkins → Manage Credentials**.
2. Under **Global Credentials**, add a **String** credential for each Bitbucket project:
   - **ID**: `bb-token-project1`, `bb-token-project2`, etc.
   - **Value**: The Bitbucket **OAuth Access Token** for the respective project.

### **2. Configure Jenkins Pipeline**
- Copy the **Jenkinsfile** into your repository.
- Modify the **`PROJECTS`** variable in the pipeline to list all Bitbucket projects.

---

## **Usage**

### **1. Run the Pipeline**
1. Open **Jenkins**, navigate to your pipeline, and click **Build Now**.
2. The pipeline will start processing each project sequentially, fetching repository details and counting branches.

### **2. Download the CSV Report**
1. After the pipeline completes, go to **Build Artifacts** in Jenkins.
2. Click **Download `branch_counts.csv`**.

---

## **CSV Output Format**

The pipeline generates a CSV file with the following format:

| **Project** | **Repository** | **Main** | **Master** | **Release/*** | **Hotfix/*** | **Feature** |
|-------------|--------------|----------|------------|---------------|--------------|-------------|
| PROJECT1    | repo1        | 2        | 1          | 3             | 0            | 5           |
| PROJECT1    | repo2        | 1        | 1          | 2             | 1            | 7           |
| PROJECT2    | repo3        | 3        | 2          | 1             | 0            | 6           |

### **Explanation of Columns**
- **Project**: The Bitbucket project.
- **Repository**: The Bitbucket repository name.
- **Main**: Number of branches named `main`.
- **Master**: Number of branches named `master`.
- **Release/***: Number of branches starting with `release/`.
- **Hotfix/***: Number of branches starting with `hotfix/`.
- **Feature**: Count of all other branches.

---

## **Jenkins Pipeline Script**
```groovy
pipeline {
    agent any

    environment {
        BITBUCKET_URL = "https://your-bitbucket-server-url"
        PROJECTS = "PROJECT1,PROJECT2,PROJECT3"
        CSV_FILE = "branch_counts.csv"
    }

    stages {
        stage('Count Branches') {
            steps {
                script {
                    def projectList = env.PROJECTS.split(",")
                    def csvContent = []

                    // Add CSV header
                    csvContent.add("Project,Repository,Main,Master,Release/*,Hotfix/*,Feature")

                    // Function to make paginated API calls using cURL and access tokens
                    def makePaginatedApiCall = { baseUrl, token ->
                        def allResults = []
                        def start = 0
                        def nextPageExists = true

                        while (nextPageExists) {
                            def url = "${baseUrl}?start=${start}&limit=100"
                            def command = """curl -s -H "Authorization: Bearer ${token}" -H "Accept: application/json" "${url}" """

                            def jsonResponse = sh(script: command, returnStdout: true).trim()
                            if (!jsonResponse) {
                                echo "ERROR: Failed to fetch data from ${url}"
                                nextPageExists = false
                                break
                            }

                            def parsedResponse = readJSON(text: jsonResponse)

                            allResults.addAll(parsedResponse.values)

                            if (parsedResponse.isLastPage) {
                                nextPageExists = false
                            } else {
                                start = parsedResponse.nextPageStart ?: 0
                            }
                        }
                        return allResults
                    }

                    withCredentials([
                        string(credentialsId: 'bb-token-project1', variable: 'BB_TOKEN_PROJECT1'),
                        string(credentialsId: 'bb-token-project2', variable: 'BB_TOKEN_PROJECT2'),
                        string(credentialsId: 'bb-token-project3', variable: 'BB_TOKEN_PROJECT3')
                    ]) {
                        def projectTokens = [
                            "PROJECT1": env.BB_TOKEN_PROJECT1,
                            "PROJECT2": env.BB_TOKEN_PROJECT2,
                            "PROJECT3": env.BB_TOKEN_PROJECT3
                        ]

                        projectList.each { project ->
                            def accessToken = projectTokens[project]

                            if (!accessToken) {
                                echo "WARNING: No access token found for project ${project}. Skipping."
                                return
                            }

                            echo "Fetching repositories in project: ${project}"

                            def reposUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
                            def repos = makePaginatedApiCall(reposUrl, accessToken)

                            if (!repos) {
                                echo "WARNING: No repositories found in project ${project} or failed to fetch data."
                                return
                            }

                            repos.each { repo ->
                                def repoName = repo.slug
                                echo "  Processing repository: ${repoName}"

                                def branchesUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
                                def branches = makePaginatedApiCall(branchesUrl, accessToken)

                                def mainBranches = 0
                                def masterBranches = 0
                                def releaseBranches = 0
                                def hotfixBranches = 0
                                def featureBranches = 0

                                branches.each { branch ->
                                    def branchName = branch.displayId
                                    if (branchName == "main") {
                                        mainBranches++
                                    } else if (branchName == "master") {
                                        masterBranches++
                                    } else if (branchName.startsWith("release/")) {
                                        releaseBranches++