package com.welli.wellibe.record;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface HealthRecordRepository
        extends JpaRepository<HealthRecord, Long> {

    List<HealthRecord> findByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<HealthRecord> findTopByUserIdAndTypeOrderByRecordedAtDesc(
            Long userId,
            HealthRecordType type
    );
}
