package com.laker.postman.plugin.redis;

import com.laker.postman.model.script.ScriptOptionUtil;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Script Redis API for pm.redis.
 */
public class ScriptRedisApi {
    private static final Pattern ARG_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");

    public Object query(Object options) {
        return execute(options);
    }

    /**
     * Execute redis command in one-shot mode.
     *
     * options:
     * host, port, db, username, password, timeoutMs
     * command/cmd, key, args, value
     */
    public Object execute(Object options) {
        Map<String, Object> map = ScriptOptionUtil.toMap(options);
        String command = ScriptOptionUtil.getRequiredString(map, "command", "cmd").toUpperCase(Locale.ROOT);
        String key = ScriptOptionUtil.getString(map, "", "key");
        Object argsObj = ScriptOptionUtil.get(map, "args");
        Object valueObj = ScriptOptionUtil.get(map, "value");

        if (key.isBlank()) {
            throw new IllegalArgumentException("Missing required option: key");
        }

        try (JedisPooled jedis = buildClient(map)) {
            return runCommand(jedis, command, key, argsObj, valueObj);
        }
    }

    private JedisPooled buildClient(Map<String, Object> map) {
        String host = ScriptOptionUtil.getString(map, "localhost", "host");
        int port = ScriptOptionUtil.getInt(map, 6379, "port");
        int db = ScriptOptionUtil.getInt(map, 0, "db", "database");
        String user = ScriptOptionUtil.getString(map, "", "username", "user");
        String pass = ScriptOptionUtil.getString(map, "", "password", "pass");
        int timeoutMs = ScriptOptionUtil.getInt(map, 10_000, "timeoutMs", "timeout");

        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .database(db)
                .connectionTimeoutMillis(timeoutMs)
                .socketTimeoutMillis(timeoutMs);
        if (!user.isBlank()) {
            builder.user(user);
        }
        if (!pass.isBlank()) {
            builder.password(pass);
        }

        JedisPooled jedis = new JedisPooled(new HostAndPort(host, port), builder.build());
        jedis.ping();
        return jedis;
    }

    private Object runCommand(JedisPooled jedis, String command, String key, Object argsObj, Object valueObj) {
        List<String> args = parseArgs(argsObj);
        return switch (command) {
            case "GET" -> jedis.get(key);
            case "SET" -> {
                String value = valueObj == null ? "" : String.valueOf(valueObj).strip();
                if (value.isBlank()) {
                    value = String.join(" ", args).strip();
                }
                if (value.isBlank()) {
                    throw new IllegalArgumentException("SET requires non-empty value");
                }
                yield jedis.set(key, value);
            }
            case "DEL" -> jedis.del(key);
            case "EXISTS" -> jedis.exists(key);
            case "TYPE" -> jedis.type(key);
            case "TTL" -> jedis.ttl(key);
            case "HGET" -> {
                if (args.isEmpty()) {
                    throw new IllegalArgumentException("HGET requires args: field");
                }
                yield jedis.hget(key, args.get(0));
            }
            case "HGETALL" -> jedis.hgetAll(key);
            case "LRANGE" -> {
                long start = args.isEmpty() ? 0L : parseLongOrThrow(args.get(0), "start");
                long end = args.size() < 2 ? 99L : parseLongOrThrow(args.get(1), "end");
                yield jedis.lrange(key, start, end);
            }
            case "SMEMBERS" -> jedis.smembers(key);
            case "ZRANGE" -> {
                long start = args.isEmpty() ? 0L : parseLongOrThrow(args.get(0), "start");
                long end = args.size() < 2 ? 99L : parseLongOrThrow(args.get(1), "end");
                yield jedis.zrange(key, start, end);
            }
            default -> throw new IllegalArgumentException("Unsupported redis command: " + command);
        };
    }

    private long parseLongOrThrow(String raw, String name) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + ": " + raw, e);
        }
    }

    private List<String> parseArgs(Object argsObj) {
        Object normalized = ScriptOptionUtil.normalize(argsObj);
        if (normalized == null) {
            return Collections.emptyList();
        }
        if (normalized instanceof Collection<?> collection) {
            List<String> list = new ArrayList<>(collection.size());
            for (Object item : collection) {
                if (item != null) {
                    list.add(String.valueOf(item));
                }
            }
            return list;
        }
        String args = String.valueOf(normalized).trim();
        if (args.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        Matcher matcher = ARG_PATTERN.matcher(args);
        while (matcher.find()) {
            if (matcher.group(1) != null) list.add(matcher.group(1));
            else if (matcher.group(2) != null) list.add(matcher.group(2));
            else if (matcher.group(3) != null) list.add(matcher.group(3));
        }
        return list;
    }
}
