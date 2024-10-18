package com.zhisangui.zojcodesandbox.securityManager;

public class MySecurityManager extends SecurityManager {

    @Override
    public void checkRead(String file) {
        // 自定义逻辑：阻止读取以 ".conf" 结尾的文件
        if (file.endsWith(".conf")) {
            throw new SecurityException("禁止读取 .conf 文件！");
        }
        // 调用父类方法，执行其他默认检查
        super.checkRead(file);
    }
}
