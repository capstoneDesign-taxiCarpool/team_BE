package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query("SELECT m FROM MessageEntity m " +
        "LEFT JOIN FETCH m.sender " +
        "WHERE m.party.id = :partyId AND m.id > :id " +
        "ORDER BY m.id ASC")
    List<MessageEntity> findByPartyIdAndIdGreaterThanOrderByIdAsc(@Param("partyId") Long partyId,
        @Param("id") Long id, Pageable pageable);

    @Query("SELECT m FROM MessageEntity m " +
        "LEFT JOIN FETCH m.sender " +
        "WHERE m.party.id = :partyId " +
        "ORDER BY m.id ASC")
    List<MessageEntity> findByPartyIdOrderByIdAsc(@Param("partyId") Long partyId,
        Pageable pageable);

    @Modifying
    @Query("UPDATE MessageEntity m SET m.sender = null WHERE m.sender = :member")
    void setSenderToNullByMember(@Param("member") MemberEntity member);

}