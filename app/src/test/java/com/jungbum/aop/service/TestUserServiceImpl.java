package com.jungbum.aop.service;

import com.jungbum.dao.UserDao;
import com.jungbum.domain.User;
import com.jungbum.service.UserServiceImpl;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;

@Service
public class TestUserServiceImpl extends UserServiceImpl {
    private String id = "madnite1"; // users(3).getId()

    public TestUserServiceImpl(UserDao userDao, MailSender mailSender) {
        super(userDao, mailSender);
    }

    protected void upgradeLevel(User user) {
        if (user.getId().equals(this.id)) {
            System.out.printf("error: user: %s%n", user);
            throw new TestUserServiceException();
        };
        super.upgradeLevel(user);
    }

    static class TestUserServiceException extends RuntimeException {
    }
}
