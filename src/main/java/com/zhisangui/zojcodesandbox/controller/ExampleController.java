package com.zhisangui.zojcodesandbox.controller;

import com.zhisangui.zojcodesandbox.codesandbox.CodeSandBox;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@CrossOrigin
@RestController("/")
public class ExampleController {
    @Resource
    private CodeSandBox javaDockerCodeSandBoxRunOnLinux;


    @GetMapping("/health")
    public String sayHello() {
        return "Hello World!";
    }

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        return javaDockerCodeSandBoxRunOnLinux.executeCode(executeCodeRequest);
    }
}
