package com.example.be.service.impl;

import com.example.be.exception.UsernameNotFoundException;
import com.example.be.model.User;
import com.example.be.repository.UserRepository;
import com.example.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findByProviderId(String id) {
        return userRepository.findByProviderId(id).orElseThrow(()->new UsernameNotFoundException("User not found"));
    }

    public boolean validateUserByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId).isPresent();
    }
}
