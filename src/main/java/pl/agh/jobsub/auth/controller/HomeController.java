package pl.agh.jobsub.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/")
    public String index(){
        return "Hello world";
    }



    @GetMapping(value = "/logged")
    public ResponseEntity privateArea(){
        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

}
