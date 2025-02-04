pipeline {
    agent any

    environment {
        BITBUCKET_URLS = "https://stash:8081,https://stash:8082"  // Comma-separated Bitbucket instances
        PROJECTS = "proj1,proj2,proj3"  // Comma-separated project keys
    }

    stages {
        stage('Count Branches') {
            steps {
                script {
                    def bitbucketUrls = env.BITBUCKET_URLS.split(",")
                    def projectList = env.PROJECTS.split(",")

                    def projectBitbucketUrls = [
                        "proj1": ["https://stash:8081", "https://stash:8082"],
                        "proj2": ["https://stash:8082"],
                        "proj3": ["https://stash:8081"]
                    ]

                    // Function to make paginated API calls using cURL
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

                    // Credential collection with both naming conventions
                    withCredentials(projectList.collectMany { project ->
                        projectBitbucketUrls[project].collect { instance ->
                            def tokenId = projectBitbucketUrls[project].size() > 1
                                ? "http-access-token-${project}-${instance.replaceAll('[^a-zA-Z0-9]', '_')}"
                                : "http-access-token-${project}"
                            string(credentialsId: tokenId, variable: "BB_TOKEN_${project}_${instance.replaceAll('[^a-zA-Z0-9]', '_')}")
                        }
                    }) {
                        def allCsvContent = []
                        allCsvContent.add("BitbucketURL,Project,Repository,Main,Master,Release/*,Hotfix/*,Feature")

                        projectList.each { project ->
                            projectBitbucketUrls[project].each { instance ->
                                def tokenVar = projectBitbucketUrls[project].size() > 1
                                    ? "BB_TOKEN_${project}_${instance.replaceAll('[^a-zA-Z0-9]', '_')}"
                                    : "BB_TOKEN_${project}"
                                def accessToken = env[tokenVar]
                                def bitbucketUrl = instance
                                def csvFile = "branch_counts_${project}_${instance.replaceAll('[^a-zA-Z0-9]', '_')}.csv"
                                def csvContent = []

                                csvContent.add("BitbucketURL,Project,Repository,Main,Master,Release/*,Hotfix/*,Feature")

                                if (!accessToken) {
                                    echo "WARNING: No access token found for project ${project} on ${bitbucketUrl}. Skipping."
                                    return
                                }

                                echo "Fetching repositories in project: ${project} from ${bitbucketUrl}"

                                def reposUrl = "${bitbucketUrl}/rest/api/1.0/projects/${project}/repos"
                                def repos = makePaginatedApiCall(reposUrl, accessToken)

                                if (!repos) {
                                    echo "WARNING: No repositories found in project ${project} on ${bitbucketUrl} or failed to fetch data."
                                    return
                                }

                                repos.each { repo ->
                                    def repoName = repo.slug
                                    echo "Processing repository: ${repoName} on ${bitbucketUrl}"

                                    def branchesUrl = "${bitbucketUrl}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
                                    def branches = makePaginatedApiCall(branchesUrl, accessToken)

                                    def branchCounts = [
                                        "main": 0,
                                        "master": 0,
                                        "release/*": 0,
                                        "hotfix/*": 0,
                                        "feature": 0
                                    ]

                                    branches.each { branch ->
                                        def branchName = branch.displayId
                                        if (branchName == "main") {
                                            branchCounts["main"]++
                                        } else if (branchName == "master") {
                                            branchCounts["master"]++
                                        } else if (branchName.startsWith("release/")) {
                                            branchCounts["release/*"]++
                                        } else if (branchName.startsWith("hotfix/")) {
                                            branchCounts["hotfix/*"]++
                                        } else {
                                            branchCounts["feature"]++
                                        }
                                    }

                                    csvContent.add("${bitbucketUrl},${project},${repoName},${branchCounts['main']},${branchCounts['master']},${branchCounts['release/*']},${branchCounts['hotfix/*']},${branchCounts['feature']}")
                                    allCsvContent.add("${bitbucketUrl},${project},${repoName},${branchCounts['main']},${branchCounts['master']},${branchCounts['release/*']},${branchCounts['hotfix/*']},${branchCounts['feature']}")
                                }

                                // Write per-instance CSV file
                                writeFile file: csvFile, text: csvContent.join("\n")
                                echo "CSV file '${csvFile}' created successfully."
                            }
                        }

                        // Write the aggregated CSV file
                        def allCsvFile = "all_projects.csv"
                        writeFile file: allCsvFile, text: allCsvContent.join("\n")
                        echo "Consolidated CSV file '${allCsvFile}' created successfully."
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def projectList = env.PROJECTS.split(",")
                def bitbucketUrls = env.BITBUCKET_URLS.split(",")

                // Collect individual project CSVs
                def csvFiles = []
                projectList.each { project ->
                    bitbucketUrls.each { instance ->
                        csvFiles.add("branch_counts_${project}_${instance.replaceAll('[^a-zA-Z0-9]', '_')}.csv")
                    }
                }
                csvFiles.add("all_projects.csv")

                archiveArtifacts artifacts: csvFiles.join(","), fingerprint: true
                echo "CSV files archived successfully. Download them from the Jenkins UI."
            }
        }
    }
}