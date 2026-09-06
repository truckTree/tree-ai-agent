package com.trucktree.treeaiagent.agent;

import com.trucktree.treeaiagent.advisor.LoggerAdvisor;
import com.trucktree.treeaiagent.constants.PromptConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LocalLifeManus extends ToolCallAgent{

    public LocalLifeManus(@Qualifier("openAiChatModel") ChatModel deepSeekChatModel){
        this.setName("LocalLifeManus");
        this.setSystemPrompt(PromptConstants.AGENT_FOOD_SYSTEM_PROMPT);
        ChatClient chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(new LoggerAdvisor()).build();
        this.setChatClient(chatClient);
    }

}
