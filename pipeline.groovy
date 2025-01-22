pipeline {
    agent any

    environment {
        BITBUCKET_URL = "https://your-bitbucket-server-url"
        PROJECTS = "PROJECT1,PROJECT2,PROJECT3"
    }

    stages {
        stage('Count Branches') {
            steps {
                script {
                    def projectList = env.PROJECTS.split(",")

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

                    withCredentials(projectList.collect { 
                        string(credentialsId: "bb-token-${it.toLowerCase()}", variable: "BB_TOKEN_${it}") 
                    }) {
                        def projectTokens = [:]

                        // Assign each project's access token dynamically
                        projectList.each { project ->
                            projectTokens[project] = env."BB_TOKEN_${project}"
                        }

                        // Process each project sequentially
                        projectList.each { project ->
                            def accessToken = projectTokens[project]
                            def csvFile = "branch_counts_${project}.csv"
                            def csvContent = []

                            // Add CSV header
                            csvContent.add("Project,Repository,Main,Master,Release/*,Hotfix/*,Feature")

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

                            // Write CSV to file for this project
                            writeFile file: csvFile, text: csvContent.join("\n")
                            echo "CSV file '${csvFile}' created successfully."
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def projectList = env.PROJECTS.split(",")
                def csvFiles = projectList.collect { "branch_counts_${it}.csv" }
                archiveArtifacts artifacts: csvFiles.join(","), fingerprint: true
                echo "CSV files archived successfully. Download them from the Jenkins UI."
            }
        }
    }
}