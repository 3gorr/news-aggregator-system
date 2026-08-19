package com.github._3gorr.joboard.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github._3gorr.joboard.model.Vacancy;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public final class JsonExporter implements Exporter {

    private final ObjectMapper mapper;

    public JsonExporter() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String format() {
        return "json";
    }

    @Override
    public void write(List<Vacancy> vacancies, Writer out) throws IOException {
        mapper.writeValue(out, vacancies);
    }
}
