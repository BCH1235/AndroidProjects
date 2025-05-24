package com.am.mytodolistapp.data;

import androidx.room.TypeConverter;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

public class DateConverter { //LocalDate를 String형식으로 변환

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date != null ? date.format(formatter) : null;
    }

    @TypeConverter
    public static LocalDate toLocalDate(String dateString) {
        return dateString != null ? LocalDate.parse(dateString, formatter) : null;
    }
}
