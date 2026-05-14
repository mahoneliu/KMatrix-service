package org.dromara.ai.blog.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.domain.KmBlogArticle;
import org.dromara.ai.blog.domain.KmBlogCategory;
import org.dromara.ai.blog.domain.bo.KmBlogCategorySaveBo;
import org.dromara.ai.blog.domain.vo.BlogCategoryVo;
import org.dromara.ai.blog.domain.vo.BlogTopicVo;
import org.dromara.ai.blog.domain.vo.GitCategoryConfigVo;
import org.dromara.ai.blog.domain.vo.GitCategoryTokenVo;
import org.dromara.ai.blog.mapper.KmBlogArticleMapper;
import org.dromara.ai.blog.mapper.KmBlogCategoryMapper;
import org.dromara.ai.blog.service.IBlogCategoryService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 博客分类与专题管理服务实现
 *
 * @author Mahone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl implements IBlogCategoryService {

    private final KmBlogCategoryMapper categoryMapper;
    private final KmBlogArticleMapper articleMapper;

    @Value("${blog.git.token.aes-key:KMatrixGitToken!}")
    private String aesKey;

    @Override
    public List<BlogCategoryVo> getCategoryTree() {
        List<BlogCategoryVo> flatList = categoryMapper.selectCategoryTreeWithCount(null);
        for (BlogCategoryVo vo : flatList) {
            if (KmBlogCategory.SOURCE_GIT.equals(vo.getSource())) {
                KmBlogCategory entity = categoryMapper.selectById(vo.getId());
                if (entity != null) {
                    vo.setHasToken(org.dromara.common.core.utils.StringUtils.isNotBlank(entity.getGitTokenEncrypted()));
                }
            }
        }
        return buildTree(flatList, 0L);
    }

    @Override
    public List<BlogTopicVo> listTopics() {
        List<BlogCategoryVo> topics = categoryMapper.selectTopics();
        return topics.stream().map(this::toTopicVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(KmBlogCategorySaveBo bo) {
        String path = resolvePath(bo.getParentId(), bo.getName(), bo.getPath());
        KmBlogCategory existing = categoryMapper.selectByPath(path);
        if (existing != null) {
            throw new ServiceException("分类路径已存在: " + path);
        }

        KmBlogCategory category = new KmBlogCategory();
        category.setParentId(bo.getParentId() != null ? bo.getParentId() : 0L);
        category.setName(bo.getName());
        category.setPath(path);
        category.setOrderNum(bo.getOrderNum() != null ? bo.getOrderNum() : 0);
        category.setSource(KmBlogCategory.SOURCE_ONLINE);
        category.setIsTopic(category.getParentId() == 0L ? KmBlogCategory.IS_TOPIC_YES : KmBlogCategory.IS_TOPIC_NO);
        category.setDatasetId(bo.getDatasetId());
        category.setTopicSlug(bo.getTopicSlug());
        category.setCustomDomain(bo.getCustomDomain());
        category.setDelFlag("0");

        categoryMapper.insert(category);
        evictCategoryCache();
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, KmBlogCategorySaveBo bo) {
        KmBlogCategory category = categoryMapper.selectById(id);
        if (category == null) throw new ServiceException("分类不存在: id=" + id);

        if (StringUtils.hasText(bo.getName())) category.setName(bo.getName());
        if (bo.getOrderNum() != null) category.setOrderNum(bo.getOrderNum());
        if (bo.getDatasetId() != null) category.setDatasetId(bo.getDatasetId());
        if (StringUtils.hasText(bo.getTopicSlug())) category.setTopicSlug(bo.getTopicSlug());
        if (bo.getCustomDomain() != null) category.setCustomDomain(bo.getCustomDomain());

        categoryMapper.updateById(category);
        evictCategoryCache();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        KmBlogCategory category = categoryMapper.selectById(id);
        if (category == null) throw new ServiceException("分类不存在: id=" + id);

        long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<KmBlogCategory>().eq(KmBlogCategory::getParentId, id));
        if (childCount > 0) throw new ServiceException("该分类下存在子分类，无法删除");

        long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<KmBlogArticle>().eq(KmBlogArticle::getCategoryId, id));
        if (articleCount > 0) throw new ServiceException("该分类下存在文章，无法删除");

        categoryMapper.deleteById(id);
        evictCategoryCache();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGitCategory(KmBlogCategorySaveBo bo) {
        if (bo.getParentId() != null && bo.getParentId() != 0L) {
            throw new ServiceException("GIT 类型分类必须是顶层专题节点（parentId 必须为 0）");
        }

        String repoUrl = bo.getPath();
        String owner = bo.getGitOwner();
        String repo = bo.getGitRepo();
        if (StringUtils.hasText(repoUrl) && (!StringUtils.hasText(owner) || !StringUtils.hasText(repo))) {
            String[] parsed = parseGitHubUrl(repoUrl);
            if (!StringUtils.hasText(owner)) owner = parsed[0];
            if (!StringUtils.hasText(repo)) repo = parsed[1];
        }

        String encryptedToken = null;
        if (StringUtils.hasText(bo.getGitToken())) {
            encryptedToken = SecureUtil.aes(aesKey.getBytes()).encryptHex(bo.getGitToken());
        }

        KmBlogCategory category = new KmBlogCategory();
        category.setParentId(0L);
        category.setName(bo.getName());
        category.setPath(resolvePath(0L, bo.getName(), null));
        category.setOrderNum(bo.getOrderNum() != null ? bo.getOrderNum() : 0);
        category.setSource(KmBlogCategory.SOURCE_GIT);
        category.setIsTopic(KmBlogCategory.IS_TOPIC_YES);
        category.setDatasetId(bo.getDatasetId());
        category.setTopicSlug(bo.getTopicSlug());
        category.setCustomDomain(bo.getCustomDomain());
        category.setGitTokenEncrypted(encryptedToken);
        category.setGitOwner(owner);
        category.setGitRepo(repo);
        category.setGitBranch(StringUtils.hasText(bo.getGitBranch()) ? bo.getGitBranch() : "main");
        category.setGitRootPath(bo.getGitRootPath());
        category.setGitPlatform(StringUtils.hasText(bo.getGitPlatform()) ? bo.getGitPlatform() : "github");
        category.setDelFlag("0");

        categoryMapper.insert(category);
        evictCategoryCache();
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateGitCategory(Long id, KmBlogCategorySaveBo bo) {
        KmBlogCategory category = categoryMapper.selectById(id);
        if (category == null || !KmBlogCategory.SOURCE_GIT.equals(category.getSource())) {
            throw new ServiceException("GIT 类型分类不存在: id=" + id);
        }

        if (StringUtils.hasText(bo.getName())) category.setName(bo.getName());
        if (bo.getOrderNum() != null) category.setOrderNum(bo.getOrderNum());
        if (bo.getTopicSlug() != null) category.setTopicSlug(bo.getTopicSlug());
        if (StringUtils.hasText(bo.getGitOwner())) category.setGitOwner(bo.getGitOwner());
        if (StringUtils.hasText(bo.getGitRepo())) category.setGitRepo(bo.getGitRepo());
        if (StringUtils.hasText(bo.getGitBranch())) category.setGitBranch(bo.getGitBranch());
        if (bo.getGitRootPath() != null) category.setGitRootPath(bo.getGitRootPath());
        if (StringUtils.hasText(bo.getGitPlatform())) category.setGitPlatform(bo.getGitPlatform());
        if (StringUtils.hasText(bo.getGitToken())) {
            category.setGitTokenEncrypted(SecureUtil.aes(aesKey.getBytes()).encryptHex(bo.getGitToken()));
        }

        categoryMapper.updateById(category);
        evictCategoryCache();
        return true;
    }

    @Override
    public GitCategoryConfigVo getGitCategoryConfig(Long categoryId) {
        KmBlogCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !KmBlogCategory.SOURCE_GIT.equals(category.getSource())) {
            throw new ServiceException("GIT 类型分类不存在: id=" + categoryId);
        }
        GitCategoryConfigVo vo = new GitCategoryConfigVo();
        vo.setCategoryId(category.getId());
        vo.setOwner(category.getGitOwner());
        vo.setRepo(category.getGitRepo());
        vo.setBranch(category.getGitBranch());
        vo.setRootPath(category.getGitRootPath());
        vo.setRepoUrl("https://github.com/" + category.getGitOwner() + "/" + category.getGitRepo());
        vo.setHasToken(org.dromara.common.core.utils.StringUtils.isNotBlank(category.getGitTokenEncrypted()));
        return vo;
    }

    @Override
    public GitCategoryTokenVo getGitCategoryToken(Long categoryId) {
        KmBlogCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !KmBlogCategory.SOURCE_GIT.equals(category.getSource())) {
            throw new ServiceException("GIT 类型分类不存在: id=" + categoryId);
        }
        GitCategoryTokenVo vo = new GitCategoryTokenVo();
        vo.setOwner(category.getGitOwner());
        vo.setRepo(category.getGitRepo());
        vo.setBranch(category.getGitBranch());
        vo.setRootPath(category.getGitRootPath());
        vo.setPlatform(StringUtils.hasText(category.getGitPlatform()) ? category.getGitPlatform() : "github");
        if (org.dromara.common.core.utils.StringUtils.isNotBlank(category.getGitTokenEncrypted())) {
            vo.setToken(SecureUtil.aes(aesKey.getBytes()).decryptStr(category.getGitTokenEncrypted()));
        }
        return vo;
    }

    private String resolvePath(Long parentId, String name, String customPath) {
        if (StringUtils.hasText(customPath)) {
            return customPath.startsWith("/") ? customPath : "/" + customPath;
        }
        if (parentId == null || parentId == 0L) {
            return "/" + name.toLowerCase().replaceAll("[\\s]+", "-");
        }
        KmBlogCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) throw new ServiceException("父分类不存在: parentId=" + parentId);
        return parent.getPath() + "/" + name.toLowerCase().replaceAll("[\\s]+", "-");
    }

    private List<BlogCategoryVo> buildTree(List<BlogCategoryVo> flatList, Long parentId) {
        List<BlogCategoryVo> result = new ArrayList<>();
        for (BlogCategoryVo vo : flatList) {
            if (parentId.equals(vo.getParentId())) {
                vo.setChildren(buildTree(flatList, vo.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private BlogTopicVo toTopicVo(BlogCategoryVo categoryVo) {
        BlogTopicVo topicVo = new BlogTopicVo();
        topicVo.setId(categoryVo.getId());
        topicVo.setName(categoryVo.getName());
        topicVo.setPath(categoryVo.getPath());
        topicVo.setDatasetId(categoryVo.getDatasetId());
        topicVo.setTopicSlug(categoryVo.getTopicSlug());
        topicVo.setCustomDomain(categoryVo.getCustomDomain());
        topicVo.setOrderNum(categoryVo.getOrderNum());
        topicVo.setArticleCount(categoryVo.getArticleCount());
        topicVo.setSource(categoryVo.getSource());
        return topicVo;
    }

    private void evictCategoryCache() {
        RedisUtils.deleteKeys("blog:category:tree*");
    }

    private String[] parseGitHubUrl(String repoUrl) {
        String url = repoUrl.endsWith(".git") ? repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String[] parts = url.split("/");
        if (parts.length < 2) throw new ServiceException("无效的 GitHub 仓库 URL: " + repoUrl);
        return new String[]{parts[parts.length - 2], parts[parts.length - 1]};
    }
}
