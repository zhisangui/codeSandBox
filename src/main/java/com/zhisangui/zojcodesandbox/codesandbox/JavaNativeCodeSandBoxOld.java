package com.zhisangui.zojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import com.zhisangui.zojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Deprecated
public class JavaNativeCodeSandBoxOld implements CodeSandBox {
    public static final String USER_CODE_DIR = "userCode";
    public static final String CODE_NAME = "Main.java";
    public static final String CLASS_NAME = "Main";
    public static final Long TIME_LIMIT = 1000L;
    public static final List<String> BLACK_LIST;
    static {
        BLACK_LIST = Arrays.asList("File", "exec");
    }

    public static void main(String[] args) {
        CodeSandBox codeSandBox = new JavaNativeCodeSandBoxOld();

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
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
        // 1. 获取用户的语言、代码，并将用户的代码写入文件
        // todo：目前只针对 java，后期可以拓展其他语言
        String code = request.getCode();

        // 黑名单限制用户的恶意代码（很不方便，慎用）
//        WordTree wordTree = new WordTree();
//        wordTree.addWords(BLACK_LIST);
//        String match = wordTree.match(code);
//        if (match != null) {
//            System.out.println("检测到恶意代码");
//            return null;
//        }

        String language = request.getLanguage();
        List<String> inputs = request.getInputs();
        // 获取用户当前工作目录
        String userDir = System.getProperty("user.dir");
        String userCodeParentPath = userDir + File.separator + USER_CODE_DIR + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + CODE_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2. 对用户的代码进行编译（注意指定编码规则），并处理编译的信息，获得 class 文件
        String compileCmd = "javac -encoding utf-8 " + userCodePath;
        System.out.println("编译命令为" + compileCmd);
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            // todo：这里的处理编译信息暂时没用到
            ExecuteMessage executeMessage = ProcessUtils.getExecuteMessage(compileProcess, "编译");
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        // 3. 运行用户的代码，附带输入用例作为参数输入
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputs) {
            try {
                // -Xmx256m 用来设置 jvm 的最大堆内存，防止恶意代码占用内存
                String securityManager = "E:\\java\\starProject\\zoj-code-sandbox\\src\\main\\resources\\securityManager";
                // 设置安全管理器
//                String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=MySecurityManager %s", userCodeParentPath, securityManager, CLASS_NAME);
                String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s %s", userCodeParentPath, CLASS_NAME);
                System.out.println("运行命令为" + execCmd);
                Process runProcess = Runtime.getRuntime().exec(execCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_LIMIT);
                        System.out.println("超时中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runInteractProcess(runProcess, inputArgs);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        // 4. 封装返回值 executeCodeResponseI
        long time = 0L;
        List<String> outputs = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            // 返回值有不正常的
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setStatus(1);
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                break;
            }
            time = Math.max(time, executeMessage.getTime());
            outputs.add(executeMessage.getMessage());
        }
        executeCodeResponse.setOutputs(outputs);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(time);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        // 5. 关闭资源
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    private ExecuteCodeResponse getErrorResponse(Exception e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(1);
        executeCodeResponse.setOutputs(new ArrayList<>());
        return executeCodeResponse;
    }
}
