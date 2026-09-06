package com.trucktree.treeaiagent.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
* 该类用于测试大模型能否调用成功s
* */
@Component
public class SpringAIInvoke /*implements CommandLineRunner*/ {

    @Resource
    @Qualifier("openAiChatModel")
    private ChatModel deepSeekChatModel;

    /*@Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = deepSeekChatModel.call(new Prompt("你好，我是树"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }*/
}
