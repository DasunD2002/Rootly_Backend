package com.backend.rootly.service;

import com.backend.rootly.domain.CreateQuestionCommentDomain;
import com.backend.rootly.domain.CreateQuestionDomain;
import com.backend.rootly.domain.ForumVoteDomain;
import com.backend.rootly.domain.QuestionSearchDomain;
import com.backend.rootly.entity.UserReg;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface QuestionService {

    ResponseEntity<Object> getQuestions(QuestionSearchDomain request, UserReg viewer, Locale locale);

    ResponseEntity<Object> getQuestion(String questionId, String commentSort, UserReg viewer, Locale locale);

    ResponseEntity<Object> createQuestion(CreateQuestionDomain request, UserReg author, Locale locale);

    ResponseEntity<Object> createComment(String questionId, CreateQuestionCommentDomain request,
                                         UserReg author, Locale locale);

    ResponseEntity<Object> voteQuestion(String questionId, ForumVoteDomain request,
                                        UserReg voter, Locale locale);

    ResponseEntity<Object> voteComment(String commentId, ForumVoteDomain request,
                                       UserReg voter, Locale locale);

    ResponseEntity<Object> bookmarkQuestion(String questionId, UserReg user, Locale locale);

    ResponseEntity<Object> removeQuestionBookmark(String questionId, UserReg user, Locale locale);
}
