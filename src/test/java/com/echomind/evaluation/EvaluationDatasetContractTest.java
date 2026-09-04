package com.echomind.evaluation;

import com.echomind.api.dto.EvalRunRequest;
import com.echomind.intent.IntentCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationDatasetContractTest {

    @Test
    void reproducibleDatasetMatchesApiContractAndKnownIntentLabels() throws Exception {
        Path dataset = Path.of("evaluation", "eval-dataset.json");
        assertThat(dataset).exists();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        EvalRunRequest request = mapper.readValue(Files.readString(dataset), EvalRunRequest.class);

        Set<String> knownLabels = java.util.Arrays.stream(IntentCategory.values())
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        assertThat(request.intentCases()).hasSizeGreaterThanOrEqualTo(10);
        assertThat(request.intentCases())
                .allSatisfy(item -> {
                    assertThat(item.message()).isNotBlank();
                    assertThat(item.expectedIntent()).isIn(knownLabels);
                });
        assertThat(request.dialogCases()).isNotEmpty();
    }
}
