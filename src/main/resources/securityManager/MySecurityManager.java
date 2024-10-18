import java.security.Permission;

public class MySecurityManager extends SecurityManager {

    @Override
    public void checkRead(String file) {
        if (file.endsWith(".conf")) {
            throw new SecurityException(".conf ");
        }
        super.checkRead(file);
    }
}

