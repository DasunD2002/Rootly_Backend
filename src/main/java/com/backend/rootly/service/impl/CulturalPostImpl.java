package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.CulturalPostCreateRequestDTO;
import com.backend.rootly.dto.request.CulturalPostEditRequestDTO;
import com.backend.rootly.dto.response.CulturalPostResponseDTO;
import com.backend.rootly.entity.CulturalPost;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.PostRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.CulturalPostService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class CulturalPostImpl implements CulturalPostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ResponseGenerator responseGenerator;
    private final ModelMapper modelMapper;

    private String getLoggedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserReg user) {
            return user.getId();
        }
        return null;
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createPost(CulturalPostCreateRequestDTO requestDTO, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(requestDTO, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        CulturalPost post = new CulturalPost();
        post.setUserId(userId);
        post.setTitle(requestDTO.title());
        post.setStory(requestDTO.story());
        post.setMedia(requestDTO.media());
        post.setCategory(requestDTO.category());
        post.setLocation(requestDTO.location());
        post.setTags(requestDTO.tags());
        post.setVisibility(requestDTO.visibility());
        post.setProofs(requestDTO.proofs());
        post.setLikeCount(0);
        post.setShareCount(0);
        post.setCommentCount(0);
        post.setComments(new java.util.ArrayList<>());
        post.setDisableComments(requestDTO.disableComments() != null ? requestDTO.disableComments() : false);
        post.setCreatedAt(java.time.Instant.now());

        CulturalPost savedPost = postRepository.save(post);

        if (log.isInfoEnabled()) {
            log.info("CulturalPost created successfully with id: {}", savedPost.getId());
        }

        CulturalPostResponseDTO responseDTO = mapToResponseDTO(savedPost);
        return responseGenerator.generateSuccessResponse(requestDTO, HttpStatus.CREATED,
                ResponseCode.POST_CREATE_SUCCESS, MessageConstant.POST_CREATE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> editPost(String postId, CulturalPostEditRequestDTO requestDTO, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(requestDTO, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(requestDTO, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        CulturalPost post = optionalPost.get();
        if (!userId.equals(post.getUserId())) {
            return responseGenerator.generateErrorResponse(requestDTO, HttpStatus.FORBIDDEN,
                    ResponseCode.POST_FORBIDDEN, MessageConstant.POST_FORBIDDEN, locale);
        }

        post.setTitle(requestDTO.title());
        post.setStory(requestDTO.story());
        post.setMedia(requestDTO.media());
        post.setCategory(requestDTO.category());
        post.setLocation(requestDTO.location());
        post.setTags(requestDTO.tags());
        post.setVisibility(requestDTO.visibility());
        post.setProofs(requestDTO.proofs());
        post.setDisableComments(requestDTO.disableComments() != null ? requestDTO.disableComments() : false);

        CulturalPost updatedPost = postRepository.save(post);

        if (log.isInfoEnabled()) {
            log.info("CulturalPost updated successfully with id: {}", updatedPost.getId());
        }

        CulturalPostResponseDTO responseDTO = mapToResponseDTO(updatedPost);
        return responseGenerator.generateSuccessResponse(requestDTO, HttpStatus.OK,
                ResponseCode.POST_UPDATE_SUCCESS, MessageConstant.POST_UPDATE_SUCCESS, locale, responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> deletePost(String postId, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        CulturalPost post = optionalPost.get();
        if (!userId.equals(post.getUserId())) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.FORBIDDEN,
                    ResponseCode.POST_FORBIDDEN, MessageConstant.POST_FORBIDDEN, locale);
        }

        postRepository.deleteById(postId);

        if (log.isInfoEnabled()) {
            log.info("CulturalPost deleted successfully with id: {}", postId);
        }

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.POST_DELETE_SUCCESS, MessageConstant.POST_DELETE_SUCCESS, locale, null);
    }

    @Override
    public ResponseEntity<Object> getPostById(String postId, Locale locale) {
        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        CulturalPostResponseDTO responseDTO = mapToResponseDTO(optionalPost.get());
        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.POST_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, locale, responseDTO);
    }

    @Override
    public ResponseEntity<Object> getAllPosts(Locale locale) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "likeCount", "commentCount");
        java.util.List<CulturalPost> posts = postRepository.findAll(sort);

        java.util.List<CulturalPostResponseDTO> responseDTOs = posts.stream()
                .map(this::mapToResponseDTO)
                .toList();

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.POST_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, locale, responseDTOs);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> savePostToProfile(String postId, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<UserReg> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        UserReg user = optionalUser.get();
        if (user.getSavedPosts() == null) {
            user.setSavedPosts(new java.util.ArrayList<>());
        }
        
        if (!user.getSavedPosts().contains(postId)) {
            user.getSavedPosts().add(postId);
            userRepository.save(user);
        }

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_SAVE, locale, null);
    }

    @Override
    public ResponseEntity<Object> getSavedPosts(Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<UserReg> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        UserReg user = optionalUser.get();
        java.util.List<String> savedPostIds = user.getSavedPosts() != null ? user.getSavedPosts() : new java.util.ArrayList<>();
        
        Iterable<CulturalPost> savedPosts = postRepository.findAllById(savedPostIds);
        java.util.List<CulturalPostResponseDTO> responseDTOs = new java.util.ArrayList<>();
        savedPosts.forEach(post -> responseDTOs.add(mapToResponseDTO(post)));

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.POST_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, locale, responseDTOs);
    }

    @Override
    public ResponseEntity<Object> getMyPosts(Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "id");
        java.util.List<CulturalPost> posts = postRepository.findByUserId(userId, sort);

        java.util.List<CulturalPostResponseDTO> responseDTOs = posts.stream()
                .map(this::mapToResponseDTO)
                .toList();

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.POST_GET_SUCCESS, MessageConstant.SUCCESSFULLY_GET, locale, responseDTOs);
    }
    @Override
    @Transactional
    public ResponseEntity<Object> likePost(String postId, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        CulturalPost post = optionalPost.get();
        if (post.getLikedBy() == null) {
            post.setLikedBy(new java.util.ArrayList<>());
        }

        String message;
        if (post.getLikedBy().contains(userId)) {
            post.getLikedBy().remove(userId);
            post.setLikeCount(Math.max(0, (post.getLikeCount() != null ? post.getLikeCount() : 1) - 1));
            message = "Successfully unliked post";
        } else {
            post.getLikedBy().add(userId);
            post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + 1);
            message = "Successfully liked post";
        }
        
        postRepository.save(post);

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, message, locale, null);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> addComment(String postId, String text, Locale locale) {
        String userId = getLoggedUserId();
        if (userId == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.UNAUTHORIZED,
                    ResponseCode.UNAUTHORIZED, MessageConstant.UNAUTHORIZED, locale);
        }

        Optional<CulturalPost> optionalPost = postRepository.findById(postId);
        if (optionalPost.isEmpty()) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.POST_NOT_FOUND, MessageConstant.POST_NOT_FOUND, locale);
        }

        CulturalPost post = optionalPost.get();
        if (Boolean.TRUE.equals(post.getDisableComments())) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.FORBIDDEN,
                    ResponseCode.RSP_ERROR, "Comments are disabled", locale);
        }

        com.backend.rootly.entity.PostComment comment = new com.backend.rootly.entity.PostComment();
        comment.setUserId(userId);
        comment.setText(text);
        comment.setCreatedAt(java.time.Instant.now());

        if (post.getComments() == null) {
            post.setComments(new java.util.ArrayList<>());
        }
        post.getComments().add(comment);
        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        
        postRepository.save(post);

        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, "Successfully added comment", locale, null);
    }

    private CulturalPostResponseDTO mapToResponseDTO(CulturalPost post) {
        CulturalPostResponseDTO dto = modelMapper.map(post, CulturalPostResponseDTO.class);
        if (post.getUserId() != null) {
            userRepository.findById(post.getUserId()).ifPresent(user -> {
                dto.setAuthorName(user.getName());
                dto.setAuthorPhoto(user.getPhotoUrl());
                if (user.getName() != null) {
                    dto.setAuthorHandle("@" + user.getName().replaceAll("\\s+", "").toLowerCase());
                }
            });
        }
        
        String currentUserId = getLoggedUserId();
        if (currentUserId != null && post.getLikedBy() != null) {
            dto.setIsLiked(post.getLikedBy().contains(currentUserId));
        } else {
            dto.setIsLiked(false);
        }

        if (dto.getComments() != null) {
            for (com.backend.rootly.dto.response.PostCommentDTO comment : dto.getComments()) {
                if (comment.getUserId() != null) {
                    userRepository.findById(comment.getUserId()).ifPresent(user -> {
                        comment.setAuthorName(user.getName() != null ? user.getName() : "Unknown");
                        comment.setAuthorPhoto(user.getPhotoUrl());
                    });
                }
            }
        }
        return dto;
    }
}
