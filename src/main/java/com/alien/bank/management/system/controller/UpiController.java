package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.UpiRequest;
import com.alien.bank.management.system.entity.UpiVpa;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.model.upi.CreateUpiIdRequest;
import com.alien.bank.management.system.model.upi.CreateUpiIdResponse;
import com.alien.bank.management.system.service.UpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upi")
@RequiredArgsConstructor
public class UpiController {

    private final UpiService upiService;

    // POST /upi/vpa/create?accountId=
    @PostMapping("/vpa/create")
    public ResponseEntity<ResponseModel> createVpa(@RequestParam Long accountId) {
        UpiVpa vpa = upiService.createVpa(accountId);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(vpa).build());
    }

    // GET /upi/vpa/me
    @GetMapping("/vpa/me")
    public ResponseEntity<ResponseModel> getMyVpas() {
        List<UpiVpa> vpas = upiService.getMyVpas();
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(vpas).build());
    }

    // POST /upi/vpa/default?vpa=
    @PostMapping("/vpa/default")
    public ResponseEntity<ResponseModel> setDefaultVpa(@RequestParam String vpa) {
        upiService.setDefaultVpa(vpa);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data("OK").build());
    }

    // POST /upi/send
    @PostMapping("/send")
    public ResponseEntity<ResponseModel> send(@RequestBody SendMoneyDto request) {
        Long txnId = upiService.sendMoney(request.fromVpa, request.toVpa, request.amount, request.note, request.pin);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(txnId).build());
    }

    // POST /upi/request
    @PostMapping("/request")
    public ResponseEntity<ResponseModel> request(@RequestBody RequestMoneyDto request) {
        UpiRequest saved = upiService.createCollectRequest(request.payerVpa, request.payeeVpa, request.amount, request.reason);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(saved).build());
    }

    // GET /upi/request/{vpa}
    @GetMapping("/request/{vpa}")
    public ResponseEntity<ResponseModel> getPending(@PathVariable String vpa) {
        List<UpiRequest> list = upiService.getPendingRequests(vpa);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(list).build());
    }

    // POST /upi/request/{id}/approve?pin=
    @PostMapping("/request/{id}/approve")
    public ResponseEntity<ResponseModel> approve(@PathVariable Long id, @RequestParam String pin) {
        Long txnId = upiService.approveRequest(id, pin);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(txnId).build());
    }

    // POST /upi/request/{id}/reject
    @PostMapping("/request/{id}/reject")
    public ResponseEntity<ResponseModel> reject(@PathVariable Long id) {
        upiService.rejectRequest(id);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data("OK").build());
    }

    // POST /upi/pin?newPin=
    @PostMapping("/pin")
    public ResponseEntity<ResponseModel> setPin(@RequestParam String newPin) {
        upiService.setOrChangeUpiPin(newPin);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data("OK").build());
    }

    // POST /upi/create - Create new UPI ID with account linking
    @PostMapping("/create")
    public ResponseEntity<ResponseModel> createUpiId(@Valid @RequestBody CreateUpiIdRequest request) {
        CreateUpiIdResponse response = upiService.createUpiId(request);
        return ResponseEntity.ok(ResponseModel.builder()
                .status(HttpStatus.OK)
                .success(true)
                .data(response)
                .build());
    }

    // DELETE /upi/{vpa} - Delete UPI ID
    @DeleteMapping("/{vpa}")
    public ResponseEntity<ResponseModel> deleteUpiId(@PathVariable String vpa) {
        upiService.deleteUpiId(vpa);
        return ResponseEntity.ok(ResponseModel.builder()
                .status(HttpStatus.OK)
                .success(true)
                .data("UPI ID deleted successfully")
                .build());
    }

    // GET /upi/{vpa} - Get UPI ID details
    @GetMapping("/{vpa}")
    public ResponseEntity<ResponseModel> getUpiId(@PathVariable String vpa) {
        UpiVpa upiVpa = upiService.getUpiIdByVpa(vpa);
        return ResponseEntity.ok(ResponseModel.builder()
                .status(HttpStatus.OK)
                .success(true)
                .data(upiVpa)
                .build());
    }

    // GET /upi/transactions/{vpa}
    @GetMapping("/transactions/{vpa}")
    public ResponseEntity<ResponseModel> getTransactionsByVpa(@PathVariable String vpa) {
        var txns = upiService.getTransactionsByVpa(vpa);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(txns).build());
    }

    // GET /upi/lookup?q=
    @GetMapping("/lookup")
    public ResponseEntity<ResponseModel> lookupVpa(@RequestParam String q) {
        List<UpiVpa> vpas = upiService.searchVpas(q);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(vpas).build());
    }

    // Minimal DTOs for controller binding
    static class SendMoneyDto {
        public String fromVpa;
        public String toVpa;
        public Double amount;
        public String note;
        public String pin;
    }
    static class RequestMoneyDto {
        public String payerVpa;
        public String payeeVpa;
        public Double amount;
        public String reason;
    }
}


