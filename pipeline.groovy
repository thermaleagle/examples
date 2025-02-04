pipeline {
    agent any

    environment {
        BITBUCKET_URLS = "https://stash:8081,https://stash:8082"  // Comma-separated Bitbucket URLs
        PROJECTS = "proj1,proj2,proj3"  // Comma-separated project keys
    }

    stages {
        stage('Count Branches') {
            steps {
                script {
                    def bitbucketUrls = env.BITBUCKET_URLS.split(",")
                    def projectList = env.PROJECTS.split(",")

                    // Map of project names to their corresponding Bitbucket instances
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

                    // Collect credentials using **existing naming convention** for port 8081
                    // Use a new convention for port 8082
                    def credentialIds = projectList.collectMany { project ->
                        def instances = projectBitbucketUrls[project]
                        instances.collect { instance ->
                            def port = instance.split(":")[1]
                            def tokenName = port == "8081" ? "BB_TOKEN_${project}" : "BB_TOKEN_${project}_8082"
                            string(credentialsId: tokenName, variable: tokenName)
                        }
                    }

                    withCredentials(credentialIds) {
                        def projectTokens = [:]

                        // Assign tokens dynamically for each project-instance combination
                        projectList.each { project ->
                            projectBitbucketUrls[project].each { instance ->
                                def port = instance.split(":")[1]
                                def tokenKey = port == "8081" ? "BB_TOKEN_${project}" : "BB_TOKEN_${project}_8082"
                                if (env[tokenKey]) {
                                    projectTokens["${project}_${port}"] = env[tokenKey]
                                }
                            }
                        }

                        def allCsvContent = []
                        allCsvContent.add("BitbucketURL,Project,Repository,Main,Master,Release/*,Hotfix/*,Feature")

                        // Process each project separately for each Bitbucket instance
                        projectList.each { project ->
                            projectBitbucketUrls[project].each { instance ->
                                def accessToken = projectTokens["${project}_${instance.split(':')[1]}"]
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

                                    // Append to consolidated report
                                    allCsvContent.add("${bitbucketUrl},${project},${repoName},${branchCounts['main']},${branchCounts['master']},${branchCounts['release/*']},${branchCounts['hotfix/*']},${branchCounts['feature']}")
                                }

                                // Write per-instance CSV file
                                writeFile file: csvFile, text: csvContent.join("\n")
                                echo "CSV file '${csvFile}' created successfully."
                            }
                        }

                        // Write the consolidated report
                        def allProjectsCsvFile = "branch_counts_all_projects.csv"
                        writeFile file: allProjectsCsvFile, text: allCsvContent.join("\n")
                        echo "Consolidated CSV file '${allProjectsCsvFile}' created successfully."
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

                def csvFiles = []
                projectList.each { project ->
                    bitbucketUrls.each { instance ->
                        csvFiles.add("branch_counts_${project}_${instance.replaceAll('[^a-zA-Z0-9]', '_')}.csv")
                    }
                }
                csvFiles.add("branch_counts_all_projects.csv")

                archiveArtifacts artifacts: csvFiles.join(","), fingerprint: true
                echo "CSV files archived successfully. Download them from the Jenkins UI."
            }
        }
    }
}