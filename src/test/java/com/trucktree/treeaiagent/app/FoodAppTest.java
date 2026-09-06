package com.trucktree.treeaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FoodAppTest {

    @Resource
    private FoodApp foodApp;

    @Test
    void doChat(){
        String chatId = "tree";
        String message = "你好，我是树";
        foodApp.doChat(message,chatId);
        message = "帮我推荐杭州大悦城附近的好吃的";
        foodApp.doChat(message,chatId);
        message = "我想吃江浙菜";
        foodApp.doChat(message,chatId);
    }

    @Test
    void doChatWithRAG(){
        String chatId = "tree";
        String message = "";
        /*message = "你好，我是树";
        foodApp.doChatWithRAG(message,chatId);*/
        message = "上海有什么好吃的";
        foodApp.doChatWithRAG(message,chatId);
    }

    @Test
    void doChatWithMCP(){
        String chatId = "tree";
        String message = "杭州大悦城附近三公里有什么好吃的中餐";
        String s = foodApp.dochatWithMCP(message, chatId);
        System.out.println("----------------------1--------------------------"+s);
        message = "我现在在浙江工商大学那，怎么去大悦城";
        String s1 = foodApp.dochatWithMCP(message, chatId);
        System.out.println("----------------------2--------------------------"+s1);
    }
}