package com.zhisangui.zojcodesandbox.codesandbox;

import com.zhisangui.zojcodesandbox.model.ExecuteCodeRequest;
import com.zhisangui.zojcodesandbox.model.ExecuteCodeResponse;

import java.util.Arrays;

/**
 * C++ 原生代码沙箱实现（模板方法使用） 虚拟机上运行
 */
public class CppNativeCodeSandBox extends CppCodeSandBoxTemplate {

    public static void main(String[] args) {
        //输入的假数据,测试专用
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest
                .builder()
                .inputs(Arrays.asList("1 2","-10 -30"))
//                        Arrays.asList(
//                                "2 5 6 -1 5 4 -7 7 0 6 -1 1 -6 7 -5 ") )
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
//                                "\n" +
//                                "        cout << endl;\n" +
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
                                "    getline(cin, inputLine1);\n" +
                                "\n" +
                                "    // 转换并校验第一个输入\n" +
                                "    a = strtol(inputLine1.c_str(), &endptr, 10);\n" +
                                "    if (*endptr != '\\0' || inputLine1.empty()) {\n" +
                                "        cerr << \"错误：第一个输入不是有效整数！\" << endl;\n" +
                                "        return 1;\n" +
                                "    }\n" +
                                "\n" +
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
        //实例化对象
        CodeSandBox codeSandBox = new CppNativeCodeSandBox();
        //调用本地方法
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        //输出结果
        System.out.println(executeCodeResponse);
    }

    /**
     * 执行代码判题 C++
     * @param request
     * @return
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        System.out.println("C++ 原生代码沙箱");
        //子类继承父类,调用父类方法
        return super.executeCode(request);
    }
}

