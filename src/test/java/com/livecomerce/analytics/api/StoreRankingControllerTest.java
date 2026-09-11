package com.livecomerce.analytics.api;

import com.livecomerce.analytics.application.port.in.GetStoreRankingUseCase;
import com.livecomerce.analytics.application.port.in.GetStoreRankingUseCase.StoreRankingView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = StoreRankingController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
class StoreRankingControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean GetStoreRankingUseCase getStoreRankingUseCase;

    @Test
    void getRanking_returns200WithSnapshot() throws Exception {
        var storeId = UUID.randomUUID();
        var view = new StoreRankingView(storeId, 1, new BigDecimal("0.812345"), new BigDecimal("4.50"),
                5, 120L, new BigDecimal("15000.00"), OffsetDateTime.now());
        when(getStoreRankingUseCase.getLatestRanking(any())).thenReturn(new PageImpl<>(List.of(view)));

        mvc.perform(get("/api/stores/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.content[0].rank").value(1));
    }

    @Test
    void getRanking_whenEmpty_returns200WithEmptyPage() throws Exception {
        when(getStoreRankingUseCase.getLatestRanking(any())).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/stores/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
