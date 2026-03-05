package com.topicscanner.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterPipelineTest {

    @Test
    void execute_allFiltersPass() {
        TopicFilter filter1 = new StubFilter("a", 10, true);
        TopicFilter filter2 = new StubFilter("b", 20, true);

        FilterPipeline pipeline = new FilterPipeline(List.of(filter2, filter1)); // out of order

        TopicContext context = createContext("Test Topic", "Some content here");
        FilterPipeline.PipelineResult result = pipeline.execute(context);

        assertTrue(result.passed());
        assertEquals(2, result.filterResults().size());
        assertEquals("a", result.filterResults().get(0).filterName()); // sorted by order
    }

    @Test
    void execute_shortCircuitsOnFailure() {
        TopicFilter pass = new StubFilter("pass", 10, true);
        TopicFilter fail = new StubFilter("fail", 20, false);
        TopicFilter neverReached = new StubFilter("never", 30, true);

        FilterPipeline pipeline = new FilterPipeline(List.of(pass, fail, neverReached));
        TopicContext context = createContext("Test", "Content");
        FilterPipeline.PipelineResult result = pipeline.execute(context);

        assertFalse(result.passed());
        assertEquals(2, result.filterResults().size()); // pass + fail, never not reached
        assertFalse(result.filterResults().get(1).passed());
    }

    @Test
    void execute_handlesFilterException() {
        TopicFilter throwing = new TopicFilter() {
            @Override public String getName() { return "thrower"; }
            @Override public int getOrder() { return 10; }
            @Override public FilterResult apply(TopicContext ctx) {
                throw new RuntimeException("boom");
            }
        };

        FilterPipeline pipeline = new FilterPipeline(List.of(throwing));
        TopicContext context = createContext("Test", "Content");
        FilterPipeline.PipelineResult result = pipeline.execute(context);

        assertFalse(result.passed());
        assertEquals(1, result.filterResults().size());
        assertTrue(result.filterResults().get(0).reason().contains("boom"));
    }

    @Test
    void execute_emptyFilters_passes() {
        FilterPipeline pipeline = new FilterPipeline(List.of());
        TopicContext context = createContext("Test", "Content");
        FilterPipeline.PipelineResult result = pipeline.execute(context);

        assertTrue(result.passed());
        assertTrue(result.filterResults().isEmpty());
    }

    @Test
    void pipelineResult_getResult() {
        TopicFilter filter = new StubFilter("test-filter", 10, true);
        FilterPipeline pipeline = new FilterPipeline(List.of(filter));
        FilterPipeline.PipelineResult result = pipeline.execute(createContext("T", "C"));

        assertNotNull(result.getResult("test-filter"));
        assertNull(result.getResult("nonexistent"));
    }

    private TopicContext createContext(String title, String content) {
        return new TopicContext(1L, title, "https://example.com", "medium",
                content, "Tech", List.of("kubernetes"), List.of());
    }

    static class StubFilter implements TopicFilter {
        private final String name;
        private final int order;
        private final boolean passes;

        StubFilter(String name, int order, boolean passes) {
            this.name = name;
            this.order = order;
            this.passes = passes;
        }

        @Override public String getName() { return name; }
        @Override public int getOrder() { return order; }
        @Override public FilterResult apply(TopicContext ctx) {
            return passes ? FilterResult.pass(name) : FilterResult.fail(name, "rejected");
        }
    }
}
