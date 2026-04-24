package org.dromara.ai.app.util;
import java.lang.reflect.Method;
import dev.langchain4j.data.message.AiMessage;

public class TestReflect {
    public static void main(String[] args) {
        System.out.println("Methods in AiMessage:");
        for (Method m : AiMessage.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getParameterCount() + " params");
        }
    }
}
