def call(String imageName, String registryCredId) {
    echo "Building Docker image ${imageName}..."
    def dockerImage = docker.build("${imageName}:latest")
    
    echo "Pushing Docker image to DockerHub..."
    docker.withRegistry('https://registry.hub.docker.com', registryCredId) {
        dockerImage.push('latest')
    }
}