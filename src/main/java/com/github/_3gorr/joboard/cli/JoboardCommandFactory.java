package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.AppContext;
import picocli.CommandLine;

public final class JoboardCommandFactory implements CommandLine.IFactory {

    private final AppContext context;
    private final CommandLine.IFactory fallback = CommandLine.defaultFactory();

    public JoboardCommandFactory(AppContext context) {
        this.context = context;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        if (cls == ListCommand.class) return cls.cast(new ListCommand(context.searchService()));
        if (cls == SearchCommand.class) return cls.cast(new SearchCommand(context.searchService()));
        if (cls == ShowCommand.class) return cls.cast(new ShowCommand(context.searchService()));
        if (cls == FetchCommand.class) return cls.cast(new FetchCommand(context.fetchService()));
        if (cls == SourcesCommand.class) return cls.cast(new SourcesCommand(context.sourceRepository()));
        if (cls == ExportCommand.class) return cls.cast(new ExportCommand(context.searchService(), context.exporterFactory()));
        if (cls == StatsCommand.class) return cls.cast(new StatsCommand(context.statsService()));
        if (cls == NotifyCommand.class) return cls.cast(new NotifyCommand(context.notificationService()));
        if (cls == HistoryCommand.class) return cls.cast(new HistoryCommand(context.historyRepository()));
        if (cls == DaemonCommand.class) return cls.cast(new DaemonCommand(context.fetchService(), context.schedulerService()));
        return fallback.create(cls);
    }
}
