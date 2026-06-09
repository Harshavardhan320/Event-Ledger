package com.event.event_gateway_service.controller;

import com.event.event_gateway_service.requestORresponse.EventRequest;
import com.event.event_gateway_service.entity.Event;
import com.event.event_gateway_service.services.EventGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController(value = "EventGatewayController")
@RequestMapping(value = "/events")
public class EventGatewayController {

    private final Logger log = LoggerFactory.getLogger(EventGatewayController.class);
    @Autowired
    private EventGatewayService eventGatewayService;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Event>> transactionEvent(@RequestBody List<EventRequest> event){
        try{
            List<Event> createdEvent = eventGatewayService.processAllEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
        }catch (Exception e){
            log.error("Something went wrong.. cannot process [ {} ]", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Event> getByEventId(@PathVariable Long id){
        try{
            Event event = eventGatewayService.getEventById(id);
            if(event != null){
                return ResponseEntity.ok(event);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Something went wrong.. try again [ {} ]", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(params = "account")
    public ResponseEntity<List<Event>> getByAccountId(@RequestParam String account){
        try{
            List<Event> events = eventGatewayService.getByAccountId(account);
            if(events != null && !events.isEmpty()){
                return ResponseEntity.ok(events);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Something went wrong.. Cannot find the events [ {} ]", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
