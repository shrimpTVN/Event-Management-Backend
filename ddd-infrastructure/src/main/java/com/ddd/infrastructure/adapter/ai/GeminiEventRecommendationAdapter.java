package com.ddd.infrastructure.adapter.ai;

import com.ddd.domain.model.port.EventRecommendationAiPort;
import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.CandidateEventRecommendation;
import com.ddd.domain.model.recommendation.StudentRecommendationContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class GeminiEventRecommendationAdapter implements EventRecommendationAiPort {

    private final String apiKey;
    private final String modelName;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiEventRecommendationAdapter(
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String modelName,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "gemini-1.5-flash";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public GeminiEventRecommendationAdapter(
            String apiKey,
            String modelName
    ) {
        this(apiKey, modelName, new ObjectMapper());
    }

    @Override
    public List<AiRecommendationResult> generateExplanations(
            StudentRecommendationContext context,
            List<CandidateEventRecommendation> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Fallback directly if no API key is provided
        if (apiKey.isBlank() || apiKey.equalsIgnoreCase("CHANGE_ME") || apiKey.contains("YOUR_")) {
            log.info("Gemini API key is not configured, applying rule-based recommendation explanations.");
            return buildFallbackResults(candidates);
        }

        try {
            return callGeminiApi(context, candidates);
        } catch (Exception e) {
            log.warn("Gemini recommendation call encountered an error: {}. Falling back to rule-based explanation.", e.getMessage());
            return buildFallbackResults(candidates);
        }
    }

    private List<AiRecommendationResult> callGeminiApi(
            StudentRecommendationContext context,
            List<CandidateEventRecommendation> candidates
    ) throws Exception {
        String prompt = buildPrompt(context, candidates);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("role", "user", "parts", List.of(textPart));

        Map<String, Object> systemInstructionObj = Map.of(
                "parts", List.of(Map.of("text", """
                        Bạn là Cố vấn học tập AI chuyên nghiệp và tận tâm tại Trường Đại học.
                        Nhiệm vụ của bạn là phân tích danh sách các sự kiện sắp diễn ra và đưa ra đánh giá điểm phù hợp (aiScore: 0.0 - 100.0) cùng lời giải thích (aiExplanation) cá nhân hóa cho sinh viên nhằm tối ưu hóa Điểm Rèn Luyện (ĐRL) và đạt các Tiêu chí Sinh viên 5 Tốt (SV5T).

                        Quy tắc đánh giá và phản hồi:
                        1. aiScore (0.0 đến 100.0):
                           - 85 - 100: Rất ưu tiên (Bù đắp điểm ĐRL còn thiếu ở các mục trọng điểm HOẶC đáp ứng tiêu chí SV5T còn nợ).
                           - 70 - 84: Phù hợp cao (Có điểm ĐRL tốt, thuộc CLB sinh viên theo dõi, hoặc sự kiện khẩn cấp sắp hết hạn/hết slot).
                           - 50 - 69: Phù hợp vừa (Phù hợp với sở thích/lịch sử tham gia).
                           - Dưới 50: Ít phù hợp hơn so với các sự kiện khác.
                        2. aiExplanation:
                           - Viết bằng tiếng Việt tự nhiên, súc tích (1-2 câu ngắn), xưng hô "Bạn".
                           - Nêu RÕ RÀNG lý do: nêu cụ thể tên danh mục ĐRL được cộng (nếu có), số điểm nhận được, tên tiêu chí SV5T cụ thể mà sự kiện đáp ứng (nếu có).
                           - Nếu sự kiện diễn ra trong 3 ngày tới hoặc còn ít slot, hãy nhắc nhẹ để sinh viên không bỏ lỡ.
                           - Không lặp lại cùng một khuôn mẫu cho tất cả các sự kiện.
                        """))
        );

        // Define JSON schema for structured outputs
        Map<String, Object> eventIdProp = Map.of("type", "INTEGER");
        Map<String, Object> aiScoreProp = Map.of("type", "NUMBER", "description", "Điểm đánh giá từ 0.0 đến 100.0");
        Map<String, Object> explanationProp = Map.of("type", "STRING", "description", "Lời giải thích 1-2 câu tiếng Việt");

        Map<String, Object> itemSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "eventId", eventIdProp,
                        "aiScore", aiScoreProp,
                        "aiExplanation", explanationProp
                ),
                "required", List.of("eventId", "aiScore", "aiExplanation")
        );

        Map<String, Object> responseSchema = Map.of(
                "type", "ARRAY",
                "items", itemSchema
        );

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema,
                "temperature", 0.3
        );

        Map<String, Object> requestPayload = Map.of(
                "systemInstruction", systemInstructionObj,
                "contents", List.of(contentObj),
                "generationConfig", generationConfig
        );

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent",
                modelName);

        String responseBody = restClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        return parseGeminiResponse(responseBody, candidates);
    }

    private String buildPrompt(StudentRecommendationContext context, List<CandidateEventRecommendation> candidates) throws Exception {
        String candidatesJson = objectMapper.writeValueAsString(candidates);

        String pointGapsDisplay = (context.missingPointGapsNamed() != null && !context.missingPointGapsNamed().isEmpty())
                ? context.missingPointGapsNamed().toString()
                : (context.missingPointGaps() != null && !context.missingPointGaps().isEmpty()
                    ? context.missingPointGaps().toString()
                    : "Không còn thiếu mục nào");

        String criteriaDisplay = (context.missingCriteriaNames() != null && !context.missingCriteriaNames().isEmpty())
                ? String.join(", ", context.missingCriteriaNames())
                : (context.missingCriteriaIds() != null && !context.missingCriteriaIds().isEmpty()
                    ? context.missingCriteriaIds().toString()
                    : "Đã hoàn thành toàn bộ");

        return """
                [THÔNG TIN SINH VIÊN]
                - Email: %s
                - Các mục Điểm Rèn Luyện (ĐRL) còn thiếu: %s
                - Các Tiêu chí Sinh viên 5 Tốt (SV5T) chưa hoàn thành: %s

                [DANH SÁCH SỰ KIỆN ỨNG VIÊN ĐÃ ĐƯỢC CHỌN LỌC]
                %s

                Hãy chấm điểm (aiScore) và tạo lời giải thích (aiExplanation) cá nhân hóa cho từng sự kiện trên.
                """.formatted(
                context.email(),
                pointGapsDisplay,
                criteriaDisplay,
                candidatesJson
        );
    }

    private List<AiRecommendationResult> parseGeminiResponse(String responseBody, List<CandidateEventRecommendation> candidates) {
        if (responseBody == null || responseBody.isBlank()) {
            return buildFallbackResults(candidates);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidatesArray = rootNode.path("candidates");
            if (!candidatesArray.isArray() || candidatesArray.isEmpty()) {
                return buildFallbackResults(candidates);
            }

            JsonNode textNode = candidatesArray.get(0).path("content").path("parts").get(0).path("text");
            String rawJson = textNode.asText().trim();

            // Extract JSON array between first '[' and last ']' safely, or strip markdown fences
            int openBracket = rawJson.indexOf('[');
            int closeBracket = rawJson.lastIndexOf(']');
            if (openBracket != -1 && closeBracket != -1 && closeBracket > openBracket) {
                rawJson = rawJson.substring(openBracket, closeBracket + 1);
            } else {
                if (rawJson.startsWith("```json")) {
                    rawJson = rawJson.substring(7);
                }
                if (rawJson.startsWith("```")) {
                    rawJson = rawJson.substring(3);
                }
                if (rawJson.endsWith("```")) {
                    rawJson = rawJson.substring(0, rawJson.length() - 3);
                }
            }
            rawJson = rawJson.trim();

            List<AiRecommendationResult> parsedList = objectMapper.readValue(
                    rawJson,
                    new TypeReference<List<AiRecommendationResult>>() {}
            );

            // Reconcile parsed results with candidates to ensure all candidate eventIds have an entry
            Map<Long, AiRecommendationResult> resultMap = new HashMap<>();
            for (AiRecommendationResult item : parsedList) {
                if (item != null && item.eventId() != null) {
                    resultMap.put(item.eventId(), item);
                }
            }

            List<AiRecommendationResult> finalResults = new ArrayList<>();
            for (CandidateEventRecommendation c : candidates) {
                if (resultMap.containsKey(c.eventId())) {
                    finalResults.add(resultMap.get(c.eventId()));
                } else {
                    finalResults.add(buildFallbackSingle(c));
                }
            }
            return finalResults;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response JSON: {}. Applying fallback.", e.getMessage());
            return buildFallbackResults(candidates);
        }
    }

    private List<AiRecommendationResult> buildFallbackResults(List<CandidateEventRecommendation> candidates) {
        List<AiRecommendationResult> results = new ArrayList<>();
        for (CandidateEventRecommendation c : candidates) {
            results.add(buildFallbackSingle(c));
        }
        return results;
    }

    private AiRecommendationResult buildFallbackSingle(CandidateEventRecommendation c) {
        StringBuilder explanation = new StringBuilder();

        if (c.totalEffectivePoints() > 0 && c.fulfillsCriteria()) {
            explanation.append(String.format("Giúp bạn tích lũy thêm %d điểm rèn luyện và hoàn thành tiêu chí %s.",
                    c.totalEffectivePoints(), c.criteriaName() != null ? c.criteriaName() : "SV5T"));
        } else if (c.totalEffectivePoints() > 0) {
            explanation.append(String.format("Giúp bạn bù đắp %d điểm rèn luyện còn thiếu trong học kỳ này.",
                    c.totalEffectivePoints()));
        } else if (c.fulfillsCriteria()) {
            explanation.append(String.format("Giúp bạn đạt tiêu chí %s cho danh hiệu Sinh viên 5 tốt.",
                    c.criteriaName() != null ? c.criteriaName() : "SV5T"));
        } else {
            explanation.append("Sự kiện phù hợp để tích lũy kinh nghiệm và mở rộng kết nối.");
        }

        if (c.daysUntilHappen() <= 3 && c.remainingSlots() > 0) {
            explanation.append(String.format(" Sự kiện diễn ra trong %d ngày tới và chỉ còn %d slot!",
                    c.daysUntilHappen(), c.remainingSlots()));
        } else if (c.remainingSlots() > 0) {
            explanation.append(String.format(" Hiện còn %d slot trống.", c.remainingSlots()));
        }

        if (c.isFollowedFanpage()) {
            explanation.append(" Thuộc CLB/Đơn vị bạn đang theo dõi.");
        }

        return new AiRecommendationResult(c.eventId(), c.baseScore(), explanation.toString().trim());
    }
}
