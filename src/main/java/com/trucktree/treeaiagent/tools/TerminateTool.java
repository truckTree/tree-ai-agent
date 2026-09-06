package com.trucktree.treeaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

public class TerminateTool {

    private volatile boolean terminated = false;

    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)
    public String doTerminate() {
        this.terminated = true;
        return "任务结束";
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void reset() {
        this.terminated = false;
    }
}
