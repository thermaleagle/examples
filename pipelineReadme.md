# **Bitbucket Branch Count Jenkins Pipeline**

This Jenkins pipeline automates the process of counting branches in Bitbucket repositories across multiple projects. It:
- ✅ **Creates a separate CSV file for each project (`branch_counts_PROJECT.csv`).**
- ✅ **Ensures CSV files are archived even if an error occurs, preventing data loss.**
- ✅ **Uses Bitbucket project-specific access tokens securely from Jenkins credentials.**
- ✅ **Processes projects sequentially for simplicity.**
- ✅ **Categorizes branches into `main`, `master`, `release/*`, `hotfix/*`, and `feature/*`.**

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

### **2. Download the CSV Reports**
1. After the pipeline completes, go to **Build Artifacts** in Jenkins.
2. Click **Download `branch_counts_PROJECT1.csv`**, `branch_counts_PROJECT2.csv`, etc.

---

## **CSV Output Format**

Each project gets its own CSV file, named as `branch_counts_PROJECT.csv`.

Example **`branch_counts_PROJECT1.csv`**:
```csv
Project,Repository,Main,Master,Release/*,Hotfix/*,Feature
PROJECT1,repo1,2,1,3,0,5
PROJECT1,repo2,1,1,2,1,7