package com.example.be.service;

import com.example.be.model.User;

public interface UserService {
    User findByProviderId(String id);
}
