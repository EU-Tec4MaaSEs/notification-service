package gr.atc.t4m.repository;

import gr.atc.t4m.model.EventMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventMappingRepository extends JpaRepository<EventMapping, Long> {
    Optional<EventMapping> findByTopic(String topic);
}
