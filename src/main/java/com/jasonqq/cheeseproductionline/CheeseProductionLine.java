package com.jasonqq.cheeseproductionline;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CheeseProductionLine implements CommandLineRunner {
    private static final int outputCapacity = 10000;
    private static final int storeCapacity = 1000;
    private static final int deliveryCapacity = 100;

    private BlockingQueue<Cheese> cheeseStore = new LinkedBlockingQueue<>(storeCapacity);
    private BlockingQueue<Milk> milkLine = new LinkedBlockingQueue<>(outputCapacity * 2);
    private BlockingQueue<StarterCulture> starterCultureLine = new LinkedBlockingQueue<>(outputCapacity);
    private ExecutorService milkProducer = Executors.newSingleThreadExecutor();
    private ExecutorService starterCultureProducer = Executors.newSingleThreadExecutor();
    private ExecutorService cheeseProducer = Executors.newSingleThreadExecutor();
    private ExecutorService deliveryCar = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        SpringApplication.run(CheeseProductionLine.class, args);
    }

    @Override
    public void run(String... args) {
        AtomicInteger milkOutput = new AtomicInteger(0);
        milkProducer.submit(() -> {
            while (milkOutput.get() < outputCapacity * 2) {
                Milk milk = new Milk();
                milkLine.add(milk);
                milkOutput.incrementAndGet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        AtomicInteger starterCultureOutput = new AtomicInteger(0);
        starterCultureProducer.submit(() -> {
            while (starterCultureOutput.get() < outputCapacity) {
                StarterCulture starterCulture = new StarterCulture();
                starterCultureLine.add(starterCulture);
                starterCultureOutput.incrementAndGet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        AtomicInteger cheeseOutput = new AtomicInteger(0);
        cheeseProducer.submit(() -> {
            while (cheeseOutput.get() < outputCapacity) {
                try {
                    milkLine.take();
                    milkLine.take();
                    starterCultureLine.take();
                    Cheese cheese = new Cheese();
                    cheeseStore.put(cheese);
                    int currentCheeseOutput = cheeseOutput.incrementAndGet();
                    log.info("currentCheeseOutput: {}", currentCheeseOutput);
                } catch (Exception e) {
                    log.error("Unexpected exception during cheese production", e);
                }
            }
        });

        deliveryCar.submit(() -> {
            AtomicInteger deliveryCount = new AtomicInteger(0);
            List<Cheese> delivery = new ArrayList<>();
            while (true) {
                try {
                    if (cheeseStore.size() >= deliveryCapacity) {
                        log.info("cheeseStore size: {}", cheeseStore.size());
                        cheeseStore.drainTo(delivery, deliveryCapacity);
                        log.info("deliveryCount: {}", deliveryCount.incrementAndGet());
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("Unexpected exception during cheese delivery", e);
                }
            }
        });
    }

    class Cheese {
        private long id;
    }

    class Milk {
        private long id;
    }

    class StarterCulture {
        private long id;
    }
}
