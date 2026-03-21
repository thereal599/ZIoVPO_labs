package com.lab2.demo.repository;

import com.lab2.demo.model.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {
    boolean existsByCode(String code);
    Optional<License> findByCode(String code);

    @Query("""
    select l
    from License l
    join DeviceLicense dl on dl.license = l
    where dl.device.id = :deviceId
      and l.user.id = :userId
      and l.product.id = :productId
      and l.blocked = false
      and l.endingDate >= CURRENT_DATE
    order by l.endingDate desc
""")
    List<License> findActiveLicense(UUID deviceId, UUID userId, UUID productId);
}