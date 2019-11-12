package application.rest;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Random;

public class StressTest {
    public static class StressTestingProperties {
        public int cpuCycles = 10000000;
        public String downloadUrl = "https://testtf-bravo.s3.amazonaws.com/crictl-v1.11.1-linux-amd64.tar.gz";
        public String writeDummyLocation = System.getProperty("user.dir");
        public String writePath = System.getProperty("user.dir");
        public int writeCycles = 1000000;

        public StressTestingProperties() {
            String env_cpuCycles = System.getenv("STRESSTEST_CPU_CYCLES");
            if (env_cpuCycles != null) {
                cpuCycles = Integer.parseInt(env_cpuCycles);
            }
            String env_downloadUrl = System.getenv("STRESSTEST_NETWORK_URL");
            if (env_downloadUrl != null) {
                downloadUrl = env_downloadUrl;
            }
            String env_writePath = System.getenv("STRESSTEST_DISK_PATH");
            if (env_writePath != null) {
                writePath = env_writePath;
            }
            String env_writeDummyLocation = System.getenv("STRESSTEST_DISK_DUMMY_PATH");
            if (env_writeDummyLocation != null) {
                writeDummyLocation = env_writeDummyLocation;
            }
            String env_writeCycles = System.getenv("STRESSTEST_DISK_CYCLES");
            if (env_writeCycles != null) {
                writeCycles = Integer.parseInt(env_writeCycles);
            }
        }
    }

    public void loadCPU(StressTestingProperties config) {
        for (int i = 0; i < config.cpuCycles; i++) {
            Math.atan(Math.sqrt(Math.pow(1, 10)));
        }
    }


    public void loadDisk(StressTestingProperties config, String postfix) {
        java.nio.file.Path testWriteFile = new File(config.writePath, "tmp.diskload" + postfix).toPath();
        File file = new File(config.writeDummyLocation, "dummyDiskIO");
        if (!file.exists()) {
            System.err.println("Dummy file for disk IO check does not exist");
        }
        try {
            Files.createDirectories(testWriteFile.getParent());
            int i = 0;
            FileOutputStream fileOutputStream = new FileOutputStream(String.valueOf(testWriteFile));
            while (i < config.writeCycles) {
                byte[] data = new byte[(int) file.length()];
                fileOutputStream.write(data);
                i++;
            }
            fileOutputStream.close();
        } catch (IOException e) {
            System.err.println("Can`t write to file: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(testWriteFile);
            } catch (IOException e) {
                System.err.println("Can`t delete file: " + e.getMessage());
            }
        }
    }

    public void loadNetwork(StressTestingProperties config) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new URL(config.downloadUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream("/dev/null/")) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("Can`t download file :" +
                    e.getMessage());
        }
    }

    public StressTest() {
//    generate file for disk io tests
        java.nio.file.Path dummy = new File(new StressTestingProperties().writeDummyLocation , "dummyDiskIO").toPath();
        if ( dummy.toFile().exists() ) {
            try {
                Files.delete(dummy);
            } catch (IOException e) {
                System.err.println("Failed to delete previous dummy file: " + e.getMessage());
                System.exit(1);
            }
            System.err.println("Dummy file already existed , but I`ve deleted it. Let us hope it wasn`t something usefull .");
        }
        try {
            Random random = new Random();
            FileOutputStream fileOutputStream = new FileOutputStream( dummy.toFile() );
            for (int i = 0 ; i < 1 ; i++ ) {
                byte[] rba  = new byte[1024] ;
                random.nextBytes(rba);
                fileOutputStream.write(rba);
            }
            fileOutputStream.close();
        } catch (IOException e) {
            System.err.println(" Can`t create dummy file for disk io at  path :" + dummy);
        }
    }

}