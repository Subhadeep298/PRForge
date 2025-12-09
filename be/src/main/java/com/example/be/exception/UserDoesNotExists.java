package com.example.be.exception;

public class UserDoesNotExists extends RuntimeException{
    public UserDoesNotExists(String message){
        super(message);
    }
}
