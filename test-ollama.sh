#!/bin/bash
# Test script specifically for Ollama integration

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  Ollama Integration Test for E-Kamus    ${NC}"
echo -e "${BLUE}=========================================${NC}"

# Test 1: Check if Ollama is running
echo -e "\n${BLUE}[TEST 1]${NC} Checking if Ollama service is running..."
ollama_status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:11434/api/version || echo "Connection failed")

if [[ "$ollama_status" == "200" ]]; then
    echo -e "${GREEN}✓ Success: Ollama service is running${NC}"
else
    echo -e "${RED}✗ Error: Ollama service is not accessible${NC}"
    echo -e "${YELLOW}Make sure Ollama is running with: ollama serve${NC}"
    exit 1
fi

# Test 2: Check if the gpt-oss:20b model is loaded
echo -e "\n${BLUE}[TEST 2]${NC} Checking if gpt-oss:20b model is available..."
model_check=$(curl -s http://localhost:11434/api/tags | grep -o "gpt-oss:20b" || echo "")

if [[ -n "$model_check" ]]; then
    echo -e "${GREEN}✓ Success: gpt-oss:20b model is available${NC}"
else
    echo -e "${YELLOW}! Warning: gpt-oss:20b model may not be pulled yet${NC}"
    echo -e "${YELLOW}Pull the model with: ollama pull gpt-oss:20b${NC}"
    
    # Ask if user wants to pull the model now
    read -p "Do you want to pull the model now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}Pulling gpt-oss:20b model... (this may take a while)${NC}"
        ollama pull gpt-oss:20b
    fi
fi

# Test 3: Test direct Ollama API with a simple prompt
echo -e "\n${BLUE}[TEST 3]${NC} Testing Ollama API directly..."

echo -e "${YELLOW}Sending test prompt to Ollama API...${NC}"
test_request=$(cat <<EOF
{
  "model": "gpt-oss:20b",
  "prompt": "Translate 'Hello' to Mandarin Chinese and provide the pinyin pronunciation",
  "stream": false
}
EOF
)

response=$(curl -s -X POST http://localhost:11434/api/generate \
    -H "Content-Type: application/json" \
    -d "$test_request" || echo "Connection failed")

if [[ "$response" == *"response"* ]]; then
    echo -e "${GREEN}✓ Success: Ollama API responded${NC}"
    echo -e "${YELLOW}Response snippet:${NC}"
    echo "$response" | grep -o '"response":"[^"]*' | sed 's/"response":"//' | head -n 3
else
    echo -e "${RED}✗ Error: Ollama API is not working correctly${NC}"
    echo -e "${YELLOW}Response:${NC} $response"
fi

echo -e "\n${BLUE}=========================================${NC}"
echo -e "${BLUE}  Ollama Integration Test Complete        ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "If all tests passed, your Ollama setup is working correctly."
echo -e "If any tests failed, check the error messages and make sure:"
echo -e "1. Ollama service is running (${YELLOW}ollama serve${NC})"
echo -e "2. The gpt-oss:20b model is pulled (${YELLOW}ollama pull gpt-oss:20b${NC})"
echo -e "3. Your Spring Boot application is configured correctly"
