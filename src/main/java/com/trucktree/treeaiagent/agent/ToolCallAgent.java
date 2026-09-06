package com.trucktree.treeaiagent.agent;

import com.trucktree.treeaiagent.tools.TerminateTool;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final TerminateTool terminateTool = new TerminateTool();

    private final ToolCallbackProvider terminateToolProvider = MethodToolCallbackProvider.builder()
            .toolObjects(terminateTool)
            .build();

    private final ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

    /*
    * 挂载高德地图MCP
    * */
    @Resource
    private ToolCallbackProvider mcpToolCallbackProvider;

    @Override
    public boolean think() {
        terminateTool.reset();

        List<ToolCallback> toolCallbacks = new ArrayList<>();
        if (mcpToolCallbackProvider != null) {
            toolCallbacks.addAll(List.of(mcpToolCallbackProvider.getToolCallbacks()));
        }
        toolCallbacks.addAll(List.of(terminateToolProvider.getToolCallbacks()));

        /*
        * 如果使用SpringAI的工具托管，没有办法直接得到工具调用的结果。在跨step时候只能拿到用户提示词和助手提示词。
        * 没有工具调用上下文，回答质量会比较差，因此通过手动管理工具调用将工具调用结果放入
        * */
        ToolCallingChatOptions toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false)
                .build();

        List<Message> requestMessages = buildMessages();
        ChatResponse chatResponse = callModel(requestMessages, toolOptions);

        if (chatResponse == null || chatResponse.getResult() == null) {
            log.warn("模型返回结果为空");
            return false;
        }

        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        if (!chatResponse.hasToolCalls()) {
            getMessageList().add(assistantMessage);
            setState(AgentState.FINISHED);
            return true;
        }

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
                new Prompt(requestMessages, toolOptions), chatResponse);
        /*
        * 直接把记忆写入List中自行维护。对于个人学习来说是比较简单合理的方法。
        * 正式产品需要自己实现ChatMemory进行记忆挂载
        * */
        List<Message> history = toolExecutionResult.conversationHistory();
        getMessageList().add(history.get(history.size() - 2));
        getMessageList().add(history.get(history.size() - 1));

        if (terminateTool.isTerminated()) {
            log.info("终止工具已被调用，任务结束");
            setState(AgentState.FINISHED);
            return false;
        }
        return true;
    }

    private List<Message> buildMessages() {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(getSystemPrompt())) {
            messages.add(new SystemMessage(getSystemPrompt()));
        }
        messages.addAll(getMessageList());
        return messages;
    }

    private ChatResponse callModel(List<Message> messages, ToolCallingChatOptions options) {
        return getChatClient().prompt()
                .messages(messages)
                .options(options)
                .call()
                .chatResponse();
    }

    /*
    * 由于SpringAI的工具调用特性，在think的时候已经手动控制住了。这里直接返回最新的助手提示词供前端展示即可
    * */
    @Override
    public String act() {
        return getMessageList().stream()
                .filter(p -> p.getMessageType() == MessageType.ASSISTANT)
                .toList().getLast().getText();
    }
}
