package com.luxoft.quote.service;

import com.luxoft.quote.dao.QuoteRepository;
import com.luxoft.quote.domain.Quote;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
public class QuoteService {

    private QuoteRepository quoteRepository;
    private LinkedList<Quote> queueToDB = new LinkedList<>();
    private ExecutorService executorService;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public void asyncSaveQuote(Quote quote) {
        executorService.submit(() -> {
            List<Quote> subListToDB = null;

            synchronized (queueToDB) {
                queueToDB.add(quote);

                if (queueToDB.size() > 10) {
                    subListToDB = new LinkedList<>(queueToDB);
                    queueToDB.removeAll(subListToDB);
                }
            }

            if (subListToDB != null  &&  subListToDB.size() > 0) {
                int[] updated = quoteRepository.batchInsertQuotes(subListToDB);
                subListToDB.clear();

                int sum = IntStream.of(updated).sum();
                return sum > 0;
            }
            return false;
        });
    }
}
