package com.zhisangui.zojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import com.zhisangui.zojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandBoxTemplate implements CodeSandBox {
    public static final String USER_CODE_DIR = "userCode";
    public static final String CODE_NAME = "Main.java";
    public static final String CLASS_NAME = "Main";
    public static final Long TIME_LIMIT = 1000L;


    /**
     * 1. 获取用户的代码，并将用户的代码写入文件
     *
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {

        // todo：目前只针对 java，后期可以拓展其他语言

        // 获取用户当前工作目录
        String userDir = System.getProperty("user.dir");
        String userCodeParentPath = userDir + File.separator + USER_CODE_DIR + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + CODE_NAME;
        log.info("用户的代码位置: {}", userCodePath);
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2. 对用户的代码进行编译（注意指定编码规则），并处理编译的信息，获得 class 文件
     *
     * @return
     */
    public ExecuteMessage compileCode(String userCodePath) {
        String compileCmd = "javac -encoding utf-8 " + userCodePath;
        System.out.println("编译命令为" + compileCmd);
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            // todo：这里的处理编译信息暂时没用到
            return ProcessUtils.getExecuteMessage(compileProcess, "编译");
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 运行用户的代码，附带输入用例作为参数输入
     *
     * @param inputs
     * @param userCodeParentPath
     * @return
     */
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodeParentPath) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputs) {
            try {
                // -Xmx256m 用来设置 jvm 的最大堆内存，防止恶意代码占用内存
                String securityManager = "E:\\java\\starProject\\zoj-code-sandbox\\src\\main\\resources\\securityManager";
                // 设置安全管理器
//                String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=MySecurityManager %s", userCodeParentPath, securityManager, CLASS_NAME);
                String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s %s", userCodeParentPath, CLASS_NAME);
//                System.out.println("运行命令为" + execCmd);
                log.info("execCmd：{}", execCmd);
                Process runProcess = Runtime.getRuntime().exec(execCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_LIMIT);
//                        System.out.println("超时中断");
                        log.info("timeout interrupt");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runInteractProcess(runProcess, inputArgs);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 封装返回值 executeCodeResponseI
     *
     * @return
     */
    public ExecuteCodeResponse processResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
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
        return executeCodeResponse;
    }

    /**
     * 5. 关闭资源
     *
     * @param userCodeFile
     * @param userCodeParentPath
     * @return
     */
    public boolean closeResource(File userCodeFile, String userCodeParentPath) {
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            log.info("删除{}", (del ? "成功" : "失败"));
            return del;
        }
        log.info("删除成功");
        return true;
    }

    /**
     * 6. 封装异常结果
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Exception e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(1);
        executeCodeResponse.setOutputs(new ArrayList<>());
        return executeCodeResponse;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        // 1. 获取用户代码，并写入文件
        String code = request.getCode();
        File userCodeFile = saveCodeToFile(code);

        // 2. 对用户的代码进行编译（注意指定编码规则），并处理编译的信息，获得 class 文件
        log.info("compile start");
        ExecuteMessage compiledExecuteMessage = compileCode(userCodeFile.getAbsolutePath());
        log.info("compile end");

        // 3. 运行用户的代码，附带输入用例作为参数输入
        List<String> inputs = request.getInputs();
        List<ExecuteMessage> executeMessageList = executeCode(inputs, userCodeFile.getParentFile().getAbsolutePath());

        // 4. 封装返回值 executeCodeResponseI
        ExecuteCodeResponse executeCodeResponse = processResponse(executeMessageList);

        // 5. 关闭资源
        boolean b = closeResource(userCodeFile, userCodeFile.getParentFile().getAbsolutePath());
        if (!b)
            throw new RuntimeException("关闭资源错误");
        return executeCodeResponse;
    }

}
