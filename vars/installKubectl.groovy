/**
 * Install kubectl + configure EKS kubeconfig
 * Idempotent — skips install if already present
 */
def call(String clusterName = 'mlops-cluster', String awsRegion = 'us-east-2') {
    sh """
        # Install kubectl if not present
        if ! command -v kubectl &>/dev/null && ! test -f ~/bin/kubectl; then
            echo "Installing kubectl..."
            mkdir -p ~/bin
            curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl"
            chmod +x kubectl
            mv kubectl ~/bin/kubectl
        fi
        export PATH=\$PATH:~/bin
        kubectl version --client --short

        # Configure EKS kubeconfig
        aws eks update-kubeconfig \
            --region ${awsRegion} \
            --name ${clusterName}

        kubectl get nodes
    """
}
