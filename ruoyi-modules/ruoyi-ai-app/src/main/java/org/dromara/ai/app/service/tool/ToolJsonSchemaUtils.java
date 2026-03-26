package org.dromara.ai.app.service.tool;
import org.dromara.ai.workflow.workflow.nodes.tool.*;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.ToolSpecification;

/**
 * 工具 JSON Schema 生成工具类
 * 根据 initParams（JSON Array）生成 LLM 可识别的 inputSchema
 *
 * initParams 格式示例:
 * [{"name":"query","type":"string","description":"查询内容","required":true}]
 */
@Slf4j
public class ToolJsonSchemaUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String generateInputSchema(String initParams) {
        if (StrUtil.isBlank(initParams)) {
            return buildEmptySchema();
        }
        try {
            JsonNode paramsNode = MAPPER.readTree(initParams);
            if (!paramsNode.isArray()) {
                return buildEmptySchema();
            }

            ObjectNode schema = MAPPER.createObjectNode();
            schema.put("type", "object");

            ObjectNode properties = MAPPER.createObjectNode();
            List<String> required = new ArrayList<>();

            for (JsonNode param : paramsNode) {
                String name = param.path("name").asText(null);
                if (StrUtil.isBlank(name)) continue;

                ObjectNode propNode = MAPPER.createObjectNode();
                propNode.put("type", mapType(param.path("type").asText("string")));
                String desc = param.path("description").asText("");
                if (StrUtil.isNotBlank(desc)) propNode.put("description", desc);
                properties.set(name, propNode);

                if (param.path("required").asBoolean(false)) required.add(name);
            }

            schema.set("properties", properties);
            if (!required.isEmpty()) {
                ArrayNode req = MAPPER.createArrayNode();
                required.forEach(req::add);
                schema.set("required", req);
            }
            return MAPPER.writeValueAsString(schema);
        } catch (Exception e) {
            log.warn("生成 inputSchema 失败", e);
            return buildEmptySchema();
        }
    }

    private static String mapType(String type) {
        return switch (type.toLowerCase()) {
            case "int", "integer", "long" -> "integer";
            case "float", "double", "number" -> "number";
            case "bool", "boolean" -> "boolean";
            case "array", "list" -> "array";
            case "object", "map", "dict" -> "object";
            default -> "string";
        };
    }

    private static String buildEmptySchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    /**
     * 根据 JSON Schema 规范构建 LangChain4j 的 ToolSpecification
     * 使用 LangChain4j 0.36.x 的 JsonObjectSchema API，确保 parameters 被正确传递给大模型
     *
     * @param name            工具名称
     * @param description     工具描述
     * @param inputSchemaJson JSON Schema 格式的参数定义
     * @return LangChain4j 的规范对象
     */
    public static ToolSpecification buildToolSpecification(String name, String description, String inputSchemaJson) {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(name)
                .description(description);

        if (StrUtil.isBlank(inputSchemaJson)) {
            // 即使没有参数，也设置一个空的 JsonObjectSchema，避免 parameters = null
            builder.parameters(JsonObjectSchema.builder().build());
            return builder.build();
        }

        try {
            JsonNode schemaNode = MAPPER.readTree(inputSchemaJson);
            JsonNode propertiesNode = schemaNode.path("properties");
            JsonNode requiredNode = schemaNode.path("required");

            List<String> requiredFields = new ArrayList<>();
            if (requiredNode.isArray()) {
                requiredNode.forEach(req -> requiredFields.add(req.asText()));
            }

            Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();

            if (propertiesNode.isObject()) {
                propertiesNode.fields().forEachRemaining(entry -> {
                    String propName = entry.getKey();
                    JsonNode propNode = entry.getValue();
                    String type = propNode.path("type").asText("string");
                    String desc = propNode.path("description").asText("");

                    JsonSchemaElement element = buildJsonSchemaElement(type, desc);
                    properties.put(propName, element);
                });
            }

            JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder()
                    .properties(properties);
            if (!requiredFields.isEmpty()) {
                schemaBuilder.required(requiredFields);
            }
            builder.parameters(schemaBuilder.build());

        } catch (Exception e) {
            log.warn("构建ToolSpecification失败, 参数解析异常: {}, 使用空schema兜底", e.getMessage());
            builder.parameters(JsonObjectSchema.builder().build());
        }

        return builder.build();
    }

    /**
     * 将 JSON Schema 类型字符串转换为 LangChain4j 的 JsonSchemaElement
     */
    private static JsonSchemaElement buildJsonSchemaElement(String type, String description) {
        return switch (type.toLowerCase()) {
            case "integer", "int", "long" -> JsonIntegerSchema.builder().description(description).build();
            case "number", "float", "double" -> JsonNumberSchema.builder().description(description).build();
            case "boolean", "bool" -> JsonBooleanSchema.builder().description(description).build();
            case "array", "list" -> JsonArraySchema.builder().description(description).build();
            default -> JsonStringSchema.builder().description(description).build();
        };
    }
}
