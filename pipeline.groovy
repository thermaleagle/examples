pipeline {
    agent any  // Runs on any available Jenkins agent

    environment {
        BITBUCKET_URL = "https://your-bitbucket-server-url"  // Replace with your Bitbucket Data Center URL
        PROJECTS = "PROJECT1,PROJECT2,PROJECT3"  // Comma-separated project keys
    }

    stages {
        stage('Fetch Credentials') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'bitbucket-creds', usernameVariable: 'BITBUCKET_USER', passwordVariable: 'BITBUCKET_PASS')]) {
                        echo "Bitbucket credentials retrieved securely."
                    }
                }
            }
        }

        stage('Count Branches') {
            steps {
                script {
                    def totalMainBranches = 0
                    def totalOtherBranches = 0

                    // Convert PROJECTS environment variable into a list
                    def projectList = env.PROJECTS.split(",")

                    // Function to make paginated API calls using cURL
                    def makePaginatedApiCall = { baseUrl ->
                        def allResults = []
                        def start = 0
                        def nextPageExists = true

                        while (nextPageExists) {
                            def url = "${baseUrl}?start=${start}"
                            def command = """curl -s -u "${env.BITBUCKET_USER}:${env.BITBUCKET_PASS}" "${url}" """

                            def jsonResponse = sh(script: command, returnStdout: true).trim()
                            if (!jsonResponse) {
                                echo "ERROR: Failed to fetch data from ${url}"
                                nextPageExists = false
                                break
                            }

                            def parsedResponse = new groovy.json.JsonSlurper().parseText(jsonResponse)
                            allResults.addAll(parsedResponse.values)

                            // Handle pagination
                            if (parsedResponse.isLastPage) {
                                nextPageExists = false
                            } else {
                                start = parsedResponse.nextPageStart
                            }
                        }
                        return allResults
                    }

                    // Loop through each project
                    projectList.each { project ->
                        echo "Fetching repositories in project: ${project}"

                        // Get all repositories in the project (paginated)
                        def reposUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
                        def repos = makePaginatedApiCall(reposUrl)

                        if (!repos) {
                            echo "WARNING: No repositories found in project ${project} or failed to fetch data."
                            return
                        }

                        repos.each { repo ->
                            def repoName = repo.slug
                            echo "  Processing repository: ${repoName}"

                            // Get all branches (paginated)
                            def branchesUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
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

                            echo "    Main Branches (main, master, release/*, hotfix/*): ${mainBranches}"
                            echo "    Other Branches: ${otherBranches}"

                            // Update total counts
                            totalMainBranches += mainBranches
                            totalOtherBranches += otherBranches
                        }
                    }

                    // Print final summary
                    echo "======================================"
                    echo "Total Main Branches Across All Projects: ${totalMainBranches}"
                    echo "Total Other Branches Across All Projects: ${totalOtherBranches}"
                    echo "======================================"
                }
            }
        }
    }
}