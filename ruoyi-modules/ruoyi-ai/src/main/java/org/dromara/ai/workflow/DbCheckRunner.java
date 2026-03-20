package org.dromara.ai.workflow;

import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component // Uncomment to run
@RequiredArgsConstructor
public class DbCheckRunner implements CommandLineRunner {
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;

    @Override
    public void run(String... args) throws Exception {
        Long modelId = 2031649814172688386L;
        KmModel model = modelMapper.selectById(modelId);
        if (model != null) {
            log.error("DB_CHECK - Model: name={}, key={}, providerId={}", 
                model.getModelName(), model.getModelKey(), model.getProviderId());
            KmModelProvider provider = providerMapper.selectById(model.getProviderId());
            if (provider != null) {
                log.error("DB_CHECK - Provider: name={}, key={}", 
                    provider.getProviderName(), provider.getProviderKey());
            }
        } else {
            log.error("DB_CHECK - Model not found for ID: {}", modelId);
        }
    }
}
