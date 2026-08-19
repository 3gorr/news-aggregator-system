package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.Source;
import com.github._3gorr.joboard.repository.SourceRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "sources", mixinStandardHelpOptions = true, description = "List or toggle configured sources.")
public final class SourcesCommand implements Callable<Integer> {

    private final SourceRepository sourceRepository;

    @Parameters(arity = "0..2", description = "Optional: <enable|disable> <code>")
    List<String> args;

    public SourcesCommand(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Override
    public Integer call() {
        if (args == null || args.isEmpty()) {
            return printAll();
        }
        if (args.size() != 2) {
            System.err.println("Usage: sources [enable|disable <code>]");
            return 2;
        }
        String action = args.get(0);
        String code = args.get(1);
        switch (action) {
            case "enable" -> {
                sourceRepository.setEnabled(code, true);
                System.out.println("Enabled: " + code);
            }
            case "disable" -> {
                sourceRepository.setEnabled(code, false);
                System.out.println("Disabled: " + code);
            }
            default -> {
                System.err.println("Unknown action: " + action);
                return 2;
            }
        }
        return 0;
    }

    private int printAll() {
        List<Source> sources = sourceRepository.findAll();
        System.out.printf("%-15s %-10s %-30s %s%n", "CODE", "STATUS", "NAME", "URL");
        for (Source s : sources) {
            System.out.printf("%-15s %-10s %-30s %s%n",
                    s.code(), s.enabled() ? "enabled" : "disabled", s.name(), s.baseUrl());
        }
        return 0;
    }
}
