package pl.agh.jobsub.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import pl.agh.jobsub.auth.model.User;

@Controller
public class JobController {

    RestTemplate restTemplate = new RestTemplate();

    @CrossOrigin
    @RequestMapping(value = "/jobs")
    public ResponseEntity getJobs(){

        String uri = "https://jobs.github.com/positions.json?description=python&location=new+york";

        String result = restTemplate.getForObject(uri, String.class);

        System.out.println(result);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }
}
