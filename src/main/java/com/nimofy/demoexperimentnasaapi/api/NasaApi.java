package com.nimofy.demoexperimentnasaapi.api;

import com.nimofy.demoexperimentnasaapi.dto.Stats;
import com.nimofy.demoexperimentnasaapi.service.ServiceSpeedTracker.SpeedTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/experiment")
public class NasaApi {
    private final SpeedTracker speedTracker;

    @GetMapping("/compare/{sol}")
    public ResponseEntity<Map<String, Double>> compareDifferentMethods(@PathVariable int sol){
        double sumCompletableFutureTimeMillis = 0;
        double sumParallelStreamTimeMillis = 0;
        double sumReactiveTimeMillis = 0;
        for (int i = 0; i < 20; i++) {
            var statsCompletableFuture = speedTracker.getMaxImageCompletableFuture(sol);
            sumCompletableFutureTimeMillis += statsCompletableFuture.timeTakenMillis();

            var statsParallelStreams = speedTracker.getMaxImageParallelStream(sol);
            sumParallelStreamTimeMillis += statsParallelStreams.timeTakenMillis();

            var statsMaxImageReactive = speedTracker.getMaxImageReactive(sol);
            sumReactiveTimeMillis += statsMaxImageReactive.timeTakenMillis();

        }

        Map<String, Double> statsMap = new HashMap<>();
        statsMap.put("COMPLETABLE_FUTURE", sumCompletableFutureTimeMillis/ 20.0);
        statsMap.put("PARALLEL_STREAM", sumParallelStreamTimeMillis / 20.0);
        statsMap.put("REACTIVE_APPROACH", sumReactiveTimeMillis / 20.0);
        return new ResponseEntity<>(statsMap, HttpStatus.OK);
    }
}
