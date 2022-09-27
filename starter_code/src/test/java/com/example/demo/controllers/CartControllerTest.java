package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CartControllerTest {

    private CartController cartController;

    private UserRepository userRepo = mock(UserRepository.class);

    private CartRepository cartRepo = mock(CartRepository.class);

    private ItemRepository itemRepo = mock(ItemRepository.class);

    private TokenUtils mockUtils = mock(TokenUtils.class);

    @Before
    public void setUp() {

        /*
        We inject mock objects into fields of the cartController (the repos and the token stuff), because
        these unit tests don't need to test the JPA or Spring Security frameworks.
         */

        cartController = new CartController();
        TestUtils.injectObjects(cartController, "userRepository", userRepo);
        TestUtils.injectObjects(cartController, "cartRepository", cartRepo);
        TestUtils.injectObjects(cartController, "itemRepository", itemRepo);
        TestUtils.injectObjects(cartController, "tokenUtils", mockUtils);
    }

    @Test
    public void add_cart_happy_path() {

        // some constants and expected values for the cart
        long happyItemId = 1L;
        BigDecimal happyItemPrice = BigDecimal.valueOf(10.99);
        String happyUserName = "bob";
        int expectedCartSize = 2;
        BigDecimal expectedTotal = BigDecimal.valueOf(expectedCartSize).multiply(happyItemPrice); // 2 * 10.99 = 21.98

        // stub for CartController's mock userRepo
        User fakeUser = new User();
        Cart fakeCart = new Cart();
        fakeCart.setUser(fakeUser);
        fakeUser.setUsername(happyUserName);
        fakeUser.setCart(fakeCart);
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        // stub for CartController's mock itemRepo
        Item fakeItem = new Item();
        fakeItem.setId(happyItemId);
        fakeItem.setPrice(happyItemPrice);
        when(itemRepo.findById(happyItemId)).thenReturn(Optional.of(fakeItem));

        // create our test request
        ModifyCartRequest r = new ModifyCartRequest();
        r.setUsername(happyUserName);
        r.setItemId(happyItemId);
        r.setQuantity(expectedCartSize); // 2

        ResponseEntity<Cart> response = cartController.addTocart(r, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Cart c = response.getBody();
        assertEquals(happyUserName, c.getUser().getUsername());
        assertEquals(expectedCartSize, c.getItems().size()); // 2
        assertEquals(expectedTotal, c.getTotal()); // 21.98
    }

    @Test
    public void add_cart_item_not_found() {

        // some constants and expected values for the cart
        long itemIdRequest = 1L;
        long itemIdRepo = 2L;
        BigDecimal itemPriceRequest = BigDecimal.valueOf(5.99);
        BigDecimal itemPriceRepo = BigDecimal.valueOf(6.99);
        String userName = "bob";
        int expectedCartSize = 0;
        BigDecimal expectedCartPrice = BigDecimal.valueOf(0.00);

        // stub for CartController's mock userRepo
        User fakeUser = new User();
        Cart fakeCart = new Cart();
        fakeCart.setUser(fakeUser);
        fakeUser.setUsername(userName);
        fakeUser.setCart(fakeCart);
        when(userRepo.findByUsername(userName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(userName);

        // stub for CartController's mock itemRepo
        Item fakeItem = new Item();
        fakeItem.setId(itemIdRepo);
        fakeItem.setPrice(itemPriceRepo);
        when(itemRepo.findById(itemIdRepo)).thenReturn(Optional.of(fakeItem));

        // create our test request
        ModifyCartRequest r = new ModifyCartRequest();
        r.setUsername(userName);
        r.setItemId(itemIdRequest);
        r.setQuantity(5);

        ResponseEntity<Cart> response = cartController.addTocart(r, "fakeToken");
        assertNotNull(response);
        // requested item was not found in the repo
        assertEquals(404, response.getStatusCodeValue());
    }



    @Test
    public void remove_cart_happy_path() {

        // some constants and expected values for the cart. Start with three items, remove one of them.
        long happyItemId = 1L;
        BigDecimal happyItemPrice = BigDecimal.valueOf(10.99);
        Item fakeItem1 = new Item();
        fakeItem1.setId(happyItemId);
        fakeItem1.setPrice(happyItemPrice);
        Item fakeItem2 = new Item();
        fakeItem2.setId(happyItemId);
        fakeItem2.setPrice(happyItemPrice);
        Item fakeItem3 = new Item();
        fakeItem3.setId(happyItemId);
        fakeItem3.setPrice(happyItemPrice);

        String happyUserName = "bob";
        int initialCartSize = 3; // three of the same item in it.
        BigDecimal initialTotal = BigDecimal.valueOf(initialCartSize).multiply(happyItemPrice); // 3 * 10.99 = 32.97

        int finalCartSize = 2;
        BigDecimal finalTotal = BigDecimal.valueOf(finalCartSize).multiply(happyItemPrice); // 2 * 10.99 = 21.98

        // stub for CartController's mock userRepo
        User fakeUser = new User();
        Cart fakeInitialCart = new Cart();
        fakeInitialCart.setUser(fakeUser);
        fakeInitialCart.setItems(Arrays.asList(fakeItem1, fakeItem2, fakeItem3));
        fakeInitialCart.setTotal(initialTotal);
        fakeUser.setUsername(happyUserName);
        fakeUser.setCart(fakeInitialCart);
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        // stub for CartController's mock itemRepo
        when(itemRepo.findById(happyItemId)).thenReturn(Optional.of(fakeItem1));

        // create our test request
        ModifyCartRequest r = new ModifyCartRequest();
        r.setUsername(happyUserName);
        r.setItemId(happyItemId);
        r.setQuantity(1); // remove 1 item from the cart

        ResponseEntity<Cart> response = cartController.removeFromcart(r, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Cart c = response.getBody();
        assertEquals(happyUserName, c.getUser().getUsername());
        assertEquals(finalCartSize, c.getItems().size()); // 2
        assertEquals(finalTotal, c.getTotal()); // 21.98
    }

    @Test
    public void remove_cart_nothing_to_remove() {

        // some constants and expected values for the final cart
        long itemId = 1L;
        BigDecimal itemPrice = BigDecimal.valueOf(10.99);
        Item fakeItem = new Item();
        fakeItem.setId(itemId);
        fakeItem.setPrice(itemPrice);

        String userName = "bob";
        int expectedCartSize = 0;
        BigDecimal expectedTotal = BigDecimal.valueOf(0);


        // stub for CartController's mock userRepo
        // user initially has no cart
        User fakeUser = new User();
        Cart fakeInitialCart = new Cart();
        fakeInitialCart.setUser(fakeUser);
        fakeUser.setUsername(userName);
        fakeUser.setCart(fakeInitialCart);
        when(userRepo.findByUsername(userName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(userName);

        // stub for CartController's mock itemRepo
        when(itemRepo.findById(itemId)).thenReturn(Optional.of(fakeItem));

        // create our test request
        ModifyCartRequest r = new ModifyCartRequest();
        r.setUsername(userName);
        r.setItemId(itemId);
        r.setQuantity(1); // remove 1 item from the empty cart

        ResponseEntity<Cart> response = cartController.removeFromcart(r, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Cart c = response.getBody();
        assertEquals(userName, c.getUser().getUsername());
        assertTrue(c.getItems() == null);
        assertTrue(c.getTotal() == null);
    }
}
