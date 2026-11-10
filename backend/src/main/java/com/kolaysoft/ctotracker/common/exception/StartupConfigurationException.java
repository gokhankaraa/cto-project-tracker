package com.kolaysoft.ctotracker.common.exception;

/**
 * Uygulamanin yanlis ya da eksik yapilandirmayla baslamasini durduran hata (or. JWT_SECRET yok, ilk admin
 * verilmemis). Sorunu ve yapilacak isi ayri tasir; {@code StartupConfigurationFailureAnalyzer} ikisini Spring Boot'un
 * "APPLICATION FAILED TO START" ozetinde gosterir, kurulumu yapan kisi yigin izinde aramaz.
 */
public class StartupConfigurationException extends IllegalStateException {

    private final String description;
    private final String action;

    public StartupConfigurationException(String description, String action) {
        super(description + " " + action);
        this.description = description;
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }
}
