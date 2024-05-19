///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS phonepe-snapshots=https://artifactory.phonepe.com/repository/snapshots, phonepe-releases=https://artifactory.phonepe.com/repository/releases, mavencentral
//DEPS com.phonepe.platform:fisheye-cli:0.0.68
//DEPS org.projectlombok:lombok:1.18.24
//JAVA 17

package com.phonepe.platform.fisheye.cli;

import com.phonepe.platform.fisheye.cli.commands.*;
import com.phonepe.platform.fisheye.cli.commands.log.LogConfCommand;
import com.phonepe.platform.fisheye.cli.commands.memory.HeapDumpCommand;
import com.phonepe.platform.fisheye.cli.commands.memory.NativeMemoryDumpCommand;
import com.phonepe.platform.fisheye.cli.commands.test.TestCommand;
import com.phonepe.platform.fisheye.cli.commands.threads.ThreadDumpCommand;
import com.phonepe.platform.fisheye.cli.config.ConfigUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.*;

@CommandLine.Command(
        name = "fisheye",
        version = "0.0.68",
        description = """
                Command line interface (CLI) for the Fisheye monitoring tool.
                This CLI connects to a Fisheye enabled Java application that runs on a remote endpoint (hostname:port)""",
        mixinStandardHelpOptions = true,
        subcommands = {
                HeapDumpCommand.class,
                ThreadDumpCommand.class,
                NativeMemoryDumpCommand.class,
                ProfilerCommand.class,
                InfoCommand.class,
                ProcessCommand.class,
                LogConfCommand.class,
                FileStorageCommand.class,
                PrepareEnvironmentCommand.class,
                TestCommand.class,
                GenerateCompletion.class
        },
        usageHelpAutoWidth = true
)
@Slf4j
@Data
public class FisheyeCli {
    @ArgGroup(exclusive = true)
    private AllArgs allArgs;

    @Data
    public static class AllArgs {
        @Option(names = {"--internal"}, description = "Set this option to run commands internal to Fisheye")
        private boolean internal;

        @ArgGroup(exclusive = false)
        private ProfilerArgs profilerArgs;
    }

    @Data
    public static class ProfilerArgs {
        @Parameters(index = "0", description = "Hostname of the machine where the target process is running.")
        private String host;

        @Parameters(index = "1", description = "Port on the machine to which the target process is listening for connections.")
        private int port;

        @Parameters(
                index = "2",
                description = """
                    Name of the target process. It could be anything provided it is consistent across different monitoring sessions.
                    Files exported for local analysis are stored in file directories containing this app name.
                    """)
        private String app;

        @ArgGroup(exclusive = true)
        private LoginInfo loginInfo;

        @Option(names = {"-S", "--secure"}, description = "Set this to use HTTPS for connecting to the target process")
        private boolean secure;

        @Option(names = {"--socks"}, description = "Use this SOCKS proxy configuration to communicate with the target process (format - host:port)")
        private String socksProxy;

        @Option(names = {"--configFile"}, description = "Path to Fisheye config file")
        private String configFilePath;
    }

    @Data
    public static class LoginInfo {
        @Option(names = {"-O", "--olympus"}, description = "Set this for olympus based login", required = true)
        private boolean isOlympus;

        @ArgGroup(exclusive = false)
        private BasicCredentials basicCredentials;
    }

    @Data
    public static class BasicCredentials {
        @Option(names = {"-U", "--username"}, description = "Fisheye username for login.", required = true)
        private String username;

        @Option(names = {"-P", "--password"}, description = "Fisheye password for login. Do not share this with any one", required = true)
        private String password;
    }

    @Spec
    private Model.CommandSpec spec;

    public static void main(String[] args) {
        try {
            val cli = new FisheyeCli();
            val cmd = new CommandLine(cli);
            CommandLine gen = cmd.getSubcommands().get("generate-completion");
            gen.getCommandSpec().usageMessage().hidden(true);
            val parseResult = cmd.parseArgs(args);
            val optionSpec = parseResult.matchedOption("--configFile");
            String configFile = optionSpec != null ? optionSpec.getValue() : null;
            ConfigUtils.init(configFile);
            int exitCode = cmd.execute(args);
            if (exitCode != 0) {
                log.error("Command exited with error exit code: {}", exitCode);
                System.exit(exitCode);
            }
        } catch (Exception e) {
            log.error("Command execution failed with exception", e);
            System.exit(1);
        }
        System.exit(0);
    }
}
