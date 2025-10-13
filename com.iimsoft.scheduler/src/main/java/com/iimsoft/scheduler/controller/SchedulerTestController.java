package com.iimsoft.scheduler.controller;

import com.iimsoft.ui.web.config.WebConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = SchedulerTestController.ENDPOINT)
public class SchedulerTestController {
    public static final String ENDPOINT = WebConfig.ENDPOINT_ROOT + "/schedulerTest";

    @GetMapping("/testScheduler")
    public void test() throws Exception {

    }
}
