package com.example.eam.Security.Repository;

import com.example.eam.Enum.SecurityEventCategory;
import com.example.eam.Enum.SecurityEventType;
import com.example.eam.Security.Entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    long countByEventTypeInAndCreatedAtBetween(Collection<SecurityEventType> types, LocalDateTime from, LocalDateTime to);

    List<SecurityEvent> findTop20ByEventTypeInOrderByCreatedAtDesc(Collection<SecurityEventType> types);

    List<SecurityEvent> findTop20ByEventTypeInAndCreatedAtBetweenOrderByCreatedAtDesc(Collection<SecurityEventType> types, LocalDateTime from, LocalDateTime to);

    List<SecurityEvent> findTop20ByCategoryInOrderByCreatedAtDesc(Collection<SecurityEventCategory> categories);

    List<SecurityEvent> findTop50ByOrderByCreatedAtDesc();
}
