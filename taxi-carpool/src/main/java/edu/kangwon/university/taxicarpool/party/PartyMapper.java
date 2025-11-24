package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.map.MapPlace;
import edu.kangwon.university.taxicarpool.map.MapPlaceDTO;
import edu.kangwon.university.taxicarpool.member.Gender;
import edu.kangwon.university.taxicarpool.party.dto.PartyCreateRequestDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyOptionDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyUpdateRequestDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;


@Component
public class PartyMapper {

    public PartyResponseDTO convertToResponseDTO(PartyEntity partyEntity) {
        MapPlaceDTO startDto = null;
        MapPlaceDTO endDto = null;
        PartyOptionDTO optionDto = null;

        MapPlace startPlace = partyEntity.getStartPlace();
        if (startPlace != null) {
            startDto = new MapPlaceDTO(
                startPlace.getName(),
                startPlace.getRoadAddressName(),
                startPlace.getX(),
                startPlace.getY()
            );
        }
        MapPlace endPlace = partyEntity.getEndPlace();
        if (endPlace != null) {
            endDto = new MapPlaceDTO(
                endPlace.getName(),
                endPlace.getRoadAddressName(),
                endPlace.getX(),
                endPlace.getY()
            );
        }

        List<Long> memberIds = partyEntity.getMemberEntities().stream()
            .map(MemberEntity::getId)
            .collect(Collectors.toList());

        Gender hostGender = partyEntity.getMemberEntities().stream()
            .filter(member -> member.getId().equals(partyEntity.getHostMemberId()))
            .findFirst()
            .map(MemberEntity::getGender)
            .orElse(null);

        PartyOption opt = partyEntity.getOptions();
        if (opt != null) {
            optionDto = new PartyOptionDTO(
                opt.isSameGenderOnly(),
                opt.isCostShareBeforeDropOff(),
                opt.isQuietMode(),
                opt.isDestinationChangeIn5Minutes()
            );
        }

        return new PartyResponseDTO(
            partyEntity.getId(),
            partyEntity.getName(),
            partyEntity.isDeleted(),
            memberIds,
            partyEntity.getHostMemberId(),
            hostGender,
            partyEntity.getEndDate(),
            optionDto,
            partyEntity.getStartDateTime(),
            partyEntity.getComment(),
            partyEntity.getCurrentParticipantCount(),
            partyEntity.getMaxParticipantCount(),
            startDto,
            endDto,
            partyEntity.getNotification(),
            partyEntity.isSavingsCalculated(),
            partyEntity.getEstimatedFare()
        );
    }

    public PartyEntity convertToEntity(PartyCreateRequestDTO createRequestDTO) {

        MapPlaceDTO sp = createRequestDTO.getStartPlace();
        MapPlaceDTO ep = createRequestDTO.getEndPlace();
        MapPlace startPlace = new MapPlace(sp.getName(), sp.getRoadAddressName(), sp.getX(),
            sp.getY());
        MapPlace endPlace = new MapPlace(ep.getName(), ep.getRoadAddressName(), ep.getX(),
            ep.getY());

        PartyOptionDTO o = createRequestDTO.getOptions();
        PartyOption options = new PartyOption(
            o.isSameGenderOnly(),
            o.isCostShareBeforeDropOff(),
            o.isQuietMode(),
            o.isDestinationChangeIn5Minutes()
        );

        return new PartyEntity(
            null,
            options,
            createRequestDTO.getStartDateTime(),
            createRequestDTO.getComment(),
            createRequestDTO.getCurrentParticipantCount(),
            createRequestDTO.getMaxParticipantCount(),
            startPlace,
            endPlace
        );
    }

    public PartyEntity convertToEntityByUpdate(PartyEntity partyEntity,
        PartyUpdateRequestDTO partyUpdateRequestDTO) {
        MapPlaceDTO sp = partyUpdateRequestDTO.getStartPlace();
        MapPlaceDTO ep = partyUpdateRequestDTO.getEndPlace();
        MapPlace startPlace = new MapPlace(sp.getName(), sp.getRoadAddressName(), sp.getX(),
            sp.getY());
        MapPlace endPlace = new MapPlace(ep.getName(), ep.getRoadAddressName(), ep.getX(),
            ep.getY());

        PartyOptionDTO o = partyUpdateRequestDTO.getOptions();
        PartyOption options = new PartyOption(
            o.isSameGenderOnly(),
            o.isCostShareBeforeDropOff(),
            o.isQuietMode(),
            o.isDestinationChangeIn5Minutes()
        );

        return partyEntity.updateParty(
            options,
            partyUpdateRequestDTO.getStartDateTime(),
            partyUpdateRequestDTO.getComment(),
            partyUpdateRequestDTO.getMaxParticipantCount(),
            startPlace,
            endPlace,
            partyUpdateRequestDTO.getNotification()
        );
    }
}
