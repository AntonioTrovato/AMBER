# AI-based Java Performance Testing Prediction Service

This repository contains the Dockerized Flask web service version exposing endpoints for predictions using FCN, OSCNN, and Rocket pre-trained models. 

Based on: [AI-driven Java Performance Testing: Balancing Result Quality with Testing Time](https://doi.org/10.1145/3691620.3695017)

## Getting Started

### Prerequisites

- Docker installed on your machine.
- Ensure you have the model files in the `results/models/` directory.

### Configuration
Project configuration is mainly included in `config.py` script.

We recomment to double-check the following settings:
1. **Models Path**: FCN_PATH, OSCNN_PATH and Rocket_PATH indicate the checkpoint file paths where to load the models. Do not include checkpoint files extension in the path string.
2. **Service Port**: The port exposed by the container. By default, we configured the service using the port 5001. 

### 1. Build the Docker Container

```bash
docker build -t jpt-service .
```

This command will create a Docker image named jpt-service using the Dockerfile present in the current directory.

### 2. Check if Port is Available
Before running the container, ensure that device port is not being used by another process

```bash
sudo lsof -i -P | grep LISTEN | grep :5001
```

Change the 5001 variable with the port to use.

### 3. Run the Docker Container

By default, we use map the container exposed port with the same port number of the device.

```bash
docker run -p 5001:5001 jpt-service
```

### 4. Check if the service is running

You can verify if the service is running by sending a GET request to the root endpoint:

```json
{
  "status": "Service is running"
}
```

### 5. Make Predictions
You can make predictions using POST requests to the following endpoints:

- FCN Model: /predict/fcn
- OSCNN Model: /predict/oscnn
- Rocket Model: /predict/rocket

```bash
curl -X POST http://localhost:5001/predict/fcn -H "Content-Type: application/json" -d '[1.0, 2.0, 3.0, 4.0, 5.0, ..., 100.0]'
```

#### Notes 
Ensure that the input JSON data contains exactly 100 elements; otherwise, the service will return an HTTP 400 error.

### Stopping the Container

```bash
docker ps        # Get the container ID
docker stop <container_id>
```