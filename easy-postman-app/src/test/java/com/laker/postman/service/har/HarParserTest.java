package com.laker.postman.service.har;

import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.BODY_TYPE_FORM_URLENCODED;
import static org.testng.Assert.*;

public class HarParserTest {

    @Test
    public void testParseHar12Request() {
        String json = """
                {
                  "log": {
                    "version": "1.2",
                    "entries": [
                      {
                        "comment": "Create Order",
                        "request": {
                          "method": "POST",
                          "url": "https://api.example.com/orders?debug=true",
                          "headers": [
                            {"name": "Content-Type", "value": "application/x-www-form-urlencoded"}
                          ],
                          "queryString": [
                            {"name": "debug", "value": "true"}
                          ],
                          "postData": {
                            "mimeType": "application/x-www-form-urlencoded",
                            "params": [
                              {"name": "id", "value": "1"},
                              {"name": "status", "value": "created"}
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
                """;

        DefaultMutableTreeNode root = HarParser.parseHar(json);
        assertNotNull(root);
        assertEquals(root.getChildCount(), 1);

        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) root.getChildAt(0);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        HttpRequestItem request = (HttpRequestItem) userObject[1];

        assertEquals(request.getName(), "Create Order");
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getUrl(), "https://api.example.com/orders?debug=true");
        assertEquals(request.getParamsList().size(), 1);
        assertEquals(request.getParamsList().get(0).getKey(), "debug");
        assertEquals(request.getBodyType(), BODY_TYPE_FORM_URLENCODED);
        assertEquals(request.getUrlencodedList().size(), 2);
        assertTrue(request.getHeadersList().stream().anyMatch(h -> "Content-Type".equals(h.getKey())));
    }
}
