/*
 * Copyright (c) 2013-2021 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/chriswhocodes/jitwatch/wiki
 */

package com.chrisnewland.jitwatch.util;

import javafx.scene.control.ListCell;

import java.util.Locale;

public class LocaleCell extends ListCell<Locale> {
    @Override
    public void updateItem(Locale locale, boolean empty) {
        super.updateItem(locale, empty);
        if (empty) {
            setText(null);
        } else {
            setText(locale.getDisplayLanguage(locale));
        }
    }
}