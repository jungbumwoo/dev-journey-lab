package com.jungbum.service;

import com.jungbum.domain.User;

public interface UserService {
    void add(User user);
    void upgradeLevels();
    void add(String name);
    void delete(String name);
}