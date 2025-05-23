package com.zhisangui.zojcodesandbox.codesandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CodeSandBoxFactory {
    public CodeSandBox getCodeSandBox(String language) {
        CodeSandBox codeSandBox = null;
        switch (language) {
            case "java":
                codeSandBox = new JavaDockerCodeSandBoxRunOnLinux();
                break;
            case "python":
                codeSandBox = new PyDockerCodeSandBoxRunOnLinux();
                break;
            case "c++":
                codeSandBox = new CppDockerCodeSandBoxRunOnLinux();
                break;
        }
        if (codeSandBox == null) {
            log.error("language {} is not supported", language);
            throw new RuntimeException("language " + language + " is not supported");
        }
        return codeSandBox;
    }
}
