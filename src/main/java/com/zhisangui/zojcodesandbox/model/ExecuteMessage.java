package com.zhisangui.zojcodesandbox.model;

import lombok.Data;

/**
 * 执行信息，用来传递 java 执行命令产生的信息
 */
@Data
public class ExecuteMessage {
    /**
     * 退出状态值（正常是0）
     */
    private Integer exitValue;

    /**
     * 正常信息
     */
    private String message;

    /**
     * 报错信息
     */
    private String errorMessage;

    /**
     * 代码运行时间
     */
    private Long time;

    /**
     * 代码占用内存
     */
    private Long memory;
}
