package com.esvar.dekanat.utilites;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

@Component
public class HibernateStatisticsLogger {

    private final Statistics statistics;

    public HibernateStatisticsLogger(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    @PreDestroy
    public void logStatistics() {

    }
}
