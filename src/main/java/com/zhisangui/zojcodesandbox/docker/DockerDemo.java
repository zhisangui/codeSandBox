package com.zhisangui.zojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.Arrays;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        String image = "nginx:latest";

        // 拉取镜像
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像：" + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//        System.out.println("下载完成");

//         创建容器
//        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
//        CreateContainerResponse createContainerResponse = containerCmd.withCmd("echo", "Hello Docker").exec();
//        String containerId = createContainerResponse.getId();
//        System.out.println(containerId); // 2cdece20ae9ef96f85c7a06d1c8306b8cdeecc6aec95278a30525e18e26fc260
//        System.out.println("创建完成");
        String containerId = "2cdece20ae9ef96f85c7a06d1c8306b8cdeecc6aec95278a30525e18e26fc260";
        // 查看容器状态
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

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        List<Image> images = listImagesCmd.exec();
        System.out.println(images.size());
        for (Image img : images) {
            // todo 看看怎么判断是否存在某个镜像
            if (img.getRepoDigests()[0].contains("openjdk"))
                System.out.println(img.getRepoDigests()[0]);
        }
    }
}
