package art.snail.naillian.backend.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloudfront.cdn}")
    private String cloudFrontCdn;

    /**
     * CloudFront를 통한 모델 다운로드 URL 반환
     * @param fileName 모델의 파일명
     * @return CloudFront URL
     */
    public String getCloudFrontModelUrl(String fileName) {
        return cloudFrontCdn + "/" + fileName;
    }
}
