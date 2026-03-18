/**
 * Enterprise Docker Build & Push to AWS ECR
 * - Multi-arch build (linux/amd64) for EKS compatibility
 * - Image tagged with: latest + git commit SHA (immutable tag)
 * - Trivy security scan — fails build on CRITICAL CVEs
 */
def call(String ecrRegistry, String imageName, String awsRegion = 'us-east-2') {
    def fullImage  = "${ecrRegistry}/${imageName}"
    def commitTag  = env.GIT_COMMIT_SHORT ?: 'unknown'
    def latestTag  = "${fullImage}:latest"
    def commitImg  = "${fullImage}:${commitTag}"

    stage('Security Scan (Trivy)') {
        sh """
            # Install Trivy if not present
            if ! command -v trivy &>/dev/null; then
                curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
            fi
            # Scan filesystem before building (fast, no Docker needed)
            trivy fs --exit-code 1 --severity CRITICAL --no-progress . || {
                echo "CRITICAL CVEs found — failing build"
                exit 1
            }
        """
    }

    stage('Build & Push Image') {
        sh """
            # Login to ECR
            aws ecr get-login-password --region ${awsRegion} | \
                docker login --username AWS --password-stdin ${ecrRegistry}

            # Build for linux/amd64 (EKS nodes are x86_64)
            docker buildx build \
                --platform linux/amd64 \
                --build-arg GIT_COMMIT=${commitTag} \
                --build-arg BUILD_DATE=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                -t ${latestTag} \
                -t ${commitImg} \
                --push .

            echo "Pushed: ${latestTag}"
            echo "Pushed: ${commitImg}"
        """
        // Store image digest for deployment traceability
        env.IMAGE_TAG    = commitTag
        env.FULL_IMAGE   = latestTag
    }
}
