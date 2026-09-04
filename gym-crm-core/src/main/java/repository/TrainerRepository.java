package repository;

import model.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    Optional<Trainer> findByUser_Username(String username);

    @Query("""
            select tr from Trainer tr
            where tr.user.isActive = true
              and tr not in (select assigned from Trainee te join te.trainers assigned
                             where te.user.username = :username)
            """)
    List<Trainer> findUnassignedFor(@Param("username") String username);
}
