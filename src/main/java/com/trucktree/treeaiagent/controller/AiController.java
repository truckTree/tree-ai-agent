package com.trucktree.treeaiagent.controller;

import com.trucktree.treeaiagent.agent.LocalLifeManus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    @Qualifier("openAiChatModel")
    private ChatModel deepSeekChatModel;

    private final Map<String, List<Message>> messageMemory = new ConcurrentHashMap<>();

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, String messageId) {
        LocalLifeManus treeManus = new LocalLifeManus(deepSeekChatModel);
        log.info("[aiController] message:{},messageId:{}",message,messageId);
        // 通过 messageId 恢复历史消息，实现记忆存储
        String key = messageId != null ? messageId : UUID.randomUUID().toString();
        List<Message> history = messageMemory.computeIfAbsent(key, k -> new ArrayList<>());
        treeManus.setMessageList(history);

        return treeManus.runStream(message);
    }
}
