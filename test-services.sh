#!/bin/bash
# Script to test if the API services are working

echo "Testing LibreTranslate..."
LIBRE_RESULT=$(curl -s -X POST "http://localhost:5001/translate" \
    -H "Content-Type: application/json" \
    -d '{"q": "hello", "source": "en", "target": "ms"}')

echo "LibreTranslate result: $LIBRE_RESULT"

echo ""
echo "Testing Ollama..."
OLLAMA_RESULT=$(curl -s -X POST "http://localhost:11434/api/generate" \
    -H "Content-Type: application/json" \
    -d '{"model": "deepseek-r1:7b", "prompt": "Say hello", "stream": false}' | head -n 10)

echo "Ollama result: $OLLAMA_RESULT"

echo ""
echo "Testing Spring Boot API..."
API_RESULT=$(curl -s -X GET "http://localhost:8080/api/translate?word=hello")
echo "API result: $API_RESULT"
