package com.topicscanner.filter.impl;

import com.topicscanner.filter.FilterResult;
import com.topicscanner.filter.TopicContext;
import com.topicscanner.filter.TopicFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filter 4: Rejects topics with insufficient content.
 * Order: 40 (cheap — length check only).
 */
@Component
public class ContentLengthFilter implements TopicFilter {

    private final int minContentLength;

    public ContentLengthFilter(
            @Value("${topicscanner.pipeline.min-content-length:200}") int minContentLength) {
        this.minContentLength = minContentLength;
    }

    @Override
    public String getName() {
        return "content-length";
    }

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public FilterResult apply(TopicContext context) {
        String content = context.getExtractedContent();
        int length = content != null ? content.length() : 0;

        if (length < minContentLength) {
            return FilterResult.fail(getName(),
                    String.format("Content too short (%d chars, minimum %d)", length, minContentLength));
        }

        return FilterResult.pass(getName(), Math.min(1.0, (double) length / 1000));
    }
}
