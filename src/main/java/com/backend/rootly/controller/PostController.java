package com.backend.rootly.controller;

import com.backend.rootly.dto.request.CulturalPostCreateRequestDTO;
import com.backend.rootly.dto.request.CulturalPostEditRequestDTO;
import com.backend.rootly.service.CulturalPostService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PostController {

    private final CulturalPostService culturalPostService;

    /**
     * Creates a new cultural post in the system.
     * Maps the incoming DTO and sets the current logged-in user as the author.
     */
    @PostMapping(value = {EndPoint.POSTS, "/posts"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createPost(
            @Validated @RequestBody CulturalPostCreateRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Create Post request");
        }
        return culturalPostService.createPost(requestDTO, locale);
    }

    /**
     * Edits an existing cultural post.
     * Ensures the post exists and the requester is the original author before updating.
     */
    @PutMapping(value = {EndPoint.POST_DETAIL, "/posts/{postId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> editPost(
            @PathVariable String postId,
            @Validated @RequestBody CulturalPostEditRequestDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Edit Post request for postId: {}", postId);
        }
        return culturalPostService.editPost(postId, requestDTO, locale);
    }

    /**
     * Deletes a specific cultural post by ID.
     * Verifies that the requester is the owner of the post before deletion.
     */
    @DeleteMapping(value = {EndPoint.POST_DETAIL, "/posts/{postId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deletePost(
            @PathVariable String postId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Delete Post request for postId: {}", postId);
        }
        return culturalPostService.deletePost(postId, locale);
    }

    /**
     * Saves a post to the logged-in user's profile for later viewing.
     * Appends the post ID to the user's saved list in MongoDB.
     */
    @PostMapping(value = {EndPoint.POST_DETAIL + "/save", "/posts/{postId}/save"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> savePostToProfile(
            @PathVariable String postId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Save Post request for postId: {}", postId);
        }
        return culturalPostService.savePostToProfile(postId, locale);
    }

    /**
     * Retrieves all posts saved by the currently logged-in user.
     * Populates the "Saved Posts" tab in the user's profile.
     */
    @GetMapping(value = {"/v1/posts/saved", "/posts/saved"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSavedPosts(
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get Saved Posts request");
        }
        return culturalPostService.getSavedPosts(locale);
    }

    /**
     * Fetches details of a single post by its ID.
     */
    @GetMapping(value = {EndPoint.POST_DETAIL, "/posts/{postId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getPostById(
            @PathVariable String postId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get Post request for postId: {}", postId);
        }
        return culturalPostService.getPostById(postId, locale);
    }

    /**
     * Fetches all posts in the system.
     * Used primarily to populate the Home Feed with dynamic content.
     */
    @GetMapping(value = {EndPoint.POSTS, "/posts"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getAllPosts(
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get All Posts request");
        }
        return culturalPostService.getAllPosts(locale);
    }
    /**
     * Fetches only the posts created by the currently logged-in user.
     * Populates the user's Profile screen with their own stories.
     */
    @GetMapping(value = {"/v1/posts/my", "/posts/my"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getMyPosts(
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Get My Posts request");
        }
        return culturalPostService.getMyPosts(locale);
    }

    @PostMapping(value = {"/v1/posts/{postId}/like", "/posts/{postId}/like"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> likePost(
            @PathVariable String postId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Like Post request for postId: {}", postId);
        }
        return culturalPostService.likePost(postId, locale);
    }

    @PostMapping(value = {"/v1/posts/{postId}/comment", "/posts/{postId}/comment"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addComment(
            @PathVariable String postId,
            @RequestBody java.util.Map<String, String> body,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("Received Add Comment request for postId: {}", postId);
        }
        String text = body.get("text");
        return culturalPostService.addComment(postId, text, locale);
    }
}
