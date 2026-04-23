package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String roomId) {
        super("Room with id '" + roomId + "' does not exist in the system.");
    }
}