package com.laker.postman.service.update;


import org.testng.annotations.Test;

public class VersionCheckerTest {

    @Test
    public void testParseReleaseJson() {
        String json = """
                {
                    "id" : 528737,
                        "tag_name" : "v2.0.6",
                        "target_commitish" : "6e70c89d1f9b32f55a558478130587a2c8af59c7",
                        "prerelease" : false,
                        "name" : "v2.0.6",
                        "body" : "1.更换icon为信鸽，其更能表达本软件的意图\r\n2.常规bug修复，UI优化",
                        "author" : {
                        "id" : 709188,
                            "login" : "lakernote",
                            "name" : "laker",
                            "avatar_url" : "https://foruda.gitee.com/avatar/1694567649421972112/709188_lakernote_1694567649.png",
                            "url" : "https://gitee.com/api/v5/users/lakernote",
                            "html_url" : "https://gitee.com/lakernote",
                            "remark" : "",
                            "followers_url" : "https://gitee.com/api/v5/users/lakernote/followers",
                            "following_url" : "https://gitee.com/api/v5/users/lakernote/following_url{/other_user}",
                            "gists_url" : "https://gitee.com/api/v5/users/lakernote/gists{/gist_id}",
                            "starred_url" : "https://gitee.com/api/v5/users/lakernote/starred{/owner}{/repo}",
                            "subscriptions_url" : "https://gitee.com/api/v5/users/lakernote/subscriptions",
                            "organizations_url" : "https://gitee.com/api/v5/users/lakernote/orgs",
                            "repos_url" : "https://gitee.com/api/v5/users/lakernote/repos",
                            "events_url" : "https://gitee.com/api/v5/users/lakernote/events{/privacy}",
                            "received_events_url" : "https://gitee.com/api/v5/users/lakernote/received_events",
                            "type" : "User"
                },
                    "created_at" : "2025-09-11T17:00:44+08:00",
                        "assets" : [ {
                    "browser_download_url" : "https://gitee.com/lakernote/easy-postman/releases/download/v2.0.6/EasyPostman-2.0.6.dmg",
                            "name" : "EasyPostman-2.0.6.dmg"
                }, {
                    "browser_download_url" : "https://gitee.com/lakernote/easy-postman/archive/refs/tags/v2.0.6.zip",
                            "name" : "v2.0.6.zip"
                }, {
                    "browser_download_url" : "https://gitee.com/lakernote/easy-postman/archive/refs/tags/v2.0.6.tar.gz",
                            "name" : "v2.0.6.tar.gz"
                } ]
                }
                
                """;


    }
}