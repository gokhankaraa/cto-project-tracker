package com.kolaysoft.ctotracker.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import com.kolaysoft.ctotracker.common.exception.StartupConfigurationException;

/**
 * Yapilandirma hatasiyla baslayamayan uygulamada yigin izi yerine kisa bir ozet: ne eksik ve ne yapilmali. Spring Boot
 * hatanin nedenler zincirinde bu turu arar (bean olusturma hatalarinin icinde de bulur). Kayit:
 * {@code META-INF/spring.factories}.
 */
public class StartupConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<StartupConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, StartupConfigurationException cause) {
        return new FailureAnalysis(cause.getDescription(), cause.getAction(), cause);
    }
}
