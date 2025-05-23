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
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker 代码沙箱实现（模板方法模式）
 * todo ：该方法运行在本地，远程连接服务器的docker，不过暂未解决将win的代码同步到docker。
 */
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate {
    public static void main(String[] args) {
        CodeSandBox codeSandBox = new JavaDockerCodeSandBox();

        // 测试恶意代码
//        String userDir = System.getProperty("user.dir");
//        String unSafeFile = userDir + File.separator + "unsafe" + File.separator + CODE_NAME;
//        String code = FileUtil.readString(unSafeFile, StandardCharsets.UTF_8);

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code("import java.util.*;\n" +
                        "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        int a = Integer.parseInt(args[0]);\n" +
                        "        int b = Integer.parseInt(args[1]);\n" +
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

    private static final Long TIME_OUT = 5000L;
    private DockerClient dockerClient;
    private String containerId;


    @Override
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodeParentPath) {
        // 1. 拉取镜像,先判断是否存在
        DefaultDockerClientConfig defaultDockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://192.168.74.129:2375")
                .build();
        dockerClient = DockerClientBuilder.getInstance(defaultDockerClientConfig).build();
        String image = "openjdk:8-alpine";
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean isExist = false;
        for (Image img : images) {
            if (img.getRepoDigests()[0].contains("openjdk")) {
                isExist = true;
            }
        }
        System.out.println(dockerClient);
        // 1.1 不存在则拉取镜像
        if (!isExist) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
        }

        // 2. 创建容器(创建映射关系，将本地的用户代码映射到服务器上) （可以在容器进行限制操作，如访问网络等）
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        // todo：模拟已经将代码同步到远程
        userCodeParentPath = "/home/zsg/code";
        System.out.println(userCodeParentPath);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        hostConfig.withCpuCount(2L)
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

        // 4. 在容器中执行代码并获取结果
        // 创建需要在容器执行的命令 (容器内执行命令的方式 docker exec {container} {command})
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputs) {
            String[] s = input.split(" ");
            String[] cmd = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, s);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmd)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();
            final Long[] memory = {0L};

            ExecuteMessage executeMessage = new ExecuteMessage();
            long time = 0L;
            StringBuilder msgBuilder = new StringBuilder();
            StringBuilder errorMsgBuilder = new StringBuilder();

            // 执行查看容器状态的命令，查看占用内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onNext(Statistics statistics) {
                    Long memoryUsage = statistics.getMemoryStats().getUsage();
                    if (memoryUsage != null)
                        memory[0] = Math.max(memoryUsage, memory[0]);
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }

                @Override
                public void close() throws IOException {
                }
            });

            final boolean[] isFirst = {true};
            // 开始执行上面创建好的（需要在容器中执行的）命令，并获取结果
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                dockerClient.execStartCmd(execId).exec(new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        String payload = new String(frame.getPayload());
                        if (!isFirst[0])
                            msgBuilder.append("\n");
                        if (StreamType.STDOUT.equals(streamType)) {
                            msgBuilder.append(payload);
                        }
                        if (StreamType.STDERR.equals(streamType))
                            errorMsgBuilder.append(payload);
                        super.onNext(frame);
                        isFirst[0] = false;
                    }
                }).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
//                return getErrorResponse(e);
                throw new RuntimeException(e);
            }
            executeMessage.setTime(time);
            executeMessage.setMemory(memory[0]);
            executeMessage.setMessage(msgBuilder.toString());
            executeMessage.setErrorMessage(errorMsgBuilder.toString());
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }

    @Override
    public ExecuteCodeResponse processResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
        long time = 0L;
        long memory = 0L;
        List<String> outputs = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            // 返回值有不正常的
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setStatus(1);
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                break;
            }
            time = Math.max(time, executeMessage.getTime());
            memory = Math.max(memory, executeMessage.getMemory());
            outputs.add(executeMessage.getMessage());
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(time);
        judgeInfo.setMemory(memory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        executeCodeResponse.setOutputs(outputs);
        return executeCodeResponse;
    }

    @Override
    public boolean closeResource(File userCodeFile, String userCodeParentPath) {
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        return super.closeResource(userCodeFile, userCodeParentPath);
    }

}