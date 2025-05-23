package com.zhisangui.zojcodesandbox.model;

import lombok.Data;

@Data
public class JudgeInfo {
    /**
     * 程序结果信息(在代码沙箱中该变量未使用，在 judge 模块用来存放代码结果，如AC、WA、TLE）
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
