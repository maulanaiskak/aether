package io.aether.api.dto;

import java.util.List;

public record PageDto<T>(List<T> content, int size) {}
