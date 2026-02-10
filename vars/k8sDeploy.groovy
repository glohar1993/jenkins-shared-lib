def call(String kubeconfigCredId, String manifestsPath = "k8s") {
    echo "Deploying to Kubernetes..."
    kubeconfig(credentialsId: kubeconfigCredId, serverUrl: "") {
        sh """
        kubectl apply -f ${manifestsPath}/deployment.yaml
        kubectl apply -f ${manifestsPath}/service.yaml
        """
    }
}
