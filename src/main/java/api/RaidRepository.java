package api;
import org.springframework.data.jpa.repository.JpaRepository;

interface RaidRepository extends JpaRepository<LogRaid, Long> {
}
