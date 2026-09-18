package com.saas.common.cqrs;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lado de LECTURA del sistema (la Q de CQRS).
 *
 * El reparto que ya existia era el grande: las escrituras van a MySQL y
 * publican al outbox; Kafka las lleva a Elasticsearch, que es de donde leen
 * las busquedas y los listados grandes. Lo que faltaba era el tercer caso —
 * los catalogos.
 *
 * Un catalogo (tipos de documento, generos, bancos, estados...) se lee en
 * practicamente cada pantalla y se escribe una vez cada varios meses. Ir a
 * MySQL cada vez es pagar una conexion, un viaje de red y un parseo por una
 * respuesta que no ha cambiado desde el arranque. Aqui vive en memoria del
 * propio servicio, y se invalida en la escritura que lo cambia.
 *
 * Se declara el {@link CacheManager} a mano en vez de dejar que lo adivine la
 * autoconfiguracion: asi los nombres de cache son explicitos y ningun
 * {@code @Cacheable} con el nombre mal escrito se crea su propia region en
 * silencio.
 *
 * ponytail: cache en el proceso, no distribuida. Con una instancia por
 * servicio —que es como corre hoy— es estrictamente mejor que Redis: cero
 * red. Si algun dia hay dos replicas del mismo servicio, una escritura en A
 * no invalida a B y el catalogo tarda un reinicio en cuadrar; el sitio del
 * cambio es esta clase, cambiando a RedisCacheManager (Redis ya esta en el
 * stack).
 */
@Configuration
@EnableCaching
public class ReadModelConfig {

    /** Listas de catalogo completas, indexadas por su ruta publica. */
    public static final String CATALOGS = "catalogs";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(CATALOGS);
        // Nadie cachea null aqui, y permitirlo esconderia un fallo de lectura
        // detras de una respuesta vacia servida durante horas.
        manager.setAllowNullValues(false);
        return manager;
    }
}
