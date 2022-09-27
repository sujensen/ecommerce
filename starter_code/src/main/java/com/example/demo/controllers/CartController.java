package com.example.demo.controllers;

import java.util.Optional;
import java.util.stream.IntStream;

import com.example.demo.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;

@RestController
@RequestMapping("/api/cart")
public class CartController {

	private static final Logger log = LoggerFactory.getLogger(CartController.class);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private TokenUtils tokenUtils;

	@PostMapping("/addToCart")
	public ResponseEntity<Cart> addTocart(@RequestBody ModifyCartRequest request,
										  @RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// username in the request must equal the user in the token payload, otherwise unauthorized.
		String tokenUser = tokenUtils.getUserFromTokenHeader(headerString);
		if (!request.getUsername().equals(tokenUser)) {
			log.warn("username (" + request.getUsername() + ") does not equal subject of token (" + tokenUser + ")");
			return ResponseEntity.status(403).build();
		}

		User user = userRepository.findByUsername(request.getUsername());
		if(user == null) {
			log.warn("username (" + request.getUsername() + ") not found in user repo.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Optional<Item> item = itemRepository.findById(request.getItemId());
		if(!item.isPresent()) {
			log.warn("item id " + request.getItemId() + " was not found in repo; not adding to the cart.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		// Can't add an item to a null cart
		if (user.getCart() == null) {
			Cart newCart = new Cart();
			newCart.setUser(user);
			user.setCart(newCart);
			user = userRepository.saveAndFlush(user);
		}

		Cart cart = user.getCart();
		IntStream.range(0, request.getQuantity()).forEach(i -> cart.addItem(item.get()));
		cartRepository.save(cart);
		return ResponseEntity.ok(cart);
	}
	
	@PostMapping("/removeFromCart")
	public ResponseEntity<Cart> removeFromcart(@RequestBody ModifyCartRequest request,
											   @RequestHeader(SecurityConstants.HEADER_STRING) String headerString) {

		// username in the request must equal the user in the token payload, otherwise unauthorized.
		String tokenUser = tokenUtils.getUserFromTokenHeader(headerString);
		if (!request.getUsername().equals(tokenUser)) {
			log.warn("username (" + request.getUsername() + ") does not equal subject of token (" + tokenUser + ")");
			return ResponseEntity.status(403).build();
		}

		User user = userRepository.findByUsername(request.getUsername());
		if(user == null) {
			log.warn("username (" + request.getUsername() + ") not found in user repo.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Optional<Item> item = itemRepository.findById(request.getItemId());
		if(!item.isPresent()) {
			log.warn("item id " + request.getItemId() + " was not found in repo; not able to remove it from the cart.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		// can't remove items from a null cart
		if (user.getCart() == null) {
			Cart newCart = new Cart();
			newCart.setUser(user);
			user.setCart(newCart);
			user = userRepository.saveAndFlush(user);
		}

		Cart cart = user.getCart();
		if (cart.getItems() != null && request.getQuantity() <= cart.getItems().size()) {
			IntStream.range(0, request.getQuantity())
					.forEach(i -> cart.removeItem(item.get()));
		}
		cartRepository.save(cart);
		return ResponseEntity.ok(cart);
	}
		
}
