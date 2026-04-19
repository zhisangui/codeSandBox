package com.zhisangui.zojcodesandbox.codesandbox;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CppDockerCodeSandBoxRunOnLinux extends CppCodeSandBoxTemplate {

    public static void main(String[] args) {
        //测试
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .inputs(
//                        Arrays.asList(
//                                "2\n5\n6 -1 5 4 -7\n7\n0 6 -1 1 -6 7 -5\n")
                        Arrays.asList("10 30","20 40")
                )
                .code(
//                        "#include <iostream>\n" +
//                                "#include <vector>\n" +
//                                "using namespace std;\n" +
//                                "\n" +
//                                "int main() {\n" +
//                                "    int t;\n" +
//                                "    cin >> t;\n" +
//                                "\n" +
//                                "    for (int testCase = 1; testCase <= t; testCase++) {\n" +
//                                "        int n;\n" +
//                                "        cin >> n;\n" +
//                                "\n" +
//                                "        vector<int> a(n + 1);\n" +
//                                "\n" +
//                                "        for (int i = 1; i <= n; i++) {\n" +
//                                "            cin >> a[i];\n" +
//                                "        }\n" +
//                                "\n" +
//                                "        int maxSum = a[1];\n" +
//                                "        int left = 1, right = 1;\n" +
//                                "        int tempStart = 1;\n" +
//                                "\n" +
//                                "        for (int i = 2; i <= n; i++) {\n" +
//                                "            if (a[i-1] < 0) {\n" +
//                                "                tempStart = i;\n" +
//                                "            } else {\n" +
//                                "                a[i] += a[i-1];\n" +
//                                "            }\n" +
//                                "\n" +
//                                "            if (a[i] > maxSum) {\n" +
//                                "                maxSum = a[i];\n" +
//                                "                left = tempStart;\n" +
//                                "                right = i;\n" +
//                                "            }\n" +
//                                "        }\n" +
//                                "\n" +
//                                "        cout << \"Case \" << testCase << \":\\n\";\n" +
//                                "        cout << maxSum << \" \" << left << \" \" << right;\n" +
//                                "        cout << endl;\n" +
//                                "\n" +
//                                "    }\n" +
//                                "\n" +
//                                "    return 0;\n" +
//                                "}\n")
                        "#include <iostream>\n" +
                                "#include <string>\n" +
                                "#include <cstdlib>\n" +        // 用于 strtol 错误检查\n" +
                                "#include <limits>\n" +         // 用于清除输入缓冲区\n" +
                                "\n" +
                                "using namespace std;\n" +
                                "\n" +
                                "int main() {\n" +
                                "    string inputLine1, inputLine2;\n" +
                                "    long a, b;\n" +
                                "    char* endptr;\n" +
                                "\n" +
                                "    // 读取第一个输入\n" +
                                "    cout << \"请输入第一个整数: \";\n" +
                                "    getline(cin, inputLine1);\n" +
                                "\n" +
                                "    // 转换并校验第一个输入\n" +
                                "    a = strtol(inputLine1.c_str(), &endptr, 10);\n" +
                                "    if (*endptr != '\\0' || inputLine1.empty()) {\n" +
                                "        cerr << \"错误：第一个输入不是有效整数！\" << endl;\n" +
                                "        return 1;\n" +
                                "    }\n" +
                                "\n" +
                                "    // 读取第二个输入\n" +
                                "    cout << \"请输入第二个整数: \";\n" +
                                "    getline(cin, inputLine2);\n" +
                                "\n" +
                                "    // 转换并校验第二个输入\n" +
                                "    b = strtol(inputLine2.c_str(), &endptr, 10);\n" +
                                "    if (*endptr != '\\0' || inputLine2.empty()) {\n" +
                                "        cerr << \"错误：第二个输入不是有效整数！\" << endl;\n" +
                                "        return 1;\n" +
                                "    }\n" +
                                "\n" +
                                "    // 计算结果\n" +
                                "    long result = a + b;\n" +
                                "    cout << \"结果：\" << result << endl;\n" +
                                "\n" +
                                "    return 0;\n" +
                                "}\n")
                .language("c++")
                .build();
        CodeSandBox codeSandBox = new CppDockerCodeSandBoxRunOnLinux();
        log.info("开始执行判题 {}");
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        log.info("结束执行判题");
        System.out.println(executeCodeResponse);
    }

    private static final Long TIME_OUT = 5000L;
    private DockerClient dockerClient;
    private String containerId;

    @Override
    public List<ExecuteMessage> executeCode(List<String> inputs, String userCodeParentPath)  {
        //拉取镜像
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(3000)
                .sslConfig(config.getSSLConfig())
                .build();
        dockerClient = DockerClientBuilder.getInstance().withDockerHttpClient(dockerHttpClient).build();

        String image = "gcc:latest";    //gcc镜像

        boolean isExist = true;

        List<Image> imageList = dockerClient.listImagesCmd().exec();

        /**
         for(Image image_ : imageList){
         //去判断docker中是否存在该镜像
         if(image_.getRepoDigests()[0].contains("gcc")){
         isExist = true;
         }
         }
         */

        if(!isExist){
            //拉取cpp镜像
            log.info("pull image start");
            //镜像不存在,拉取镜像
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            //获取回调
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
                @Override
                public void onNext(PullResponseItem item) {
                    log.info("执行回调: {}", item);
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            pullImageCmd.exec(pullImageResultCallback);
            log.info("pull image end");
        }

        //2. 创建容器(创建映射关系，将本地的用户代码映射到容器上) （使用 HostConfig 在容器内进行限制操作，如访问网络，cpu使用等）
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image)
                .withName("codeSandBox");
        HostConfig hostConfig = new HostConfig()
                .withBinds(new Bind(userCodeParentPath,new Volume("/app")))
                .withCpuCount(2L)
                .withMemorySwap(0L)
                .withMemory(256 * 1000 * 1000L);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(false)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        containerId = createContainerResponse.getId();

        //3.启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 4. 在容器中执行代码并获取结果
        return this.runInteractProcess(inputs);
    }

    /**
     * 通过流的方式将输入传给程序
     *
     * @param inputs
     * @return
     */
    private List<ExecuteMessage> runInteractProcess(List<String> inputs) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputs) {
            String[] s = input.split(" ");
            input = String.join("\n", s) + "\n";
            // 修改后的命令数组：先编译C++代码，再运行可执行文件
//            String runCommand = "g++ -o /app/Main /app/Main.cpp -std=c++11 && ./app/Main ";
            String runCommand = "./app/Main ";

            String[] cmd = new String[]{"/bin/sh", "-c", runCommand};

            ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());

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
//                                    System.out.println("stdout: " + payload);
                                    log.info("stdout: {}", payload);
                                    msgBuilder.append(payload);
                                }
                                if (StreamType.STDERR.equals(streamType)) {
                                    log.info("stderr: {}", payload);
//                                    System.out.println("stderr: " + payload);
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
        log.info("移除容器");
        return super.closeResource(userCodeFile, userCodeParentPath);
    }
}
