package com.zhisangui.zojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException, IOException {
//        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
//        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
//                .dockerHost(config.getDockerHost())
//                .sslConfig(config.getSSLConfig())
//                .maxConnections(3000)
//                .build();
//        DockerClient dockerClient = DockerClientBuilder.getInstance().withDockerHttpClient(dockerHttpClient).build();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println(item.toString());
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//        System.out.println("pull end");
//
//        // 拉取镜像
//        Info exec = dockerClient.infoCmd().exec();
//        System.out.println(exec);

//         创建容器
//        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
//        CreateContainerResponse createContainerResponse = containerCmd.withCmd("echo", "Hello Docker").exec();
//        String containerId = createContainerResponse.getId();
//        System.out.println(containerId); // 2cdece20ae9ef96f85c7a06d1c8306b8cdeecc6aec95278a30525e18e26fc260
//        System.out.println("创建完成");
//         查看容器状态
//        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
//        List<Container> containerList =
//                listContainersCmd.withShowAll(true).exec();
//        containerList.forEach(System.out::println);

        // 启动容器
//        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
//        startContainerCmd.exec();
//        System.out.println("启动完成");

        // 查看日志
//        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId);
//        logContainerCmd
//                .withStdErr(true)
//                .withStdOut(true)
//                .exec(new LogContainerResultCallback() {
//                    @Override
//                    public void onNext(Frame item) {
//                        System.out.println(item.getStreamType());
//                        System.out.println("日志：" + new String(item.getPayload()));
//                        super.onNext(item);
//                    }
//            }).awaitCompletion();
//        System.out.println("日志打印完毕");

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd().withImageNameFilter(image);
        List<Image> images = listImagesCmd.exec();
        images.forEach(System.out::println);
        for (Image img : images) {
            // todo 看看怎么判断是否存在某个镜像
            if (img.getRepoDigests()[0].contains("openjdk"))
                System.out.println(img);
        }
    }
}
