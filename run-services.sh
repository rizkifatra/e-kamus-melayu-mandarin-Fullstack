#!/bin/bash
# Setup and run LibreTranslate and Ollama services together

set -e

echo "Starting e-Kamus Melayu-Mandarin services..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    echo "Visit: https://www.docker.com/get-started"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Stop and remove existing containers if they're running
echo "Cleaning up existing containers..."
docker stop libretranslate-ms-zh ollama libretranslate-ms-zh-setup 2>/dev/null || true
docker rm libretranslate-ms-zh ollama libretranslate-ms-zh-setup 2>/dev/null || true

# Start services with docker-compose
echo "Starting services with Docker Compose..."
docker compose up -d

echo "Waiting for services to initialize..."
sleep 5

# Check if Ollama is running
if ! docker ps | grep -q ollama; then
    echo "Error: Ollama failed to start."
    docker logs ollama
    exit 1
fi

echo "Ollama is running. Pulling the deepseek model..."
# First try the specific model name we want
docker exec ollama ollama pull deepseek-r1:7b && \
echo "Successfully pulled deepseek-r1:7b model" || \
echo "Trying alternative model names..."

# If the specific model failed, try alternatives
if ! docker exec ollama ollama list | grep -q "deepseek"; then
    echo "Trying alternative deepseek models..."
    docker exec ollama ollama pull deepseek:7b || \
    docker exec ollama ollama pull deepseek-chat || \
    docker exec ollama ollama pull deepseek || \
    echo "Warning: Could not pull any deepseek model. Check available models with 'docker exec ollama ollama list'"
fi

# Check what model was actually pulled and update application.properties
MODEL_NAME=$(docker exec ollama ollama list | grep "deepseek" | head -n 1 | awk '{print $1}')
if [ -n "$MODEL_NAME" ]; then
    echo "Successfully pulled model: $MODEL_NAME"
    # Update the model name in the application properties
    sed -i.bak "s/deepseek.api.key=.*/deepseek.api.key=$MODEL_NAME/" ./backend/src/main/resources/application.properties
    echo "Updated application.properties to use model: $MODEL_NAME"
else
    echo "No deepseek model was pulled successfully. Please check Ollama configuration."
    # Default to generic "deepseek" in application.properties
    sed -i.bak "s/deepseek.api.key=.*/deepseek.api.key=deepseek/" ./backend/src/main/resources/application.properties
fi

# Check if LibreTranslate is running
if ! docker ps | grep -q libretranslate-ms-zh; then
    echo "Error: LibreTranslate failed to start."
    docker logs libretranslate-ms-zh
    exit 1
fi

echo "=============================="
echo "Services are running!"
echo "=============================="
echo "LibreTranslate API: http://localhost:5001"
echo "LibreTranslate Web UI: http://localhost:5001"
echo "Ollama API: http://localhost:11434"
echo ""
echo "To check available Ollama models, run: docker exec ollama ollama list"
echo "To stop all services, run: docker compose down"
echo "To check logs, run:"
echo "  - LibreTranslate: docker logs -f libretranslate-ms-zh"
echo "  - Ollama: docker logs -f ollama"
