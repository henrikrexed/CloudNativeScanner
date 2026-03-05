package com.topicscanner.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes the ordered filter pipeline on a topic.
 * Filters are sorted by order (cheapest first) and executed sequentially.
 * Short-circuits on first failure.
 */
@Component
public class FilterPipeline {

    private static final Logger logger = LoggerFactory.getLogger(FilterPipeline.class);

    private final List<TopicFilter> filters;

    public FilterPipeline(List<TopicFilter> filters) {
        this.filters = filters.stream()
                .sorted(Comparator.comparingInt(TopicFilter::getOrder))
                .toList();
        logger.info("FilterPipeline initialized with {} filters: {}", this.filters.size(),
                this.filters.stream().map(TopicFilter::getName).toList());
    }

    /**
     * Run all filters on the topic context.
     *
     * @return list of filter results (all passed, or all up to and including the first failure)
     */
    public PipelineResult execute(TopicContext context) {
        List<FilterResult> results = new ArrayList<>();

        for (TopicFilter filter : filters) {
            try {
                FilterResult result = filter.apply(context);
                results.add(result);

                if (!result.passed()) {
                    logger.debug("Topic {} rejected by {}: {}",
                            context.getTopicId(), filter.getName(), result.reason());
                    return new PipelineResult(false, results);
                }

                logger.trace("Topic {} passed {}", context.getTopicId(), filter.getName());
            } catch (Exception e) {
                logger.warn("Filter {} threw exception for topic {}: {}",
                        filter.getName(), context.getTopicId(), e.getMessage());
                results.add(FilterResult.fail(filter.getName(), "Error: " + e.getMessage()));
                return new PipelineResult(false, results);
            }
        }

        return new PipelineResult(true, results);
    }

    public record PipelineResult(boolean passed, List<FilterResult> filterResults) {
        public FilterResult getResult(String filterName) {
            return filterResults.stream()
                    .filter(r -> r.filterName().equals(filterName))
                    .findFirst()
                    .orElse(null);
        }
    }
}
