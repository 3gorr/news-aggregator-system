package com.github._3gorr.joboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._3gorr.joboard.db.AppConfig;
import com.github._3gorr.joboard.db.DataSourceFactory;
import com.github._3gorr.joboard.export.ExporterFactory;
import com.github._3gorr.joboard.repository.JdbcNotificationRepository;
import com.github._3gorr.joboard.repository.JdbcSourceRepository;
import com.github._3gorr.joboard.repository.JdbcStatsRepository;
import com.github._3gorr.joboard.repository.JdbcVacancyHistoryRepository;
import com.github._3gorr.joboard.repository.JdbcVacancyRepository;
import com.github._3gorr.joboard.repository.NotificationRepository;
import com.github._3gorr.joboard.repository.SourceRepository;
import com.github._3gorr.joboard.repository.StatsRepository;
import com.github._3gorr.joboard.repository.VacancyHistoryRepository;
import com.github._3gorr.joboard.repository.VacancyRepository;
import com.github._3gorr.joboard.service.FetchService;
import com.github._3gorr.joboard.service.NotificationService;
import com.github._3gorr.joboard.service.SchedulerService;
import com.github._3gorr.joboard.service.SearchService;
import com.github._3gorr.joboard.service.StatsService;
import com.github._3gorr.joboard.source.HabrCareerSource;
import com.github._3gorr.joboard.source.HhApiSource;
import com.github._3gorr.joboard.source.HttpFetcher;
import com.github._3gorr.joboard.source.JdkHttpFetcher;
import com.github._3gorr.joboard.source.SourceRegistry;
import com.github._3gorr.joboard.source.VacancySource;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public final class AppContext implements AutoCloseable {

    private final DataSource dataSource;
    private final SourceRepository sourceRepository;
    private final VacancyRepository vacancyRepository;
    private final VacancyHistoryRepository historyRepository;
    private final SearchService searchService;
    private final FetchService fetchService;
    private final ExporterFactory exporterFactory;
    private final StatsService statsService;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;

    private AppContext(DataSource dataSource,
                       SourceRepository sourceRepository,
                       VacancyRepository vacancyRepository,
                       VacancyHistoryRepository historyRepository,
                       SearchService searchService,
                       FetchService fetchService,
                       ExporterFactory exporterFactory,
                       StatsService statsService,
                       NotificationService notificationService,
                       SchedulerService schedulerService) {
        this.dataSource = dataSource;
        this.sourceRepository = sourceRepository;
        this.vacancyRepository = vacancyRepository;
        this.historyRepository = historyRepository;
        this.searchService = searchService;
        this.fetchService = fetchService;
        this.exporterFactory = exporterFactory;
        this.statsService = statsService;
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
    }

    public static AppContext create() {
        AppConfig config = AppConfig.load();
        DataSource ds = DataSourceFactory.create(config);
        Clock clock = Clock.systemUTC();

        SourceRepository sources = new JdbcSourceRepository(ds);
        VacancyRepository vacancies = new JdbcVacancyRepository(ds);
        VacancyHistoryRepository history = new JdbcVacancyHistoryRepository(ds, clock);
        NotificationRepository notifications = new JdbcNotificationRepository(ds, clock);
        StatsRepository stats = new JdbcStatsRepository(ds);

        ObjectMapper mapper = new ObjectMapper();
        HttpFetcher http = new JdkHttpFetcher(
                config.httpUserAgent(),
                Duration.ofSeconds(config.httpTimeoutSeconds()));
        SourceRegistry registry = new SourceRegistry(List.<VacancySource>of(
                new HhApiSource(http, mapper),
                new HabrCareerSource(http)));

        SearchService searchService = new SearchService(vacancies);
        FetchService fetchService = new FetchService(registry, sources, vacancies, history, clock);
        ExporterFactory exporters = ExporterFactory.defaults();
        StatsService statsService = new StatsService(stats);
        NotificationService notificationService = new NotificationService(notifications, System.out);
        fetchService.addListener(notificationService);

        SchedulerService scheduler = new SchedulerService();

        return new AppContext(ds, sources, vacancies, history, searchService, fetchService,
                exporters, statsService, notificationService, scheduler);
    }

    public SourceRepository sourceRepository() { return sourceRepository; }
    public VacancyRepository vacancyRepository() { return vacancyRepository; }
    public VacancyHistoryRepository historyRepository() { return historyRepository; }
    public SearchService searchService() { return searchService; }
    public FetchService fetchService() { return fetchService; }
    public ExporterFactory exporterFactory() { return exporterFactory; }
    public StatsService statsService() { return statsService; }
    public NotificationService notificationService() { return notificationService; }
    public SchedulerService schedulerService() { return schedulerService; }

    @Override
    public void close() {
        schedulerService.close();
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
    }
}
