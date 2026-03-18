/**
 * Enterprise Kubernetes Deploy
 * - Rolling update with configurable strategy
 * - Waits for rollout to complete (or rolls back on failure)
 * - Smoke test after deploy
 * - Supports staging and production namespaces
 */
def call(String namespace = 'default', String imageTag = 'latest',
         String ecrRegistry = '', String appName = 'flask-deployment') {

    def fullImage = "${ecrRegistry}/mlops-flask-app:${imageTag}"

    sh """
        # Update image in deployment (atomic — no yaml file editing needed)
        kubectl set image deployment/${appName} \
            flask-container=${fullImage} \
            --namespace=${namespace} \
            --record

        # Wait for rollout — timeout 5 min, rollback on failure
        kubectl rollout status deployment/${appName} \
            --namespace=${namespace} \
            --timeout=300s || {
                echo "Rollout failed — rolling back"
                kubectl rollout undo deployment/${appName} --namespace=${namespace}
                exit 1
            }

        echo "--- Deployment Status ---"
        kubectl get pods -n ${namespace} -l app=flask-app
        kubectl get svc  -n ${namespace} flask-service
    """

    // Smoke test — hit /health on the deployed pod
    sh """
        POD=\$(kubectl get pod -n ${namespace} -l app=flask-app \
               -o jsonpath='{.items[0].metadata.name}')
        kubectl exec \$POD -n ${namespace} -- \
            curl -sf http://localhost:5001/health | python3 -c \
            "import sys,json; r=json.load(sys.stdin); sys.exit(0 if r.get('status')=='healthy' else 1)"
        echo "Smoke test PASSED"
    """
}
