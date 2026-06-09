package com.event.event_gateway_service.servicesCommunition;

import com.event.event_gateway_service.requestORresponse.EventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "serviceConnection", url = "http://account-service:8012/accounts")
public interface ServiceConnection {

    @PostMapping(value = "/{accountId}/transactions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTransaction(@RequestBody EventRequest eventRequest, @PathVariable String accountId);
}
