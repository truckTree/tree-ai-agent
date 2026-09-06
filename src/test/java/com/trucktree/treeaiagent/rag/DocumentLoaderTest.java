package com.trucktree.treeaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentLoaderTest {

    @Resource
    private DocumentLoader documentLoader;

    @Test
    void loadMarkdowns() {
        List<Document> documents = documentLoader.loadMarkdowns();
        System.out.println(documents);
    }
}