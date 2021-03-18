package com.ibm.moviecatalogservice.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.ibm.moviecatalogservice.models.CatalogItem;
import com.ibm.moviecatalogservice.models.Movie;
import com.ibm.moviecatalogservice.models.Rating;
import com.ibm.moviecatalogservice.models.UserRating;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class CatalogResource {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    WebClient.Builder webClientBuilder;
    
  //LEAVE EVERYTHING DEFAULT AND CONFIGURE FALLBACK 

    @RequestMapping("/{userId}")
    @HystrixCommand(fallbackMethod = "getFallbackCatalog")
    public List<CatalogItem> getCatalog(@PathVariable("userId") String userId) {

        UserRating userRating = restTemplate.getForObject("http://ratings-data-service/ratingsdata/user/" + userId, UserRating.class);

        return userRating.getRatings().stream()
                .map(rating -> {
                    Movie movie = restTemplate.getForObject("http://movie-info-service/movies/" + rating.getMovieId(), Movie.class);
                    return new CatalogItem(movie.getName(), movie.getDescription(), rating.getRating());
                })
                .collect(Collectors.toList());

    }

//CALLBACK SHOULD HAVE SAME SIGNATURE OF ACTUAL METHOD 
//ALWAYS CALLBACK HAS TO BE SIMPLE HARD CODED CONTENT OR READ FROM CACHE DO NOT CONNECT ANOTHER SERVICE WHICH MAY RESULT 
//IN TO ISSUE AND CREATING 
// ANOTHER CALLBACK FOR CALLBACK IS NOT A GOOD IDEA 
    
    
public List<CatalogItem> getFallbackCatalog(@PathVariable("userId") String userId) {

		return Arrays.asList(new CatalogItem("No Movie", "", 0));
}
}

//KILL INFO SERVICE TO TEST THE SERVICE.
//localhost:8081/catalog/bvr

/*
Alternative WebClient way
Movie movie = webClientBuilder.build().get().uri("http://localhost:8082/movies/"+ rating.getMovieId())
.retrieve().bodyToMono(Movie.class).block();
*/