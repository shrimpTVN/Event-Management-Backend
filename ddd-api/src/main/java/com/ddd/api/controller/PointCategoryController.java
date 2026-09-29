package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/point-categories")
@RequiredArgsConstructor
public class PointCategoryController {

    private final PointCategoryService pointCategoryService;

    @PostMapping("/admin")
    public BaseResponse<PointCategoryResponse> createPointCategory(@RequestBody PointCategoryRequest request) {
        // Implement the logic to create a new point category
        // For example, call a service method to handle the creation
        PointCategoryResponse response = pointCategoryService.createPointCategory(request);
        return BaseResponse.ok();
    }
}
