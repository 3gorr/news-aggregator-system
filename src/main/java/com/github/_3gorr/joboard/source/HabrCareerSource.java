package com.github._3gorr.joboard.source;

import com.github._3gorr.joboard.model.Salary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HabrCareerSource implements VacancySource {

    public static final String CODE = "habr_career";
    private static final Logger LOG = LoggerFactory.getLogger(HabrCareerSource.class);

    private static final Pattern EXTERNAL_ID = Pattern.compile("/vacancies/(\\d+)");
    private static final Pattern NUMBER = Pattern.compile("(\\d[\\d\\s\\u00A0]*)");

    private final HttpFetcher http;
    private final String baseUrl;

    public HabrCareerSource(HttpFetcher http) {
        this(http, "https://career.habr.com");
    }

    public HabrCareerSource(HttpFetcher http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawVacancy> fetch(FetchQuery query) {
        List<RawVacancy> all = new ArrayList<>();
        for (int page = 1; page <= query.maxPages(); page++) {
            String url = buildUrl(query, page);
            String body;
            try {
                body = http.get(url);
            } catch (IOException e) {
                throw new SourceFetchException("Habr Career request failed: " + url, e);
            }
            Document doc = Jsoup.parse(body, baseUrl);
            Elements cards = doc.select("div.vacancy-card");
            if (cards.isEmpty()) {
                break;
            }
            for (Element card : cards) {
                try {
                    RawVacancy raw = parseCard(card);
                    if (raw != null) all.add(raw);
                } catch (RuntimeException e) {
                    LOG.warn("Skipping malformed habr card: {}", e.getMessage());
                }
            }
        }
        return all;
    }

    private String buildUrl(FetchQuery query, int page) {
        StringBuilder sb = new StringBuilder(baseUrl).append("/vacancies?page=").append(page);
        query.text().ifPresent(text ->
                sb.append("&q=").append(URLEncoder.encode(text, StandardCharsets.UTF_8)));
        return sb.toString();
    }

    private RawVacancy parseCard(Element card) {
        Element titleLink = card.selectFirst(".vacancy-card__title a");
        if (titleLink == null) return null;

        String title = titleLink.text().trim();
        String href = titleLink.attr("abs:href");
        if (href.isBlank()) {
            href = baseUrl + titleLink.attr("href");
        }
        Matcher m = EXTERNAL_ID.matcher(href);
        if (!m.find()) return null;
        String externalId = m.group(1);

        String company = textOrNull(card.selectFirst(".vacancy-card__company-title"));
        if (company == null) {
            company = textOrNull(card.selectFirst(".vacancy-card__company a"));
        }

        String city = extractCity(card);
        Salary salary = extractSalary(card);

        Element timeEl = card.selectFirst("time.vacancy-card__date");
        if (timeEl == null) timeEl = card.selectFirst("time");
        Instant published = parsePublished(timeEl);

        String requirements = textOrNull(card.selectFirst(".vacancy-card__skills"));

        return new RawVacancy(
                externalId, href, title, company, city, salary,
                null, null, requirements, published);
    }

    /**
     * City lives in a chip with a "placemark" SVG icon inside {@code .vacancy-card__meta}.
     * Fall back to legacy selectors used by the test fixture.
     */
    private static String extractCity(Element card) {
        Elements chips = card.select(".vacancy-card__meta .basic-chip");
        if (!chips.isEmpty()) {
            for (Element chip : chips) {
                Element use = chip.selectFirst("svg use");
                if (use == null) continue;
                String href = use.attr("xlink:href");
                if (href.isEmpty()) href = use.attr("href");
                if (href.toLowerCase().contains("placemark")) {
                    String text = textOrNull(chip.selectFirst(".chip-with-icon__text"));
                    return text != null ? text : textOrNull(chip);
                }
            }
            return null; // modern chip layout, but no location chip → remote-only / unspecified
        }
        String fallback = textOrNull(card.selectFirst(".vacancy-card__meta > div"));
        if (fallback != null) return fallback;
        return textOrNull(card.selectFirst(".vacancy-card__meta span"));
    }

    /**
     * Habr shows a "predicted" salary block when the actual one is missing — don't treat
     * that as a real value. Real-salary text lives directly inside .vacancy-card__salary.
     */
    private static Salary extractSalary(Element card) {
        Element salaryEl = card.selectFirst(".vacancy-card__salary");
        if (salaryEl == null) return Salary.none();
        if (salaryEl.selectFirst(".predicted-salary") != null) {
            return Salary.none();
        }
        return parseSalary(salaryEl.text());
    }

    static Salary parseSalary(String text) {
        if (text == null || text.isBlank()) {
            return Salary.none();
        }
        String lower = text.toLowerCase();
        String currency = detectCurrency(text);

        List<Integer> nums = extractNumbers(text);
        if (nums.isEmpty()) return new Salary(null, null, currency);

        if (lower.contains("от") && nums.size() == 1) {
            return new Salary(nums.get(0), null, currency);
        }
        if (lower.contains("до") && nums.size() == 1) {
            return new Salary(null, nums.get(0), currency);
        }
        if (nums.size() >= 2) {
            return new Salary(nums.get(0), nums.get(1), currency);
        }
        return new Salary(nums.get(0), nums.get(0), currency);
    }

    private static List<Integer> extractNumbers(String text) {
        List<Integer> result = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String compact = m.group(1).replaceAll("[\\s\\u00A0]", "");
            if (!compact.isEmpty()) {
                try {
                    result.add(Integer.parseInt(compact));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private static String detectCurrency(String text) {
        if (text.contains("₽") || text.toLowerCase().contains("руб")) return "RUB";
        if (text.contains("$") || text.toLowerCase().contains("usd")) return "USD";
        if (text.contains("€") || text.toLowerCase().contains("eur")) return "EUR";
        return null;
    }

    private static Instant parsePublished(Element timeEl) {
        if (timeEl == null) {
            return Instant.now();
        }
        String dt = timeEl.attr("datetime");
        if (dt.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(dt);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(dt).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        return Instant.now();
    }

    private static String textOrNull(Element el) {
        if (el == null) return null;
        String t = el.text().trim();
        return t.isEmpty() ? null : t;
    }
}
