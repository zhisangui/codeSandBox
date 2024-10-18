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
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker 代码沙箱实现（模板方法模式）,该方法运行在服务器上，即连接本地docker
 */
@Component
public class JavaDockerCodeSandBoxRunOnLinux extends JavaCodeSandBoxTemplate {
    public static void main(String[] args) {
        CodeSandBox codeSandBox = new JavaDockerCodeSandBoxRunOnLinux();

        // 测试恶意代码
//        String userDir = System.getProperty("user.dir");
//        String unSafeFile = userDir + File.separator + "unsafe" + File.separator + CODE_NAME;
//        String code = FileUtil.readString(unSafeFile, StandardCharsets.UTF_8);

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code("import java.util.Scanner;\n" +
                        "import java.util.Arrays;\n" +
                        "\n" +
                        "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        Scanner scanner = new Scanner(System.in);\n" +
                        "        \n" +
                        "        int t = scanner.nextInt();  // 读取测试用例数量\n" +
                        "        \n" +
                        "        for (int testCase = 1; testCase <= t; testCase++) {\n" +
                        "            int n = scanner.nextInt();  // 读取数组的大小\n" +
                        "            int[] a = new int[n + 1];   // 数组下标从1开始\n" +
                        "            \n" +
                        "            // 读取数组元素\n" +
                        "            for (int i = 1; i <= n; i++) {\n" +
                        "                a[i] = scanner.nextInt();\n" +
                        "            }\n" +
                        "            \n" +
                        "            // 动态规划求解最大子段和问题\n" +
                        "            int maxSum = a[1];  // 初始最大和为第一个元素\n" +
                        "            int currentStart = 1, left = 1, right = 1;\n" +
                        "            int tempStart = 1;  // 临时起始下标\n" +
                        "\n" +
                        "            for (int i = 2; i <= n; i++) {\n" +
                        "                if (a[i - 1] < 0) {\n" +
                        "                    tempStart = i;  // 如果前面的和为负数，重置起始下标\n" +
                        "                } else {\n" +
                        "                    a[i] += a[i - 1];  // 累加前面的和\n" +
                        "                }\n" +
                        "\n" +
                        "                // 更新最大和及左右边界\n" +
                        "                if (a[i] > maxSum) {\n" +
                        "                    maxSum = a[i];\n" +
                        "                    left = tempStart;\n" +
                        "                    right = i;\n" +
                        "                }\n" +
                        "            }\n" +
                        "            \n" +
                        "            // 输出结果\n" +
                        "            System.out.println(\"Case \" + testCase + \":\");\n" +
                        "            System.out.println(maxSum + \" \" + left + \" \" + right);\n" +
                        "            if (testCase < t) {\n" +
                        "                System.out.println();  // 多个测试用例之间空行\n" +
                        "            }\n" +
                        "        }\n" +
                        "\n" +
                        "        scanner.close();\n" +
                        "    }\n" +
                        "}\n")
//                .code("import java.util.*;\n" +
//                        "public class Main {\n" +
//                        "    public static void main(String[] args) {\n" +
//                        "        int a = Integer.parseInt(args[0]);\n" +
//                        "        int b = Integer.parseInt(args[1]);\n" +
//                        "        System.out.println(\"结果为：\" + (a + b));\n" +
//                        "    }\n" +
//                        "}")
//                .language("java")
//                .inputs(Arrays.asList("111232 1123213", "3312 4231"))
                .inputs(Arrays.asList("2\n5\n6 -1 5 4 -7\n7\n0 6 -1 1 -6 7 -5\n"))
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
//        String output = executeCodeResponse.getOutputs().get(0);
//        output = output.replaceAll("\\n$", "");
//
//        String ans = "Case 1:\n14 1 4\n\nCase 2:\n7 1 6";
//        System.out.println("output: ");
//        System.out.println(output);
//
//        System.out.println("ans: ");
//        System.out.println(ans);
//        if (ans.equals(output))
//            System.out.println("equals");
        System.out.println(executeCodeResponse);
    }

    private static final Long TIME_OUT = 5000L;
    private DockerClient dockerClient;
    private String containerId;

    @Override
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodeParentPath) {
        // 1. 拉取镜像,先判断是否存在
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(3000)
                .build();
        dockerClient = DockerClientBuilder.getInstance().withDockerHttpClient(dockerHttpClient).build();
        String image = "openjdk:8-alpine";
        List<Image> images = dockerClient.listImagesCmd().exec();
        boolean isExist = false;
        for (Image img : images) {
            if (img.getRepoDigests()[0].contains("openjdk")) {
                isExist = true;
            }
        }
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

//        return this.runProcessByArgs(inputs);
        return this.runInteractProcess(inputs);
    }

    /**
     * 通过流的方式将输入传给程序
     * @param inputs
     * @return
     */
    private List<ExecuteMessage> runInteractProcess(List<String> inputs) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputs) {
            String[] s = input.split(" ");
            input = String.join("\n", s) + "\n";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
            String[] cmd = new String[]{"java", "-cp", "/app", "Main"};
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
                        memory[0] = Math.max(memoryUsage / 1000L, memory[0]);
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
                dockerClient.execStartCmd(execId)
                        .withDetach(false)
                        .withTty(false)
                        .withStdIn(inputStream)
                        .exec(new ExecStartResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                StreamType streamType = frame.getStreamType();
                                String payload = new String(frame.getPayload());
//                                if (!isFirst[0] && !payload.equals("\n"))
//                                    msgBuilder.append("\n");
//                                msgBuilder.append(payload);
//                                System.out.println("payload: " + payload);
                                if (StreamType.STDOUT.equals(streamType)) {
                                    System.out.println("stdout: " + payload);
                                    msgBuilder.append(payload);
                                }
                                if (StreamType.STDERR.equals(streamType)) {
                                    System.out.println("stderr: " + payload);
                                    errorMsgBuilder.append(payload);
                                }
                                super.onNext(frame);
                                isFirst[0] = false;
                            }
                        })
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
            } catch (Exception e) {
//                return getErrorResponse(e);
                throw new RuntimeException(e);
            } finally {
                statsCmd.close();
            }
            try {
                inputStream.close();
            } catch (IOException e) {
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

    /**
     * 通过main方法的参数来获取程序输入
     * @param inputs 程序输入
     * @return
     */
    private List<ExecuteMessage> runProcessByArgs(List<String> inputs) {
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
                        memory[0] = Math.max(memoryUsage / 1000L, memory[0]);
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
