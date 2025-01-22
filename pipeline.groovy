pipeline {
    agent any

    environment {
        BITBUCKET_URL = "https://your-bitbucket-server-url"
        PROJECTS = "PROJECT1,PROJECT2,PROJECT3"
        CSV_FILE = "branch_counts.csv"
        MAX_EXECUTORS = 10  // Maximum parallel jobs
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

        stage('Count Branches in Parallel') {
            steps {
                script {
                    def projectList = env.PROJECTS.split(",")
                    def csvContent = []

                    // Add CSV header
                    csvContent.add("Project,Repository,Main,Master,Release/*,Hotfix/*,Other")

                    // Function to make paginated API calls using cURL and readJSON
                    def makePaginatedApiCall = { baseUrl ->
                        def allResults = []
                        def start = 0
                        def nextPageExists = true

                        while (nextPageExists) {
                            def url = "${baseUrl}?start=${start}&limit=100"
                            def command = """curl -s -u "${env.BITBUCKET_USER}:${env.BITBUCKET_PASS}" "${url}" """

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

                    def parallelTasks = [:]

                    projectList.each { project ->
                        parallelTasks["Process_${project}"] = {
                            lock(resource: "Parallel_Lock", quantity: env.MAX_EXECUTORS.toInteger()) {
                                node {
                                    echo "Fetching repositories in project: ${project}"

                                    def reposUrl = "${env.BITBUCKET_URL}/rest/api/1.0/projects/${project}/repos"
                                    def repos = makePaginatedApiCall(reposUrl)

                                    if (!repos) {
                                        echo "WARNING: No repositories found in project ${project} or failed to fetch data."
                                        return
                                    }

                                    def repoTasks = [:]

                                    repos.each { repo ->
                                        def repoName = repo.slug
                                        repoTasks["Repo_${repoName}"] = {
                                            lock(resource: "Parallel_Lock", quantity: env.MAX_EXECUTORS.toInteger()) {
                                                node {
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

                                                    csvContent.add("${project},${repoName},${mainBranches},${masterBranches},${releaseBranches},${hotfixBranches},${otherBranches}")
                                                }
                                            }
                                        }
                                    }

                                    parallel repoTasks
                                }
                            }
                        }
                    }

                    parallel parallelTasks

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