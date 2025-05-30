package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.chatting.ChattingService;
import edu.kangwon.university.taxicarpool.chatting.MessageType;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import edu.kangwon.university.taxicarpool.party.dto.PartyDTO.PartyCreateRequestDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyDTO.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyDTO.PartyUpdateRequestDTO;
import edu.kangwon.university.taxicarpool.party.partyException.MemberAlreadyInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyAlreadyDeletedException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyFullException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyGetCustomException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException;
import edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyMapper partyMapper;
    private final MemberRepository memberRepository;
    private final ChattingService chattingService;

    @Autowired
    PartyService(PartyRepository partyRepository,
        PartyMapper partyMapper,
        MemberRepository memberRepository, ChattingService chattingService
    ) {
        this.partyRepository = partyRepository;
        this.partyMapper = partyMapper;
        this.memberRepository = memberRepository;
        this.chattingService = chattingService;
    }

    public PartyResponseDTO getParty(Long partyId) {
        PartyEntity partyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        return partyMapper.convertToResponseDTO(partyEntity);
    }

    public Page<PartyResponseDTO> getPartyList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
        Page<PartyEntity> partyEntities = partyRepository.findAllByIsDeletedFalse(pageable);
        return partyEntities.map(partyMapper::convertToResponseDTO);
    }

    @Transactional
    public Page<PartyResponseDTO> getCustomPartyList(
        Double userDepartureLng,
        Double userDepartureLat,
        Double userDestinationLng,
        Double userDestinationLat,
        LocalDateTime userDepartureTime,
        Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size);

        if (userDepartureTime != null && userDepartureTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("출발 시간은 현재 시간보다 이후여야 합니다.");
        }

        // 각 그룹(출발지, 도착지, 출발시간)의 누락 여부 확인
        boolean missingDeparture = (userDepartureLng == null || userDepartureLat == null);
        boolean missingDestination = (userDestinationLng == null || userDestinationLat == null);
        boolean missingDepartureTime = (userDepartureTime == null);

        int missingCount = 0;
        if (missingDeparture) {
            missingCount++;
        }
        if (missingDestination) {
            missingCount++;
        }
        if (missingDepartureTime) {
            missingCount++;
        }

        // 2개 이상의 정보가 누락되었으면 예외 발생
        if (missingCount >= 2) {
            throw new PartyGetCustomException("출발지, 도착지, 출발시간에 대한 정보 중 2개 이상 넣어주세요!");
        }

        Page<PartyEntity> partyEntities = null;

        // 모든 정보가 있는 경우
        if (!missingDeparture && !missingDestination && !missingDepartureTime) {
            partyEntities = partyRepository.findCustomPartyList(
                userDepartureLng,
                userDepartureLat,
                userDestinationLng,
                userDestinationLat,
                userDepartureTime,
                pageable);

            // 출발지 정보가 누락된 경우
        } else if (missingDeparture) {
            partyEntities = partyRepository.findCustomPartyList(
                userDestinationLng,
                userDestinationLat,
                userDepartureTime,
                pageable);

            // 도착지 정보가 누락된 경우
        } else if (missingDestination) {
            partyEntities = partyRepository.findCustomPartyList(
                userDepartureLng,
                userDepartureLat,
                userDepartureTime,
                pageable);

            // 출발시간이 누락된 경우
        } else if (missingDepartureTime) {
            partyEntities = partyRepository.findCustomPartyList(
                userDepartureLng,
                userDepartureLat,
                userDestinationLng,
                userDestinationLat,
                pageable);

        }

        return partyEntities.map(partyMapper::convertToResponseDTO);

    }

    @Transactional
    public PartyResponseDTO createParty(PartyCreateRequestDTO createRequestDTO,
        Long CreatorMemberId) {

        PartyEntity partyEntity = partyMapper.convertToEntity(createRequestDTO);

        if (CreatorMemberId != null) {
            partyEntity.setHostMemberId(
                CreatorMemberId); // creatorMemberId(파티를 만든 멤버의 ID)를 HostMemberId로 설정
        } else {
            throw new IllegalArgumentException("파티방을 만든 멤버의 Id가 null임.");
        }

        // 처음 방 만든 멤버도 그 파티방의 멤버로 등록하는 것임.(이거 안 해놓으면 프론트한테 요청 2번 요청해야함.)
        MemberEntity member = memberRepository.findById(CreatorMemberId)
            .orElseThrow(() -> new MemberNotFoundException("파티방을 만든 멤버가 존재하지 않습니다."));
        partyEntity.getMemberEntities().add(member);

        partyEntity.setCurrentParticipantCount(1); // 방 만들고, 현재 인원 1명으로 설정

        PartyEntity savedPartyEntity = partyRepository.save(partyEntity);
        return partyMapper.convertToResponseDTO(savedPartyEntity);
    }

    @Transactional
    public PartyResponseDTO updateParty(Long partyId, Long memberId,
        PartyUpdateRequestDTO updateRequestDTO) {
        PartyEntity existingPartyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));

        if (!existingPartyEntity.getHostMemberId().equals(memberId)) {
            throw new UnauthorizedHostAccessException("호스트만 수정할 수 있습니다.");
        }

        partyMapper.convertToEntityByUpdate(existingPartyEntity, updateRequestDTO);

        PartyEntity savedPartyEntity = partyRepository.save(existingPartyEntity);
        return partyMapper.convertToResponseDTO(savedPartyEntity);
    }


    @Transactional
    public Map<String, Object> deleteParty(Long partyId, Long memberId) {
        PartyEntity partyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        if (!partyEntity.getHostMemberId().equals(memberId)) {
            throw new UnauthorizedHostAccessException("호스트만 삭제할 수 있습니다.");
        }

        partyEntity.setDeleted(true);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "파티가 성공적으로 삭제되었습니다.");
        response.put("deletedPartyId", partyId);

        return response;
    }

    // 멤버가 파티방의 멤버로 참가하는 로직의 메서드
    @Transactional
    public PartyResponseDTO joinParty(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        if (party.isDeleted()) {
            throw new PartyAlreadyDeletedException("이미 삭제된 파티입니다.");
        }
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다."));

        if (party.getMemberEntities().contains(member)) {
            throw new MemberAlreadyInPartyException("이미 이 파티에 참여한 멤버입니다.");
        }

        party.getMemberEntities().add(member);

        // 새로운 멤버가 파티 참가시, 현재인원 1명 추가
        int currentParticipantCount = party.getCurrentParticipantCount();
        if (currentParticipantCount < party.getMaxParticipantCount()) {
            currentParticipantCount += 1;
            party.setCurrentParticipantCount(currentParticipantCount);
        } else {
            throw new PartyFullException("현재 파티의 참여중인 인원수가 가득찼습니다.");
        }

        PartyEntity savedParty = partyRepository.save(party);
        chattingService.createSystemMessage(party, member, MessageType.ENTER);
        return partyMapper.convertToResponseDTO(savedParty);
    }

    @Transactional
    public PartyResponseDTO leaveParty(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다."));

        if (!party.getMemberEntities().contains(member)) {
            throw new MemberNotInPartyException("이 멤버는 해당 파티에 속해있지 않습니다.");
        }

        // 호스트인 멤버가 파티를 떠나려고 할 때의 로직을 위한 isHostLeaving
        boolean isHostLeaving = (party.getHostMemberId() != null
            && party.getHostMemberId().equals(memberId));

        // 파티에서 멤버 제거
        party.getMemberEntities().remove(member);

        // 파티의 현재 인원 수 감소시키기
        int currentParticipantCount = party.getCurrentParticipantCount();
        if (currentParticipantCount > 1) {
            currentParticipantCount -= 1;
            party.setCurrentParticipantCount(currentParticipantCount);
        } else {
            // 앱의 플로우상 마지막으로 떠나는 멤버가 호스트일수밖에 없어서, 해당 코드가 필요하지 않을 것 같긴한데, 혹시 몰라서 일단 추가해둠.
            party.setDeleted(true);
            return partyMapper.convertToResponseDTO(party);
        }

        // 호스트인 멤버가 파티를 떠나려고 할 때의 로직.
        if (isHostLeaving) {
            List<MemberEntity> remaining = party.getMemberEntities();
            if (remaining.isEmpty()) {
                // 아무도 없으면 삭제 처리
                party.setDeleted(true);
                return partyMapper.convertToResponseDTO(party);
            } else {
                // 첫 멤버를 새 호스트로(호스트 제외하고 가장 빨리 들어온 멤버)
                MemberEntity nextHost = remaining.get(0);
                party.setHostMemberId(nextHost.getId());
            }
        }

        PartyEntity saved = partyRepository.save(party);
        chattingService.createSystemMessage(party, member, MessageType.LEAVE);
        return partyMapper.convertToResponseDTO(saved);
    }

    /**
     * 사용자가 속한 모든 파티를 조회합니다.
     *
     * @param memberId 사용자 ID
     * @return 사용자가 속한 모든 파티 목록
     */
    @Transactional(readOnly = true)
    public List<PartyResponseDTO> getMyParties(Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException("해당 멤버가 존재하지 않습니다: " + memberId);
        }

        List<PartyEntity> activeParties = partyRepository.findAllActivePartiesByMemberId(memberId);

        return activeParties.stream()
            .map(partyMapper::convertToResponseDTO)
            .toList();
    }


}
