package com.trucktree.treeaiagent.app;

import com.trucktree.treeaiagent.advisor.LoggerAdvisor;
import com.trucktree.treeaiagent.advisor.ReReadingAdvisor;
import com.trucktree.treeaiagent.constants.PromptConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/*
* 各类型的问答客户端，适用于简单的一问一答
* */
@Component
@Slf4j
public class FoodApp {

    private final ChatClient chatClient;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public FoodApp(@Qualifier("openAiChatModel") ChatModel deepSeekChatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        chatClient = ChatClient.builder(deepSeekChatModel)
                .defaultSystem(PromptConstants.FOOD_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new LoggerAdvisor()
                ).build();
    }

    public String doChat(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt().user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /*
    * 通过文档读取器读取本地文档到内存向量数据库实现的最简单的查询增强
    * */
    public String doChatWithRAG(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt().user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /*
    * 通过MCP调用进行对话
    * */
    public String dochatWithMCP(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt().user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
