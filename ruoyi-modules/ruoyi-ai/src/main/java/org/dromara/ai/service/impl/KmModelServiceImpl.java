package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.bo.KmModelBo;
import org.dromara.ai.domain.vo.KmModelVo;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.service.IKmModelService;
import org.dromara.ai.util.ModelConnectionTester;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI模型配置Service业务层处理
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmModelServiceImpl implements IKmModelService {

    private final KmModelMapper baseMapper;
    private final KmModelProviderMapper providerMapper;

    @Override
    public List<KmModelVo> queryList(KmModelBo bo) {
        LambdaQueryWrapper<KmModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getProviderId() != null, KmModel::getProviderId, bo.getProviderId());
        lqw.like(StrUtil.isNotBlank(bo.getModelName()), KmModel::getModelName, bo.getModelName());
        lqw.eq(StrUtil.isNotBlank(bo.getModelType()), KmModel::getModelType, bo.getModelType());
        lqw.eq(StrUtil.isNotBlank(bo.getModelSource()), KmModel::getModelSource, bo.getModelSource());
        List<KmModelVo> list = baseMapper.selectVoList(lqw);

        // 填充供应商图标
        if (!list.isEmpty()) {
            List<KmModelProvider> providers = providerMapper.selectList(Wrappers.emptyWrapper());
            java.util.Map<Long, String> iconMap = providers.stream()
                    .collect(java.util.stream.Collectors.toMap(KmModelProvider::getProviderId,
                            KmModelProvider::getIconUrl, (v1, v2) -> v1));
            list.forEach(m -> m.setProviderIcon(iconMap.get(m.getProviderId())));
        }

        return list;
    }

    @Override
    public KmModelVo queryById(Long modelId) {
        return baseMapper.selectVoById(modelId);
    }

    @Override
    public Boolean insertByBo(KmModelBo bo) {
        KmModel add = MapstructUtils.convert(bo, KmModel.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmModelBo bo) {
        KmModel update = MapstructUtils.convert(bo, KmModel.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public String testConnection(KmModelBo bo) {
        log.info("开始测试模型连接: modelName={}, modelKey={}, providerId={}",
                bo.getModelName(), bo.getModelKey(), bo.getProviderId());

        try {
            // 参数验证
            if (StrUtil.isBlank(bo.getModelKey())) {
                return "基础模型不能为空";
            }
            if (bo.getProviderId() == null) {
                return "供应商ID不能为空";
            }

            // 查询供应商信息
            KmModelProvider provider = providerMapper.selectById(bo.getProviderId());
            if (provider == null) {
                return "供应商不存在";
            }

            String providerKey = provider.getProviderKey();
            String apiKey = bo.getApiKey();
            String apiBase = StrUtil.isNotBlank(bo.getApiBase()) ? bo.getApiBase() : provider.getDefaultEndpoint();
            String modelKey = bo.getModelKey();

            // 根据供应商类型调用对应的测试方法
            return switch (providerKey) {
                case "openai" -> ModelConnectionTester.testOpenAI(apiKey, apiBase, modelKey);
                case "ollama" -> ModelConnectionTester.testOllama(apiBase, modelKey);
                case "qwen" -> ModelConnectionTester.testQwen(apiKey, modelKey);
                case "bailian" -> ModelConnectionTester.testQwen(apiKey, modelKey);
                case "gemini" -> ModelConnectionTester.testGemini(apiKey, modelKey);
                default -> "暂不支持该供应商的连接测试: " + providerKey;
            };
        } catch (Exception e) {
            log.error("模型连接测试失败", e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    @Override
    public Long copyModel(Long modelId) {
        // 查询原模型
        KmModel original = baseMapper.selectById(modelId);
        if (original == null) {
            throw new org.dromara.common.core.exception.ServiceException("原模型不存在");
        }

        // 创建新模型
        KmModel copy = new KmModel();
        copy.setModelName("副本-" + original.getModelName());
        copy.setModelKey(original.getModelKey());
        copy.setModelType(original.getModelType());
        copy.setModelSource(original.getModelSource());
        copy.setProviderId(original.getProviderId());
        copy.setApiKey(original.getApiKey());
        copy.setApiBase(original.getApiBase());
        copy.setStatus(original.getStatus());
        copy.setRemark(original.getRemark());

        baseMapper.insert(copy);
        return copy.getModelId();
    }
}
