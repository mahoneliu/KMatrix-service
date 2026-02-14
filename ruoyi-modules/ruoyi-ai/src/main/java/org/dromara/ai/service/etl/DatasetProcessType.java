package org.dromara.ai.service.etl;

/**
 * 数据集处理类型常量
 *
 * @author Mahone
 * @date 2026-02-01
 */
public final class DatasetProcessType {

    private DatasetProcessType() {
    }

    /**
     * 通用文件: pdf、office、纯文本类
     */
    public static final String GENERIC_FILE = "GENERIC_FILE";

    /**
     * QA对: 问答对，支持表格 excel、csv
     */
    public static final String QA_PAIR = "QA_PAIR";

    /**
     * 在线文档: 用户输入文本（支持富文本）
     */
    public static final String ONLINE_DOC = "ONLINE_DOC";

    /**
     * 网页链接: 用户输入网页链接
     */
    public static final String WEB_LINK = "WEB_LINK";
}
