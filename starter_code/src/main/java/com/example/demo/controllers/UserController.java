package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

@RestController
@RequestMapping("/api/user")
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private TokenUtils tokenUtils;

	/*
	This endpoint is like looking at a shopper's profile page on the ecommerce site.  It requires auth'n,
	and also requires that the authenticated shopper can only look at their own profile page!
	 */
	@GetMapping("/id/{id}")
	public ResponseEntity<User> findById(@PathVariable Long id,
										 @RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// user ID in the path must match with the user in the token payload, otherwise unauthorized.
		String tokenUserName = tokenUtils.getUserFromTokenHeader(headerString);
		User tokenUser = userRepository.findByUsername(tokenUserName);
		if (tokenUser == null) {
			// the authenticated user (username in the token) is not found in the repo, so this request is unauth'zd.
			log.warn("Token subject (" + tokenUserName + ") was not found in the user repo.");
			return ResponseEntity.status(403).build();
		}
		long tokenUserId = tokenUser.getId();
		if (id != tokenUserId) {
			log.warn("user id requested (" + id + ") does not match the authenticated user in the repo.");
			return ResponseEntity.status(403).build();
		}

		Optional<User> u = userRepository.findById(id);
		return ResponseEntity.of(u);
	}

	/*
	This endpoint is like looking at a shopper's profile page on the ecommerce site.  It requires auth'n,
	and also requires that the authenticated shopper can only look at their own profile page!
	 */
	@GetMapping("/{username}")
	public ResponseEntity<User> findByUserName(@PathVariable String username,
											   @RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// username in path must equal the user in the token payload, otherwise unauthorized.
		String tokenUser = tokenUtils.getUserFromTokenHeader(headerString);
		if (!username.equals(tokenUser)) {
			log.warn("username (" + username + ") does not equal subject of token (" + tokenUser + ")");
			return ResponseEntity.status(403).build();
		}
		User user = userRepository.findByUsername(username);
		return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
	}

	/*
	Authentication is NOT required to create a new user (i.e. for a shopper to create login credentials)
	 */
	@PostMapping("/create")
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {
		User user = new User();
		user.setUsername(createUserRequest.getUsername());

		Cart cart = new Cart();
		cartRepository.save(cart);
		user.setCart(cart);
		if(createUserRequest.getPassword().length() < 7 ||
				!createUserRequest.getPassword().equals(createUserRequest.getConfirmPassword())){
			log.warn("User not created: either length is less than 7 or pass and conf pass do not match. " +
					"Unable to create " + createUserRequest.getUsername());
			return ResponseEntity.badRequest().build();
		}
		user.setPassword(bCryptPasswordEncoder.encode(createUserRequest.getPassword()));
		userRepository.save(user);
		log.info("User was created: name set with " + createUserRequest.getUsername());
		return ResponseEntity.ok(user);
	}
	
}
