package url_shortener.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import url_shortener.entity.Url;
import url_shortener.service.UrlService;

@RestController
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        String originalUrl =
                urlService.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(302)
                .header("Location", originalUrl)
                .build();
    }
}