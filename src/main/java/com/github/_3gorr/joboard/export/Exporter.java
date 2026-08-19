package com.github._3gorr.joboard.export;

import com.github._3gorr.joboard.model.Vacancy;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface Exporter {

    String format();

    void write(List<Vacancy> vacancies, Writer out) throws IOException;
}
