// Seeds demo products (women's fashion) into an existing seller account, grouped by category.
// Requires Node 18+. The API has no idempotency guard on SKU — re-running a group duplicates its products.
//
// Required env vars:
//   API_BASE_URL   e.g. http://localhost:8080 (default)
//   SEED_EMAIL     seller account email/phone used to log in
//   SEED_PASSWORD  seller account password
//   PEXELS_API_KEY free key from https://www.pexels.com/api/ (licensed for commercial use, no attribution required)
//
// Optional env var:
//   SEED_ONLY      comma-separated group keys to seed (casual, sport, calzado, bolsas). Defaults to all groups.
//
// Usage:
//   API_BASE_URL=http://localhost:8080 SEED_EMAIL=... SEED_PASSWORD=... PEXELS_API_KEY=... node scripts/seed-demo-products.mjs
//   SEED_ONLY=calzado,bolsas ... node scripts/seed-demo-products.mjs   # seed just the new categories

const BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";
const EMAIL = process.env.SEED_EMAIL;
const PASSWORD = process.env.SEED_PASSWORD;
const PEXELS_API_KEY = process.env.PEXELS_API_KEY;

if (!EMAIL || !PASSWORD || !PEXELS_API_KEY) {
  console.error("Missing required env vars: SEED_EMAIL, SEED_PASSWORD, PEXELS_API_KEY");
  process.exit(1);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const CASUAL_PRODUCTS = [
  { sku: "CAS-001", name: "Blusa de Lino Manga Larga Beige", price: 449, query: "woman linen blouse beige fashion" },
  { sku: "CAS-002", name: "Playera Básica Algodón Blanco", price: 299, query: "woman white basic t-shirt fashion" },
  { sku: "CAS-003", name: "Playera Oversize Estampada Negro", price: 349, query: "woman oversized graphic tshirt black" },
  { sku: "CAS-004", name: "Vestido Midi Floral Verano", price: 799, query: "woman floral midi dress summer" },
  { sku: "CAS-005", name: "Vestido Camisero Denim", price: 749, query: "woman denim shirt dress" },
  { sku: "CAS-006", name: "Falda Plisada Midi Café", price: 549, query: "woman pleated midi skirt brown" },
  { sku: "CAS-007", name: "Falda Cargo Satinada Kaki", price: 599, query: "woman satin cargo skirt khaki" },
  { sku: "CAS-008", name: "Jeans Mom Fit Azul Claro", price: 799, query: "woman mom fit jeans light blue" },
  { sku: "CAS-009", name: "Jeans Skinny Tiro Alto Negro", price: 749, query: "woman high waisted skinny jeans black" },
  { sku: "CAS-010", name: "Pantalón Palazzo Fluido Terracota", price: 649, query: "woman palazzo pants terracotta" },
  { sku: "CAS-011", name: "Short Denim Deslavado", price: 449, query: "woman denim shorts washed" },
  { sku: "CAS-012", name: "Blazer Oversize Estructurado Camel", price: 999, query: "woman oversized blazer camel fashion" },
  { sku: "CAS-013", name: "Cardigan Tejido Punto Mostaza", price: 649, query: "woman knit cardigan mustard" },
  { sku: "CAS-014", name: "Suéter Cuello Alto Crema", price: 599, query: "woman turtleneck sweater cream" },
  { sku: "CAS-015", name: "Sudadera Oversize Crop Gris", price: 549, query: "woman cropped oversized hoodie grey" },
  { sku: "CAS-016", name: "Chaleco Acolchado Sin Mangas Negro", price: 699, query: "woman quilted vest black fashion" },
  { sku: "CAS-017", name: "Kimono Estampado Playero", price: 499, query: "woman printed kimono beachwear" },
  { sku: "CAS-018", name: "Jumpsuit Palazzo Elegante Vino", price: 899, query: "woman palazzo jumpsuit wine color" },
  { sku: "CAS-019", name: "Top Cropped Canalé Rosa", price: 299, query: "woman ribbed crop top pink" },
  { sku: "CAS-020", name: "Blusa Satinada Manga Globo Champagne", price: 599, query: "woman satin blouse balloon sleeve" },
  { sku: "CAS-021", name: "Camisa Oxford Rayas Azul", price: 549, query: "woman striped oxford shirt blue" },
  { sku: "CAS-022", name: "Vestido Wrap Envolvente Estampado", price: 849, query: "woman wrap dress printed fashion" },
  { sku: "CAS-023", name: "Falda Lápiz Tweed Negro", price: 649, query: "woman tweed pencil skirt black" },
  { sku: "CAS-024", name: "Pantalón Wide Leg Lino Blanco", price: 699, query: "woman wide leg linen pants white" },
  { sku: "CAS-025", name: "Chamarra Mezclilla Clásica Azul", price: 799, query: "woman classic denim jacket blue" },
  { sku: "CAS-026", name: "Chaqueta Biker Vegana Negra", price: 899, query: "woman vegan leather biker jacket black" },
  { sku: "CAS-027", name: "Sudadera con Capucha Básica Lavanda", price: 549, query: "woman basic hoodie lavender" },
  { sku: "CAS-028", name: "Blusa Cuello V Seda Artificial Coral", price: 599, query: "woman v-neck satin blouse coral" },
  { sku: "CAS-029", name: "Vestido Cami Slip Satín Negro", price: 699, query: "woman satin slip dress black" },
  { sku: "CAS-030", name: "Overol Corto Denim Claro", price: 649, query: "woman denim overall shorts light" },
  { sku: "CAS-031", name: "Top Halter Anudado Blanco", price: 349, query: "woman halter top white fashion" },
  { sku: "CAS-032", name: "Falda Midi Satinada Esmeralda", price: 599, query: "woman satin midi skirt emerald" },
  { sku: "CAS-033", name: "Pantalón Culotte Cuadros", price: 649, query: "woman culotte pants plaid" },
  { sku: "CAS-034", name: "Cárdigan Largo Oversize Beige", price: 749, query: "woman long oversized cardigan beige" },
  { sku: "CAS-035", name: "Blusa Bordada Boho Blanca", price: 549, query: "woman embroidered boho blouse white" },
  { sku: "CAS-036", name: "Vestido Suéter Acanalado Gris", price: 699, query: "woman ribbed sweater dress grey" },
  { sku: "CAS-037", name: "Chaleco Tejido Punto Trenzado", price: 549, query: "woman cable knit vest fashion" },
  { sku: "CAS-038", name: "Playera Estampado Gráfico Retro", price: 349, query: "woman retro graphic tshirt fashion" },
  { sku: "CAS-039", name: "Falda Denim Botones Frontales", price: 549, query: "woman denim button front skirt" },
  { sku: "CAS-040", name: "Vestido Camisero Rayas Manga Corta", price: 649, query: "woman striped shirt dress short sleeve" },
];

const SPORT_PRODUCTS = [
  { sku: "SPO-001", name: "Leggings Alta Compresión Negro", price: 449, query: "woman black compression leggings activewear" },
  { sku: "SPO-002", name: "Leggings Estampado Print Digital", price: 499, query: "woman printed leggings activewear" },
  { sku: "SPO-003", name: "Top Deportivo Bralette Soporte Medio", price: 349, query: "woman sports bra medium support" },
  { sku: "SPO-004", name: "Bra Deportivo Alto Impacto", price: 449, query: "woman high impact sports bra" },
  { sku: "SPO-005", name: "Conjunto Deportivo Legging + Top Coral", price: 899, query: "woman matching activewear set coral" },
  { sku: "SPO-006", name: "Short Deportivo Running Gris", price: 349, query: "woman running shorts grey activewear" },
  { sku: "SPO-007", name: "Short Ciclista Compresión Negro", price: 349, query: "woman cycling shorts compression black" },
  { sku: "SPO-008", name: "Sudadera Deportiva Oversize Gris Jaspe", price: 599, query: "woman oversized athletic hoodie heather grey" },
  { sku: "SPO-009", name: "Chamarra Rompevientos Ligera Neón", price: 749, query: "woman windbreaker jacket neon activewear" },
  { sku: "SPO-010", name: "Playera Técnica Dry-Fit Blanco", price: 349, query: "woman dry fit athletic shirt white" },
  { sku: "SPO-011", name: "Playera Técnica Manga Larga UV", price: 399, query: "woman long sleeve UV athletic shirt" },
  { sku: "SPO-012", name: "Jogger Deportivo Fleece Negro", price: 599, query: "woman fleece jogger pants black" },
  { sku: "SPO-013", name: "Jogger Deportivo Cargo Verde Militar", price: 649, query: "woman cargo jogger pants olive green" },
  { sku: "SPO-014", name: "Falda Short Tenis Blanca", price: 449, query: "woman tennis skirt white activewear" },
  { sku: "SPO-015", name: "Chaleco Deportivo Acolchado", price: 649, query: "woman quilted athletic vest" },
  { sku: "SPO-016", name: "Conjunto Yoga Legging + Crop Lavanda", price: 849, query: "woman yoga set lavender activewear" },
  { sku: "SPO-017", name: "Legging Push Up Efecto Levantamiento", price: 599, query: "woman push up leggings activewear" },
  { sku: "SPO-018", name: "Top Cruzado Yoga Sin Costuras", price: 399, query: "woman seamless yoga top crossover" },
  { sku: "SPO-019", name: "Sudadera Zip Up Media Cremallera", price: 649, query: "woman half zip athletic sweatshirt" },
  { sku: "SPO-020", name: "Chamarra Térmica Running Invierno", price: 899, query: "woman thermal running jacket winter" },
  { sku: "SPO-021", name: "Short Básico Entrenamiento Azul Marino", price: 299, query: "woman training shorts navy blue" },
  { sku: "SPO-022", name: "Playera Sin Mangas Tank Top Deportivo", price: 299, query: "woman athletic tank top" },
  { sku: "SPO-023", name: "Legging Capri 3/4 Estampado", price: 449, query: "woman capri leggings printed activewear" },
  { sku: "SPO-024", name: "Conjunto Deportivo Sudadera + Jogger", price: 999, query: "woman matching tracksuit set" },
  { sku: "SPO-025", name: "Top Deportivo Espalda Cruzada", price: 399, query: "woman cross back sports top" },
  { sku: "SPO-026", name: "Chamarra Softshell Impermeable", price: 999, query: "woman softshell waterproof jacket" },
  { sku: "SPO-027", name: "Legging Tiro Alto Control Abdomen", price: 549, query: "woman high waist leggings tummy control" },
  { sku: "SPO-028", name: "Short Falda Pilates Rosa", price: 399, query: "woman pilates skort pink activewear" },
  { sku: "SPO-029", name: "Sudadera Crop Deportiva Estampada", price: 449, query: "woman cropped athletic sweatshirt printed" },
  { sku: "SPO-030", name: "Conjunto Running Playera + Short", price: 699, query: "woman running outfit set" },
];

const CALZADO_PRODUCTS = [
  { sku: "CAL-001", name: "Tenis Blancos Urbanos", price: 699, query: "woman white sneakers fashion" },
  { sku: "CAL-002", name: "Tenis Plataforma Chunky", price: 799, query: "woman platform chunky sneakers fashion" },
  { sku: "CAL-003", name: "Zapatilla Stiletto Negro", price: 899, query: "woman black stiletto heels" },
  { sku: "CAL-004", name: "Sandalias Tacón Cuadrado Nude", price: 749, query: "woman block heel sandals nude" },
  { sku: "CAL-005", name: "Botines Punta Fina Café", price: 899, query: "woman pointed toe ankle boots brown" },
  { sku: "CAL-006", name: "Botas Altas Cuero Negro", price: 1199, query: "woman knee high leather boots black" },
  { sku: "CAL-007", name: "Mocasines Clásicos Camel", price: 649, query: "woman loafers camel fashion" },
  { sku: "CAL-008", name: "Sandalias Planas Trenzadas", price: 449, query: "woman flat braided sandals" },
  { sku: "CAL-009", name: "Zapatos Oxford Vintage", price: 749, query: "woman oxford shoes vintage fashion" },
  { sku: "CAL-010", name: "Huaraches Artesanales Mexicanos", price: 549, query: "mexican huaraches sandals woman" },
  { sku: "CAL-011", name: "Sandalias Gladiador Negro", price: 599, query: "woman gladiator sandals black" },
  { sku: "CAL-012", name: "Tenis Slip-On Casual", price: 599, query: "woman slip on sneakers casual" },
  { sku: "CAL-013", name: "Zapatillas Ballet Flats Rosa", price: 499, query: "woman ballet flats pink" },
  { sku: "CAL-014", name: "Botines Chelsea Ante", price: 949, query: "woman suede chelsea boots" },
  { sku: "CAL-015", name: "Sandalias Con Plataforma Verano", price: 649, query: "woman platform sandals summer" },
];

const BOLSAS_PRODUCTS = [
  { sku: "BOL-001", name: "Bolsa Tote Cuero Grande", price: 999, query: "woman leather tote bag large" },
  { sku: "BOL-002", name: "Bolsa Crossbody Mini", price: 549, query: "woman mini crossbody bag" },
  { sku: "BOL-003", name: "Cartera Clutch Elegante", price: 699, query: "woman elegant clutch purse" },
  { sku: "BOL-004", name: "Mochila Urbana Casual", price: 799, query: "woman casual urban backpack" },
  { sku: "BOL-005", name: "Bolsa Bandolera Cuero Café", price: 849, query: "woman brown leather shoulder bag" },
  { sku: "BOL-006", name: "Bolsa Tejida Artesanal", price: 599, query: "woman woven straw bag" },
  { sku: "BOL-007", name: "Cartera Cadena Dorada", price: 649, query: "woman gold chain purse" },
  { sku: "BOL-008", name: "Bolsa Shopper Lona", price: 449, query: "woman canvas shopper bag" },
  { sku: "BOL-009", name: "Bolsa Saddle Vintage", price: 899, query: "woman saddle bag vintage fashion" },
  { sku: "BOL-010", name: "Monedero Piel Pequeño", price: 349, query: "woman small leather wallet" },
  { sku: "BOL-011", name: "Bolsa Hobo Suave", price: 799, query: "woman hobo bag soft leather" },
  { sku: "BOL-012", name: "Cartera Sobre Satinada", price: 549, query: "woman satin envelope clutch" },
  { sku: "BOL-013", name: "Bolsa Baguette Retro", price: 699, query: "woman baguette bag retro fashion" },
  { sku: "BOL-014", name: "Mochila Piel Mini", price: 899, query: "woman mini leather backpack" },
  { sku: "BOL-015", name: "Bolsa Playera Transparente", price: 399, query: "woman clear beach bag" },
];

async function login() {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ contact: EMAIL, password: PASSWORD }),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status} ${await res.text()}`);
  const body = await res.json();
  return body.accessToken;
}

async function authFetch(path, token, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  });
  if (!res.ok) throw new Error(`${options.method || "GET"} ${path} -> ${res.status} ${await res.text()}`);
  return res.status === 204 ? null : res.json();
}

async function ensureCategory(name, slug, token) {
  const existing = await (await fetch(`${BASE_URL}/api/categories`)).json();
  const found = existing.find((c) => c.slug === slug);
  if (found) return found.id;

  const created = await authFetch("/api/categories", token, {
    method: "POST",
    body: JSON.stringify({ name, slug }),
  });
  return created.id;
}

const imageCache = new Map();

async function searchImage(query) {
  if (imageCache.has(query)) return imageCache.get(query);

  const res = await fetch(`https://api.pexels.com/v1/search?query=${encodeURIComponent(query)}&per_page=1&orientation=portrait`, {
    headers: { Authorization: PEXELS_API_KEY },
  });
  if (!res.ok) throw new Error(`Pexels search failed for "${query}": ${res.status}`);
  const body = await res.json();
  const url = body.photos?.[0]?.src?.large;
  if (!url) throw new Error(`No Pexels image found for "${query}"`);

  imageCache.set(query, url);
  return url;
}

async function createProductWithStock(item, categoryId, token) {
  const imageUrl = await searchImage(item.query);

  const product = await authFetch("/api/products", token, {
    method: "POST",
    body: JSON.stringify({
      name: item.name,
      description: item.name,
      basePrice: item.price,
      currency: "MXN",
      sku: item.sku,
      categoryId,
      images: [{ url: imageUrl, position: 0, primary: true }],
    }),
  });

  const variantId = product.variants?.[0]?.id;
  if (!variantId) throw new Error(`No default variant returned for ${item.sku}`);

  const quantity = Math.floor(Math.random() * 46) + 15; // 15–60 units
  await authFetch(`/api/products/${product.id}/variants/${variantId}/stock`, token, {
    method: "POST",
    body: JSON.stringify({ quantity }),
  });

  return { sku: item.sku, name: item.name, quantity };
}

const GROUPS = [
  { key: "casual", name: "Moda Casual Mujer", slug: "moda-casual-mujer", items: CASUAL_PRODUCTS },
  { key: "sport", name: "Ropa Deportiva Mujer", slug: "ropa-deportiva-mujer", items: SPORT_PRODUCTS },
  { key: "calzado", name: "Calzado", slug: "calzado", items: CALZADO_PRODUCTS },
  { key: "bolsas", name: "Bolsas y Carteras", slug: "bolsas-y-carteras", items: BOLSAS_PRODUCTS },
];

async function main() {
  console.log("Logging in...");
  const token = await login();

  const only = process.env.SEED_ONLY ? process.env.SEED_ONLY.split(",") : null;
  const groups = only ? GROUPS.filter((g) => only.includes(g.key)) : GROUPS;

  console.log("Ensuring categories...");
  const all = [];
  for (const group of groups) {
    const categoryId = await ensureCategory(group.name, group.slug, token);
    all.push(...group.items.map((p) => ({ ...p, categoryId })));
  }

  const created = [];
  const failed = [];

  for (const [index, item] of all.entries()) {
    try {
      const result = await createProductWithStock(item, item.categoryId, token);
      created.push(result);
      console.log(`[${index + 1}/${all.length}] OK   ${result.sku} — ${result.name} (stock: ${result.quantity})`);
    } catch (err) {
      failed.push({ sku: item.sku, error: err.message });
      console.log(`[${index + 1}/${all.length}] FAIL ${item.sku} — ${err.message}`);
    }
    await sleep(150); // stay well within Pexels' rate limit
  }

  console.log(`\nDone. Created: ${created.length}. Failed: ${failed.length}.`);
  if (failed.length) {
    console.log("Failed SKUs:", failed.map((f) => f.sku).join(", "));
  }
}

main().catch((err) => {
  console.error("Fatal error:", err);
  process.exit(1);
});
