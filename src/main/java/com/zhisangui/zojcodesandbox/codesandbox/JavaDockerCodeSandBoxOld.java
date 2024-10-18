package com.zhisangui.zojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import com.zhisangui.zojcodesandbox.model.ExecuteMessage;
import com.zhisangui.zojcodesandbox.model.JudgeInfo;
import com.zhisangui.zojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Deprecated
public class JavaDockerCodeSandBoxOld implements CodeSandBox{

    public static final String USER_CODE_DIR = "userCode";
    public static final String CODE_NAME = "Main.java";
    public static final Long TIME_OUT = 5000L;

    public static void main(String[] args) {
        CodeSandBox codeSandBox = new JavaDockerCodeSandBoxOld();

        // 测试恶意代码
//        String userDir = System.getProperty("user.dir");
//        String unSafeFile = userDir + File.separator + "unsafe" + File.separator + CODE_NAME;
//        String code = FileUtil.readString(unSafeFile, StandardCharsets.UTF_8);

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(  "import java.util.*;\n" +
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

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(0);
        // 1. 获取用户的语言、代码，并将用户的代码写入文件
        // todo：目前只针对 java，后期可以拓展其他语言
        String code = request.getCode();
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

        // 3. 拉取镜像,先判断是否存在
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean isExist = false;
        for (Image img : images) {
            if (img.getRepoDigests()[0].contains("openjdk")) {
                isExist = true;
            }
        }
        // 不存在则拉取镜像
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
                return getErrorResponse(e);
            }
            System.out.println("下载完成");
        }

        // 4. 创建容器(创建映射关系，将本地的用户代码映射到服务器上) （可以在容器进行限制操作，如访问网络等）
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        hostConfig.withCpuCount(2L)
                .withMemorySwap(0L)
                .withMemory(256 * 1000 * 1000L);
        // todo: seccomp了解
        // profile.json文件： {
                    //  "defaultAction": "SCMP_ACT_ALLOW",
                    //  "syscalls": [
                    //    {
                    //      "name": "write",
                    //      "action": "SCMP_ACT_ALLOW"
                    //    },
                    //    {
                    //      "name": "read",
                    //      "action": "SCMP_ACT_ALLOW"
                    //    }
                    //  ]
                    //}
        // String profileConfig = ResourceUtil.readUtf8Str("profile.json");
        // hostConfig.withSecurityOpts(Arrays.asList("seccomp=" + profileConfig));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(false)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        String containerId = createContainerResponse.getId();

        // 5. 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 6. 在容器中执行代码并获取结果
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
                public void onStart(Closeable closeable) {}

                @Override
                public void onNext(Statistics statistics) {
                    Long memoryUsage = statistics.getMemoryStats().getUsage();
                    if (memoryUsage != null)
                        memory[0] = Math.max(memoryUsage, memory[0]);
                }
                @Override
                public void onError(Throwable throwable) {}
                @Override
                public void onComplete() {}
                @Override
                public void close() throws IOException {}
            });

            // 开始执行上面创建好的（需要在容器中执行的）命令，并获取结果
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                dockerClient.execStartCmd(execId).exec(new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        String payload = new String(frame.getPayload());
                        if (StreamType.STDOUT.equals(streamType))
                            msgBuilder.append(payload);
                        if (StreamType.STDERR.equals(streamType))
                            errorMsgBuilder.append(payload);
                        super.onNext(frame);
                    }
                }).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                return getErrorResponse(e);
            }
            executeMessage.setTime(time);
            executeMessage.setMemory(memory[0]);
            executeMessage.setMessage(msgBuilder.toString());
            executeMessage.setErrorMessage(errorMsgBuilder.toString());
            executeMessageList.add(executeMessage);
        }

        // 7. 封装返回值 executeCodeResponse
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
        // 8. 关闭资源
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        System.out.println("删除容器");
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
