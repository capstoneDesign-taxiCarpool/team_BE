package edu.kangwon.university.taxicarpool.map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "지도 검색 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @Operation(
        summary = "장소 검색",
        description = "키워드와 좌표를 기준으로 주변 장소를 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<MapSearchResponseDTO> searchPlaces(
        @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword
    ) {
        MapSearchResponseDTO result = mapService.search(keyword);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "좌표로 주소 변환", description = "위도/경도를 받아 해당 위치의 장소명(건물명)을 반환합니다.")
    @GetMapping("/reverse-geocoding")
    public ResponseEntity<MapSearchResponseDTO> getAddressByCoordinates(
        @Parameter(description = "위도(y)", required = true) @RequestParam double latitude,
        @Parameter(description = "경도(x)", required = true) @RequestParam double longitude
    ) {
        MapSearchResponseDTO result = mapService.reverseGeocoding(latitude, longitude);
        return ResponseEntity.ok(result);
    }

}
