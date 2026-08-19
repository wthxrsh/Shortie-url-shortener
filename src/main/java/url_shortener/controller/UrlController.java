package url_shortener.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import url_shortener.dto.CreateUrlRequest;
import url_shortener.entity.Url;
import url_shortener.service.UrlService;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<Url> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        Url url = urlService.createShortUrl(
                request.getOriginalUrl(),
                request.getExpiresAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(url);
    }
    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<Url> getStatistics(
            @PathVariable String shortCode) {

        return ResponseEntity.ok(
                urlService.getStatistics(shortCode)
        );
    }
}