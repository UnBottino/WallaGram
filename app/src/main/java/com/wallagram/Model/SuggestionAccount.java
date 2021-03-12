package com.wallagram.Model;

public class SuggestionAccount {
    private final String suggestionName;
    private final String suggestionImgUrl;

    public SuggestionAccount(String suggestionName, String suggestionImgUrl) {
        this.suggestionName = suggestionName;
        this.suggestionImgUrl = suggestionImgUrl;
    }

    public String getSuggestionName() {
        return suggestionName;
    }

    public String getSuggestionImgUrl() {
        return suggestionImgUrl;
    }
}
