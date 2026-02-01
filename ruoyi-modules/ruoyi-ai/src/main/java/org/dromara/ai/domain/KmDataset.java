package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Map;

/**
 * 数据集对象 km_dataset
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_dataset", autoResultMap = true)
public class KmDataset extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 类型 (FILE/WEB/MANUAL)
     */
    private String type;

    /**
     * ETL配置 (JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * 处理类型: GENERIC_FILE, QA_PAIR, ONLINE_DOC, WEB_LINK
     */
    private String processType;

    /**
     * 是否系统预设数据集 (系统预设数据集不可删除)
     */
    private Boolean isSystem;

    /**
     * 数据来源类型: FILE_UPLOAD(上传文件), TEXT_INPUT(文本输入), WEB_CRAWL(网页爬取)
     */
    private String sourceType;

    /**
     * 最小分块大小 (token)
     */
    private Integer minChunkSize;

    /**
     * 最大分块大小 (token)
     */
    private Integer maxChunkSize;

    /**
     * 分块重叠大小 (token)
     */
    private Integer chunkOverlap;

    /**
     * 支持的文件格式 (逗号分隔,如: "pdf,docx,txt" 或 "*" 表示全部)
     */
    private String allowedFileTypes;

    /**
     * 数据来源类型常量
     */
    public static final class SourceType {
        public static final String FILE_UPLOAD = "FILE_UPLOAD";
        public static final String TEXT_INPUT = "TEXT_INPUT";
        public static final String WEB_CRAWL = "WEB_CRAWL";

        private SourceType() {
        }
    }
}
