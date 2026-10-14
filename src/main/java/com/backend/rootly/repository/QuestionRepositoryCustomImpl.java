package com.backend.rootly.repository;

import com.backend.rootly.entity.Question;
import com.backend.rootly.enums.QuestionCategory;
import com.backend.rootly.enums.QuestionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class QuestionRepositoryCustomImpl implements QuestionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Question> search(String searchText, QuestionCategory category,
                                 boolean unanswered, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("status").is(QuestionStatus.OPEN));
        if (category != null) {
            filters.add(Criteria.where("category").is(category));
        }
        if (unanswered) {
            filters.add(Criteria.where("commentCount").is(0));
        }
        if (searchText != null && !searchText.isBlank()) {
            String normalizedSearch = searchText.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(normalizedSearch), Pattern.CASE_INSENSITIVE);
            List<Criteria> searchableFields = new ArrayList<>(List.of(
                    Criteria.where("title").regex(pattern),
                    Criteria.where("body").regex(pattern),
                    Criteria.where("location").regex(pattern)
            ));
            String lowerCaseSearch = normalizedSearch.toLowerCase(Locale.ROOT);
            Arrays.stream(QuestionCategory.values())
                    .filter(value -> value.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerCaseSearch)
                            || value.getSlug().contains(lowerCaseSearch)
                            || value.name().toLowerCase(Locale.ROOT).contains(lowerCaseSearch))
                    .forEach(value -> searchableFields.add(Criteria.where("category").is(value)));
            filters.add(new Criteria().orOperator(searchableFields.toArray(Criteria[]::new)));
        }

        Criteria criteria = new Criteria().andOperator(filters.toArray(Criteria[]::new));
        long total = mongoTemplate.count(Query.query(criteria), Question.class);
        List<Question> questions = mongoTemplate.find(Query.query(criteria).with(pageable), Question.class);
        return new PageImpl<>(questions, pageable, total);
    }
}
