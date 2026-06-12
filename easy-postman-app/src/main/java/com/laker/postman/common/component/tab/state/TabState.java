package com.laker.postman.common.component.tab.state;

import lombok.Getter;

@Getter
public class TabState {

    private final String title;
    private TabIndicatorType indicatorType = TabIndicatorType.NONE;
    private IndicatorPosition indicatorPosition = IndicatorPosition.AFTER_TEXT;
    private int number = 0;

    public TabState(String title) {
        this.title = title;
    }

    public void setIndicator(TabIndicatorType type) {
        this.indicatorType = type;
    }

    public void setIndicatorPosition(IndicatorPosition position) {
        this.indicatorPosition = position;
    }

    public void setNumber(int number) {
        this.indicatorType = TabIndicatorType.NUMBER;
        this.number = number;
    }

    public void clearIndicator() {
        this.indicatorType = TabIndicatorType.NONE;
    }
    
    public static TabState getBeforeTextState(String title){
    	TabState state = new TabState(title);
    	state.setIndicatorPosition(IndicatorPosition.BEFORE_TEXT);
    	return state;
    }
}