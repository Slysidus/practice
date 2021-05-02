package com.oxymore.practice.controller;

public class ControllerInitException extends Exception {
    public ControllerInitException(String controller, String message) {
        super(String.format("Initialization of controller '%s' failed with error '%s'", controller, message));
    }

    public ControllerInitException(Throwable throwable) {
        super(throwable);
    }
}
