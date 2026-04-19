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
public abstract class PythonCodeSandBoxTemplate implements CodeSandBox {
    public static final String USER_CODE_DIR = "userCode";
    public static final String CODE_NAME = "main.py";
    public static final Long TIME_LIMIT = 1000L;

    /**
     * 1. 保存代码到文件
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String userCodeParentPath = userDir + File.separator + USER_CODE_DIR + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + CODE_NAME;
        log.info("用户的代码位置: {}", userCodePath);
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2. 执行Python代码
     */
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodePath) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputs) {
            try {
                // Python执行命令
                String execCmd = String.format("python %s", userCodePath);
                log.info("execCmd：{}", execCmd);

                Process runProcess = Runtime.getRuntime().exec(execCmd);

                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_LIMIT);
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
     * 3. 处理响应结果
     */
    public ExecuteCodeResponse processResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
        long time = 0L;
        List<String> outputs = new ArrayList<>();

        for (ExecuteMessage executeMessage : executeMessageList) {
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
     * 4. 清理资源
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

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        // 1. 保存代码到文件
        String code = request.getCode();
        File userCodeFile = saveCodeToFile(code);

        // 2. 执行代码
        List<String> inputs = request.getInputs();
        List<ExecuteMessage> executeMessageList = executeCode(inputs, userCodeFile.getAbsolutePath());

        // 3. 处理响应
        ExecuteCodeResponse executeCodeResponse = processResponse(executeMessageList);

        // 4. 清理资源
        boolean b = closeResource(userCodeFile, userCodeFile.getParentFile().getAbsolutePath());
        if (!b) {
            throw new RuntimeException("关闭资源错误");
        }
        return executeCodeResponse;
    }
}