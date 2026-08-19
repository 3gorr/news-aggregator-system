package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.NotificationFilter;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.repository.NotificationRepository;

import java.io.PrintStream;
import java.util.List;

public final class NotificationService implements FetchListener {

    private final NotificationRepository repository;
    private final PrintStream out;

    public NotificationService(NotificationRepository repository, PrintStream out) {
        this.repository = repository;
        this.out = out;
    }

    public long add(String name, String query, String city, Integer minSalary) {
        return repository.add(name, query, city, minSalary);
    }

    public List<NotificationFilter> all() {
        return repository.findAll();
    }

    public boolean remove(String name) {
        return repository.removeByName(name);
    }

    public List<NotificationFilter> matchingFilters(Vacancy v) {
        return repository.findAll().stream().filter(f -> f.matches(v)).toList();
    }

    @Override
    public void onInserted(Vacancy v) {
        notify("NEW", v);
    }

    @Override
    public void onUpdated(Vacancy v) {
        notify("UPDATED", v);
    }

    private void notify(String label, Vacancy v) {
        for (NotificationFilter f : repository.findAll()) {
            if (f.matches(v)) {
                out.printf("[notify:%s] %s — %s @ %s · %s%n",
                        f.name(), label, v.title(),
                        v.company() == null ? "?" : v.company(),
                        v.url());
            }
        }
    }
}
