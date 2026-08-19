package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Vacancy;

public interface FetchListener {

    default void onInserted(Vacancy v) {}

    default void onUpdated(Vacancy v) {}
}
