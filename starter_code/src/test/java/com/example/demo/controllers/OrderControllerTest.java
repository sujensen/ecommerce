package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderControllerTest {

    private OrderController orderController;

    private UserRepository userRepo = mock(UserRepository.class);

    private OrderRepository orderRepo = mock(OrderRepository.class);

    private TokenUtils mockUtils = mock(TokenUtils.class);

    @Before
    public void setUp() {

        /*
        We inject mock objects into fields of the orderController (the repos and the token stuff), because
        these unit tests don't need to test the JPA or Spring Security frameworks.
         */

        orderController = new OrderController();
        TestUtils.injectObjects(orderController, "userRepository", userRepo);
        TestUtils.injectObjects(orderController, "orderRepository", orderRepo);
        TestUtils.injectObjects(orderController, "tokenUtils", mockUtils);
    }

    @Test
    public void submit_order_happy_path() {

        // some constants and expected values for the order
        String happyUserName = "bob";
        BigDecimal cartTotal = BigDecimal.valueOf(11.99);

        // stub for OrderController's mock userRepo
        User fakeUser = new User();
        Cart fakeCart = new Cart();
        fakeCart.setUser(fakeUser);
        fakeCart.setItems(new ArrayList<Item>());
        fakeCart.setTotal(cartTotal);
        fakeUser.setUsername(happyUserName);
        fakeUser.setCart(fakeCart);
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        ResponseEntity<UserOrder> response = orderController.submit(happyUserName, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        UserOrder c = response.getBody();
        assertEquals(happyUserName, c.getUser().getUsername());
        assertEquals(cartTotal, c.getTotal()); // 11.99
    }

    @Test
    public void submit_order_no_cart() {

        // stub for OrderController's mock userRepo
        String happyUserName = "bob";
        User fakeUser = new User();
        fakeUser.setUsername(happyUserName); // no cart for this user
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        ResponseEntity<UserOrder> response = orderController.submit(happyUserName, "fakeToken");
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    public void get_orders_empty_list() {

        // stub for OrderController's mock userRepo
        String happyUserName = "bob";
        User fakeUser = new User();
        fakeUser.setUsername(happyUserName); // no cart for this user
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser(happyUserName, "fakeToken");
        assertNotNull(response);
        System.out.println("get empty orders response code = " + response.getStatusCodeValue());
        assertEquals(0, response.getBody().size());
    }
}
