package com.example.demo.controllers;

import java.util.List;

import com.example.demo.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;

@RestController
@RequestMapping("/api/order")
public class OrderController {

	private static final Logger log = LoggerFactory.getLogger(OrderController.class);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private TokenUtils tokenUtils;


	@PostMapping("/submit/{username}")
	public ResponseEntity<UserOrder> submit(@PathVariable String username,
											@RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// username in the request must equal the user in the token payload, otherwise unauthorized.
		String tokenUser = tokenUtils.getUserFromTokenHeader(headerString);
		if (!username.equals(tokenUser)) {
			log.warn("username (" + username + ") does not equal subject of token (" + tokenUser + ")");
			return ResponseEntity.status(403).build();
		}

		User user = userRepository.findByUsername(username);
		if(user == null) {
			log.warn("username (" + username + ") not found.");
			return ResponseEntity.notFound().build();
		}

		// can't submit an order without a cart
		if (user.getCart() == null) {
			log.warn("User " + username + " tried to submit an order without a cart.");
			return ResponseEntity.badRequest().build();
		}

		UserOrder order = UserOrder.createFromCart(user.getCart());
		orderRepository.save(order);
		log.info("User " + username + " successfully placed an order.");
		return ResponseEntity.ok(order);
	}
	
	@GetMapping("/history/{username}")
	public ResponseEntity<List<UserOrder>> getOrdersForUser(@PathVariable String username,
															@RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// username in the request must equal the user in the token payload, otherwise unauthorized.
		String tokenUser = tokenUtils.getUserFromTokenHeader(headerString);
		if (!username.equals(tokenUser)) {
			log.warn("username (" + username + ") does not equal subject of token (" + tokenUser + ")");
			return ResponseEntity.status(403).build();
		}

		User user = userRepository.findByUsername(username);
		if(user == null) {
			log.warn("username (" + username + ") not found.");
			return ResponseEntity.notFound().build();
		}
		log.info("user " + username + " successfully viewed order history");
		return ResponseEntity.ok(orderRepository.findByUser(user));
	}
}
