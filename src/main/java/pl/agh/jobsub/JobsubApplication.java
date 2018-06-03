package pl.agh.jobsub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.agh.jobsub.auth.configuration.CustomUserDetails;
import pl.agh.jobsub.auth.model.Role;
import pl.agh.jobsub.auth.model.User;
import pl.agh.jobsub.auth.repository.UserRepository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

@SpringBootApplication
public class JobsubApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobsubApplication.class, args);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder(8);
	}

	@Autowired
	public void authenticationManager(AuthenticationManagerBuilder builder, UserRepository repository) throws Exception {
		if(repository.count()==0){
			repository.save(new User("user123","{bcrypt}"+bCryptPasswordEncoder().encode("pass123"), Arrays.asList(new Role("USER"),new Role(("ACTUATOR")))));
			//repository.save(User.withDefaultPasswordEncoder().username("user").password("user").roles("USER").build(); )
		}
		builder.userDetailsService(s -> new CustomUserDetails(repository.findByUsername(s)));
	}
}
