package com.ddd.application.mapper;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.dto.recommendation.PointCategoryGainDto;
import com.ddd.domain.model.recommendation.RecommendationFeatures.Gain;
import com.ddd.domain.model.recommendation.RecommendationFeatures.Ranked;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecommendationDtoMapper {
    PointCategoryGainDto toPointGain(Gain gain);

    default EventRecommendationDto toDto(Ranked rank, EventInfoDto event, long daysUntilHappen,
                                         boolean followsFanpage) {
        var candidate = rank.candidate();
        var score = candidate.score();
        var benefits = score.benefits();
        return new EventRecommendationDto(
                event.withRegistrationInfo(candidate.registeredCount(), followsFanpage, rank.full(), rank.registrationClosed()),
                rank.effectiveScore(), score.explanation(), candidate.remainingSlots(), daysUntilHappen,
                benefits.fulfillsCriteria(), benefits.pointGains().stream().map(this::toPointGain).toList(),
                followsFanpage, benefits.fanpageMatch() ? 3 : 0, Math.min(3, benefits.typeMatches()),
                rank.registrationClosed(), rank.full(), candidate.viewed(), score.personalized(),
                rank.urgencyScore(), candidate.readPenalty(), rank.effectiveScore());
    }
}
