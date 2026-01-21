package com.jungbum.service;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    public void add(String name) {
        if ("error".equals(name)) throw new RuntimeException("DB error");
        System.out.println("add user: " + name);
    }
    public void delete(String name) {
        System.out.println("delete user: " + name);
    }
}