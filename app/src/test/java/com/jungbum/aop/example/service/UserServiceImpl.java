package com.jungbum.aop.example.service;

public class UserServiceImpl implements UserService {
    public void add(String name) {
        if ("error".equals(name)) throw new RuntimeException("DB error");
        System.out.println("add user: " + name);
    }
    public void delete(String name) {
        System.out.println("delete user: " + name);
    }
}