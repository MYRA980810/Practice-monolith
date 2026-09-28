-- Closed category taxonomy: categories are curated via migrations.
-- display_order drives GET /api/categories ordering (NULLS LAST, then name);
-- featured marks the categories eligible for the storefront chip bar.
ALTER TABLE categories
    ADD COLUMN display_order INT     NULL,
    ADD COLUMN featured      BOOLEAN NOT NULL DEFAULT false;

-- V16 seeds use gen_random_uuid(), so they are matched by slug.
-- Featured first (editorial order), then the remaining seeds in V16 order, 'otro' last.
-- Seller-created categories keep display_order NULL and featured = false (listed at the end).
UPDATE categories c
SET display_order = v.display_order,
    featured      = v.featured
FROM (VALUES
    ('moda-femenina',        1, true),
    ('moda-masculina',       2, true),
    ('belleza',              3, true),
    ('accesorios-moda',      4, true),
    ('calzado',              5, true),
    ('joyeria-relojes',      6, true),
    ('electronica',          7, true),
    ('hogar-decoracion',     8, true),
    ('cocina-alimentos',     9, false),
    ('deportes-fitness',    10, false),
    ('juguetes-juegos',     11, false),
    ('mascotas',            12, false),
    ('salud-bienestar',     13, false),
    ('arte-manualidades',   14, false),
    ('libros-educacion',    15, false),
    ('bebes-ninos',         16, false),
    ('automotriz',          17, false),
    ('herramientas',        18, false),
    ('computacion',         19, false),
    ('musica-instrumentos', 20, false),
    ('viajes-turismo',      21, false),
    ('ropa-deportiva',      22, false),
    ('bolsas-carteras',     23, false),
    ('suplementos',         24, false),
    ('otro',                25, false)
) AS v(slug, display_order, featured)
WHERE c.slug = v.slug;
