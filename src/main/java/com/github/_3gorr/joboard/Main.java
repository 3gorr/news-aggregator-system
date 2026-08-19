package com.github._3gorr.joboard;

import com.github._3gorr.joboard.cli.DaemonCommand;
import com.github._3gorr.joboard.cli.ExportCommand;
import com.github._3gorr.joboard.cli.FetchCommand;
import com.github._3gorr.joboard.cli.HistoryCommand;
import com.github._3gorr.joboard.cli.JoboardCommandFactory;
import com.github._3gorr.joboard.cli.ListCommand;
import com.github._3gorr.joboard.cli.NotifyCommand;
import com.github._3gorr.joboard.cli.SearchCommand;
import com.github._3gorr.joboard.cli.ShowCommand;
import com.github._3gorr.joboard.cli.SourcesCommand;
import com.github._3gorr.joboard.cli.StatsCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "joboard",
        mixinStandardHelpOptions = true,
        version = "joboard 0.1.0",
        description = "Job vacancy aggregator (HH API + career.habr.com).",
        subcommands = {
                FetchCommand.class,
                ListCommand.class,
                SearchCommand.class,
                ShowCommand.class,
                ExportCommand.class,
                StatsCommand.class,
                NotifyCommand.class,
                HistoryCommand.class,
                DaemonCommand.class,
                SourcesCommand.class
        }
)
public final class Main implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        try (AppContext ctx = AppContext.create()) {
            int exit = new CommandLine(new Main(), new JoboardCommandFactory(ctx))
                    .setCaseInsensitiveEnumValuesAllowed(true)
                    .execute(args);
            System.exit(exit);
        }
    }
}
