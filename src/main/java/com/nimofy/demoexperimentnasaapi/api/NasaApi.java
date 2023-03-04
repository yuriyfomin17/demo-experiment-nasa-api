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
    public ResponseEntity<Map<String, Stats>> compareDifferentMethods(@PathVariable int sol){
        var statsCompletableFuture = speedTracker.getMaxImageCompletableFuture(sol);
        var statsParallelStreams = speedTracker.getMaxImageParallelStream(sol);
        var statsMaxImageReactive = speedTracker.getMaxImageReactive(sol);
        Map<String, Stats> statsMap = new HashMap<>();
        statsMap.put("COMPLETABLE_FUTURE", statsCompletableFuture);
        statsMap.put("PARALLEL_STREAM", statsParallelStreams);
        statsMap.put("REACTIVE_APPROACH", statsMaxImageReactive);
        return new ResponseEntity<>(statsMap, HttpStatus.OK);
    }
}
