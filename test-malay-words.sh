#!/bin/bash

# Colors for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
RESET='\033[0m'

# URL of your API - make sure this matches your actual endpoint
API_URL="http://localhost:8080/api/translate"

# Function to test a word and display the response
test_word() {
    local word=$1
    echo -e "${YELLOW}===================================${RESET}"
    echo -e "${GREEN}Testing word: ${BLUE}$word${RESET}"
    echo -e "${YELLOW}===================================${RESET}"
    
    # Call the API
    response=$(curl -s -X GET "$API_URL?word=$word" -H "accept: application/json")
    
    # Extract fields and display the response nicely formatted
    echo "$response" | jq '.'
    
    echo -e "\n"
}

# Clear cache first
echo -e "${BLUE}Clearing translation cache...${RESET}"
curl -s -X POST "http://localhost:8080/api/cache/clear" | jq '.'
echo -e "\n"

# Test each word
words=("makan" "tidur" "cantik" "muram" "layu" "gerun" "pintar" "cepat" "lambat" "tinggi" "pendek" "baik" "marah" "gembira" "sedih")

for word in "${words[@]}"; do
    test_word "$word"
done

echo -e "${GREEN}Testing complete!${RESET}"
