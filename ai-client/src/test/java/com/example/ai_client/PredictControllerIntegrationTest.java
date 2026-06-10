package com.example.ai_client;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PredictControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @Test
    @DisplayName("POST /api/predict → 200 OK з результатом")
    void testPredict_endToEnd() throws Exception {
        when(aiService.predict("привіт")).thenReturn("Отримав: привіт");

        mockMvc.perform(post("/api/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"привіт\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Отримав: привіт"));
    }

    @Test
    @DisplayName("POST /api/predict з порожнім текстом → 200 OK")
    void testPredict_emptyText() throws Exception {
        when(aiService.predict("")).thenReturn("Отримав: ");

        mockMvc.perform(post("/api/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Отримав: "));
    }
}