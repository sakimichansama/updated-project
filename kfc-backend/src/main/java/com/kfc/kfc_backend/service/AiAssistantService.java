package com.kfc.kfc_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private static final int MAX_QUERY_ROWS = 100;
    private static final int MAX_SQL_REPAIR_ATTEMPTS = 2;
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+(?:[`\"]?[a-zA-Z_][a-zA-Z0-9_]*[`\"]?\\.)?[`\"]?([a-zA-Z_][a-zA-Z0-9_]*)[`\"]?");
    private static final Pattern BLOCKED_SQL_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|create|truncate|replace|merge|call|grant|revoke|set|use|load|lock|unlock|show|describe|explain|into|outfile|infile)\\b");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+\\d+");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${deepseek.api-key:}")
    private String apiKey;
    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;
    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = buildRestTemplate();

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(45));
        return new RestTemplate(factory);
    }

    public Map<String, Object> chat(String question) {
        String userQuestion = question == null ? "" : question.trim();
        if (userQuestion.isBlank()) {
            return response("Please enter a question.", "local", false, false, null, null, null);
        }
        if (apiKey == null || apiKey.isBlank()) {
            return response("DeepSeek is not configured. Set DEEPSEEK_API_KEY to enable schema-based SQL routing.",
                    "local", false, false, null, null, null);
        }

        try {
            DatabaseSchema schema = exportDatabaseSchema();
            String plannerOutput = callDeepSeekForPlan(userQuestion, schema.promptText());
            Map<String, Object> plan = parsePlannerOutput(plannerOutput);
            boolean queryDatabase = toBoolean(plan.get("queryDatabase"));

            if (!queryDatabase) {
                String directAnswer = callDeepSeekDirect(userQuestion);
                return response(directAnswer, "deepseek", true, false, null, null, null);
            }

            BusinessQueryResult queryResult = executeBusinessQuery(userQuestion, schema, stringValue(plan.get("sql")));
            return response(queryResult.answer(), "deepseek-sql", true, true,
                    queryResult.sql(), queryResult.rowCount(), queryResult.rows());
        } catch (Exception e) {
            if (!looksLikeBusinessQuestion(userQuestion)) {
                try {
                    String directAnswer = callDeepSeekDirect(userQuestion);
                    return response(directAnswer, "deepseek", true, false, null, null, null);
                } catch (Exception directException) {
                    return response("DeepSeek call failed. Reason: " + directException.getMessage(),
                            "deepseek-error", false, false, null, null, null);
                }
            }
            log.warn("Assistant business query failed after SQL planning/repair. question={}", userQuestion, e);
            return response(businessQueryFailureMessage(userQuestion),
                    "deepseek-sql-error", true, true, null, null, null);
        }
    }

    private String callDeepSeekForPlan(String question, String schemaText) {
        String currentDate = LocalDate.now().toString();
        return callDeepSeek(List.of(
                Map.of("role", "system", "content",
                        "You are a database routing assistant for a KFC store management system. "
                                + "Decide whether the user question should query the business database. "
                                + "Use only the provided schema. If database data is needed, return only JSON with "
                                + "{\"queryDatabase\":true,\"sql\":\"SELECT ...\"}. "
                                + "If database data is not needed, return only JSON with "
                                + "{\"queryDatabase\":false,\"answer\":\"direct answer\"}. "
                                + "Weather, news, translation, coding, math, general knowledge, and any question "
                                + "that does not require the store tables must be queryDatabase=false. "
                                + "SQL rules: MySQL-compatible read-only SELECT only; use listed tables and columns only; "
                                + "do not query password or hidden columns; add LIMIT 100 for detail rows; "
                                + "verify every table name, column name, alias, aggregate, and date expression against the schema before returning; "
                                + "use the current date " + currentDate + " for relative date ranges. "
                                + "Do not wrap JSON in markdown."),
                Map.of("role", "user", "content",
                        "Database schema:\n" + schemaText + "\n\nQuestion:\n" + question)
        ), 0.0);
    }

    private String callDeepSeekForSqlRepair(String question,
                                            String schemaText,
                                            String failedSql,
                                            String errorMessage,
                                            List<SqlAttemptFailure> failures) {
        String currentDate = LocalDate.now().toString();
        return callDeepSeek(List.of(
                Map.of("role", "system", "content",
                        "You repair failed SQL for a KFC store management system. "
                                + "Return only JSON with {\"sql\":\"SELECT ...\"}. "
                                + "Use only the provided schema and answer the original question with one MySQL-compatible read-only SELECT. "
                                + "Do not use password or hidden columns. Do not use write operations, multiple statements, comments, subqueries, SHOW, DESCRIBE, or EXPLAIN. "
                                + "Add LIMIT 100 for detail rows. Verify every table name, column name, alias, aggregate, and date expression against the schema. "
                                + "Use the current date " + currentDate + " for relative date ranges. Do not wrap JSON in markdown."),
                Map.of("role", "user", "content",
                        "Database schema:\n" + schemaText
                                + "\n\nOriginal question:\n" + question
                                + "\n\nFailed SQL:\n" + failedSql
                                + "\n\nSQL validation/execution error:\n" + errorMessage
                                + "\n\nPrevious failed attempts:\n" + toJson(failures))
        ), 0.0);
    }

    private String callDeepSeekWithSqlResult(String question, String sql, List<Map<String, Object>> rows) {
        return callDeepSeek(List.of(
                Map.of("role", "system", "content",
                        "You are a KFC store operations analyst. Answer in the same language as the user. "
                                + "Use only the SQL result for business facts. If the result is empty, say that no matching data was found. "
                                + "Give concise conclusions, reasons, and practical suggestions when the data supports them. "
                                + "Do not include the SQL statement in the final answer."),
                Map.of("role", "user", "content",
                        "Question:\n" + question
                                + "\n\nExecuted SQL:\n" + sql
                                + "\n\nSQL result rows:\n" + toJson(rows))
        ), 0.2);
    }

    private String callDeepSeekDirect(String question) {
        return callDeepSeek(List.of(
                Map.of("role", "system", "content",
                        "You are a helpful assistant. Answer directly in the same language as the user. "
                                + "Do not invent store database facts."),
                Map.of("role", "user", "content", question)
        ), 0.7);
    }

    private BusinessQueryResult executeBusinessQuery(String question, DatabaseSchema schema, String initialSql) {
        String sqlCandidate = initialSql;
        List<SqlAttemptFailure> failures = new ArrayList<>();

        for (int attempt = 0; attempt <= MAX_SQL_REPAIR_ATTEMPTS; attempt++) {
            SqlQueryResult queryResult;
            try {
                queryResult = executeReadOnlySql(sqlCandidate, schema.tableNames());
            } catch (Exception sqlException) {
                String safeError = sanitizeSqlError(sqlException);
                failures.add(new SqlAttemptFailure(sqlCandidate, safeError));
                log.warn("KFC Assistant SQL attempt {} failed. sql={}, error={}", attempt + 1, sqlCandidate, safeError);

                if (attempt >= MAX_SQL_REPAIR_ATTEMPTS) {
                    break;
                }

                String repairOutput = callDeepSeekForSqlRepair(
                        question,
                        schema.promptText(),
                        sqlCandidate,
                        safeError,
                        failures
                );
                Map<String, Object> repairPlan = parsePlannerOutput(repairOutput);
                sqlCandidate = stringValue(repairPlan.get("sql"));
                continue;
            }

            log.info("KFC Assistant SQL executed successfully after {} attempt(s): {}", attempt + 1, queryResult.sql());
            String answer = callDeepSeekWithSqlResult(question, queryResult.sql(), queryResult.rows());
            return new BusinessQueryResult(answer, queryResult.sql(), queryResult.rowCount(), queryResult.rows());
        }

        throw new RuntimeException("business query failed after SQL repair attempts");
    }

    private SqlQueryResult executeReadOnlySql(String rawSql, Set<String> allowedTables) {
        String sql = normalizeSql(rawSql);
        validateReadOnlySql(sql, allowedTables);
        String limitedSql = ensureLimit(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(limitedSql);
        List<Map<String, Object>> resultRows = rows.size() > MAX_QUERY_ROWS ? rows.subList(0, MAX_QUERY_ROWS) : rows;
        return new SqlQueryResult(limitedSql, resultRows.size(), resultRows);
    }

    private String callDeepSeek(List<Map<String, String>> messages, double temperature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", messages
        );
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), Map.class);
        Map responseBody = response.getBody();
        if (responseBody == null || responseBody.get("choices") == null) {
            throw new RuntimeException("empty response");
        }
        List choices = (List) responseBody.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("empty choices");
        }
        Map choice = (Map) choices.get(0);
        Map message = (Map) choice.get("message");
        return String.valueOf(message.get("content"));
    }

    private DatabaseSchema exportDatabaseSchema() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            throw new RuntimeException("data source is not available");
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<TableInfo> tables = readTables(metaData, connection.getCatalog(), null);
            if (tables.isEmpty()) {
                tables = readTables(metaData, null, null);
            }
            tables.sort(Comparator.comparing(table -> table.name().toLowerCase(Locale.ROOT)));
            return new DatabaseSchema(tables);
        } catch (SQLException e) {
            throw new RuntimeException("failed to export database schema: " + e.getMessage(), e);
        }
    }

    private List<TableInfo> readTables(DatabaseMetaData metaData, String catalog, String schemaPattern) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (ResultSet tableRs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
            while (tableRs.next()) {
                String schema = tableRs.getString("TABLE_SCHEM");
                String tableName = tableRs.getString("TABLE_NAME");
                if (isSystemTable(schema, tableName) || !seen.add(tableName.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                tables.add(new TableInfo(tableName, readColumns(metaData, catalog, schema, tableName)));
            }
        }
        return tables;
    }

    private List<ColumnInfo> readColumns(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (ResultSet columnRs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (columnRs.next()) {
                String columnName = columnRs.getString("COLUMN_NAME");
                if ("password".equalsIgnoreCase(columnName)) {
                    continue;
                }
                columns.add(new ColumnInfo(columnName, columnRs.getString("TYPE_NAME")));
            }
        }
        return columns;
    }

    private boolean isSystemTable(String schema, String tableName) {
        String normalizedSchema = schema == null ? "" : schema.toLowerCase(Locale.ROOT);
        String normalizedTable = tableName == null ? "" : tableName.toLowerCase(Locale.ROOT);
        return normalizedSchema.equals("information_schema")
                || normalizedSchema.equals("mysql")
                || normalizedSchema.equals("performance_schema")
                || normalizedSchema.equals("sys")
                || normalizedTable.startsWith("qrtz_")
                || normalizedTable.equals("flyway_schema_history");
    }

    private Map<String, Object> parsePlannerOutput(String content) {
        String json = extractJsonObject(content);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("queryDatabase", false);
            fallback.put("answer", content);
            return fallback;
        }
    }

    private String extractJsonObject(String content) {
        String value = content == null ? "" : content.trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return "{\"queryDatabase\":false,\"answer\":" + toJson(value) + "}";
    }

    private String normalizeSql(String sql) {
        String value = sql == null ? "" : sql.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("(?is)^```sql\\s*", "")
                    .replaceFirst("(?is)^```\\s*", "")
                    .replaceFirst("(?is)```\\s*$", "")
                    .trim();
        }
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.isBlank()) {
            throw new RuntimeException("model did not return SQL");
        }
        return value;
    }

    private void validateReadOnlySql(String sql, Set<String> allowedTables) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select")) {
            throw new RuntimeException("only SELECT SQL is allowed");
        }
        if (sql.contains(";") || lower.contains("--") || lower.contains("/*") || lower.contains("*/") || lower.contains("#")) {
            throw new RuntimeException("comments and multiple statements are not allowed");
        }
        if (lower.contains(" from (") || lower.contains(" join (")) {
            throw new RuntimeException("subqueries are not allowed");
        }
        if (BLOCKED_SQL_PATTERN.matcher(lower).find()) {
            throw new RuntimeException("generated SQL contains a blocked keyword");
        }
        if (Pattern.compile("(?i)\\bpassword\\b").matcher(sql).find()) {
            throw new RuntimeException("password fields cannot be queried");
        }

        Matcher matcher = TABLE_PATTERN.matcher(sql);
        Set<String> usedTables = new TreeSet<>();
        while (matcher.find()) {
            usedTables.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        if (usedTables.isEmpty()) {
            throw new RuntimeException("no query table was detected");
        }
        for (String table : usedTables) {
            if (!allowedTables.contains(table)) {
                throw new RuntimeException("table is not in the exported schema: " + table);
            }
        }
    }

    private String ensureLimit(String sql) {
        if (LIMIT_PATTERN.matcher(sql).find()) {
            return sql;
        }
        return sql + " LIMIT " + MAX_QUERY_ROWS;
    }

    private boolean looksLikeBusinessQuestion(String question) {
        String text = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String[] keywords = {
                "inventory", "stock", "replenish", "product", "sales", "sale", "order", "revenue",
                "profit", "margin", "cost", "material", "labor", "payroll", "salary", "wage",
                "employee", "attendance", "performance", "waste", "loss", "inbound", "outbound",
                "purchase", "supplier", "store", "operation", "kfc",
                "\u5e93\u5b58", "\u5b58\u8d27", "\u8865\u8d27", "\u5546\u54c1", "\u4ea7\u54c1",
                "\u9500\u552e", "\u8425\u6536", "\u8425\u4e1a\u989d", "\u8ba2\u5355",
                "\u5229\u6da6", "\u6bdb\u5229", "\u51c0\u5229", "\u6210\u672c",
                "\u85aa\u8d44", "\u5de5\u8d44", "\u5458\u5de5", "\u4eba\u4e8b", "\u5de5\u65f6", "\u7ee9\u6548",
                "\u62a5\u635f", "\u635f\u8017", "\u5165\u5e93", "\u51fa\u5e93",
                "\u91c7\u8d2d", "\u4f9b\u5e94\u5546", "\u95e8\u5e97", "\u7ecf\u8425"
        };
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String sanitizeSqlError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replaceAll("(?is)\\s+", " ").trim();
        message = message.replaceAll("(?i)(password\\s*[=:]\\s*)[^\\s,;]+", "$1[redacted]");
        message = message.replaceAll("(?i)(using password:\\s*)[^\\s,;]+", "$1[redacted]");
        return message.length() > 1200 ? message.substring(0, 1200) + "...[truncated]" : message;
    }

    private String businessQueryFailureMessage(String question) {
        if (containsChinese(question)) {
            return "业务数据查询暂时失败，我已经尝试重新生成查询语句。请稍后重试，或换一种更具体的问法。";
        }
        return "The business data query could not be completed after retrying. Please try again later or ask a more specific question.";
    }

    private boolean containsChinese(String value) {
        return value != null && Pattern.compile("[\\u4e00-\\u9fff]").matcher(value).find();
    }

    private String toJson(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.length() > 16000 ? json.substring(0, 16000) + "...[truncated]" : json;
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> response(String answer,
                                         String provider,
                                         boolean modelConnected,
                                         boolean businessQuery,
                                         String sql,
                                         Integer rowCount,
                                         List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("provider", provider);
        result.put("modelConnected", modelConnected);
        result.put("businessQuery", businessQuery);
        result.put("queryDatabase", businessQuery);
        if (sql != null) {
            result.put("sql", sql);
        }
        if (rowCount != null) {
            result.put("rowCount", rowCount);
        }
        if (rows != null) {
            result.put("rows", rows);
        }
        return result;
    }

    private record DatabaseSchema(List<TableInfo> tables) {
        private String promptText() {
            StringBuilder builder = new StringBuilder();
            for (TableInfo table : tables) {
                builder.append("- ").append(table.name()).append("(");
                for (int i = 0; i < table.columns().size(); i++) {
                    ColumnInfo column = table.columns().get(i);
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(column.name()).append(" ").append(column.type());
                }
                builder.append(")\n");
            }
            return builder.toString();
        }

        private Set<String> tableNames() {
            Set<String> names = new TreeSet<>();
            for (TableInfo table : tables) {
                names.add(table.name().toLowerCase(Locale.ROOT));
            }
            return names;
        }
    }

    private record TableInfo(String name, List<ColumnInfo> columns) {
    }

    private record ColumnInfo(String name, String type) {
    }

    private record SqlQueryResult(String sql, Integer rowCount, List<Map<String, Object>> rows) {
    }

    private record BusinessQueryResult(String answer, String sql, Integer rowCount, List<Map<String, Object>> rows) {
    }

    private record SqlAttemptFailure(String sql, String error) {
    }
}
