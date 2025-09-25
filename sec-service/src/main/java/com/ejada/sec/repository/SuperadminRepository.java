package com.ejada.sec.repository;
import com.ejada.sec.domain.Superadmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SuperadminRepository extends JpaRepository<Superadmin, Long> {
    Optional<Superadmin> findByUsername(String username);
    Optional<Superadmin> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
 Optional<Superadmin> findByUsernameOrEmail(String username, String email);
    
    @Query("SELECT COUNT(s) FROM Superadmin s WHERE s.enabled = true AND s.locked = false")
    long countActiveSuperadmins();
    
    @Query("SELECT s FROM Superadmin s WHERE s.username = :identifier OR s.email = :identifier")
    Optional<Superadmin> findByIdentifier(@Param("identifier") String identifier);
}