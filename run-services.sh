#!/bin/bash
# Setup and run services for E-Kamus Melayu-Mandarin

set -e

echo "=============================="
echo "E-Kamus Melayu-Mandarin Setup"
echo "=============================="
echo "Starting translation service..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed. Please install Docker first."
    echo "Visit: https://www.docker.com/get-started"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    echo "Error: Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Stop and remove existing containers if they're running
echo "Cleaning up existing containers..."
docker stop libretranslate-ms-zh 2>/dev/null || true
docker rm libretranslate-ms-zh 2>/dev/null || true

# Start LibreTranslate with docker-compose
echo "Starting LibreTranslate service with Docker Compose..."
docker compose up -d

echo "Waiting for LibreTranslate to initialize..."
sleep 5

# Check if LibreTranslate is running
if ! docker ps | grep -q libretranslate-ms-zh; then
    echo "Error: LibreTranslate failed to start."
    docker logs libretranslate-ms-zh
    exit 1
fi

echo "=============================="
echo "LibreTranslate service is running!"
echo "=============================="
echo "LibreTranslate API: http://localhost:5001"
echo "LibreTranslate Web UI: http://localhost:5001"
echo ""

# Instructions for starting Ollama with the new model
echo "=============================="
echo "Instructions for Ollama with gpt-oss:20b"
echo "=============================="
echo "To use the Spring Boot application with the gpt-oss:20b model:"
echo ""
echo "1. Install Ollama from https://ollama.com if not already installed"
echo "2. Run the following command to pull the gpt-oss:20b model:"
echo "   ollama pull gpt-oss:20b"
echo ""
echo "3. Run the Ollama service:"
echo "   ollama serve"
echo ""
echo "4. In a new terminal, start the Spring Boot application:"
echo "   cd backend"
echo "   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "5. For production mode without Ollama dependency:"
echo "   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod"
echo ""
echo "To stop the LibreTranslate service, run: docker compose down"
