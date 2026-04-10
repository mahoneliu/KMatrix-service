package org.dromara.ai.api.domain.vo.config;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话界面 / 欢迎页等前端 UI 配置
 *
 * @author caoxupei
 */
@Data
public class AppUiSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否启用欢迎页等扩展 UI（由前端解释）
     */
    private Boolean enabled;

    /**
     * 头部区域：标题、副标题、头图 URL
     */
    private Hero hero;

    /**
     * 功能卡片（如前 2x2 网格）
     */
    private List<FeatureItem> features;

    /**
     * 示例问题文案（点击可填入输入框）
     */
    private List<String> suggestedQuestions;

    /**
     * 为 true 时不注入开场白气泡（与 ChatPanel 逻辑配合时使用）
     */
    private Boolean hidePrologueBubble;

    @Data
    public static class Hero implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private String subtitle;
        private String imageUrl;
    }

    @Data
    public static class FeatureItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 图标标识，如 mdi:camera 或本地图标名，由前端解析 */
        private String icon;
        private String title;
        private String description;
        /**
         * 点击卡片时填入输入框的提示词；为空时前端可回退为 title 或 description
         */
        private String inputPrompt;
    }

    public static AppUiSetting empty() {
        AppUiSetting s = new AppUiSetting();
        s.setEnabled(false);
        s.setHero(new Hero());
        s.setFeatures(new ArrayList<>());
        s.setSuggestedQuestions(new ArrayList<>());
        s.setHidePrologueBubble(false);
        return s;
    }
}
