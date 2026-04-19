package com.zhisangui.zojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import com.zhisangui.zojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * gcc编译器
 */
@Slf4j
public class CppCodeSandBoxTemplate implements CodeSandBox{
    //通用属性
    private static final String USER_CODE_DIR = "userCode"; //用户运行代码的首目录
    private static final Long TIME_LIMIT = 1000L;   //运行的时间限制
    private static final String CODE_NAME = "Main.cpp";
    private static final String CLASS_NAME = "Main";

    /**
     * 1. 获取用户的代码，并将用户的代码写入文件
     * @param code 用户代码
     * @return
     */
    private File saveCodeToFile(String code){
        //获取当前用户工作的目录
        String userDir = System.getProperty("user.dir");
        //生成代码所在的路径
        String userCodeParentPath = userDir + File.separator + USER_CODE_DIR + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + CODE_NAME;
        //将代码写入到文件
        File file = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        log.info("用户代码位置: {}" , userCodePath);
        //返回文件
        return file;
    }

    /**
     * 2. 对用户的代码进行编译（注意指定编码规则），并处理编译的信息，获得 已编译好的文件
     * @return
     */
    private ExecuteMessage compileCode(String userCodeParentPath) {
        //要获取源文件即cpp文件以及编译过后的文件路径
        String sourceFilePath = userCodeParentPath + File.separator + CODE_NAME;
        String outputFilePath = userCodeParentPath + File.separator + CLASS_NAME;
        //cmd编译指令
        String compileCmd = String.format("g++ -o %s %s -std=c++11", outputFilePath, sourceFilePath);
        System.out.println("编译命令为: " + compileCmd);
        try{
            //执行编译
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            //获取编译结果并返回
            return ProcessUtils.getExecuteMessage(compileProcess,"编译");
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     * 3. 运行用户的代码，附带输入用例作为参数输入
     * @param inputs
     * @param userCodeParentPath
     * @return
     */
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodeParentPath) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        String executablePath = userCodeParentPath + File.separator + CLASS_NAME; // 修正可执行文件路径
        for (String inputArgs : inputs) {
            try {
                //String execCmd = String.format("ulimit -v 256000; %s %s", userCodeParentPath, CLASS_NAME);
                // 构造完整的 Shell 命令
                String[] cmdArray = {
                        "/bin/bash",
                        "-c",
                        String.format("ulimit -v 256000; %s", executablePath)
                };

                log.info("execCmd：{}", cmdArray);
                Process runProcess = Runtime.getRuntime().exec(cmdArray);
                // **通过标准输入流写入输入数据**

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
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 封装返回值 executeCodeResponseI
     * @return
     */
    public ExecuteCodeResponse processResponse(List<ExecuteMessage> executeMessageList) {
        System.out.println("4.封装返回值...");
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
        long time = 0L;
        List<String> outputList = new ArrayList<>();
        for(ExecuteMessage executeMessage : executeMessageList){
            //返回值有不正常的
            if(StringUtils.isNotBlank(executeMessage.getErrorMessage())){
                executeCodeResponse.setStatus(1);
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                break;
            }
            //运行时间
            time = Math.max(time,executeMessage.getTime());
            outputList.add(executeMessage.getMessage());
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(time);
        executeCodeResponse.setOutputs(outputList);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5. 关闭资源
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
        return true;
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        //1.将代码写入到文件中
        String code = request.getCode();
        File userCodeFile = this.saveCodeToFile(code);

        //2.编译代码
        ExecuteMessage executeMessage = compileCode(userCodeFile.getParentFile().getAbsolutePath());

        //3.运行代码
        List<String> inputs = request.getInputs();
        List<ExecuteMessage> executeMessageList = executeCode(inputs, userCodeFile.getParentFile().getAbsolutePath());

        //4.封装数据
        ExecuteCodeResponse executeCodeResponse = processResponse(executeMessageList);

        //5.关闭资源
        boolean b = closeResource(userCodeFile, userCodeFile.getParentFile().getAbsolutePath());
        if (!b)
            throw new RuntimeException("关闭资源错误");
        return executeCodeResponse;
    }
}
