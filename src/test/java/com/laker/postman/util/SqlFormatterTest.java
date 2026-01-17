package com.laker.postman.util;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * SqlFormatter 测试类
 * 测试 SQL 格式化和压缩功能
 */
public class SqlFormatterTest {

    // ==================== 格式化测试 ====================

    @Test(description = "格式化复杂 SQL 查询")
    public void testFormatComplexQuery() {
        String input = "select u.id,u.name,u.email,u.created_at,o.order_id,o.total,o.status from users u left join orders o on u.id=o.user_id where u.status=1 and u.created_at>='2024-01-01' and (o.total>100 or o.status='paid') group by u.id having count(o.order_id)>0 order by u.created_at desc,o.total desc limit 100";

        String expected = """
                SELECT u.id,
                  u.name,
                  u.email,
                  u.created_at,
                  o.order_id,
                  o.total,
                  o.status
                  FROM users u
                    LEFT JOIN orders o ON u.id = o.user_id
                    WHERE u.status = 1
                      AND u.created_at >= '2024-01-01'
                      AND (o.total > 100
                      OR o.status = 'paid')
                      GROUP BY u.id
                      HAVING count(o.order_id) > 0
                      ORDER BY u.created_at DESC,
                      o.total DESC
                      LIMIT 100;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("=== 格式化复杂 SQL 查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化基本 SELECT 查询")
    public void testFormatBasicSelect() {
        String input = "select id,name,email from users where status=1 and age>18 order by created_at desc";

        String expected = """
                SELECT id,
                  name,
                  email
                  FROM users
                    WHERE status = 1
                      AND age > 18
                      ORDER BY created_at DESC;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化基本 SELECT 查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化带多个 JOIN 的查询")
    public void testFormatMultipleJoins() {
        String input = "select u.name,o.order_id,p.product_name from users u inner join orders o on u.id=o.user_id left join products p on o.product_id=p.id where u.status='active'";

        String expected = """
                SELECT u.name,
                  o.order_id,
                  p.product_name
                  FROM users u
                    INNER JOIN orders o ON u.id = o.user_id
                    LEFT JOIN products p ON o.product_id = p.id
                    WHERE u.status = 'active';""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化带多个 JOIN 的查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化带子查询的 SQL")
    public void testFormatSubquery() {
        String input = "select * from (select user_id,count(*) as order_count from orders group by user_id) t where order_count>5";

        String expected = """
                SELECT *
                  FROM (SELECT user_id,
                  count(*) AS order_count
                  FROM orders
                    GROUP BY user_id) t
                    WHERE order_count > 5;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化带子查询的 SQL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 INSERT 语句")
    public void testFormatInsert() {
        String input = "insert into users(name,email,status,created_at) values('张三','zhang@example.com','active','2024-01-01'),('李四','li@example.com','active','2024-01-02')";

        String expected = """
                INSERT INTO users(name,
                  email,
                  status,
                  created_at)
                  VALUES('张三',
                  'zhang@example.com',
                  'active',
                  '2024-01-01'),
                  ('李四',
                  'li@example.com',
                  'active',
                  '2024-01-02');""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 INSERT 语句 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 UPDATE 语句")
    public void testFormatUpdate() {
        String input = "update users set status='inactive',updated_at=now() where last_login<'2023-01-01' and status='active'";

        String expected = """
                UPDATE users
                  SET status = 'inactive',
                  updated_at = now()
                  WHERE last_login < '2023-01-01'
                    AND status = 'active';""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 UPDATE 语句 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 DELETE 语句")
    public void testFormatDelete() {
        String input = "delete from users where status='inactive' and last_login<'2023-01-01'";

        String expected = """
                DELETE
                  FROM users
                    WHERE status = 'inactive'
                      AND last_login < '2023-01-01';""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 DELETE 语句 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 CASE WHEN 表达式")
    public void testFormatCaseWhen() {
        String input = "select id,name,case when score>=90 then 'A' when score>=80 then 'B' when score>=60 then 'C' else 'D' end as grade from exam where type='final'";

        String expected = """
                SELECT id,
                  name,
                  CASE
                  WHEN score >= 90 THEN 'A'
                  WHEN score >= 80 THEN 'B'
                  WHEN score >= 60 THEN 'C'
                  ELSE 'D'
                  END AS grade
                  FROM exam
                    WHERE type = 'final';""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 CASE WHEN 表达式 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 UNION ALL 查询")
    public void testFormatUnionAll() {
        String input = "select id,name from users where status=1 union all select id,name from admins where active=1";

        String expected = """
                SELECT id,
                  name
                  FROM users
                    WHERE status = 1
                    UNION ALL
                SELECT id,
                  name
                  FROM admins
                    WHERE active = 1;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 UNION ALL 查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 IN 子查询")
    public void testFormatInSubquery() {
        String input = "select * from users where id in (select user_id from orders where total>1000)";

        String expected = """
                SELECT *
                  FROM users
                    WHERE id IN (SELECT user_id
                  FROM orders
                    WHERE total > 1000);""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 IN 子查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 RIGHT JOIN 和 FULL OUTER JOIN")
    public void testFormatRightAndFullJoin() {
        String input = "select * from users u right join orders o on u.id=o.user_id full outer join products p on o.product_id=p.id";

        String expected = """
                SELECT *
                  FROM users u
                    RIGHT JOIN orders o ON u.id = o.user_id
                    FULL OUTER JOIN products p ON o.product_id = p.id;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 RIGHT JOIN 和 FULL OUTER JOIN ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 BETWEEN 和 LIKE")
    public void testFormatBetweenAndLike() {
        String input = "select * from products where price between 100 and 500 and name like '%phone%' and stock>0";

        String expected = """
                SELECT *
                  FROM products
                    WHERE price BETWEEN 100 AND 500
                      AND name LIKE '%phone%'
                      AND stock > 0;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 BETWEEN 和 LIKE ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 IS NULL 和 IS NOT NULL")
    public void testFormatIsNull() {
        String input = "select * from users where email is null or phone is not null and deleted_at is null";

        String expected = """
                SELECT *
                  FROM users
                    WHERE email IS NULL
                      OR phone IS NOT NULL
                      AND deleted_at IS NULL;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 IS NULL 和 IS NOT NULL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化 EXISTS 子查询")
    public void testFormatExistsSubquery() {
        String input = "select * from users u where exists (select 1 from orders o where o.user_id=u.id and o.total>1000)";

        String expected = """
                SELECT *
                  FROM users u
                    WHERE EXISTS (SELECT 1
                  FROM orders o
                    WHERE o.user_id = u.id
                      AND o.total > 1000);""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化 EXISTS 子查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    @Test(description = "格式化聚合函数和 DISTINCT")
    public void testFormatAggregateWithDistinct() {
        String input = "select count(distinct user_id),sum(total),avg(score),max(created_at) from orders where status='paid' group by date(created_at)";

        String expected = """
                SELECT count(DISTINCT user_id),
                  sum(total),
                  avg(score),
                  max(created_at)
                  FROM orders
                    WHERE status = 'paid'
                      GROUP BY date(created_at);""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化聚合函数和 DISTINCT ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);
        assertEquals(result, expected);
    }

    // ==================== 压缩测试 ====================

    @Test(description = "压缩格式化的 SQL")
    public void testCompressFormattedSQL() {
        String input = """
                SELECT u.id ,
                  u.name ,
                  u.email
                  FROM users u
                    WHERE u.status = 1
                      AND u.created_at >= '2024-01-01';""";

        String expected = "SELECT u.id, u.name, u.email FROM users u WHERE u.status = 1 AND u.created_at >= '2024-01-01';";

        String result = SqlFormatter.compress(input);

        assertNotNull(result);
        System.out.println("\n=== 压缩格式化的 SQL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n压缩结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);

        assertEquals(result, expected);
    }

    @Test(description = "压缩带注释的 SQL")
    public void testCompressWithComments() {
        String input = """
                SELECT id, -- 用户ID
                  name, -- 用户名
                  email /* 邮箱地址 */
                FROM users
                WHERE status = 1; /* 只查询激活用户 */""";

        String expected = "SELECT id, name, email FROM users WHERE status = 1;";

        String result = SqlFormatter.compress(input);

        assertNotNull(result);
        System.out.println("\n=== 压缩带注释的 SQL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n压缩结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);

        assertEquals(result, expected);
    }

    @Test(description = "压缩复杂查询")
    public void testCompressComplexQuery() {
        String input = """
                SELECT
                    u.id,
                    u.name,
                    COUNT(o.order_id) as order_count
                FROM users u
                LEFT JOIN orders o
                    ON u.id = o.user_id
                WHERE u.status = 1
                    AND u.created_at >= '2024-01-01'
                GROUP BY u.id
                HAVING COUNT(o.order_id) > 0
                ORDER BY order_count DESC
                LIMIT 100;""";

        String expected = "SELECT u.id, u.name, COUNT(o.order_id) as order_count FROM users u LEFT JOIN orders o ON u.id = o.user_id WHERE u.status = 1 AND u.created_at >= '2024-01-01' GROUP BY u.id HAVING COUNT(o.order_id) > 0 ORDER BY order_count DESC LIMIT 100;";

        String result = SqlFormatter.compress(input);

        assertNotNull(result);
        System.out.println("\n=== 压缩复杂查询 ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n压缩结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);

        assertEquals(result, expected);
    }

    // ==================== 格式化与压缩互逆测试 ====================

    @Test(description = "格式化后再压缩应该得到简洁的 SQL")
    public void testFormatThenCompress() {
        String input = "select id,name,email from users where status=1 and age>18";

        String formatted = SqlFormatter.format(input);
        String compressed = SqlFormatter.compress(formatted);

        assertNotNull(formatted);
        assertNotNull(compressed);

        System.out.println("\n=== 格式化后再压缩 ===");
        System.out.println("原始 SQL:");
        System.out.println(input);
        System.out.println("\n格式化后:");
        System.out.println(formatted);
        System.out.println("\n压缩后:");
        System.out.println(compressed);

        assertTrue(formatted.contains("\n"), "格式化后应包含换行");
        assertFalse(compressed.contains("\n"), "压缩后不应包含换行");
    }

    @Test(description = "压缩后再格式化应该保持功能")
    public void testCompressThenFormat() {
        String input = """
                SELECT
                    u.id,
                    u.name
                FROM users u
                WHERE u.status = 1;""";

        String compressed = SqlFormatter.compress(input);
        String formatted = SqlFormatter.format(compressed);

        assertNotNull(compressed);
        assertNotNull(formatted);

        System.out.println("\n=== 压缩后再格式化 ===");
        System.out.println("原始 SQL:");
        System.out.println(input);
        System.out.println("\n压缩后:");
        System.out.println(compressed);
        System.out.println("\n再格式化:");
        System.out.println(formatted);

        assertFalse(compressed.contains("\n"), "压缩后不应包含换行");
        assertTrue(formatted.contains("\n"), "格式化后应包含换行");
    }

    // ==================== 边界情况测试 ====================

    @Test(description = "格式化 null SQL")
    public void testFormatNullSQL() {
        String result = SqlFormatter.format(null);
        assertNull(result);
    }

    @Test(description = "格式化空字符串")
    public void testFormatEmptySQL() {
        String input = "";
        String result = SqlFormatter.format(input);
        assertEquals(result, input);
    }

    @Test(description = "压缩 null SQL")
    public void testCompressNullSQL() {
        String result = SqlFormatter.compress(null);
        assertNull(result);
    }

    @Test(description = "压缩空字符串")
    public void testCompressEmptySQL() {
        String input = "";
        String result = SqlFormatter.compress(input);
        assertEquals(result, input);
    }

    @Test(description = "格式化包含字符串字面量的 SQL")
    public void testFormatWithStringLiterals() {
        String input = "select * from users where name='John Doe' and city='New York' and email='john@example.com'";

        String expected = """
                SELECT *
                  FROM users
                    WHERE name = 'John Doe'
                      AND city = 'New York'
                      AND email = 'john@example.com';""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化包含字符串字面量的 SQL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);

        assertEquals(result, expected);
    }

    @Test(description = "格式化包含数字的 SQL")
    public void testFormatWithNumbers() {
        String input = "select * from products where price>99.99 and stock>=100 and discount<=0.5";

        String expected = """
                SELECT *
                  FROM products
                    WHERE price > 99.99
                      AND stock >= 100
                      AND discount <= 0.5;""";

        String result = SqlFormatter.format(input);

        assertNotNull(result);
        System.out.println("\n=== 格式化包含数字的 SQL ===");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n格式化结果:");
        System.out.println(result);
        System.out.println("\n预期结果:");
        System.out.println(expected);

        assertEquals(result, expected);
    }
}

