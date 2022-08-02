package io.blackdeer.restdocsmaven.hub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/hub")
public class HubController {

    private final HubRepository hubRepository;

    @Autowired
    public HubController(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    @GetMapping("/get/{id}")
    public ResponseEntity getHub(@PathVariable Long id) {
        Optional<Hub> hubOptional = hubRepository.findById(id);
        if (hubOptional.isPresent()) {
            return new ResponseEntity(hubOptional.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/list")
    public ResponseEntity getAllHubs() {
        return new ResponseEntity(hubRepository.findAll(), HttpStatus.OK);
    }
}
