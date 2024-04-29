package it.gov.pagopa.payment.notice.generator.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class WorkingDirectoryUtils {

    public static File createWorkingDirectory() throws IOException {
        File workingDirectory = new File("temp");
        if (!workingDirectory.exists()) {
            Files.createDirectory(workingDirectory.toPath());
        }
        return workingDirectory;
    }

}
