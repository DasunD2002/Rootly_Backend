package com.backend.rootly.repository;

import com.backend.rootly.entity.Question;
import com.backend.rootly.enums.QuestionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionRepositoryCustom {

    Page<Question> search(String query, QuestionCategory category, boolean unanswered, Pageable pageable);
}
