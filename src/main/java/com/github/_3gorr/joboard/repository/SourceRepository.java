package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.Source;

import java.util.List;
import java.util.Optional;

public interface SourceRepository {

    List<Source> findAll();

    List<Source> findEnabled();

    Optional<Source> findByCode(String code);

    void setEnabled(String code, boolean enabled);
}
