package com.ecomerce.ecomerce_web.exception;

public class InsufficientStockException extends RuntimeException{
    private InsufficientStockException(String message){
        super(message);
    }
}
