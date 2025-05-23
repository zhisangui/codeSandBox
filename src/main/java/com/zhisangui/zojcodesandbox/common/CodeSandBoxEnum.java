package com.zhisangui.zojcodesandbox.common;

public enum CodeSandBoxEnum {
    INTERFACE_NORMAL("接口正常", 0),
    INTERFACE_ERROR("接口异常", 1);


    private String text;
    private Integer value;
    CodeSandBoxEnum (String text, Integer value) {
        this.text = text;
        this.value = value;
    }
    public String getText() {
        return text;
    }
    public Integer getValue() {
        return value;
    }
}
