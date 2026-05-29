package com.laker.postman.common;

/**
 * 可刷新组件接口
 * <p>
 * 实现此接口的组件将在以下场景自动刷新：
 * <ul>
 *   <li>语言切换时 - 更新所有文本内容</li>
 *   <li>主题切换时 - 更新组件外观</li>
 *   <li>字体切换时 - 更新字体样式</li>
 * </ul>
 *
 * @author laker
 */
public interface IRefreshable {

    /**
     * 刷新组件
     * <p>
     * 此方法会在语言、主题、字体改变时被调用。
     * 实现类应该：
     * <ul>
     *   <li>更新所有国际化文本（使用 I18nUtil.getMessage）</li>
     *   <li>重新布局组件（调用 revalidate）</li>
     *   <li>重绘组件（调用 repaint）</li>
     * </ul>
     */
    void refresh();
}

