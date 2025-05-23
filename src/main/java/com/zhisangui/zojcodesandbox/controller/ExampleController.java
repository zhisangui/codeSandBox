package com.zhisangui.zojcodesandbox.controller;

import com.zhisangui.zojcodesandbox.codesandbox.CodeSandBox;
import com.zhisangui.zojcodesandbox.codesandbox.CodeSandBoxFactory;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@CrossOrigin
@RestController
@RequestMapping("/")
public class ExampleController {
    @Autowired
    private CodeSandBoxFactory codeSandBoxFactory;


    @GetMapping("/health")
    public String sayHello() {
        return "Hello World!";
    }

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        return codeSandBoxFactory.getCodeSandBox(executeCodeRequest.getLanguage()).executeCode(executeCodeRequest);
    }
}
