package com.jhklim.investsim.site.upbit;

import com.jhklim.investsim.site.upbit.dto.CandleData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitRestClient {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.upbit.com/v1/candles/minutes/1";

    public List<CandleData> getCandles(String market, int count) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("market", market)
                .queryParam("count", count)
                .toUriString();

        try {
            CandleData[] response = restTemplate.getForObject(url, CandleData[].class);
            if (response == null) return Collections.emptyList();

            List<CandleData> candles = Arrays.asList(response);
            Collections.reverse(candles); // 업비트는 최신순으로 주기 때문에 역순 정렬 (오래된 것 → 최신 순)

            log.info("캔들 {}개 로드 완료 - {}", candles.size(), market);
            return candles;

        } catch (Exception e) {
            log.error("캔들 데이터 로드 실패 - {}", market, e);
            return Collections.emptyList();
        }
    }
}
