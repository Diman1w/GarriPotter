package com.example.demo.controller;

import org.apache.commons.io.input.BOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/load")
public class Rest {
    @CrossOrigin
    @PostMapping("/WebLoadTxt/{minWordLength}")
    public ResponseEntity<String> handleFileUpload(@PathVariable int minWordLength, @RequestBody byte[] fileBytes) {
        try {
            if (fileBytes == null || fileBytes.length == 0) {
                return new ResponseEntity<>("File is empty", HttpStatus.BAD_REQUEST);
            }

            // Определение кодировки файла только по началу файла
            UniversalDetector detector = new UniversalDetector(null);
            int bytesRead = Math.min(fileBytes.length, 512);
            detector.handleData(fileBytes, 0, bytesRead);
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8.name();
            }

            // Создание карты для подсчета слов
            ConcurrentHashMap<String, Integer> wordCountMap = new ConcurrentHashMap<>();

            // Построчное чтение и обработка файла
            try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
                 InputStreamReader streamReader = new InputStreamReader(new BOMInputStream(inputStream), encoding);
                 BufferedReader reader = new BufferedReader(streamReader)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] words = line.split("\\P{L}+");
                    for (String word : words) {
                        if (word.length() > minWordLength) {
                            word = word.toLowerCase();
                            wordCountMap.merge(word, 1, Integer::sum);
                        }
                    }
                }
            }

            // Формирование ответа
            StringBuilder responseBuilder = new StringBuilder("Top 10 words longer than " + minWordLength + " characters:\n");
            wordCountMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> responseBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
            String response = responseBuilder.toString();

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to process the file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}