package com.laker.postman.panel.collections.left.action;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.UUID;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.GROUP;
import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

/**
 * 树节点克隆工具类
 */
@UtilityClass
public class TreeNodeCloner {

    /**
     * 深拷贝分组节点及其所有子节点
     */
    public static DefaultMutableTreeNode deepCopyGroupNode(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        Object[] obj = null;
        if (userObj instanceof Object[] originalObj) {
            // 创建新数组并深拷贝内容
            obj = new Object[originalObj.length];
            obj[0] = originalObj[0]; // type字段直接复制
            // 深拷贝 RequestGroup 对象
            if (originalObj[1] instanceof RequestGroup originalGroup) {
                obj[1] = JsonUtil.deepCopy(originalGroup, RequestGroup.class);
                // 生成新的ID
                ((RequestGroup) obj[1]).setId(UUID.randomUUID().toString());
            } else {
                obj[1] = originalObj[1]; // 如果是字符串，直接复制
            }
        }
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(obj);

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            copy.add(copyChildNode(child));
        }

        return copy;
    }

    /**
     * 拷贝子节点
     */
    private static DefaultMutableTreeNode copyChildNode(DefaultMutableTreeNode child) {
        Object childUserObj = child.getUserObject();
        if (!(childUserObj instanceof Object[] childObj)) {
            return new DefaultMutableTreeNode(childUserObj);
        }

        if (GROUP.equals(childObj[0])) {
            return deepCopyGroupNode(child);
        } else if (REQUEST.equals(childObj[0])) {
            return copyRequestNode(childObj);
        }

        return new DefaultMutableTreeNode(childUserObj);
    }

    /**
     * 深拷贝请求节点
     */
    private static DefaultMutableTreeNode copyRequestNode(Object[] childObj) {
        HttpRequestItem item = (HttpRequestItem) childObj[1];
        HttpRequestItem copyItem = JsonUtil.deepCopy(item, HttpRequestItem.class);
        copyItem.setId(java.util.UUID.randomUUID().toString());
        Object[] reqObj = new Object[]{REQUEST, copyItem};
        return new DefaultMutableTreeNode(reqObj);
    }

    /**
     * 递归克隆树节点（用于创建选择树）
     */
    public static DefaultMutableTreeNode cloneTreeNode(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(
                userObj instanceof Object[] ? ((Object[]) userObj).clone() : userObj
        );

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            copy.add(cloneTreeNode(child));
        }

        return copy;
    }
}

