#!/bin/bash

# E-Kamus API Test Script
# Tests the Spring Boot API endpoints to verify functionality

# Configuration
API_HOST="http://localhost:8080"
WORD_ENDPOINT="/api/translate"
HEALTH_ENDPOINT="/actuator/health"
TEST_WORDS=("baik" "cantik" "hijau" "besar" "kecil")

# Text colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
YELLOW='\033[0;33m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  E-Kamus Melayu-Mandarin API Test Tool  ${NC}"
echo -e "${BLUE}=========================================${NC}"

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is not installed. Please install curl to run this test.${NC}"
    exit 1
fi

# Check if jq is installed (for JSON formatting)
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq is not installed. Output will not be formatted.${NC}"
    JQ_AVAILABLE=false
else
    JQ_AVAILABLE=true
fi

# Function to format JSON
format_json() {
    if [ "$JQ_AVAILABLE" = true ]; then
        echo "$1" | jq .
    else
        echo "$1"
    fi
}

# Test 1: Health check
echo -e "\n${BLUE}[TEST 1]${NC} Checking if the Spring Boot application is running..."
health_response=$(curl -s "$API_HOST$HEALTH_ENDPOINT" || echo "Connection failed")

if [[ "$health_response" == *"UP"* ]]; then
    echo -e "${GREEN}✓ Success: API is up and running${NC}"
else
    echo -e "${RED}✗ Error: API is not responding or not running${NC}"
    echo -e "${YELLOW}Response:${NC} $health_response"
    echo -e "${YELLOW}Check if Spring Boot application is running on port 8080${NC}"
    exit 1
fi

# Test 2: Test LibreTranslate connectivity via dictionary endpoint
echo -e "\n${BLUE}[TEST 2]${NC} Testing translation with a simple word (tests LibreTranslate connection)..."
simple_word="baik"
echo -e "${YELLOW}Testing word:${NC} $simple_word"

response=$(curl -s -X GET "$API_HOST$WORD_ENDPOINT?word=$simple_word" \
    -H "Accept: application/json" || echo "Connection failed")

if [[ "$response" == *"mandarinWord"* && "$response" != *"Translation failed"* ]]; then
    echo -e "${GREEN}✓ Success: Translation API is working${NC}"
    echo -e "${YELLOW}Response:${NC}"
    format_json "$response"
else
    echo -e "${RED}✗ Error: Translation API is not working correctly${NC}"
    echo -e "${YELLOW}Response:${NC}"
    format_json "$response"
fi

# Test 3: Test Ollama AI service with word explanation
echo -e "\n${BLUE}[TEST 3]${NC} Testing AI explanation feature (tests Ollama/gpt-oss:20b connection)..."
ai_test_word="cantik"
echo -e "${YELLOW}Testing word:${NC} $ai_test_word"

response=$(curl -s -X GET "$API_HOST$WORD_ENDPOINT?word=$ai_test_word" \
    -H "Accept: application/json" || echo "Connection failed")

if [[ "$response" == *"explanation"* && "$response" != *"Unable to generate explanation"* ]]; then
    echo -e "${GREEN}✓ Success: AI explanation service is working${NC}"
    # Extract just the explanation field
    if [ "$JQ_AVAILABLE" = true ]; then
        explanation=$(echo "$response" | jq -r .explanation)
        echo -e "${YELLOW}Explanation:${NC} ${explanation:0:100}..." # Show first 100 chars
        echo -e "${YELLOW}Full response available in response.json${NC}"
        echo "$response" > response.json
    else
        echo -e "${YELLOW}Response:${NC} (truncated)"
        echo "${response:0:200}..."
    fi
else
    echo -e "${RED}✗ Error: AI explanation service is not working correctly${NC}"
    echo -e "${YELLOW}Response:${NC}"
    format_json "$response"
    echo -e "${YELLOW}Check if Ollama is running and gpt-oss:20b model is loaded${NC}"
fi

# Test 4: Multiple word test
echo -e "\n${BLUE}[TEST 4]${NC} Testing multiple words (stress test)..."

for word in "${TEST_WORDS[@]}"; do
    echo -e "\n${YELLOW}Testing word:${NC} $word"
    
    response=$(curl -s -X GET "$API_HOST$WORD_ENDPOINT?word=$word" \
        -H "Accept: application/json" || echo "Connection failed")
    
    if [[ "$response" == *"mandarinWord"* && "$response" != *"Translation failed"* ]]; then
        echo -e "${GREEN}✓ Success: '$word' translation successful${NC}"
        if [ "$JQ_AVAILABLE" = true ]; then
            mandarin_word=$(echo "$response" | jq -r .mandarinWord)
            echo -e "${YELLOW}Mandarin word:${NC} $mandarin_word"
        fi
    else
        echo -e "${RED}✗ Error: '$word' translation failed${NC}"
    fi
done

echo -e "\n${BLUE}=========================================${NC}"
echo -e "${BLUE}  API Test Complete  ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "If you encountered any issues:"
echo -e "1. Check if ${YELLOW}LibreTranslate${NC} is running in Docker (port 5001)"
echo -e "2. Check if ${YELLOW}Ollama${NC} is running with gpt-oss:20b model loaded"
echo -e "3. Check if ${YELLOW}Spring Boot${NC} application is running in dev mode"
echo -e "\nYou can run each service manually:"
echo -e "• LibreTranslate: ${YELLOW}docker compose up -d${NC}"
echo -e "• Ollama: ${YELLOW}ollama serve${NC} (in one terminal)"
echo -e "• Spring Boot: ${YELLOW}cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev${NC}"
