package com.github._3gorr.joboard.source;

import java.util.List;

public interface VacancySource {

    String code();

    List<RawVacancy> fetch(FetchQuery query);
}
