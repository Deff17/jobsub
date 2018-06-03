package pl.agh.jobsub.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.agh.jobsub.auth.model.User;

public interface UserRepository extends JpaRepository<User,Long> {

    User findByUsername(String username);
}