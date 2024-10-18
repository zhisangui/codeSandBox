//// 1. 长时间占用资源--使用超时中断
//public class Main {
//
//    public static void main(String[] args) throws InterruptedException {
//        long ONE_HOUR = 60 * 60 * 1000L;
//        Thread.sleep(ONE_HOUR);
//        System.out.println("睡完了");
//    }
//}


//// 2. 占用系统内存
//import java.util.ArrayList;
//import java.util.List;
//
//public class Main {
//
//    public static void main(String[] args) throws InterruptedException {
//        List<byte[]> bytes = new ArrayList<>();
//        while (true) {
//            bytes.add(new byte[10000]);
//        }
//    }
//}


// 3. 读取服务器文件（文件信息泄露）
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
//public class Main {
//
//    public static void main(String[] args) throws InterruptedException, IOException {
//        String userDir = System.getProperty("user.dir");
//        String filePath = userDir + File.separator + "src/main/resources/application.yml";
//        List<String> allLines = Files.readAllLines(Paths.get(filePath));
//        System.out.println(String.join("\n", allLines));
//    }
//}

//// 4. 向服务器写文件
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.Arrays;
//
//public class Main {
//
//    public static void main(String[] args) throws InterruptedException, IOException {
//        String userDir = System.getProperty("user.dir");
//        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
//        String errorProgram = "java -version 2>&1";
//        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
//        System.out.println("写木马成功，你完了哈哈");
//    }
//}


// 5. 执行服务器的程序（甚至木马）

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();
        // 分批获取进程的正常输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // 逐行读取
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(compileOutputLine);
        }
        System.out.println("执行异常程序成功");
    }
}
