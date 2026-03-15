package com.laker.postman.service.apipost;

import com.laker.postman.model.*;
import com.laker.postman.service.common.CollectionParseResult;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;
import static org.testng.Assert.*;

/**
 * ApiPostCollectionParser 单元测试
 * 测试 ApiPost Collection 解析功能
 */
public class ApiPostCollectionParserTest {

    @Test
    public void testParseApiPostCollection_BasicStructure() {
        String json = """
                {
                    "name": "Test API",
                    "apis": [
                        {
                            "target_id": "req1",
                            "target_type": "api",
                            "parent_id": "0",
                            "name": "Get User",
                            "method": "GET",
                            "url": "https://api.example.com/user"
                        }
                    ]
                }
                """;

        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var requestNode = result.getChildren().get(0);
        assertTrue(requestNode.isRequest());
        assertEquals(requestNode.asRequest().getName(), "Get User");
        assertEquals(requestNode.asRequest().getMethod(), "GET");
    }

    @Test
    public void testParseApiPostCollection_WithFolder() {
        String json = """
                {
                    "name": "Test Collection",
                    "apis": [
                        {
                            "target_id": "folder1",
                            "target_type": "folder",
                            "parent_id": "0",
                            "name": "User API"
                        },
                        {
                            "target_id": "req1",
                            "target_type": "api",
                            "parent_id": "folder1",
                            "name": "Get User",
                            "method": "GET",
                            "url": "https://api.example.com/user"
                        }
                    ]
                }
                """;

        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var folderNode = result.getChildren().get(0);
        assertTrue(folderNode.isGroup());
        assertEquals(folderNode.asGroup().getName(), "User API");
        assertEquals(folderNode.getChildren().size(), 1);
    }

    @Test
    public void testParseApiPostCollection_InvalidJson() {
        String json = "invalid json";
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }

    @Test
    public void testParseApiPostCollection_MissingApis() {
        String json = """
                {
                    "name": "Test"
                }
                """;
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }

    @Test
    public void testParseApiPostCollection_EmptyApis() {
        String json = """
                {
                    "name": "Empty Collection",
                    "apis": []
                }
                """;
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }
}
