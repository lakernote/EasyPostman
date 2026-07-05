package com.laker.postman.panel.collections.tree.handler;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestTreeHandlerWiringTest extends AbstractSwingUiTest {

    @Test
    public void mouseHandlerShouldShareCoordinatorWithPopupMenu() throws Exception {
        JTree tree = createTree();
        RequestTreeCoordinator coordinator = new RequestTreeCoordinator(tree, null);

        RequestTreeMouseHandler handler = new RequestTreeMouseHandler(
                tree,
                null,
                coordinator,
                new NoopOpenActions()
        );

        RequestTreePopupMenu popupMenu = field(handler, "popupMenu", RequestTreePopupMenu.class);
        assertSame(field(handler, "coordinator", RequestTreeCoordinator.class), coordinator);
        assertSame(field(popupMenu, "coordinator", RequestTreeCoordinator.class), coordinator);
    }

    @Test
    public void keyboardHandlerShouldUseProvidedCoordinator() throws Exception {
        JTree tree = createTree();
        RequestTreeCoordinator coordinator = new RequestTreeCoordinator(tree, null);

        RequestTreeKeyboardHandler handler = new RequestTreeKeyboardHandler(
                tree,
                null,
                coordinator,
                new NoopOpenActions()
        );

        assertSame(field(handler, "coordinator", RequestTreeCoordinator.class), coordinator);
    }

    @Test
    public void renameMenuItemShouldUseEditIcon() throws Exception {
        JTree tree = createTree();
        RequestTreePopupMenu popupMenu = new RequestTreePopupMenu(
                tree,
                null,
                new RequestTreeCoordinator(tree, null)
        );
        JPopupMenu menu = new JPopupMenu();

        method(RequestTreePopupMenu.class, "addRenameAndDeleteMenuItems", JPopupMenu.class, boolean.class)
                .invoke(popupMenu, menu, false);

        JMenuItem renameItem = (JMenuItem) menu.getComponent(0);
        Icon renameIcon = renameItem.getIcon();
        assertTrue(renameIcon instanceof FlatSVGIcon);
        assertEquals(((FlatSVGIcon) renameIcon).getName(), "icons/edit.svg");
    }

    private static JTree createTree() {
        return new JTree(new DefaultTreeModel(new DefaultMutableTreeNode(CollectionTreePanel.ROOT)));
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static Method method(Class<?> target, String name, Class<?>... parameterTypes) throws Exception {
        Method method = target.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static class NoopOpenActions implements RequestTreeOpenActions {
        @Override
        public void openTransientRequest(HttpRequestItem item) {
        }

        @Override
        public void openFixedRequest(HttpRequestItem item) {
        }

        @Override
        public void openTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        }

        @Override
        public void openFixedGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        }

        @Override
        public void openTransientSavedResponse(SavedResponse savedResponse) {
        }

        @Override
        public void openFixedSavedResponse(SavedResponse savedResponse) {
        }
    }
}
