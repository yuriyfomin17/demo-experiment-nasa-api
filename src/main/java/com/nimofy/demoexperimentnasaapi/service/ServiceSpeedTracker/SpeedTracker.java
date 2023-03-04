package com.nimofy.demoexperimentnasaapi.service.ServiceSpeedTracker;

import com.nimofy.demoexperimentnasaapi.dto.Image;
import com.nimofy.demoexperimentnasaapi.dto.ImageSrc;
import com.nimofy.demoexperimentnasaapi.dto.Photos;
import com.nimofy.demoexperimentnasaapi.dto.Stats;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
public class SpeedTracker {
    private final RestTemplate restTemplate;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Value("${nasa.api.url}")
    private String NASA_URl;
    @Value("${nasa.api.key}")
    private String NASA_KEY;


    public Stats getMaxImageCompletableFuture(int sol) {
        var url = buildUriBySol(sol);
        var photos = restTemplate.getForEntity(url, Photos.class).getBody();
        Objects.requireNonNull(photos);
        List<CompletableFuture<Image>> completableFutures = photos.photos().stream()
                .map(ImageSrc::img_src)
                .map(imgSrc -> CompletableFuture.supplyAsync(() -> createImageFromSrc(imgSrc), threadPoolTaskExecutor))
                .toList();
        var startTime = System.currentTimeMillis();
        var image = completableFutures.stream().map(CompletableFuture::join).max(Comparator.comparing(Image::size)).orElseThrow();
        var endTime = System.currentTimeMillis() - startTime;
        return new Stats(image, endTime);
    }

    public Stats getMaxImageParallelStream(int sol) {
        var url = buildUriBySol(sol);
        var photos = restTemplate.getForEntity(url, Photos.class).getBody();
        Objects.requireNonNull(photos);
        long startTime = System.currentTimeMillis();
        var image = photos.photos().parallelStream()
                .map(ImageSrc::img_src)
                .map(this::createImageFromSrc)
                .max(Comparator.comparing(Image::size))
                .orElseThrow();
        long timeElapsed = System.currentTimeMillis() - startTime;
        return new Stats(image, timeElapsed);
    }
    public Stats getMaxImageReactive(int sol){
        var url = buildUriBySol(sol);
        long startTime = System.currentTimeMillis();
        var res = WebClient.create(url.toString())
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(resp -> resp.bodyToMono(Photos.class))
                .flatMapMany(photos -> Flux.fromIterable(photos.photos()))
                .flatMap(imageSrc ->
                        WebClient.create(imageSrc.img_src())
                                .head()
                                .exchangeToMono(ClientResponse::toBodilessEntity)
                                .map(HttpEntity::getHeaders)
                                .map(HttpHeaders::getLocation)
                                .map(URI::toString)
                                .flatMap(redirectedPictureUrl -> WebClient.create(redirectedPictureUrl)
                                        .head()
                                        .exchangeToMono(ClientResponse::toBodilessEntity)
                                        .map(HttpEntity::getHeaders)
                                        .map(HttpHeaders::getContentLength)
                                        .map(size -> new Image(redirectedPictureUrl, size))
                                )

                ).reduce((p1, p2) -> p1.size() > p2.size() ? p1 : p2).block();
        long timeElapsed = System.currentTimeMillis() - startTime;
        return new Stats(res, timeElapsed);
    }

    private Image createImageFromSrc(String imageSrc) {
        var headers = restTemplate.headForHeaders(imageSrc);
        var redirectedUrl = headers.getLocation();
        Objects.requireNonNull(redirectedUrl);
        var redirectedUrlHeaders = restTemplate.headForHeaders(redirectedUrl);
        var size = redirectedUrlHeaders.getContentLength();
        return new Image(redirectedUrl.toString(), size);


    }


    private URI buildUriBySol(int sol) {
        return UriComponentsBuilder.fromHttpUrl(NASA_URl)
                .queryParam("api_key", NASA_KEY)
                .queryParam("sol", sol)
                .build().toUri();

    }
}
