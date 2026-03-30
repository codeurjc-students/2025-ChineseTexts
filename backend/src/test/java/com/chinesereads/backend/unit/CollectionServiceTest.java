package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.CollectionRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.CollectionService;
import com.chinesereads.backend.dto.CollectionDTO;
import com.chinesereads.backend.dto.CollectionMapper;
import com.chinesereads.backend.dto.CollectionMapperImpl;

public class CollectionServiceTest {

    private CollectionRepository collectionRepository;
    private UserRepository userRepository;
    private CollectionService collectionService;
    private CollectionMapper collectionMapper;

    @BeforeEach
    public void setUp() {
        collectionRepository = mock(CollectionRepository.class);
        userRepository = mock(UserRepository.class);
        collectionMapper = new CollectionMapperImpl();
        collectionService = new CollectionService();

        injectField(collectionService, "collectionRepository", collectionRepository);
        injectField(collectionService, "userRepository", userRepository);
        injectField(collectionService, "collectionMapper", collectionMapper);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Test unitario 1: Cuando se crea una colección, se guarda con el usuario correcto
    @Test
    @DisplayName("When a collection is created, it should be saved with the correct user")
    public void testCreateCollection() {
        User user = new User("u@u.com", "User", "encoded", "es", "USER");
        Collection saved = new Collection("My Collection", LocalDate.now(), user, null);

        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(user));
        when(collectionRepository.save(any(Collection.class))).thenReturn(saved);

        CollectionDTO result = collectionService.createCollection("My Collection", "u@u.com");

        assertNotNull(result);
        assertEquals("My Collection", result.title());
        verify(collectionRepository, times(1)).save(any(Collection.class));
    }

    // Test unitario 2: Cuando se obtienen las colecciones de un usuario, se devuelven correctamente
    @Test
    @DisplayName("When collections are fetched for a user, the correct list is returned")
    public void testGetUserCollections() {
        User user = new User("u@u.com", "User", "encoded", "es", "USER");
        Collection col1 = new Collection("HSK1 Words", LocalDate.now(), user, null);
        Collection col2 = new Collection("HSK2 Words", LocalDate.now(), user, null);

        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(user));
        when(collectionRepository.findByUser(user)).thenReturn(List.of(col1, col2));

        List<CollectionDTO> result = collectionService.getUserCollections("u@u.com");

        assertEquals(2, result.size());
        assertEquals("HSK1 Words", result.get(0).title());
        assertEquals("HSK2 Words", result.get(1).title());
    }

    // Test unitario 3: Cuando se borra una colección de otro usuario, lanza excepción
    @Test
    @DisplayName("When a collection belonging to another user is deleted, an exception is thrown")
    public void testDeleteCollectionUnauthorized() {
        User owner = new User("owner@u.com", "Owner", "encoded", "es", "USER");
        Collection col = new Collection("Col", LocalDate.now(), owner, null);

        when(collectionRepository.findById(1L)).thenReturn(Optional.of(col));

        assertThrows(RuntimeException.class, () ->
                collectionService.deleteCollection(1L, "other@u.com"));

        verify(collectionRepository, times(0)).deleteById(any());
    }
}