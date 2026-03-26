package org.dromara.ai.workflow.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.model.config.KmAiProperties;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.api.enums.AiModelType;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbCheckRunner implements CommandLineRunner {
    private final KmModelMapper modelMapper;
    private final KmAiProperties aiProperties;

    @Override
    public void run(String... args) {
        if (aiProperties.isUnifiedEmbeddingModel()) {
            log.info("Checking unified embedding model configuration...");
            KmModel defaultModel = modelMapper.selectOne(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, AiModelType.EMBEDDING.getCode())
                .eq(KmModel::getIsDefault, 1));
            
            if (defaultModel == null) {
                log.error("CRITICAL: ai.unified-embedding-model is enabled, but no default embedding model is set in the database!");
            } else {
                log.info("Unified embedding model check passed: {}", defaultModel.getModelName());
            }
        }
    }
}
