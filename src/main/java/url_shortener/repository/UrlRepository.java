package url_shortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import url_shortener.entity.Url;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}