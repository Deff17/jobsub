package pl.agh.jobsub;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.CompositeFilter;

import javax.servlet.Filter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@SpringBootApplication
@RestController
@EnableOAuth2Client
@EnableAuthorizationServer
@Order(6)
public class JobsubApplication extends WebSecurityConfigurerAdapter {

    @Autowired
    OAuth2ClientContext oauth2ClientContext;

    @RequestMapping({"/user", "/me"})
    public Map<String, String> user(Principal principal) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", principal.getName());
        return map;
    }

    @CrossOrigin
    @RequestMapping(value = "/api/offers", method = GET)
    @ResponseBody
    public String returnJobsFromGithubJobs(@RequestParam("job") String job,
                                           @RequestParam("city") String city,
                                           @RequestParam("salary") String salary) {

        System.out.println("Job received from frontend:" + job);
        String githubJobsURLBasic = "https://jobs.github.com/positions.json?";
        String githubJobsURLFull = githubJobsURLBasic.concat("description=" + job.split(" ")[0])
                .concat("&location=" + city);

        String searchGovURLBasic = "https://jobs.search.gov/jobs/search.json?size=50&";
        String searchGovURLFull = searchGovURLBasic.concat("query=" + job.replace(" ", "+") + "+in+" + city);
        JSONArray githubJobsJsonArray = null;
        JSONArray searchGovJsonArray = null;
        try {
            githubJobsJsonArray = getJSONArrayfromURLString(githubJobsURLFull);
            searchGovJsonArray = getJSONArrayfromURLString(searchGovURLFull);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < githubJobsJsonArray.length(); i++) {
            try {
                githubJobsJsonArray.getJSONObject(i).remove("company");
                githubJobsJsonArray.getJSONObject(i).remove("company_url");
                githubJobsJsonArray.getJSONObject(i).remove("how_to_apply");
                githubJobsJsonArray.getJSONObject(i).remove("company_logo");
                githubJobsJsonArray.getJSONObject(i).remove("created_at");
                githubJobsJsonArray.getJSONObject(i).remove("id");
                githubJobsJsonArray.getJSONObject(i).remove("type");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < searchGovJsonArray.length(); i++) {
            try {
                searchGovJsonArray.getJSONObject(i).remove("end_date");
                searchGovJsonArray.getJSONObject(i).remove("start_date");
                searchGovJsonArray.getJSONObject(i).remove("maximum");
                searchGovJsonArray.getJSONObject(i).remove("minimum");
                searchGovJsonArray.getJSONObject(i).remove("rate_interval_code");
                searchGovJsonArray.getJSONObject(i).remove("id");
                String location = searchGovJsonArray.getJSONObject(i).get("locations").toString()
                        .replaceAll("\\[", "").replaceAll("]", "");
                searchGovJsonArray.getJSONObject(i).remove("locations");
                searchGovJsonArray.getJSONObject(i).put("location", location);

                String title = searchGovJsonArray.getJSONObject(i).get("position_title").toString();
                searchGovJsonArray.getJSONObject(i).remove("position_title");
                searchGovJsonArray.getJSONObject(i).put("title", title);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return githubJobsJsonArray.toString().replaceFirst("]", ", ")
                + searchGovJsonArray.toString().replaceFirst("\\[", "");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http.antMatcher("/**").authorizeRequests().antMatchers("/**", "/login**", "/webjars/**", "/error**").permitAll().anyRequest()
                .authenticated().and().exceptionHandling()
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/")).and().logout()
                .logoutSuccessUrl("/").permitAll().and().csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        // @formatter:on
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.antMatcher("/me").authorizeRequests().anyRequest().authenticated();
            // @formatter:on
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(JobsubApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<OAuth2ClientContextFilter> oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<OAuth2ClientContextFilter>();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    @Bean
    @ConfigurationProperties("github")
    public ClientResources github() {
        return new ClientResources();
    }

    @Bean
    @ConfigurationProperties("facebook")
    public ClientResources facebook() {
        return new ClientResources();
    }

    private Filter ssoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        filters.add(ssoFilter(facebook(), "/login/facebook"));
        filters.add(ssoFilter(github(), "/login/github"));
        filter.setFilters(filters);
        return filter;
    }

    private Filter ssoFilter(ClientResources client, String path) {
        OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationFilter = new OAuth2ClientAuthenticationProcessingFilter(path);
        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
        oAuth2ClientAuthenticationFilter.setRestTemplate(oAuth2RestTemplate);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(client.getResource().getUserInfoUri(),
                client.getClient().getClientId());
        tokenServices.setRestTemplate(oAuth2RestTemplate);
        oAuth2ClientAuthenticationFilter.setTokenServices(tokenServices);
        return oAuth2ClientAuthenticationFilter;
    }

    private static JSONArray getJSONArrayfromURLString(String urlString) throws Exception {

        URL url = new URL(urlString);
        // open the url stream, wrap it an a few "readers"
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        // write the output to stdout
        String line;
        JSONArray jsonArray = null;
        if ((line = reader.readLine()) != null) {
            jsonArray = new JSONArray(line);
        }

        // close our reader
        reader.close();
        return jsonArray;
    }
}

class ClientResources {

    @NestedConfigurationProperty
    private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

    @NestedConfigurationProperty
    private ResourceServerProperties resource = new ResourceServerProperties();

    public AuthorizationCodeResourceDetails getClient() {
        return client;
    }

    public ResourceServerProperties getResource() {
        return resource;
    }
}