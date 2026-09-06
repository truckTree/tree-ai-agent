package com.trucktree.treeaiagent.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {

    private static final long SSE_TIMEOUT_MS = 2 * 60 * 1000L;

    private String name;

    private String systemPrompt;

    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int maxSteps = 10;

    private int currentStep = 0;

    private ChatClient chatClient;

    private List<Message> messageList = new ArrayList<>();

    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtils.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        state = AgentState.RUNNING;

        messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);

                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        }
    }


    public SseEmitter runStream(String userPrompt) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out");
            try {
                emitter.send("超时，请稍后再试");
            } catch (Exception e) {
                log.warn("Failed to send timeout message", e);
            }
            state = AgentState.IDLE;
            emitter.complete();
        });
        if (this.state != AgentState.IDLE) {
            emitter.completeWithError(new RuntimeException("Cannot run agent from state: " + this.state));
            return emitter;
        }
        if (StringUtils.isBlank(userPrompt)) {
            emitter.completeWithError(new RuntimeException("Cannot run agent with empty user prompt"));
            return emitter;
        }

        state = AgentState.RUNNING;

        messageList.add(new UserMessage(userPrompt));

        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step " + stepNumber + "/" + maxSteps);

                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    emitter.send(result);
                }

                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    emitter.send("Terminated: Reached max steps (" + maxSteps + ")");
                }
                emitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    public abstract String step();
}
