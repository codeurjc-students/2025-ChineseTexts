package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.CollectionRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.dto.CollectionDTO;
import com.chinesereads.backend.dto.CollectionMapper;
import com.chinesereads.backend.dto.FlashcardDTO;
import com.chinesereads.backend.dto.FlashcardMapper;

@Service
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private FlashcardMapper flashcardMapper;

    public List<CollectionDTO> getUserCollections(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return collectionMapper.toDTO(collectionRepository.findByUser(user));
    }

    public CollectionDTO createCollection(String title, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Collection collection = new Collection(title, LocalDate.now(), user, null);
        return collectionMapper.toDTO(collectionRepository.save(collection));
    }

    public void deleteCollection(Long id, String email) {
        Collection collection = collectionRepository.findById(id).orElseThrow();
        if (!collection.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        collectionRepository.deleteById(id);
    }

    public List<FlashcardDTO> getCollectionFlashcards(Long collectionId, String email) {
        Collection collection = collectionRepository.findById(collectionId).orElseThrow();
        if (!collection.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        return flashcardMapper.toDTO(collection.getFlashcards());
    }
}