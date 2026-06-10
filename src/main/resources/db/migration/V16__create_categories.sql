CREATE TABLE categories (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'PENDING_REVIEW')),
    created_by  UUID         REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO categories (id, name, slug) VALUES
    (gen_random_uuid(), 'Belleza y Cuidado Personal', 'belleza'),
    (gen_random_uuid(), 'Moda Femenina',              'moda-femenina'),
    (gen_random_uuid(), 'Moda Masculina',             'moda-masculina'),
    (gen_random_uuid(), 'Accesorios de Moda',         'accesorios-moda'),
    (gen_random_uuid(), 'Electrónica',                'electronica'),
    (gen_random_uuid(), 'Hogar y Decoración',         'hogar-decoracion'),
    (gen_random_uuid(), 'Cocina y Alimentos',         'cocina-alimentos'),
    (gen_random_uuid(), 'Deportes y Fitness',         'deportes-fitness'),
    (gen_random_uuid(), 'Juguetes y Juegos',          'juguetes-juegos'),
    (gen_random_uuid(), 'Mascotas',                   'mascotas'),
    (gen_random_uuid(), 'Salud y Bienestar',          'salud-bienestar'),
    (gen_random_uuid(), 'Joyería y Relojes',          'joyeria-relojes'),
    (gen_random_uuid(), 'Arte y Manualidades',        'arte-manualidades'),
    (gen_random_uuid(), 'Libros y Educación',         'libros-educacion'),
    (gen_random_uuid(), 'Bebés y Niños',              'bebes-ninos'),
    (gen_random_uuid(), 'Automotriz',                 'automotriz'),
    (gen_random_uuid(), 'Herramientas',               'herramientas'),
    (gen_random_uuid(), 'Computación',                'computacion'),
    (gen_random_uuid(), 'Música e Instrumentos',      'musica-instrumentos'),
    (gen_random_uuid(), 'Viajes y Turismo',           'viajes-turismo'),
    (gen_random_uuid(), 'Ropa Deportiva',             'ropa-deportiva'),
    (gen_random_uuid(), 'Calzado',                    'calzado'),
    (gen_random_uuid(), 'Bolsas y Carteras',          'bolsas-carteras'),
    (gen_random_uuid(), 'Suplementos',                'suplementos'),
    (gen_random_uuid(), 'Otro',                       'otro');
