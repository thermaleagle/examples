pipeline {
    agent any

    environment {
        BITBUCKET_URL = "https://your-bitbucket-server-url"
        PROJECTS = "PROJECT1,PROJECT2,PROJECT3"
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

        stage('Count Branches and Generate Report') {
            steps {
                script {
                    def projectList = env.PROJECTS.split(",")
                    def outputTable = []

                    // Function to make paginated API calls using cURL and readJSON
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

                            // Use readJSON for CPS-safe parsing
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

                    projectList.each { project ->
                        echo "Fetching repositories in project: ${project}"

                        def reposUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
                        def repos = makePaginatedApiCall(reposUrl)

                        if (!repos) {
                            echo "WARNING: No repositories found in project ${project} or failed to fetch data."
                            return
                        }

                        repos.each { repo ->
                            def repoName = repo.slug
                            echo "  Processing repository: ${repoName}"

                            def branchesUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos/${repoName}/branches"
                            def branches = makePaginatedApiCall(branchesUrl)

                            def mainBranches = 0
                            def masterBranches = 0
                            def releaseBranches = 0
                            def hotfixBranches = 0
                            def otherBranches = 0

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
                                    otherBranches++
                                }
                            }

                            outputTable.add("${project}\t${repoName}\t${mainBranches}\t${masterBranches}\t${releaseBranches}\t${hotfixBranches}\t${otherBranches}")
                        }
                    }

                    // Print the table header
                    echo "=========================================="
                    echo "Project\tRepository\tMain\tMaster\tRelease/*\tHotfix/*\tOther"
                    echo "=========================================="

                    // Print tab-separated values for Excel copy-paste
                    outputTable.each { row ->
                        echo row
                    }

                    echo "=========================================="
                    echo "Copy the above table and paste it into Excel."
                    echo "=========================================="
                }
            }
        }
    }
}