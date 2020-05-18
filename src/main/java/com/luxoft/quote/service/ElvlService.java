package com.luxoft.quote.service;

import com.luxoft.quote.businesslogic.Calculator;
import com.luxoft.quote.dao.ElvlRepository;
import com.luxoft.quote.domain.Elvl;
import com.luxoft.quote.domain.Quote;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
public class ElvlService {

    private ConcurrentHashMap<String, Elvl> elvlCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Object> elvlSync = new ConcurrentHashMap<>();
    private Calculator calculator;
    private ElvlRepository elvlRepository;
    private ExecutorService executorService;

    public ElvlService(ElvlRepository elvlRepository, Calculator calculator) {
        this.elvlRepository = elvlRepository;
        this.calculator = calculator;
        this.executorService = Executors.newFixedThreadPool(2);
        initCache();
    }

    private void initCache() {
        Collection<Elvl> elvls = elvlRepository.fetchAllElvls();
        for (Elvl elvl: elvls) {
            elvlCache.put(elvl.getIsin(), elvl);
        }
    }

    public void addQuote(Quote quote) {
        String isin = quote.getIsin();
        elvlSync.putIfAbsent(isin, new Object());
        Elvl elvl;
        synchronized (elvlSync.get(isin)) {
            elvl = elvlCache.get(isin);
            elvl = calculator.calculateElvl(quote, elvl);
            elvlCache.put(isin, elvl);
        }

        // async elvl save to db
        List<Elvl> elvls = Arrays.asList(elvl);
        executorService.submit(() -> {
            int[] updated = elvlRepository.batchUpdateElvls(elvls);
            int sum = IntStream.of(updated).sum();
            return sum > 0;
        });
    }

    public Elvl getElvl(String isin) {
        return elvlCache.get(isin);
    }

    public List<Elvl> getElvls() {
        return new ArrayList(elvlCache.values());
    }
}
