package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Source;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.model.VacancyHistoryEntry.Operation;
import com.github._3gorr.joboard.repository.SourceRepository;
import com.github._3gorr.joboard.repository.VacancyHistoryRepository;
import com.github._3gorr.joboard.repository.VacancyRepository;
import com.github._3gorr.joboard.source.FetchQuery;
import com.github._3gorr.joboard.source.RawVacancy;
import com.github._3gorr.joboard.source.SourceFetchException;
import com.github._3gorr.joboard.source.SourceRegistry;
import com.github._3gorr.joboard.source.VacancySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FetchService {

    private static final Logger LOG = LoggerFactory.getLogger(FetchService.class);

    private final SourceRegistry registry;
    private final SourceRepository sourceRepository;
    private final VacancyRepository vacancyRepository;
    private final VacancyHistoryRepository historyRepository;
    private final Clock clock;
    private final List<FetchListener> listeners = new CopyOnWriteArrayList<>();

    public FetchService(SourceRegistry registry,
                        SourceRepository sourceRepository,
                        VacancyRepository vacancyRepository,
                        VacancyHistoryRepository historyRepository,
                        Clock clock) {
        this.registry = registry;
        this.sourceRepository = sourceRepository;
        this.vacancyRepository = vacancyRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    public void addListener(FetchListener listener) {
        listeners.add(listener);
    }

    public FetchReport fetchAll(FetchQuery query) {
        FetchReport report = new FetchReport();
        for (Source source : sourceRepository.findEnabled()) {
            registry.find(source.code()).ifPresent(vs -> runOne(vs, source, query, report));
        }
        return report;
    }

    public FetchReport fetchOne(String code, FetchQuery query) {
        FetchReport report = new FetchReport();
        Source source = sourceRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source: " + code));
        if (!source.enabled()) {
            LOG.warn("Source {} is disabled, fetch skipped", code);
            return report;
        }
        VacancySource vs = registry.find(code)
                .orElseThrow(() -> new IllegalStateException("Source registered in DB but no implementation: " + code));
        runOne(vs, source, query, report);
        return report;
    }

    private void runOne(VacancySource vs, Source source, FetchQuery query, FetchReport report) {
        FetchReport.SourceStats stats = report.forSource(source.code());
        List<RawVacancy> raws;
        try {
            raws = vs.fetch(query);
        } catch (SourceFetchException e) {
            LOG.error("Fetch failed for {}: {}", source.code(), e.getMessage());
            stats.failed++;
            return;
        }
        Instant now = Instant.now(clock);
        for (RawVacancy raw : raws) {
            try {
                processOne(source, raw, now, stats);
            } catch (RuntimeException e) {
                LOG.warn("Failed to persist vacancy {} from {}: {}",
                        raw.externalId(), source.code(), e.getMessage());
                stats.failed++;
            }
        }
        LOG.info("Source {} done: +{} ~{} ={} !{}",
                source.code(), stats.inserted, stats.updated, stats.unchanged, stats.failed);
    }

    private void processOne(Source source, RawVacancy raw, Instant fetchedAt, FetchReport.SourceStats stats) {
        String hash = ContentHasher.hash(raw);
        Optional<Vacancy> existing = vacancyRepository.findByExternalId(source.id(), raw.externalId());

        if (existing.isEmpty()) {
            Vacancy v = toVacancy(null, source.id(), raw, fetchedAt, hash);
            long id = vacancyRepository.insert(v);
            Vacancy persisted = toVacancy(id, source.id(), raw, fetchedAt, hash);
            historyRepository.log(id, source.id(), raw.externalId(), Operation.INSERT);
            stats.inserted++;
            notifyInserted(persisted);
            return;
        }

        Vacancy current = existing.get();
        if (hash.equals(current.contentHash())) {
            stats.unchanged++;
            return;
        }
        Vacancy updated = toVacancy(current.id(), source.id(), raw, fetchedAt, hash);
        vacancyRepository.update(updated);
        historyRepository.log(current.id(), source.id(), raw.externalId(), Operation.UPDATE);
        stats.updated++;
        notifyUpdated(updated);
    }

    private void notifyInserted(Vacancy v) {
        for (FetchListener l : listeners) {
            try { l.onInserted(v); } catch (RuntimeException e) {
                LOG.warn("Listener onInserted failed: {}", e.getMessage());
            }
        }
    }

    private void notifyUpdated(Vacancy v) {
        for (FetchListener l : listeners) {
            try { l.onUpdated(v); } catch (RuntimeException e) {
                LOG.warn("Listener onUpdated failed: {}", e.getMessage());
            }
        }
    }

    private static Vacancy toVacancy(Long id, long sourceId, RawVacancy raw, Instant fetchedAt, String hash) {
        return new Vacancy(
                id, sourceId, raw.externalId(), raw.url(),
                raw.title(), raw.company(), raw.city(), raw.salary(),
                raw.employmentType(), raw.description(), raw.requirements(),
                raw.publishedAt(), fetchedAt, hash);
    }
}
