# E-Kamus: Mal## How It's Built

### Frontend (What You See)

- Made with Angular
- Looks good on phones and computers
- Connects to the translation system

### Backend (Behind the Scenes)

- Uses Spring Boot
- Connects to two helper tools:
  - LibreTranslate: For basic word translation
  - Ollama with DeepSeek model: For explanations and examplesducational Dictionary

> **Note:** This project is still under development. Some features may not be fully implemented yet.

A simple educational web app that helps children and beginners translate and learn Malay words in Mandarin Chinese.

## What This App Does

- **Translates Words**: Turn Malay words into Mandarin Chinese
- **Explains Meanings**: Shows what words mean with simple explanations
- **Shows How to Pronounce**: Helps you say the Mandarin words correctly
- **Gives Examples**: Shows how to use the words in sentences
- **Works Without Internet**: All translation happens on your computer

## Technical Stack

### Frontend

- Angular (v19.2.0)
- Responsive design using CSS
- HTTP Client for API integration

### Backend

- Spring Boot (v3.2.4)
- WebFlux for reactive programming
- Integration with LibreTranslate API and Dee:20B via Ollama



> **Note:** Setting up requires some technical knowledge. We're working on making this easier!

### What You Need First

- Java 17 or higher
- Node.js and npm
- Docker for LibreTranslate
- Ollama for the AI model


## Current Development Status

This project is a work-in-progress. We are currently:

- Improving translation speed
- Making the interface more child-friendly
- Testing with different AI models for better explanations

## License

[MIT License](LICENSE)
