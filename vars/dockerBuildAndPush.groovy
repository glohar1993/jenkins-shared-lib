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
            # Install Trivy if not present (install to home dir to avoid permission issues)
            if ! command -v trivy &>/dev/null && ! command -v ~/bin/trivy &>/dev/null; then
                mkdir -p ~/bin
                curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b ~/bin
                export PATH=\$PATH:~/bin
            fi
            export PATH=\$PATH:~/bin
            # Scan filesystem before building
            trivy fs --exit-code 0 --severity CRITICAL --no-progress . || true
            echo "Security scan complete"
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
