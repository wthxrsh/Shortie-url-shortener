package url_shortener.service;

import org.springframework.stereotype.Service;
import url_shortener.entity.Url;
import url_shortener.exception.UrlNotFoundException;
import url_shortener.repository.UrlRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final RedisService redisService;

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int SHORT_CODE_LENGTH = 6;

    private final Random random = new Random();

    public UrlService(UrlRepository urlRepository, RedisService redisService) {
        this.urlRepository = urlRepository;
        this.redisService = redisService;
    }

    public Url createShortUrl(
            String originalUrl,
            LocalDateTime expiresAt) {

        String shortCode = generateUniqueShortCode();
        if (expiresAt != null &&
                expiresAt.isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Expiration time must be in the future"
            );
        }

        Url url = new Url();

        url.setOriginalUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(expiresAt);
        url.setClickCount(0L);

        Url savedUrl = urlRepository.save(url);

        // Cache the URL
        if (expiresAt != null) {

            Duration ttl = Duration.between(
                    LocalDateTime.now(),
                    expiresAt
            );

            if (!ttl.isZero() && !ttl.isNegative()) {

                redisService.saveUrl(
                        savedUrl.getShortCode(),
                        savedUrl.getOriginalUrl(),
                        ttl
                );
            }

        } else {

            // No expiration → use default 1 hour cache TTL
            redisService.saveUrl(
                    savedUrl.getShortCode(),
                    savedUrl.getOriginalUrl()
            );
        }

        return savedUrl;
    }

    private String generateUniqueShortCode() {

        String shortCode;

        do {
            shortCode = generateRandomCode();
        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    private String generateRandomCode() {

        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
    public String getOriginalUrl(String shortCode) {

        String cachedUrl = redisService.getUrl(shortCode);

        if (cachedUrl != null) {

            incrementClickCount(shortCode);

            return cachedUrl;
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found")
                );

        if (url.getExpiresAt() != null &&
                url.getExpiresAt().isBefore(LocalDateTime.now())) {

            throw new UrlNotFoundException(
                    "Short URL has expired"
            );
        }

        incrementClickCount(url);

        // Cache it
        if (url.getExpiresAt() != null) {

            Duration ttl = Duration.between(
                    LocalDateTime.now(),
                    url.getExpiresAt()
            );

            if (!ttl.isZero() && !ttl.isNegative()) {

                redisService.saveUrl(
                        url.getShortCode(),
                        url.getOriginalUrl(),
                        ttl
                );
            }

        } else {

            redisService.saveUrl(
                    url.getShortCode(),
                    url.getOriginalUrl()
            );
        }

        return url.getOriginalUrl();
    }
    private void incrementClickCount(String shortCode) {

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found")
                );

        incrementClickCount(url);
    }

    private void incrementClickCount(Url url) {

        url.setClickCount(
                url.getClickCount() + 1
        );

        urlRepository.save(url);
    }

    public Url getStatistics(String shortCode) {

        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(
                                "Short URL not found"
                        )
                );
    }
}