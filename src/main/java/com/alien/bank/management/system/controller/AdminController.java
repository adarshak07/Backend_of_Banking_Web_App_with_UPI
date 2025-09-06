package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Role;
import com.alien.bank.management.system.entity.Transaction;
import com.alien.bank.management.system.entity.TransactionType;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.TransactionRepository;
import com.alien.bank.management.system.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseModel> getSummary() {
        long users = userRepository.count();
        long accounts = accountRepository.count();
        long txns = transactionRepository.count();
        Double totalBalance = accountRepository.findAll().stream().map(Account::getBalance).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("accounts", accounts);
        result.put("txns", txns);
        result.put("totalBalance", totalBalance);

        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(result).build());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseModel> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> p = userRepository.findAll(pageable);

        List<Map<String, Object>> items = p.getContent().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole() != null ? u.getRole().name() : null);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", p.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(result).build());
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseModel> updateUserRole(@PathVariable Long id, @RequestBody ChangeRoleRequest body) {
        User user = userRepository.findById(id).orElseThrow();
        if (body.getRole() != null) {
            try {
                user.setRole(Role.valueOf(body.getRole().toUpperCase()));
            } catch (Exception ignored) {}
        }
        userRepository.save(user);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data("OK").build());
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseModel> listAccounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Account> p = accountRepository.findAll(pageable);
        List<Map<String, Object>> items = p.getContent().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("balance", a.getBalance());
            m.put("user", a.getUser() != null ? a.getUser().getEmail() : null);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", p.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(result).build());
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseModel> listTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Date from,
            @RequestParam(required = false) Date to,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> p;
        // Simple approach: filter using repository findAll, then in-memory filtering for demo
        p = transactionRepository.findAll(pageable);

        List<Map<String, Object>> items = p.getContent().stream()
                .filter(t -> accountId == null || (t.getAccount() != null && Objects.equals(t.getAccount().getId(), accountId)))
                .filter(t -> type == null || t.getType() == parseType(type))
                .filter(t -> from == null || !t.getTimestamp().before(from))
                .filter(t -> to == null || !t.getTimestamp().after(to))
                .filter(t -> min == null || t.getAmount() >= min)
                .filter(t -> max == null || t.getAmount() <= max)
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("type", t.getType().name());
                    m.put("amount", t.getAmount());
                    m.put("timestamp", t.getTimestamp());
                    m.put("accountId", t.getAccount() != null ? t.getAccount().getId() : null);
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", p.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(result).build());
    }

    private TransactionType parseType(String t) {
        try { return TransactionType.valueOf(t.toUpperCase()); } catch (Exception e) { return null; }
    }

    @Data
    public static class ChangeRoleRequest {
        private String role;
    }
}


