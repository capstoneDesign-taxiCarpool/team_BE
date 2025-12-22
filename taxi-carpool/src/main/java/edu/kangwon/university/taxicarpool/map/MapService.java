package edu.kangwon.university.taxicarpool.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kangwon.university.taxicarpool.map.exception.KakaoApiParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MapService {

    private final RestTemplate restTemplate;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;
    private final double LATITUDE_KNU = 37.869129;
    private final double LONGITUDE_KNU = 127.742718;

    public MapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 키워드로 장소를 검색합니다.
     *
     * <p>강원대학교 좌표를 기준으로 반경 2km 내의 결과를 1페이지, 15건, 정확도순으로 조회합니다.
     * 응답 본문에서 {@code place_name}, {@code road_address_name}, {@code x}, {@code y}
     * 필드가 모두 존재하는 항목만 결과에 포함합니다.</p>
     *
     * @param keyword 검색 키워드(예: "카페", "버스정류장")
     * @return 검색 결과 목록을 담은 {@link edu.kangwon.university.taxicarpool.map.MapSearchResponseDTO}
     * @throws edu.kangwon.university.taxicarpool.map.exception.KakaoApiParseException
     *         응답 본문을 JSON으로 변환하거나 필드 파싱에 실패한 경우
     */
    public MapSearchResponseDTO search(String keyword) {
        String url = UriComponentsBuilder.fromUriString(
                "https://dapi.kakao.com/v2/local/search/keyword.json")
            .queryParam("query", keyword)
            .queryParam("x", LATITUDE_KNU)
            .queryParam("y", LONGITUDE_KNU)
            .queryParam("radius", 2000) // 반경 2km로 고정
            .queryParam("page", 1)
            .queryParam("size", 15)
            .queryParam("sort", "accuracy")
            .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        // JSON -> 객체 변환
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
            JsonNode documents = root.get("documents");

            List<MapPlaceDTO> places = new ArrayList<>();

            for (JsonNode doc : documents) {
                // 필수 필드가 하나라도 없으면 해당 객체 건너뛰기
                if (doc.get("place_name") == null ||
                    doc.get("road_address_name") == null ||
                    doc.get("x") == null ||
                    doc.get("y") == null) {
                    continue;
                }

                MapPlaceDTO place = new MapPlaceDTO(
                    doc.get("place_name").asText(),
                    doc.get("road_address_name").asText(),
                    doc.get("x").asDouble(),
                    doc.get("y").asDouble()
                );
                places.add(place);
            }

            return new MapSearchResponseDTO(places);

        } catch (JsonProcessingException e) {
            throw new KakaoApiParseException("카카오 API 응답 파싱 실패");
        }
    }

    public MapSearchResponseDTO reverseGeocoding(double latitude, double longitude) {
        try {
            String keywordUrl = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", "강원대학교")
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .queryParam("radius", 200)
                .queryParam("sort", "distance")
                .build()
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                keywordUrl, HttpMethod.GET, entity, String.class
            );

            MapSearchResponseDTO result = parseKeywordResponse(response.getBody(), latitude, longitude);

            if (!result.getPlaces().isEmpty() &&
                !"알 수 없는 장소".equals(result.getPlaces().get(0).getName())) {
                return result;
            }

        } catch (Exception e) {
            System.out.println("1차 건물 검색 실패, 2차 주소 변환 시도: " + e.getMessage());
        }

        return searchAddressAsFallback(latitude, longitude);
    }

    private MapSearchResponseDTO parseKeywordResponse(String jsonBody, double lat, double lng) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<MapPlaceDTO> places = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode documents = root.get("documents");

            if (documents.isArray() && documents.size() > 0) {
                JsonNode doc = documents.get(0);

                String placeName = doc.get("place_name").asText();
                String roadAddr = doc.get("road_address_name").asText();
                String jibunAddr = doc.get("address_name").asText();

                String fullAddress = (roadAddr != null && !roadAddr.isEmpty()) ? roadAddr : jibunAddr;

                places.add(new MapPlaceDTO(placeName, fullAddress, lng, lat));
            } else {
                places.add(new MapPlaceDTO("알 수 없는 장소", "", lng, lat));
            }

        } catch (JsonProcessingException e) {
            return new MapSearchResponseDTO(List.of(new MapPlaceDTO("알 수 없는 장소", "", lng, lat)));
        }

        return new MapSearchResponseDTO(places);
    }

    private MapSearchResponseDTO searchAddressAsFallback(double latitude, double longitude) {
        String addressUrl = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v2/local/geo/coord2address.json")
            .queryParam("x", longitude)
            .queryParam("y", latitude)
            .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                addressUrl, HttpMethod.GET, entity, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode documents = root.get("documents");

            if (documents.isArray() && documents.size() > 0) {
                JsonNode doc = documents.get(0);

                String addressName = "";

                if (doc.has("road_address") && !doc.get("road_address").isNull()) {
                    addressName = doc.get("road_address").get("address_name").asText();
                } else {
                    addressName = doc.get("address").get("address_name").asText();
                }

                return new MapSearchResponseDTO(List.of(
                    new MapPlaceDTO(addressName, addressName, longitude, latitude)
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new MapSearchResponseDTO(List.of(
            new MapPlaceDTO("위치 정보 없음", "주소를 찾을 수 없습니다.", longitude, latitude)
        ));
    }
}
