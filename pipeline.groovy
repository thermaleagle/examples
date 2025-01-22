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

                    // Function to get the correct access token for a given project
                    def getAccessToken = { project ->
                        return credentials("bb-token-${project.toLowerCase()}") // Assuming credentials IDs are "bb-token-project1", etc.
                    }

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

                    // Process each project sequentially
                    projectList.each { project ->
                        def accessToken = getAccessToken(project)

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
                                } else if (branchName.startsWith("hotfix/")) {
                                    hotfixBranches++
                                } else {
                                    featureBranches++
                                }
                            }

                            csvContent.add("${project},${repoName},${mainBranches},${masterBranches},${releaseBranches},${hotfixBranches},${featureBranches}")
                        }
                    }

                    // Write CSV to file
                    writeFile file: env.CSV_FILE, text: csvContent.join("\n")
                    echo "CSV file '${env.CSV_FILE}' created successfully."
                }
            }
        }

        stage('Archive CSV as Artifact') {
            steps {
                archiveArtifacts artifacts: env.CSV_FILE, fingerprint: true
                echo "CSV file archived successfully. Download it from the Jenkins UI."
            }
        }
    }
}