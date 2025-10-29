package edu.kangwon.university.taxicarpool.auth.reset;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByJti(String jti);
    void deleteAllByMember(MemberEntity member);
}
