package com.schoolbell.model;

import java.io.Serializable;

public record RadioStation(
    Integer id,
    String name,
    String url,
    String faviconUrl
) implements Serializable {}
