package edu.kangwon.university.taxicarpool.appLink;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppleUrlController {

    @GetMapping("/.well-known/apple-app-site-association")
    public ResponseEntity<String> appleAppSiteAssociation() {
        String jsonContent = """
            {
              "applinks": {
                "apps": [],
                "details": [
                  {
                    "appID": "VAXVS3URWH.knu.taxi.carpool",
                    "components": [
                      {
                        "/": "/reset_password",
                        "comment": "비밀번호 재설정 페이지 연결"
                      }
                    ]
                  }
                ]
              }
            }
            """;

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonContent);
    }
}