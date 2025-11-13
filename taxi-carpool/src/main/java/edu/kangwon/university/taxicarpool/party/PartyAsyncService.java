package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.fcm.FcmPushService;
import edu.kangwon.university.taxicarpool.fcm.dto.PushMessageDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import edu.kangwon.university.taxicarpool.party.PartyUtil.PartyUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyAsyncService {

    @Value("${kakaomobility.api.key}")
    private String kakaoMobilityApiKey;
    private final MemberRepository memberRepository;
    private final FcmPushService fcmPushService;

    /**
     * [비동기 작업 1] 카카오 API를 호출하여 예상 요금을 가져옵니다.
     */
    @Async
    public CompletableFuture<Long> getKakaoFareAsync(PartyEntity partyEntity) {
        log.info("Starting Kakao API call in thread: {}", Thread.currentThread().getName());
        try {
            double[] coords = PartyUtil.getValidatedCoords(partyEntity);
            double sx = coords[0], sy = coords[1], ex = coords[2], ey = coords[3];
            String[] od = PartyUtil.toOriginDestination(sx, sy, ex, ey);
            String origin = od[0], destination = od[1];

            LocalDateTime depTime = PartyUtil.ensureFutureDeparture(partyEntity.getStartDateTime());
            String departureTime = PartyUtil.formatDeparture(depTime);

            String url = PartyUtil.buildFutureDirectionsUrl(origin, destination, departureTime);
            String kakaoBody = PartyUtil.fetchKakaoDirectionsJson(url, kakaoMobilityApiKey);
            long totalTaxiFare = PartyUtil.extractTaxiFare(kakaoBody);

            return CompletableFuture.completedFuture(totalTaxiFare);
        } catch (Exception e) {
            log.warn("Kakao API call failed", e);
            return CompletableFuture.completedFuture(0L);
        }
    }

    /**
     * [비동기 작업 2] 멤버의 파티 생성 횟수를 증가시킵니다.
     */
    @Async
    @Transactional
    public void updateMemberCountAsync(Long memberId) {
        log.info("Starting member count update in thread: {}", Thread.currentThread().getName());
        try {
            MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("멤버를 찾을 수 없습니다."));
            member.incrementPartyCreateCount();
            memberRepository.save(member);
        } catch (Exception e) {
            log.error("Failed to update member count for memberId: {}", memberId, e);
        }
    }

    /**
     * [비동기 작업 3] FCM 푸시 알림을 전송합니다. (Fire-and-forget)
     */
    @Async
    public void sendFcmNotificationAsync(PartyEntity partyEntity, Long creatorMemberId) {
        log.info("Starting FCM send in thread: {}", Thread.currentThread().getName());
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH시 mm분");
            String formattedTime = partyEntity.getStartDateTime().format(timeFormatter);
            String destinationName = partyEntity.getEndPlace().getName();

            String title = String.format("%s %s행 파티 생성 완료!", formattedTime, destinationName);
            String fcmBody = "새로운 카풀 파티가 성공적으로 생성되었습니다.";

            PushMessageDTO pushMessage = PushMessageDTO.builder()
                .title(title)
                .body(fcmBody)
                .type("PARTY_CREATED")
                .build();

            fcmPushService.sendPushToUser(creatorMemberId, pushMessage);

        } catch (Exception e) {
            log.warn("FCM 전송에 실패했으나 파티 생성은 완료되었습니다.", e);
        }
    }
}