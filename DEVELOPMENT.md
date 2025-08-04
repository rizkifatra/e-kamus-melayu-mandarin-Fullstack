# Development Guide for LibreTranslate Integration

This project uses LibreTranslate for translating from Malay to Traditional Mandarin. For development, you have two options:

## Option 1: Use the Public API (Default)

The default configuration uses the public LibreTranslate API at `https://libretranslate.com`.

This is the simplest option for development but may have:

- Usage limits
- Rate limiting
- Potential downtime

## Option 2: Run LibreTranslate Locally (Recommended for Serious Development)

For better reliability and no rate limits, run LibreTranslate locally with Docker:

```bash
docker run -it --rm -p 5000:5000 libretranslate/libretranslate
```

Then update `application.properties`:

```properties
libretranslate.api.url=http://localhost:5000
```

## API Request Example

```javascript
// Example request to LibreTranslate API
const response = await fetch("https://libretranslate.com/translate", {
  method: "POST",
  body: JSON.stringify({
    q: "Selamat pagi!", // Malay text ("Good morning!")
    source: "ms", // ISO code for Malay
    target: "zh-TW", // ISO code for Traditional Chinese
    format: "text",
  }),
  headers: { "Content-Type": "application/json" },
});

const result = await response.json();
console.log(result.translatedText); // Expected: "早安！"
```

This is for reference - the backend service already handles these API calls for you.

## Development Testing

When developing, you can use these Malay adjectives for testing:

- "cantik" (beautiful)
- "cepat" (fast)
- "lambat" (slow)
- "tinggi" (tall)
- "pendek" (short)

The mock AI service will generate placeholder explanations and examples in development mode.
