/**
 * Enterprise Git Checkout
 * Supports branch strategies: feature/*, release/*, hotfix/*, main
 * Auto-sets build description and tags commit with build info
 */
def call(String repoUrl, String branch, String credId) {
    echo "Checking out ${branch} from ${repoUrl}"

    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        extensions: [
            [$class: 'CloneOption', depth: 1, shallow: true, timeout: 10],
            [$class: 'PruneStaleBranch'],
            [$class: 'CleanBeforeCheckout']
        ],
        userRemoteConfigs: [[credentialsId: credId, url: repoUrl]]
    ])

    // Capture commit metadata for traceability
    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    env.GIT_BRANCH_NAME  = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
    env.GIT_AUTHOR      = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
    env.GIT_MESSAGE     = sh(script: "git log -1 --pretty=format:'%s'", returnStdout: true).trim()

    currentBuild.description = "${env.GIT_BRANCH_NAME} | ${env.GIT_COMMIT_SHORT} | ${env.GIT_AUTHOR}"
    echo "Commit: ${env.GIT_COMMIT_SHORT} by ${env.GIT_AUTHOR} — ${env.GIT_MESSAGE}"
}
