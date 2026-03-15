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
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setFocusable(false);

        // DYNAMIC 模式：选项改变时触发重新布局
        if (widthMode == WidthMode.DYNAMIC) {
            addActionListener(e -> revalidate());
        }
    }

    /**
     * 核心方法：重写 getPreferredSize()。
     * <p>
     * 每次布局管理器询问尺寸时，我们都在这里计算——
     * 此时组件已经在窗口中，super.getPreferredSize() 由 FlatComboBoxUI 计算，
     * 完全准确（含 HiDPI、cell insets、箭头按钮、边框）。
     * <p>
     * DYNAMIC 模式的关键：临时只放一个选项（当前选中项）让 UI 计算宽度，
     * 这样 BasicComboBoxUI 就不会遍历所有选项取最宽值。
     */
    @Override
    public Dimension getPreferredSize() {
        if (widthMode == WidthMode.FIXED_CUSTOM && customWidth > 0) {
            Dimension d = super.getPreferredSize();
            return new Dimension(customWidth, d.height);
        }

        if (widthMode == WidthMode.FIXED_MAX) {
            // 直接让 super 遍历所有选项取最宽值，这正是 BasicComboBoxUI 的默认行为
            return super.getPreferredSize();
        }

        // DYNAMIC：只用当前选中项计算宽度
        return calcDynamicSize();
    }

    @Override
    public Dimension getMaximumSize() {
        // BoxLayout 会用 getMaximumSize() 限制组件，让它等于 preferredSize 即可
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(Math.min(80, d.width), d.height);
    }

    /**
     * 临时用只含当前选中项的 ComboBoxModel 替换，让 super.getPreferredSize()
     * 只量当前项的宽度，然后还原。全程在 EDT 上同步执行，无线程问题。
     */
    private Dimension calcDynamicSize() {
        Object selected = getSelectedItem();

        // 构造只有当前选中项的临时 model
        DefaultComboBoxModel<Object> tempModel = new DefaultComboBoxModel<>();
        if (selected != null) {
            tempModel.addElement(selected);
            tempModel.setSelectedItem(selected);
        }

        // 暂存真实 model，换成临时 model
        ComboBoxModel<E> realModel = getModel();
        @SuppressWarnings("unchecked")
        ComboBoxModel<E> castedTemp = (ComboBoxModel<E>) tempModel;
        // 直接操作父类，绕过子类可能的监听器
        super.setModel(castedTemp);

        Dimension d = super.getPreferredSize();

        // 还原真实 model
        super.setModel(realModel);

        return d;
    }


    /**
     * 设置宽度模式
     *
     * @param widthMode 宽度模式
     */
    public void setWidthMode(WidthMode widthMode) {
        this.widthMode = widthMode;
        revalidate();
    }

    /**
     * 设置自定义固定宽度
     *
     * @param width 宽度（像素）
     */
    public void setCustomWidth(int width) {
        this.widthMode = WidthMode.FIXED_CUSTOM;
        this.customWidth = width;
        revalidate();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        revalidate();
    }

    @Override
    public void addItem(E item) {
        super.addItem(item);
        if (widthMode == WidthMode.FIXED_MAX) revalidate();
    }

    @Override
    public void removeItem(Object anObject) {
        super.removeItem(anObject);
        if (widthMode == WidthMode.FIXED_MAX) revalidate();
    }

    @Override
    public void removeAllItems() {
        super.removeAllItems();
        if (widthMode == WidthMode.FIXED_MAX) revalidate();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        revalidate();
    }
}
