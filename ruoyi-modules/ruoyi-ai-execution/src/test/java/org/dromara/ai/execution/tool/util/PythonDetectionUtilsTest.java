package org.dromara.ai.execution.tool.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PythonDetectionUtilsTest {

    @Test
    public void testDetect() {
        PythonDetectionUtils.resetCache();
        String path = PythonDetectionUtils.detect(null);
        System.out.println("Detected Python path: " + path);
        if (path != null) {
            Assertions.assertTrue(PythonDetectionUtils.verifyPython(path));
        }
    }

    @Test
    public void testVerifyInvalidPath() {
        Assertions.assertFalse(PythonDetectionUtils.verifyPython("invalid_python_path_123456"));
    }
}
