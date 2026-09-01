package com.echomind.agent;

import com.echomind.intent.IntentCategory;
import com.echomind.intent.UrgencyLevel;
import com.echomind.llm.LlmGateway;
import com.echomind.trace.RequestTraceStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOrchestratorTest {

    private final LlmGateway deterministicLlm = (system, prompt, temperature, maxTokens) -> "处理完成";

    @Test
    void routesCompositeRequestToPrimaryAndSupportingAgentsAndRecordsTrace() {
        RequestTraceStore traceStore = new RequestTraceStore();
        Map<AgentType, List<BaseAgent>> pool = Map.of(
                AgentType.GENERAL, List.of(new GeneralAgent(deterministicLlm, null)),
                AgentType.TECHNICAL, List.of(new TechnicalAgent(deterministicLlm, null)),
                AgentType.BILLING, List.of(new BillingAgent(deterministicLlm, null))
        );
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, pool, traceStore);
        AgentRequest request = new AgentRequest(
                "登录报401，而且重复扣款、支付失败，需要退款",
                "user-1",
                "conversation-1",
                "",
                List.of(),
                Map.of("error_code", List.of("401"), "amount", List.of("99元")),
                IntentCategory.TECHNICAL_LOGIN,
                "technical",
                UrgencyLevel.HIGH,
                0.9,
                "req-1"
        );

        OrchestratorResult result = orchestrator.run(request);

        assertThat(result.primaryAgent()).isEqualTo(AgentType.TECHNICAL);
        assertThat(result.supportingAgents()).contains(AgentType.BILLING);
        assertThat(result.agentTypes()).contains(AgentType.TECHNICAL, AgentType.BILLING);
        assertThat(result.routingReason()).contains("primary=technical", "supporting=billing");
        assertThat(traceStore.find("req-1")).isPresent();
        assertThat(traceStore.find("req-1").orElseThrow().supportingAgents()).contains("billing");
    }
}
