package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Filter 1: Rejects topics with URLs already in the database.
 * Order: 10 (very cheap — single DB lookup).
 */
@Component
public class DuplicateUrlFilter implements TopicFilter {

    private final JdbcTemplate jdbcTemplate;

    public DuplicateUrlFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() {
        return "duplicate-url";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topics WHERE url = ? AND id != ? AND pipeline_stage = 'analyzed'",
                Integer.class,
                context.getUrl(), context.getTopicId());

        if (count != null && count > 0) {
            return FilterResult.fail(getName(), "Duplicate URL already analyzed");
        }
        return FilterResult.pass(getName());
    }
}
