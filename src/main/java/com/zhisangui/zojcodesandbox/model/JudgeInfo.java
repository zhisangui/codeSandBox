package com.zhisangui.zojcodesandbox.model;

import lombok.Data;

@Data
public class JudgeInfo {
    /**
     * 程序结果信息(todo,该变量还没有用）
     */
    private String message;

    /**
     * 程序耗时(返回用例中最大耗时)
     */
    private Long time;

    /**
     * 占用内存(返回用例中最大占用内存)
     */
    private Long memory;
}
