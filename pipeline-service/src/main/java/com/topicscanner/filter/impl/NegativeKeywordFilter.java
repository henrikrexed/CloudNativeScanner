package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import org.springframework.stereotype.Component;

/**
 * Filter 2: Rejects topics whose title or content matches negative keywords.
 * Order: 20 (very cheap — string matching only).
 */
@Component
public class NegativeKeywordFilter implements TopicFilter {

    @Override
    public String getName() {
        return "negative-keyword";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        if (context.getNegativeKeywords().isEmpty()) {
            return FilterResult.pass(getName());
        }

        String fullText = context.getFullText().toLowerCase();

        for (String keyword : context.getNegativeKeywords()) {
            if (fullText.contains(keyword.toLowerCase())) {
                return FilterResult.fail(getName(),
                        "Matched negative keyword: " + keyword);
            }
        }

        return FilterResult.pass(getName());
    }
}
