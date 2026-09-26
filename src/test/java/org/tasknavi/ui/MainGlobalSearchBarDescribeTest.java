package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainGlobalSearchBarDescribeTest {

    @Test
    void describeFiltersShowsNoneWhenEmpty() {
        String text = MainGlobalSearchBar.describeFilters(null, AppMessages.FILTER_ALL);
        assertFalse(text.isBlank());
        assertTrue(text.toLowerCase().contains("all")
                || text.contains("全件")
                || text.contains("なし")
                || text.contains("No filter"));
    }

    @Test
    void describeFiltersIncludesKeywordAndStatus() {
        String text = MainGlobalSearchBar.describeFilters("design", Task.STATUS_IN_PROGRESS);
        assertTrue(text.contains("design"));
        assertTrue(text.contains(AppMessages.statusDisplay(Task.STATUS_IN_PROGRESS))
                || text.contains("IN_PROGRESS")
                || text.contains("進行"));
    }
}
