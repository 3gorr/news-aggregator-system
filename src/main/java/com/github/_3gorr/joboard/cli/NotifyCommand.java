package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.NotificationFilter;
import com.github._3gorr.joboard.service.NotificationService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "notify", mixinStandardHelpOptions = true, description = "Manage saved notification filters.",
        subcommands = {
                NotifyCommand.Add.class,
                NotifyCommand.List.class,
                NotifyCommand.Remove.class
        })
public final class NotifyCommand implements Callable<Integer> {

    private final NotificationService service;

    public NotifyCommand(NotificationService service) {
        this.service = service;
    }

    @Override
    public Integer call() {
        System.err.println("Usage: notify <add|list|remove> ...");
        return 2;
    }

    public NotificationService service() {
        return service;
    }

    @Command(name = "add", description = "Save a new notification filter.")
    public static final class Add implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NotifyCommand parent;

        @Option(names = "--name", required = true) String name;
        @Option(names = "--query") String query;
        @Option(names = "--city") String city;
        @Option(names = "--min-salary") Integer minSalary;

        @Override
        public Integer call() {
            long id = parent.service().add(name, query, city, minSalary);
            System.out.printf("Added notification filter #%d (%s)%n", id, name);
            return 0;
        }
    }

    @Command(name = "list", description = "Show all saved notification filters.")
    public static final class List implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NotifyCommand parent;

        @Override
        public Integer call() {
            java.util.List<NotificationFilter> all = parent.service().all();
            if (all.isEmpty()) {
                System.out.println("(no notification filters)");
                return 0;
            }
            System.out.printf("%-20s %-20s %-15s %s%n", "NAME", "QUERY", "CITY", "MIN SALARY");
            for (NotificationFilter f : all) {
                System.out.printf("%-20s %-20s %-15s %s%n",
                        f.name(),
                        f.query() == null ? "—" : f.query(),
                        f.city() == null ? "—" : f.city(),
                        f.minSalary() == null ? "—" : f.minSalary());
            }
            return 0;
        }
    }

    @Command(name = "remove", description = "Delete a saved notification filter by name.")
    public static final class Remove implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NotifyCommand parent;

        @Parameters(index = "0") String name;

        @Override
        public Integer call() {
            boolean removed = parent.service().remove(name);
            if (!removed) {
                System.err.println("Filter not found: " + name);
                return 1;
            }
            System.out.println("Removed: " + name);
            return 0;
        }
    }
}
