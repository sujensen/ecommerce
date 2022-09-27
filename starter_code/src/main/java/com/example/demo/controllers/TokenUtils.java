package com.example.demo.controllers;

import com.auth0.jwt.JWT;
import com.example.demo.security.SecurityConstants;
import org.springframework.stereotype.Component;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

@Component
public class TokenUtils {

    public String getUserFromTokenHeader(String headerString) {
        String tokenUser = JWT.require(HMAC512(SecurityConstants.SECRET.getBytes())).build()
                .verify(headerString.replace(SecurityConstants.TOKEN_PREFIX, ""))
                .getSubject();
        // could be null
        return tokenUser;
    }
}
