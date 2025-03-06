package art.snail.naillian.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoAuthRequest {
    /**
     * 카카오 플랫폼 로그인 액세스 토큰
     */
    private String kakaoAccessToken;
}
