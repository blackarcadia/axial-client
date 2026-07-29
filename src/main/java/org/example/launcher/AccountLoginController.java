package org.example.launcher;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AccountLoginController {
    public void start() {
        try {
            runLogin();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    ex.getMessage(),
                    "Login failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void runLogin() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        Path accountsDir = ClientPaths.accountsDir();
        Files.createDirectories(accountsDir);

        Path temp = accountsDir.resolve("temp.json");
        AuthManager auth = new AuthManager(temp);
        AuthResult result = auth.authenticate();
        Path target = accountsDir.resolve(result.playerName + ".json");
        Files.deleteIfExists(target);
        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.createDirectories(ClientPaths.activeAccountPointer().getParent());
        Files.writeString(ClientPaths.activeAccountPointer(), target.getFileName().toString());
    }
}
