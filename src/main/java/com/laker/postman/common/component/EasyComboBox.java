package com.laker.postman.common.component;

import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 增强型下拉框组件
 * <p>
 * 特性：
 * - 根据当前选中项动态调整宽度，节省空间
 * - 支持固定宽度模式
 * - 自动适配 FlatLaf 主题
 * - 统一的字体和样式
 * </p>
 *
 * @param <E> 下拉框项的类型
 */
public class EasyComboBox<E> extends JComboBox<E> {

    /**
     * 宽度模式
     */
    public enum WidthMode {
        /**
         * 根据当前选中项动态调整宽度
         */
        DYNAMIC,
        /**
         * 根据所有选项中最长的项固定宽度
         */
        FIXED_MAX,
        /**
         * 手动指定固定宽度
         */
        FIXED_CUSTOM
    }

    private WidthMode widthMode = WidthMode.DYNAMIC;
    private int customWidth = -1;
    private int padding = 38; // 左右边距 + 下拉箭头按钮

    /**
     * 创建默认的动态宽度下拉框
     */
    public EasyComboBox() {
        this(WidthMode.DYNAMIC);
    }

    /**
     * 创建指定宽度模式的下拉框
     *
     * @param widthMode 宽度模式
     */
    public EasyComboBox(WidthMode widthMode) {
        super();
        this.widthMode = widthMode;
        initComponent();
    }

    /**
     * 创建指定宽度模式和选项的下拉框
     *
     * @param items     下拉框选项
     * @param widthMode 宽度模式
     */
    public EasyComboBox(E[] items, WidthMode widthMode) {
        super(items);
        this.widthMode = widthMode;
        initComponent();
    }

    /**
     * 创建动态宽度的下拉框
     *
     * @param items 下拉框选项
     */
    public EasyComboBox(E[] items) {
        this(items, WidthMode.DYNAMIC);
    }

    /**
     * 创建指定固定宽度的下拉框
     *
     * @param items 下拉框选项
     * @param width 固定宽度（像素）
     */
    public EasyComboBox(E[] items, int width) {
        super(items);
        this.widthMode = WidthMode.FIXED_CUSTOM;
        this.customWidth = width;
        initComponent();
    }

    /**
     * 初始化组件
     */
    private void initComponent() {
        // 设置字体
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setFocusable(false); // 取消焦点框

        // 添加选项变化监听器
        if (widthMode == WidthMode.DYNAMIC) {
            addActionListener(e -> updateWidth());
        }

        // 初始化宽度
        updateWidth();
    }

    /**
     * 更新下拉框宽度
     */
    private void updateWidth() {
        int optimalWidth = switch (widthMode) {
            case FIXED_MAX -> calculateMaxItemWidth();
            case FIXED_CUSTOM -> customWidth > 0 ? customWidth : calculateCurrentItemWidth();
            default -> calculateCurrentItemWidth();
        };

        // 获取合适的高度
        // 先调用父类的 getPreferredSize() 获取默认尺寸
        Dimension defaultSize = super.getPreferredSize();
        int height = defaultSize.height;

        // 如果高度过小，使用默认的 ComboBox 高度
        if (height < 20) {
            // 根据字体计算合理的高度
            FontMetrics fm = getFontMetrics(getFont());
            height = fm.getHeight() + 8; // 字体高度 + 上下边距
            // 确保最小高度为 28px（常见的 ComboBox 高度）
            height = Math.max(height, 28);
        }

        // 设置尺寸
        Dimension size = new Dimension(optimalWidth, height);
        setPreferredSize(size);
        // 设置最小尺寸
        setMinimumSize(new Dimension(Math.min(80, optimalWidth), height));
        setMaximumSize(size);
        // 通知布局管理器重新布局
        revalidate();
    }

    /**
     * 计算当前选中项的宽度
     */
    private int calculateCurrentItemWidth() {
        Object selectedItem = getSelectedItem();
        if (selectedItem == null) {
            return 80; // 默认最小宽度
        }

        String text = selectedItem.toString();
        if (text == null || text.isEmpty()) {
            return 80;
        }

        // 确保字体已初始化
        Font font = getFont();
        if (font == null) {
            return 80;
        }

        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(text);

        // 计算实际需要的宽度：文本宽度 + padding
        // 不设置硬性最小宽度，让短文本可以更紧凑
        return textWidth + padding;
    }

    /**
     * 计算所有选项中最长的宽度
     */
    private int calculateMaxItemWidth() {
        if (getItemCount() == 0) {
            return 80; // 默认最小宽度
        }

        // 确保字体已初始化
        Font font = getFont();
        if (font == null) {
            return 80;
        }

        FontMetrics fm = getFontMetrics(font);
        int maxWidth = 0;

        for (int i = 0; i < getItemCount(); i++) {
            E item = getItemAt(i);
            if (item != null) {
                String text = item.toString();
                if (text != null && !text.isEmpty()) {
                    int width = fm.stringWidth(text);
                    maxWidth = Math.max(maxWidth, width);
                }
            }
        }

        // 计算实际需要的宽度：最大文本宽度 + padding
        return maxWidth + padding;
    }

    /**
     * 设置宽度模式
     *
     * @param widthMode 宽度模式
     */
    public void setWidthMode(WidthMode widthMode) {
        this.widthMode = widthMode;
        updateWidth();
    }

    /**
     * 设置自定义固定宽度
     *
     * @param width 宽度（像素）
     */
    public void setCustomWidth(int width) {
        this.widthMode = WidthMode.FIXED_CUSTOM;
        this.customWidth = width;
        updateWidth();
    }

    /**
     * 设置内边距（影响宽度计算）
     *
     * @param padding 内边距（像素），包括左右边距和下拉箭头宽度
     */
    public void setPadding(int padding) {
        this.padding = padding;
        updateWidth();
    }

    @Override
    public void addItem(E item) {
        super.addItem(item);
        if (widthMode == WidthMode.FIXED_MAX) {
            updateWidth();
        }
    }

    @Override
    public void removeItem(Object anObject) {
        super.removeItem(anObject);
        if (widthMode == WidthMode.FIXED_MAX) {
            updateWidth();
        }
    }

    @Override
    public void removeAllItems() {
        super.removeAllItems();
        if (widthMode == WidthMode.FIXED_MAX) {
            updateWidth();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换后重新计算宽度
        if (widthMode != null) {
            updateWidth();
        }
    }
}

