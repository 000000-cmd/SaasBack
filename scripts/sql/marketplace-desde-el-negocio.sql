-- =====================================================================
-- EL DIRECTORIO DEJA DE PEDIR LO QUE EL NEGOCIO YA ESCRIBIO
-- ---------------------------------------------------------------------
-- `business_public_profile` se habia convertido en una segunda ficha del
-- negocio: titular, descripcion, portada, categoria, direccion y
-- coordenadas. Todo eso YA existe:
--
--   categoria  -> business.BusinessTypeId (se elige al crear el negocio,
--                 y los codigos de business_type son literalmente los
--                 mismos: BARBERSHOP, SALON, SPA, NAILS)
--   titular    -> business_landing.Tagline       ("Mi pagina")
--   descripcion-> business_landing.About         ("Mi pagina")
--   portada    -> business_landing.HeroImageUrl  ("Mi pagina")
--   direccion  -> branch.AddressLine             ("Sedes")
--   ciudad     -> branch.MunicipalityId          ("Sedes")
--
-- Dos copias del mismo dato no se mantienen solas: se cambia la
-- direccion en Sedes y el directorio sigue enseñando la vieja. Aqui se
-- borra la copia y el directorio pasa a leer el original.
--
-- Lo que SE QUEDA es lo unico que solo existe para el directorio: si
-- quiere aparecer, si esta verificado y como lo califican.
--
-- Las coordenadas se mudan a `branch`, que es donde de verdad viven: un
-- punto en el mapa es un LOCAL, no una empresa. Un negocio con tres
-- sedes tiene tres sitios, no uno.
-- =====================================================================

-- 1) Las coordenadas, a la sede.
ALTER TABLE branch
    ADD COLUMN Latitude  DECIMAL(10,7) NULL AFTER AddressLine,
    ADD COLUMN Longitude DECIMAL(10,7) NULL AFTER Latitude;

-- 2) Rescatar lo que algun dueño ya hubiera escrito en la ficha, para que
--    el cambio no le borre su trabajo.

--    2a) Si no tenia pagina, se le crea con lo que puso en la ficha.
INSERT INTO business_landing (Id, BusinessId, Tagline, About, HeroImageUrl,
                              Published, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), p.BusinessId, p.Headline, p.Description, p.CoverImageUrl,
       FALSE, TRUE, TRUE, NOW(6), NOW(6)
  FROM business_public_profile p
 WHERE NOT EXISTS (SELECT 1 FROM business_landing l WHERE l.BusinessId = p.BusinessId)
   AND (p.Headline IS NOT NULL OR p.Description IS NOT NULL OR p.CoverImageUrl IS NOT NULL);

--    2b) Si ya tenia pagina, solo se rellenan los huecos. Lo que el dueño
--        escribio en "Mi pagina" manda: es lo que ha estado viendo.
UPDATE business_landing l
  JOIN business_public_profile p ON p.BusinessId = l.BusinessId
   SET l.Tagline      = COALESCE(NULLIF(l.Tagline, ''), p.Headline),
       l.About        = COALESCE(NULLIF(l.About, ''), p.Description),
       l.HeroImageUrl = COALESCE(NULLIF(l.HeroImageUrl, ''), p.CoverImageUrl);

--    2c) Las coordenadas, a la sede principal.
UPDATE branch b
  JOIN business_public_profile p ON p.BusinessId = b.BusinessId
   SET b.Latitude = p.Latitude, b.Longitude = p.Longitude
 WHERE b.IsMain = 1 AND p.Latitude IS NOT NULL AND b.Latitude IS NULL;

-- 3) Fuera la copia.
--    Los indices van primero: llevan dentro columnas que van a dejar de existir.
ALTER TABLE business_public_profile
    DROP INDEX idx_bpp_listed,
    DROP INDEX idx_bpp_municipality;

ALTER TABLE business_public_profile
    DROP COLUMN Headline,
    DROP COLUMN Description,
    DROP COLUMN CategoryCode,
    DROP COLUMN CoverImageUrl,
    DROP COLUMN GalleryJson,
    DROP COLUMN MunicipalityId,
    DROP COLUMN AddressLine,
    DROP COLUMN Latitude,
    DROP COLUMN Longitude;

ALTER TABLE business_public_profile
    ADD KEY idx_bpp_listed (IsListed);
