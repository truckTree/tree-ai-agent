package com.trucktree.treeaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LocalLifeManusTest {

    @Resource
    private LocalLifeManus localLifeManus;

    @Test
    void agentTest(){
        String user = "我现在在滨江宝龙城，我要去滨江银泰城吃饭，给我推荐几家好吃的店。并且告诉我怎么到那边";
        String answer = localLifeManus.run(user);
        System.out.println(answer);
    }
}
