package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserControllerTest {

    private UserController userController;

    private UserRepository userRepo = mock(UserRepository.class);

    private CartRepository cartRepo = mock(CartRepository.class);

    private BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

    private TokenUtils mockUtils = mock(TokenUtils.class);

    @Before
    public void setUp() {

        /*
        We inject mock objects into fields of the userController (the repos and the token stuff), because
        these unit tests don't need to test the JPA or Spring Security frameworks.
         */

        userController = new UserController();
        TestUtils.injectObjects(userController, "userRepository", userRepo);
        TestUtils.injectObjects(userController, "cartRepository", cartRepo);
        TestUtils.injectObjects(userController, "bCryptPasswordEncoder", encoder);
        TestUtils.injectObjects(userController, "tokenUtils", mockUtils);
    }

    // Bare minimum (sanity) positive use case test
    @Test
    public void create_user_happy_path() {
        /*
        stubbing: whenever this line--encoder.encode("testPassword")--is encountered, such as in the
        UserController method, make sure to return "thisIsHashed"
         */
        when(encoder.encode("testPassword")).thenReturn("thisIsHashed");

        CreateUserRequest r = new CreateUserRequest();
        r.setUsername("test");
        r.setPassword("testPassword");
        r.setConfirmPassword("testPassword");

        ResponseEntity<User> response = userController.createUser(r);
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        User u = response.getBody();
        assertNotNull(u);
        assertEquals(0, u.getId());
        assertEquals("test", u.getUsername());
        assertEquals("thisIsHashed", u.getPassword());
    }

    @Test
    public void create_user_mismatched_passwords() {
        CreateUserRequest r = new CreateUserRequest();
        r.setUsername("test");
        r.setPassword("testPassword");
        r.setConfirmPassword("differentPassword");

        ResponseEntity<User> response = userController.createUser(r);
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    public void find_by_id_happy_path() {
        long happyUserId = 5L;
        String happyUserName = "bob";

        // stub for UserController's mock userRepo
        User fakeUser = new User();
        fakeUser.setId(happyUserId);
        when(userRepo.findById(happyUserId)).thenReturn(Optional.of(fakeUser));
        when(userRepo.findByUsername(happyUserName)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUserName);

        ResponseEntity<User> response = userController.findById(happyUserId, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        User u = response.getBody();
        assertEquals(happyUserId, u.getId());
    }

    @Test
    public void find_by_name_happy_path() {
        String happyUser = "bob";

        // stub for UserController's mock userRepo
        User fakeUser = new User();
        fakeUser.setUsername(happyUser);
        when(userRepo.findByUsername(happyUser)).thenReturn(fakeUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(happyUser);

        ResponseEntity<User> response = userController.findByUserName(happyUser, "fakeToken");
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        User u = response.getBody();
        assertEquals(happyUser, u.getUsername());
    }

    @Test
    public void find_by_name_unauthorized_bad_token() {
        // bob is going to ask for sally's data: unauthorized!
        String userInPath = "sally";
        String userInToken = "bob";

        // stub for UserController's mock userRepo.
        // the user in the request path does exist in the db.
        User dbUser = new User();
        dbUser.setUsername(userInPath);
        when(userRepo.findByUsername(userInPath)).thenReturn(dbUser);

        // stub for getting username out of a mock token
        when(mockUtils.getUserFromTokenHeader("fakeToken")).thenReturn(userInToken);

        ResponseEntity<User> response = userController.findByUserName(userInPath, "fakeToken");
        assertNotNull(response);
        assertEquals(403, response.getStatusCodeValue());
    }
}
