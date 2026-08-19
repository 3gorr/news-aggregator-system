package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.NotificationFilter;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    long add(String name, String query, String city, Integer minSalary);

    List<NotificationFilter> findAll();

    Optional<NotificationFilter> findByName(String name);

    boolean removeByName(String name);
}
