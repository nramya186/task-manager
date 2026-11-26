package service;

import model.Task;

import java.util.List;

public record TaskPage(List<Task> items, int page, int pageSize, int totalItems, int totalPages) { }