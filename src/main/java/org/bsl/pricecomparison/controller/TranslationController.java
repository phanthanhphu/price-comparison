package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.request.TranslationRequest;
import org.bsl.pricecomparison.response.TranslationResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document; // Correct import for Jsoup Document
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/translate")
public class TranslationController {

    private static final Logger logger = LoggerFactory.getLogger(TranslationController.class);

    public TranslationController() {
        logger.info("TranslationController initialized for web scraping-based translation.");
    }

    @PostMapping("/vi-to-en")
    public ResponseEntity<TranslationResponse> translateVietnameseToEnglish(@RequestBody TranslationRequest request) {
        logger.info("Received translation request: {}", request != null ? request.getText() : "null");
        try {
            // Validate input
            if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
                logger.warn("Empty or invalid input text received");
                return ResponseEntity.badRequest()
                        .body(new TranslationResponse("", "Input text cannot be empty"));
            }

            // Encode text for URL
            String encodedText = URLEncoder.encode(request.getText(), StandardCharsets.UTF_8.toString());
            String url = "https://translate.google.com/m?sl=vi&tl=en&q=" + encodedText;

            // Send HTTP request and parse response
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000) // 10 seconds timeout
                    .get();

            // Extract translated text
            String translatedText = doc.select("div.result-container").first().text();

            logger.info("Translation successful: {}", translatedText);
            return ResponseEntity.ok(new TranslationResponse(translatedText, null));

        } catch (org.jsoup.HttpStatusException e) {
            logger.error("HTTP error during translation: {}", e.getMessage());
            String errorMessage = "Translation failed: ";
            if (e.getStatusCode() == 429) {
                errorMessage += "Too many requests. Please try again later.";
            } else {
                errorMessage += "Unable to connect to translation service.";
            }
            return ResponseEntity.status(500)
                    .body(new TranslationResponse("", errorMessage));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(new TranslationResponse("", "Unexpected error: " + e.getMessage()));
        }
    }
}