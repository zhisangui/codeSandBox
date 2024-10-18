package com.zhisangui.zojcodesandbox.utils;

import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

/**
 * 用来处理进程相关的工具类
 */
public class ProcessUtils {

    /**
     * 获取进程执行的相关信息
     *
     * @param process 进行
     * @param status  当前在执行那个步骤
     * @return 进程执行的相关信息
     */
    public static ExecuteMessage getExecuteMessage(Process process, String status) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = process.waitFor();
            executeMessage.setExitValue(exitValue);
            // todo: 时间和内存还没有进行记录

            // 正常返回
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader normalBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            normalBufferedReader.lines().forEach(line -> {
                stringBuilder.append(line).append("\n");
                // todo： 这里输出乱码
                System.out.println(status + ": " + line);
            });
            normalBufferedReader.close();
            executeMessage.setMessage(stringBuilder.toString());

            // 非正常返回
            if (exitValue != 0) {
                StringBuilder errorMsgStringBuilder = new StringBuilder();
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                errorBufferedReader.lines().forEach(line -> {
                    errorMsgStringBuilder.append(line).append("\n");
                    System.out.println(status + ": " + line);
                });
                errorBufferedReader.close();
                executeMessage.setErrorMessage(errorMsgStringBuilder.toString());
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            return executeMessage;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行交互程序
     */
    public static ExecuteMessage runInteractProcess(Process process, String inputArgs) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            // 注意各个参数应该以换行符隔开，否则会被接收为一个参数
            String[] s = inputArgs.split(" ");
            inputArgs = String.join("\n", s) + "\n";
            bufferedWriter.write(inputArgs);
            bufferedWriter.flush();
            bufferedWriter.close();
            return ProcessUtils.getExecuteMessage(process, "运行");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
