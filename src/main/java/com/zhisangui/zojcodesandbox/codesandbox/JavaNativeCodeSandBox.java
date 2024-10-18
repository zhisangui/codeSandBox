package com.zhisangui.zojcodesandbox.codesandbox;

import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Java 原生代码沙箱实现（模板方法使用）
 */
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate{
    public static void main(String[] args) {
        CodeSandBox codeSandBox = new JavaNativeCodeSandBox();

        // 测试恶意代码
//        String userDir = System.getProperty("user.dir");
//        String unSafeFile = userDir + File.separator + "unsafe" + File.separator + CODE_NAME;
//        String code = FileUtil.readString(unSafeFile, StandardCharsets.UTF_8);

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(  "import java.util.*;\n" +
                        "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "    Scanner scanner = new Scanner(System.in);\n" +
                        "        int a = scanner.nextInt();\n" +
                        "        int b = scanner.nextInt();\n" +
                        "        System.out.println(\"结果为：\" + (a + b));\n" +
                        "    }\n" +
                        "}")
//                .code(code)
                .language("java")
                .inputs(Arrays.asList("111232 1123213", "3312 4231"))
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        System.out.println("java 原生沙箱");
        return super.executeCode(request);
    }

}
