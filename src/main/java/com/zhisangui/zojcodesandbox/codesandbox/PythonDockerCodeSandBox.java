package com.zhisangui.zojcodesandbox.codesandbox;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PythonDockerCodeSandBox extends PythonCodeSandBoxTemplate {
    private static final Long TIME_OUT = 5000L;
    private DockerClient dockerClient;
    private String containerId;

    @Override
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodePath) {
        // 1. 初始化Docker客户端
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(3000)
                .build();
        dockerClient = DockerClientBuilder.getInstance().withDockerHttpClient(dockerHttpClient).build();

        String image = "python";

        // 2. 创建容器
        String userCodeParentPath = new File(userCodePath).getParent();
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)
                .withName("pythonSandBox");

        HostConfig hostConfig = new HostConfig()
                .withBinds(new Bind(userCodeParentPath, new Volume("/app")))
                .withCpuCount(2L)
                .withMemorySwap(0L)
                .withMemory(256 * 1000 * 1000L);

        CreateContainerResponse createContainerResponse = createContainerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(false)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        containerId = createContainerResponse.getId();

        // 3. 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 4. 执行代码并获取结果
        return runInteractProcess(inputs, userCodePath);
    }

//    private List<ExecuteMessage> runInteractProcess(List<String> inputs, String userCodePath) {
//        List<ExecuteMessage> executeMessageList = new ArrayList<>();
//        String codeFileName = new File(userCodePath).getName();
//
//        for (String input : inputs) {
//            String[] s = input.split(" ");
//            input = String.join("\n", s) + "\n";
//            ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
//
//            String[] cmd = new String[]{"python", "/app/" + codeFileName};
//            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                    .withCmd(cmd)
//                    .withAttachStderr(true)
//                    .withAttachStdin(true)
//                    .withAttachStdout(true)
//                    .withTty(false)
//                    .exec();
//
//            String execId = execCreateCmdResponse.getId();
//            final Long[] memory = {0L};
//
//            ExecuteMessage executeMessage = new ExecuteMessage();
//            long time = 0L;
//            StringBuilder msgBuilder = new StringBuilder();
//            StringBuilder errorMsgBuilder = new StringBuilder();
//
//            // 监控内存使用
//            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
//            statsCmd.exec(new ResultCallback<Statistics>() {
//                @Override
//                public void onStart(Closeable closeable) {}
//
//                @Override
//                public void onNext(Statistics statistics) {
//                    Long memoryUsage = statistics.getMemoryStats().getUsage();
//                    if (memoryUsage != null)
//                        memory[0] = Math.max(memoryUsage / 1000L, memory[0]);
//                }
//
//                @Override public void onError(Throwable throwable) {}
//                @Override public void onComplete() {}
//                @Override public void close() throws IOException {}
//            });
//
//            // 执行代码
//            try {
//                StopWatch stopWatch = new StopWatch();
//                stopWatch.start();
//
//                dockerClient.execStartCmd(execId)
//                        .withDetach(false)
//                        .withTty(false)
//                        .withStdIn(inputStream)
//                        .exec(new ExecStartResultCallback() {
//                            @Override
//                            public void onNext(Frame frame) {
//                                StreamType streamType = frame.getStreamType();
//                                String payload = new String(frame.getPayload());
//                                if (StreamType.STDOUT.equals(streamType)) {
//                                    log.info("stdout: {}", payload);
//                                    msgBuilder.append(payload);
//                                }
//                                if (StreamType.STDERR.equals(streamType)) {
//                                    log.info("stderr: {}", payload);
//                                    errorMsgBuilder.append(payload);
//                                }
//                                super.onNext(frame);
//                            }
//                        })
//                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
//
//                stopWatch.stop();
//                time = stopWatch.getLastTaskTimeMillis();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            } finally {
//                statsCmd.close();
//            }
//
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            executeMessage.setTime(time);
//            executeMessage.setMemory(memory[0]);
//            executeMessage.setMessage(msgBuilder.toString());
//            executeMessage.setErrorMessage(errorMsgBuilder.toString());
//            executeMessageList.add(executeMessage);
//        }
//        return executeMessageList;
//    }
private List<ExecuteMessage> runInteractProcess(List<String> inputs, String userCodePath) {
    List<ExecuteMessage> executeMessageList = new ArrayList<>();
    String codeFileName = new File(userCodePath).getName();

    for (String input : inputs) {
        // 标准化输入格式：确保以换行符结尾
        String processedInput = input.trim();
        if (!processedInput.endsWith("\n")) {
            processedInput += "\n";
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(processedInput.getBytes());

        String[] cmd = new String[]{"python", "/app/" + codeFileName};
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmd)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(false)
                .exec();

        String execId = execCreateCmdResponse.getId();
        final Long[] memory = {0L};

        ExecuteMessage executeMessage = new ExecuteMessage();
        StringBuilder msgBuilder = new StringBuilder();
        StringBuilder errorMsgBuilder = new StringBuilder();

        // 监控内存使用
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        statsCmd.exec(new ResultCallback<Statistics>() {
            @Override public void onNext(Statistics statistics) {
                Long memoryUsage = statistics.getMemoryStats().getUsage();
                if (memoryUsage != null) {
                    memory[0] = Math.max(memoryUsage / 1000L, memory[0]);
                }
            }
            @Override public void onStart(Closeable closeable) {}
            @Override public void onError(Throwable throwable) {}
            @Override public void onComplete() {}
            @Override public void close() throws IOException {}
        });

        // 执行代码
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            dockerClient.execStartCmd(execId)
                    .withDetach(false)
                    .withTty(false)
                    .withStdIn(inputStream)
                    .exec(new ExecStartResultCallback() {
                        @Override
                        public void onNext(Frame frame) {
                            StreamType streamType = frame.getStreamType();
                            String payload = new String(frame.getPayload());
                            if (StreamType.STDOUT.equals(streamType)) {
                                msgBuilder.append(payload);
                            } else if (StreamType.STDERR.equals(streamType)) {
                                errorMsgBuilder.append(payload);
                            }
                        }
                    })
                    .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            errorMsgBuilder.append("执行超时或发生异常: ").append(e.getMessage());
        } finally {
            stopWatch.stop();
            statsCmd.close();
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("关闭输入流失败", e);
            }
        }

        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        executeMessage.setMemory(memory[0]);
        executeMessage.setMessage(msgBuilder.toString().trim());
        executeMessage.setErrorMessage(errorMsgBuilder.toString().trim());
        executeMessageList.add(executeMessage);
    }
    return executeMessageList;
}
    @Override
    public boolean closeResource(File userCodeFile, String userCodeParentPath) {
        if (dockerClient != null && containerId != null) {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("移除容器");
        }
        return super.closeResource(userCodeFile, userCodeParentPath);
    }
}