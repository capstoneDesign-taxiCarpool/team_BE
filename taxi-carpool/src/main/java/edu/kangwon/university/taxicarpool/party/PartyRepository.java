package edu.kangwon.university.taxicarpool.party;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, Long> {

    Optional<PartyEntity> findById(Long partyId);

    Optional<PartyEntity> findByIdAndIsDeletedFalse(Long partyId);

    @Query(value = "SELECT p FROM party p " +
        "WHERE p.isDeleted = false " +
        "AND p.startDateTime >= :now " +
        "AND NOT EXISTS (SELECT 1 FROM p.memberEntities m WHERE m.id = :memberId)",
        countQuery = "SELECT COUNT(p) FROM party p " +
            "WHERE p.isDeleted = false " +
            "AND p.startDateTime >= :now " +
            "AND NOT EXISTS (SELECT 1 FROM p.memberEntities m WHERE m.id = :memberId)")
    Page<PartyEntity> findGeneralPartyListNotJoined(
        @Param("memberId") Long memberId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    // 모든 파라미터가 온 경우
    @Query(value = "SELECT p.*, " +
        " (ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.start_longitude, ' ', p.start_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDepartureLng, ' ', :userDepartureLat, ')'))" +
        " ) + " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " )" +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
        "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId) " +
        "ORDER BY total_distance ASC, ABS(TIMESTAMPDIFF(MINUTE, p.start_date_time, :userDepartureTime)) ASC",
        countQuery = "SELECT COUNT(*) FROM party p " +
            "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
            "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId)",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("memberId") Long memberId,
        @Param("userDepartureLng") Double userDepartureLng,
        @Param("userDepartureLat") Double userDepartureLat,
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        @Param("userDepartureTime") LocalDateTime userDepartureTime,
        Pageable pageable
    );

    // 출발지 또는 도착지에 대한 파라미터가 오지 않은 경우(오버로딩)
    @Query(value = "SELECT p.*, " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
        "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId) " +
        "ORDER BY total_distance ASC, ABS(TIMESTAMPDIFF(MINUTE, p.start_date_time, :userDepartureTime)) ASC",
        countQuery = "SELECT COUNT(*) FROM party p " +
            "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
            "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId)",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("memberId") Long memberId,
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        @Param("userDepartureTime") LocalDateTime userDepartureTime,
        Pageable pageable
    );

    // 출발 시간 파라미터가 오지 않은 경우
    @Query(value = "SELECT p.*, " +
        " (ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.start_longitude, ' ', p.start_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDepartureLng, ' ', :userDepartureLat, ')'))" +
        " ) + " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " )" +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
        "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId) " +
        "ORDER BY total_distance ASC, p.start_date_time ASC",
        countQuery = "SELECT COUNT(*) FROM party p " +
            "WHERE p.is_deleted = false AND p.start_date_time >= NOW() " +
            "AND p.party_id NOT IN (SELECT pm.party_id FROM party_member pm WHERE pm.member_id = :memberId)",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("memberId") Long memberId,
        @Param("userDepartureLng") Double userDepartureLng,
        @Param("userDepartureLat") Double userDepartureLat,
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        Pageable pageable
    );

    @Query("SELECT p FROM party p JOIN p.memberEntities m WHERE m.id = :memberId AND p.isDeleted = false " +
        "ORDER BY " +
        // 1. 종료되지 않은 파티(1)가 종료된 파티(2)보다 먼저 오도록 정렬
        "CASE WHEN p.startDateTime >= :now THEN 1 ELSE 2 END ASC, " +
        // 2. 종료되지 않은 파티 그룹 내에서는 출발 시간 오름차순 (가까운 순)
        "CASE WHEN p.startDateTime >= :now THEN p.startDateTime END ASC, " +
        // 3. 종료된 파티 그룹 내에서는 출발 시간 내림차순 (최근에 끝난 순)
        "CASE WHEN p.startDateTime < :now THEN p.startDateTime END DESC")
    List<PartyEntity> findAllByMemberIdSorted(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /**
     * 출발 알림을 보내야 하는 파티 목록을 조회합니다.
     * @param after 지금으로부터 10분 뒤 시간
     * @param before 지금으로부터 11분 뒤 시간
     * @return 조건에 맞는 파티 엔티티 목록
     */
    @Query("SELECT p FROM party p WHERE p.isDeleted = false AND p.departureNotificationSent = false AND p.startDateTime > :after AND p.startDateTime <= :before")
    List<PartyEntity> findPartiesForDepartureReminder(
        @Param("after") LocalDateTime after,
        @Param("before") LocalDateTime before
    );

}
