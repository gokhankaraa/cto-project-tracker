package com.kolaysoft.ctotracker.security;

/**
 * Bir degisikligi yapan kisi, degisiklik gecmisi icin. Token'dan okunur (veritabanina gidilmez): id {@code sub},
 * ad {@code name} alanindan. Token'siz oturumda (testlerdeki sahte kullanici) id bos, ad oturum adidir.
 */
public record Actor(Long id, String name) {
}
