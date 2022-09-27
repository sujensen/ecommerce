package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemControllerTest {

    private ItemController itemController;

    private ItemRepository itemRepo = mock(ItemRepository.class);

    @Before
    public void setUp() {

        /*
        We inject mock objects into fields of the ItemController (the item repo), because
        these unit tests don't need to test the repo itself.
         */
        itemController = new ItemController();
        TestUtils.injectObjects(itemController, "itemRepository", itemRepo);
    }

    @Test
    public void get_items_happy_path() {

        // stub for ItemController's mock itemRepo
        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("item1");
        item1.setPrice(BigDecimal.valueOf(1.00));
        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("item2");
        item2.setPrice(BigDecimal.valueOf(2.00));

        when(itemRepo.findAll()).thenReturn(Arrays.asList(item1, item2));

        ResponseEntity<List<Item>> response = itemController.getItems();
        assertEquals(200, response.getStatusCodeValue());
        List<Item> items = response.getBody();
        assertEquals(2, items.size());
        assertEquals(BigDecimal.valueOf(1.00), items.get(0).getPrice());
        assertEquals("item2", items.get(1).getName());
    }

    @Test
    public void get_item_by_id_happy_path() {

        // stub for ItemController's mock itemRepo
        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("item1");
        item1.setPrice(BigDecimal.valueOf(1.00));
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item1));

        ResponseEntity<Item> response = itemController.getItemById(1L);
        assertEquals(200, response.getStatusCodeValue());
        Item i = response.getBody();
    }

    @Test
    public void get_item_by_name_not_found() {

        // there are no items in our mock repo

        ResponseEntity<List<Item>> response = itemController.getItemsByName("foobar");
        // Not Found
        assertEquals(404, response.getStatusCodeValue());
    }
}
