package com.github._3gorr.joboard.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._3gorr.joboard.model.Salary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class HhApiSource implements VacancySource {

    public static final String CODE = "hh";
    private static final Logger LOG = LoggerFactory.getLogger(HhApiSource.class);

    private static final DateTimeFormatter PUBLISHED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private final HttpFetcher http;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public HhApiSource(HttpFetcher http, ObjectMapper mapper) {
        this(http, mapper, "https://api.hh.ru");
    }

    public HhApiSource(HttpFetcher http, ObjectMapper mapper, String baseUrl) {
        this.http = http;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawVacancy> fetch(FetchQuery query) {
        List<RawVacancy> all = new ArrayList<>();
        for (int page = 0; page < query.maxPages(); page++) {
            String url = buildUrl(query, page);
            String body;
            try {
                body = http.get(url);
            } catch (IOException e) {
                throw new SourceFetchException("HH API request failed: " + url, e);
            }
            JsonNode root;
            try {
                root = mapper.readTree(body);
            } catch (IOException e) {
                throw new SourceFetchException("HH API returned invalid JSON for " + url, e);
            }
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                try {
                    all.add(toRaw(item));
                } catch (RuntimeException e) {
                    LOG.warn("Skipping malformed HH item: {}", e.getMessage());
                }
            }
            int totalPages = root.path("pages").asInt(1);
            if (page + 1 >= totalPages) break;
        }
        return all;
    }

    private String buildUrl(FetchQuery query, int page) {
        StringBuilder sb = new StringBuilder(baseUrl)
                .append("/vacancies?page=").append(page)
                .append("&per_page=").append(query.perPage());
        query.text().ifPresent(text ->
                sb.append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8)));
        return sb.toString();
    }

    private static RawVacancy toRaw(JsonNode item) {
        String externalId = item.path("id").asText();
        String url = item.path("alternate_url").asText();
        String title = item.path("name").asText();
        String company = textOrNull(item.path("employer").path("name"));
        String city = textOrNull(item.path("area").path("name"));
        Salary salary = parseSalary(item.path("salary"));
        String employmentType = textOrNull(item.path("employment").path("name"));
        String responsibility = textOrNull(item.path("snippet").path("responsibility"));
        String requirements = textOrNull(item.path("snippet").path("requirement"));
        Instant published = parseInstant(item.path("published_at").asText());

        return new RawVacancy(
                externalId, url, title, company, city, salary,
                employmentType, responsibility, requirements, published);
    }

    private static Salary parseSalary(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return Salary.none();
        }
        Integer from = node.hasNonNull("from") ? node.get("from").asInt() : null;
        Integer to = node.hasNonNull("to") ? node.get("to").asInt() : null;
        String currency = textOrNull(node.path("currency"));
        if (currency != null && currency.equalsIgnoreCase("RUR")) {
            currency = "RUB";
        }
        return new Salary(from, to, currency);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String v = node.asText();
        return v == null || v.isBlank() ? null : v;
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing published_at");
        }
        return OffsetDateTime.parse(raw, PUBLISHED_AT_FORMAT).toInstant();
    }
}
