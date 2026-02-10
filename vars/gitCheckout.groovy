def call(String repoUrl, String branch, String credId) {
    echo "Checking out code from ${repoUrl}..."
    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [[credentialsId: credId, url: repoUrl]]
    ])
}