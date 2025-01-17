package org.kendar.globaltest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.System.exit;
import static org.kendar.globaltest.LocalFileUtils.pathOf;

public class Main {
    private static final Function<String, Boolean> findHamProcesses = (psLine) ->
            psLine.contains("java") &&
                    psLine.contains("httpanswering") &&
                    !psLine.contains("globaltest");
    private static Map<String, String> env;
    private static ProcessUtils _processUtils;

    private static void killApacheLogger() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "error");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "error");
        System.setProperty("log4j.logger.org.apache.http", "error");
        System.setProperty("log4j.logger.org.apache.http.wire", "error");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "error");
    }

    private static void doExit(int i) {
        try {
            if (i == 0) {
                _processUtils.killProcesses(findHamProcesses);
            } else {
                var scanner = new Scanner(System.in); // create scanner
                LogWriter.warn("");
                LogWriter.warn("there was an error");       // prompt u
                LogWriter.warn("Would you like to leave the processes running? (y/n default)");       // prompt user
                var result = scanner.next().toLowerCase(Locale.ROOT);
                if (result.isEmpty() || result.equalsIgnoreCase("n")) {
                    _processUtils.killProcesses(findHamProcesses);
                }
                exit(i);
            }
        } catch (Exception ex) {
            exit(1);
        }
    }


    private static void runBuild(String buildDir, String releasePath, String script) throws Exception {
        LogWriter.info("BEG " + script);
        var runDir = buildDir;
        var runFile = pathOf(runDir, script + LocalFileUtils.execScriptExt());
        var logDir = releasePath;

        new ProcessRunner(env).withCommand(runFile).withStartingPath(runDir).run();

        for (var file : new File(logDir).listFiles(((dir, name) -> name.toLowerCase().endsWith(".log")))) {
            try (var reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                var hasError = false;
                while (line != null) {
                    if (line.toLowerCase(Locale.ROOT).contains("error")) {
                        LogWriter.errror("[%s] %s", script, line);
                        hasError = true;
                    }
                    // read next line
                    line = reader.readLine();
                }
                if (hasError) {
                    doExit(1);
                }

            }
        }


        LogWriter.info("END " + script);
    }


    private static void cleanDirectory(String path) throws Exception {
        File directory = new File(path);
        if (directory.exists()) {
            FileUtils.cleanDirectory(directory);
        }
    }


    private static void testCalendarSample(String calendarPath) throws Exception {
        LogWriter.info("Testing calendar/scripts/be");
        start(pathOf(calendarPath, "scripts"), "be").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1:8100/api/v1/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Testing calendar/scripts/bemongo");
        start(pathOf(calendarPath, "scripts"), "bemongo").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1:8100/api/v1/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Testing calendar/scripts/fe");
        start(pathOf(calendarPath, "scripts"), "fe").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1:8080/api/v1/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Testing calendar/scripts/gateway");
        start(pathOf(calendarPath, "scripts"), "gateway").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1:8090/api/v1/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Testing calendar/scripts/ham");
        start(pathOf(calendarPath, "scripts"), "ham").runBackground();
        HttpChecker.checkForSite(60, "http://www.local.test/api/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);


        LogWriter.info("Testing calendar/rundb");
        start(calendarPath, "rundb").runBackground();
        HttpChecker.checkForSite(60, "http://localhost:8082").onError(() -> doExit(1)).run();
        LogWriter.info("Testing calendar/scripts/bedb");
        start(pathOf(calendarPath, "scripts"), "bedb").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1:8100/api/v1/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);
    }

    private static void testLocalHam(String releasePath) throws Exception {
        LogWriter.info("Testing ham/local.run");
        start(pathOf(releasePath, "ham"), "local.run").runBackground();
        HttpChecker.checkForSite(60, "http://127.0.0.1/api/health").onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Testing ham/proxy.run");
        start(pathOf(releasePath, "ham"), "proxy.run").runBackground();
        HttpChecker.checkForSite(60, "http://www.local.test/api/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);
    }

    private static void applyReleasePermissions(String releasePath) throws Exception {
        _processUtils.chmodExec(pathOf(releasePath, "ham"), "sh");
        _processUtils.chmodExec(pathOf(releasePath, "simpledns"), "sh");
        _processUtils.chmodExec(pathOf(releasePath, "calendar"), "sh");
        _processUtils.chmodExec(pathOf(releasePath, "calendar", "scripts"), "sh");
    }

    private static void buildDeploymentArtifacts(String startingPath, String hamVersion, String buildDir, String releasePath) throws Exception {
        cleanDirectory(releasePath);

        runBuild(buildDir, releasePath, "build_release");
        TarUtils.extract(pathOf(startingPath, "release"), "ham-" + hamVersion, () -> doExit(1));
        runBuild(buildDir, releasePath, "build_release_samples");
        TarUtils.extract(pathOf(startingPath, "release"), "ham-samples-" + hamVersion, () -> doExit(1));
    }


    private static void handleDockerErrors(String a, Process p) {
        if (a.toLowerCase(Locale.ROOT).startsWith("error") || a.toLowerCase(Locale.ROOT).startsWith("couldn't connect")) {
            LogWriter.errror("");
            LogWriter.errror(a);
            p.destroy();
            doExit(1);
        }
    }


    private static void handleDockTagged(String s, Process process) {
        if (s.toLowerCase(Locale.ROOT).startsWith("successfully tagged")) {
            LogWriter.info("" + s);
        }
    }


    private static void buildDockerImages(String buildDir) throws Exception {
        LogWriter.info("Building docker");
        start(buildDir, "build_docker", Main::handleDockerErrors, Main::handleDockTagged).run();
        LogWriter.info("Building docker samples");
        start(buildDir, "build_docker_samples", Main::handleDockerErrors, Main::handleDockTagged).run();
    }

    private static void testDockerCalendarAndQuotesSamples(String dockerIp, String samplesDir) throws Exception {
        LogWriter.info("Starting composer calendar");
        startComposer(pathOf(samplesDir, "calendar", "hub_composer"), "docker-compose-local.yml", "down").runBackground();
        Sleeper.sleep(3000);
        startComposer(pathOf(samplesDir, "calendar", "hub_composer"), "docker-compose-local.yml", "up", Main::handleDockerErrors).runBackground();
        HttpChecker.checkForSite(60, "http://www.local.test/api/health").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://www.sample.test/api/v1/health").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://gateway.sample.test/api/v1/health").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://be.sample.test/api/v1/health").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        startComposer(pathOf(samplesDir, "calendar", "hub_composer"), "docker-compose-local.yml", "down").runBackground();


        LogWriter.info("Starting composer quotes");
        startComposer(pathOf(samplesDir, "quotes", "hub_composer"), "docker-compose-local.yml", "down").runBackground();
        Sleeper.sleep(3000);
        startComposer(pathOf(samplesDir, "quotes", "hub_composer"), "docker-compose-local.yml", "up", Main::handleDockerErrors).runBackground();

        Sleeper.sleep(3000);
        HttpChecker.checkForSite(60, "http://www.local.test/api/health").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://www.quotes.test/api/health/index.php").withProxy(dockerIp, 1081).onError(() -> doExit(1)).run();
        startComposer(pathOf(samplesDir, "quotes", "hub_composer"), "docker-compose-local.yml", "down").runBackground();

        Sleeper.sleep(3000);
    }

    private static void handleRunErrors(String a, Process process) {
        if (a.toLowerCase(Locale.ROOT).contains("[error]") ||
                a.toLowerCase(Locale.ROOT).contains("error starting applicationcontext") ||
                a.toLowerCase(Locale.ROOT).contains("build failure")) {
            LogWriter.errror("");
            LogWriter.errror(a);
            process.destroy();
            doExit(1);
        }
    }


    private static void testCalendarSampleFull(String calendarPath) throws Exception {
        LogWriter.info("Testing calendar/runcalendar");
        start(pathOf(calendarPath), "runcalendar").runBackground();
        HttpChecker.checkForSite(60, "http://www.local.test/api/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://www.sample.test/api/v1/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://localhost/int/gateway.sample.test/api/v1/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        HttpChecker.checkForSite(60, "http://localhost/int/be.sample.test/api/v1/health").withProxy("127.0.0.1", 1081).onError(() -> doExit(1)).run();
        _processUtils.killProcesses(findHamProcesses);

        LogWriter.info("Starting calendar");
    }

    public static boolean available(int port) {
        if (port > 63000 || port <= 0) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {

        try {
            if (!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC) {
                var isSudo = CheckSudo.isSudo();
                if (!isSudo) {
                    System.err.println("[WARNING] BEWARE!");
                    System.err.println("[WARNING] NOT RUNNING AS SUDO. PORTS LIKE 80,443 and 53 ARE NOT USUALLY ALLOWED");
                    Sleeper.sleep(1000);
                }
            }
            if (!available(80)) {
                System.err.println("[ERROR] Port 80 in use!");
                doExit(1);
            }
            if (!available(443)) {
                System.err.println("[ERROR] Port 443 in use!");
                doExit(1);
            }
            if (!available(53)) {
                System.err.println("[ERROR] Port 53 (DNS) in use!");
                if (!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC) {
                    System.err.println("[ERROR] Check if you are using a linux system");
                    System.err.println("[ERROR] for systemd-resolved");
                    System.err.println("[ERROR] stop it with: ");
                    System.err.println("\t\tsudo systemctl stop systemd-resolved");
                    System.err.println("[ERROR] modify /etc/systemd/resolved.conf");
                    System.err.println("\t\t[Resolve]");
                    System.err.println("\t\tDNS=8.8.8.8");
                    System.err.println("\t\tDNSStubListener=no");
                    System.err.println("[ERROR] then restart");
                    System.err.println("\t\tsudo ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf");
                }
                doExit(1);
            }
            killApacheLogger();

            var startingPath = System.getenv("STARTING_PATH");
            var logPath = System.getenv("LOG_PATH");
            var hamVersion = System.getenv("HAM_VERSION");
            var dockerIp = System.getenv("DOCKER_IP");
            var dockerHost = System.getenv("DOCKER_HOST");

            //hamVersion = "4.1.5";

            //startingPath = "C:\\Data\\Github\\HttpAnsweringMachine";
            //dockerIp="192.168.56.2";

            //startingPath = "/Users/edaros/Personal/Github/HttpAnsweringMachine";
            //dockerIp="192.168.1.40";

            //dockerHost="tcp://"+dockerIp+":23750";

            env = new HashMap<>();
            var currentEnv = System.getenv();
            for (var kvp :
                    currentEnv.entrySet()) {
                env.put(kvp.getKey(), kvp.getValue());
            }

            env.put("STARTING_PATH", startingPath);
            env.put("LOG_PATH", logPath);
            env.put("HAM_VERSION", hamVersion);
            env.put("DOCKER_IP", dockerIp);
            env.put("DOCKER_HOST", dockerHost);
            env.put("GLOBAL_LOG_ON_CONSOLE", "true");
            _processUtils = new ProcessUtils(env);
            _processUtils.killProcesses(findHamProcesses);

            LocalFileUtils.dos2unix(startingPath, "sh");

            var buildDir = pathOf(startingPath, "scripts", "build");
            var samplesDir = pathOf(startingPath, "samples");
            var releasePath = pathOf(startingPath, "release");
            var calendarPath = pathOf(releasePath, "calendar");

            buildDeploymentArtifacts(startingPath, hamVersion, buildDir, releasePath);
            testAndGenerateJacoco(startingPath);
            applyReleasePermissions(releasePath);
            testLocalHam(releasePath);
            testCalendarSample(calendarPath);
            testCalendarSampleFull(calendarPath);
            buildDockerImages(buildDir);
            testDockerCalendarAndQuotesSamples(dockerIp, samplesDir);

            _processUtils.killProcesses(findHamProcesses);
            exit(0);
        } catch (Exception ex) {
            LogWriter.errror(ex);
            doExit(2);
        }

    }

    private static void testAndGenerateJacoco(String startingPath) throws Exception {

        _processUtils.killProcesses(findHamProcesses);
        LogWriter.info("Unit test ham & report");
        start(pathOf(startingPath, "scripts", "globaltest"), "test.run", Main::handleRunErrors).run();
        if (SystemUtils.IS_OS_WINDOWS) {
            HttpChecker.checkForSite(5, "http://127.0.0.1/api/shutdown").noError().run();
            _processUtils.sigtermProcesses(findHamProcesses);
        } else {
            _processUtils.sigtermProcesses(findHamProcesses);
        }
        var path = Path.of(startingPath, "ham", "api.test", "target", "jacoco_starter.exec");
        var now = System.currentTimeMillis();
        var end = now + 5 * 60 * 1000;

        LogWriter.info("Waiting for coverage data");
        while ((!Files.exists(path) || Files.size(path) == 0) && end > System.currentTimeMillis()) {
            System.out.print(".");
            Sleeper.sleep(1000);
        }
        if (!Files.exists(path) || Files.size(path) == 0) {
            LogWriter.errror("Error loading jacoco reports " + path);
            doExit(1);
        } else {
            LogWriter.info("OK");
        }

        _processUtils.sigtermProcesses(findHamProcesses);

        LogWriter.info("Starting selenium test");
        var pr = new ProcessRunner(env).
                asShell().
                withCommand("mvn").
                withParameter("test").
                withStartingPath(pathOf(startingPath, "globaltest")).
                withErr(Main::handleRunErrors).
                run();
        LogWriter.info("Finished selenium test");
        _processUtils.sigtermProcesses(findHamProcesses);
        start(pathOf(startingPath, "scripts", "globaltest"), "test.jacoco", Main::handleRunErrors).run();
    }


    @SafeVarargs
    private static ProcessRunner start(String dir, String script,
                                       BiConsumer<String, Process>... biConsumers) {

        /*if(!SystemUtils.IS_OS_WINDOWS){
            script="./"+script;
        }*/
        var pr = new ProcessRunner(env).
                asShell().
                withCommand(script + LocalFileUtils.execScriptExt()).
                withStartingPath(dir).
                withNoOutput();
        if (biConsumers.length > 0) {
            pr.withErr(biConsumers[0]);
        }
        if (biConsumers.length > 1) {
            pr.withOut(biConsumers[1]);
        }
        return pr;
    }


    @SafeVarargs
    private static ProcessRunner startComposer(String dir, String composer, String sense,
                                               BiConsumer<String, Process>... biConsumers) {

        var pr = new ProcessRunner(env).asShell().
                withCommand("docker-compose").
                withParameter("-f").withParameter(composer).
                withParameter(sense).withStartingPath(dir).withNoOutput();
        if (biConsumers.length > 0) {
            pr.withErr(biConsumers[0]);
        }
        if (biConsumers.length > 1) {
            pr.withOut(biConsumers[1]);
        }
        return pr;
    }


}

