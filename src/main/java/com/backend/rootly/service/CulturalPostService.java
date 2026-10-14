package com.backend.rootly.service;

import com.backend.rootly.dto.request.CulturalPostCreateRequestDTO;
import com.backend.rootly.dto.request.CulturalPostEditRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface CulturalPostService {

    /**
     * Validates and persists a new cultural post to the database.
     */
    ResponseEntity<Object> createPost(CulturalPostCreateRequestDTO requestDTO, Locale locale);

    /**
     * Updates an existing post with new data provided by the original author.
     */
    ResponseEntity<Object> editPost(String postId, CulturalPostEditRequestDTO requestDTO, Locale locale);

    /**
     * Removes a post from the database based on ID, provided the requester owns it.
     */
    ResponseEntity<Object> deletePost(String postId, Locale locale);

    /**
     * Retrieves the details of a single post for viewing.
     */
    ResponseEntity<Object> getPostById(String postId, Locale locale);

    /**
     * Retrieves all cultural posts sorted by popularity/engagement metrics.
     */
    ResponseEntity<Object> getAllPosts(Locale locale);
    /**
     * Appends a post to the logged-in user's list of saved items.
     */
    ResponseEntity<Object> savePostToProfile(String postId, Locale locale);

    /**
     * Fetches all posts that the logged-in user has saved for later viewing.
     */
    ResponseEntity<Object> getSavedPosts(Locale locale);
    
    /**
     * Fetches all posts originally authored by the currently logged-in user.
     */
    ResponseEntity<Object> getMyPosts(Locale locale);

    ResponseEntity<Object> likePost(String postId, Locale locale);

    ResponseEntity<Object> addComment(String postId, String text, Locale locale);
}
