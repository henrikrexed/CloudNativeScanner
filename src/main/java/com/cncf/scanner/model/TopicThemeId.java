package com.cncf.scanner.model;

import java.io.Serializable;
import java.util.Objects;

public class TopicThemeId implements Serializable {
    
    private Long topic;
    private Long theme;
    
    public TopicThemeId() {}
    
    public TopicThemeId(Long topic, Long theme) {
        this.topic = topic;
        this.theme = theme;
    }
    
    public Long getTopic() {
        return topic;
    }
    
    public void setTopic(Long topic) {
        this.topic = topic;
    }
    
    public Long getTheme() {
        return theme;
    }
    
    public void setTheme(Long theme) {
        this.theme = theme;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicThemeId that = (TopicThemeId) o;
        return Objects.equals(topic, that.topic) && Objects.equals(theme, that.theme);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(topic, theme);
    }
}

