package com.trucktree.treeaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* 基于内存实现的向量数据库
* */
@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentLoader documentLoader;

    @Bean
    VectorStore vectorStore(EmbeddingModel dashscopeEmbeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        simpleVectorStore.add(documentLoader.loadMarkdowns());
        return simpleVectorStore;
    }
}
