
package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suggest")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping("/contacts")
    public ResponseEntity<ResponseModel> contacts(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        List<SuggestContact> res = suggestionService.searchContacts(q, limit);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(res).build());
    }

    @GetMapping("/operators")
    public ResponseEntity<ResponseModel> operators(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        List<SuggestOperator> res = suggestionService.searchOperators(q, limit);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(res).build());
    }

    @GetMapping("/plans")
    public ResponseEntity<ResponseModel> plans(@RequestParam Long operatorId) {
        List<SuggestPlan> res = suggestionService.getPlansForOperator(operatorId);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(res).build());
    }

    @GetMapping("/movies")
    public ResponseEntity<ResponseModel> movies(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        List<SuggestMovie> res = suggestionService.searchMovies(q, limit);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(res).build());
    }

    @GetMapping("/restaurants")
    public ResponseEntity<ResponseModel> restaurants(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        List<SuggestRestaurant> res = suggestionService.searchRestaurants(q, limit);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(res).build());
    }
}
