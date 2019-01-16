import ammonite.ops.BasePath;
import ammonite.ops.PathConvertible;
import ammonite.ops.PathConvertible$;
import sjsonnet.SjsonnetMain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws IOException {
        String json = "{ \"user_id\": 7 }";
        String jsonnet = "{ \"uid\": payload.user_id }";
        String result = transform(json, jsonnet);
        System.out.println(result);
    }

    private static String transform(String json, String jsonnet) throws IOException {
        Path tempDirectory = getWorkingDirectory();

        Path libraryFile = getLibraryFile(tempDirectory);
        String libraryPrefix = "local portx = import '" + libraryFile.getFileName() + "';";

        Path payloadFile = getPayloadFile(tempDirectory, json);
        String payloadPrefix = "local payload = import '" + payloadFile.getFileName() + "';";

        String script = libraryPrefix + "\n" + payloadPrefix + "\n" + jsonnet;

        Path scriptFile = getScriptFile(tempDirectory, script);

        Path outputFile = getOutputFile(tempDirectory);

        sjsonnet.SjsonnetMain.main0(
                new String[]{ "-o", outputFile.getFileName().toString(), scriptFile.getFileName().toString() },
                SjsonnetMain.createParseCache(),
                new ByteArrayInputStream(new byte[0]),
                System.out,  // I'm leaving this as stdout and stderr, but they can be easily replaced
                System.err,
                ammonite.ops.Path.apply(tempDirectory, PathConvertible.NioPathConvertible$.MODULE$)
        );

        return new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8);

    }

    private static Path getScriptFile(Path tempDirectory, String script) throws IOException {
        Path scriptFile = Files.createTempFile(tempDirectory, "script", ".jsonnet");
        scriptFile.toFile().deleteOnExit();

        Files.write(scriptFile, script.getBytes(StandardCharsets.UTF_8));
        return scriptFile;
    }

    private static Path getOutputFile(Path tempDirectory) throws IOException {
        Path outputFile = Files.createTempFile(tempDirectory, "output", ".json");
        outputFile.toFile().deleteOnExit();

        return outputFile;
    }

    private static Path getPayloadFile(Path tempDirectory, String json) throws IOException {
        Path payloadFile = Files.createTempFile(tempDirectory, "payload", ".json");
        payloadFile.toFile().deleteOnExit();

        Files.write(payloadFile, json.getBytes(StandardCharsets.UTF_8));
        return payloadFile;
    }

    private static Path getLibraryFile(Path tempDirectory) throws IOException {
        Path libraryFile = tempDirectory.resolve("portx.libsonnet");
        libraryFile.toFile().deleteOnExit();

        Files.copy(Example.class.getResourceAsStream("/portx.libsonnet"), libraryFile);
        return libraryFile;
    }

    private static Path getWorkingDirectory() throws IOException {
        Path tempDirectory  = Files.createTempDirectory("example");
        tempDirectory.toFile().deleteOnExit();
        return tempDirectory;
    }
}
