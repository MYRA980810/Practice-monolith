// Seeds demo products (women's fashion) into an existing seller account, grouped by category.
// Test data only — products are created with no images (empty images array).
// Requires Node 18+. The API has no idempotency guard on SKU — re-running a group duplicates its products.
//
// Required env vars:
//   API_BASE_URL   e.g. http://localhost:8080 (default)
//   SEED_EMAIL     seller account email/phone used to log in
//   SEED_PASSWORD  seller account password
//
// Optional env var:
//   SEED_ONLY      comma-separated group keys to seed (casual, sport, calzado, bolsas, vestidos, pantalones, ropa-interior). Defaults to all groups.
//
// Usage:
//   API_BASE_URL=http://localhost:8080 SEED_EMAIL=... SEED_PASSWORD=... node scripts/seed-demo-products.mjs
//   SEED_ONLY=vestidos,pantalones,ropa-interior ... node scripts/seed-demo-products.mjs   # seed just the new categories

const BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";
const EMAIL = process.env.SEED_EMAIL;
const PASSWORD = process.env.SEED_PASSWORD;

if (!EMAIL || !PASSWORD) {
  console.error("Missing required env vars: SEED_EMAIL, SEED_PASSWORD");
  process.exit(1);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const CASUAL_PRODUCTS = [
  { sku: "CAS-001", name: "Blusa de Lino Manga Larga Beige", price: 449 },
  { sku: "CAS-002", name: "Playera Básica Algodón Blanco", price: 299 },
  { sku: "CAS-003", name: "Playera Oversize Estampada Negro", price: 349 },
  { sku: "CAS-004", name: "Vestido Midi Floral Verano", price: 799 },
  { sku: "CAS-005", name: "Vestido Camisero Denim", price: 749 },
  { sku: "CAS-006", name: "Falda Plisada Midi Café", price: 549 },
  { sku: "CAS-007", name: "Falda Cargo Satinada Kaki", price: 599 },
  { sku: "CAS-008", name: "Jeans Mom Fit Azul Claro", price: 799 },
  { sku: "CAS-009", name: "Jeans Skinny Tiro Alto Negro", price: 749 },
  { sku: "CAS-010", name: "Pantalón Palazzo Fluido Terracota", price: 649 },
  { sku: "CAS-011", name: "Short Denim Deslavado", price: 449 },
  { sku: "CAS-012", name: "Blazer Oversize Estructurado Camel", price: 999 },
  { sku: "CAS-013", name: "Cardigan Tejido Punto Mostaza", price: 649 },
  { sku: "CAS-014", name: "Suéter Cuello Alto Crema", price: 599 },
  { sku: "CAS-015", name: "Sudadera Oversize Crop Gris", price: 549 },
  { sku: "CAS-016", name: "Chaleco Acolchado Sin Mangas Negro", price: 699 },
  { sku: "CAS-017", name: "Kimono Estampado Playero", price: 499 },
  { sku: "CAS-018", name: "Jumpsuit Palazzo Elegante Vino", price: 899 },
  { sku: "CAS-019", name: "Top Cropped Canalé Rosa", price: 299 },
  { sku: "CAS-020", name: "Blusa Satinada Manga Globo Champagne", price: 599 },
  { sku: "CAS-021", name: "Camisa Oxford Rayas Azul", price: 549 },
  { sku: "CAS-022", name: "Vestido Wrap Envolvente Estampado", price: 849 },
  { sku: "CAS-023", name: "Falda Lápiz Tweed Negro", price: 649 },
  { sku: "CAS-024", name: "Pantalón Wide Leg Lino Blanco", price: 699 },
  { sku: "CAS-025", name: "Chamarra Mezclilla Clásica Azul", price: 799 },
  { sku: "CAS-026", name: "Chaqueta Biker Vegana Negra", price: 899 },
  { sku: "CAS-027", name: "Sudadera con Capucha Básica Lavanda", price: 549 },
  { sku: "CAS-028", name: "Blusa Cuello V Seda Artificial Coral", price: 599 },
  { sku: "CAS-029", name: "Vestido Cami Slip Satín Negro", price: 699 },
  { sku: "CAS-030", name: "Overol Corto Denim Claro", price: 649 },
  { sku: "CAS-031", name: "Top Halter Anudado Blanco", price: 349 },
  { sku: "CAS-032", name: "Falda Midi Satinada Esmeralda", price: 599 },
  { sku: "CAS-033", name: "Pantalón Culotte Cuadros", price: 649 },
  { sku: "CAS-034", name: "Cárdigan Largo Oversize Beige", price: 749 },
  { sku: "CAS-035", name: "Blusa Bordada Boho Blanca", price: 549 },
  { sku: "CAS-036", name: "Vestido Suéter Acanalado Gris", price: 699 },
  { sku: "CAS-037", name: "Chaleco Tejido Punto Trenzado", price: 549 },
  { sku: "CAS-038", name: "Playera Estampado Gráfico Retro", price: 349 },
  { sku: "CAS-039", name: "Falda Denim Botones Frontales", price: 549 },
  { sku: "CAS-040", name: "Vestido Camisero Rayas Manga Corta", price: 649 },
];

const SPORT_PRODUCTS = [
  { sku: "SPO-001", name: "Leggings Alta Compresión Negro", price: 449 },
  { sku: "SPO-002", name: "Leggings Estampado Print Digital", price: 499 },
  { sku: "SPO-003", name: "Top Deportivo Bralette Soporte Medio", price: 349 },
  { sku: "SPO-004", name: "Bra Deportivo Alto Impacto", price: 449 },
  { sku: "SPO-005", name: "Conjunto Deportivo Legging + Top Coral", price: 899 },
  { sku: "SPO-006", name: "Short Deportivo Running Gris", price: 349 },
  { sku: "SPO-007", name: "Short Ciclista Compresión Negro", price: 349 },
  { sku: "SPO-008", name: "Sudadera Deportiva Oversize Gris Jaspe", price: 599 },
  { sku: "SPO-009", name: "Chamarra Rompevientos Ligera Neón", price: 749 },
  { sku: "SPO-010", name: "Playera Técnica Dry-Fit Blanco", price: 349 },
  { sku: "SPO-011", name: "Playera Técnica Manga Larga UV", price: 399 },
  { sku: "SPO-012", name: "Jogger Deportivo Fleece Negro", price: 599 },
  { sku: "SPO-013", name: "Jogger Deportivo Cargo Verde Militar", price: 649 },
  { sku: "SPO-014", name: "Falda Short Tenis Blanca", price: 449 },
  { sku: "SPO-015", name: "Chaleco Deportivo Acolchado", price: 649 },
  { sku: "SPO-016", name: "Conjunto Yoga Legging + Crop Lavanda", price: 849 },
  { sku: "SPO-017", name: "Legging Push Up Efecto Levantamiento", price: 599 },
  { sku: "SPO-018", name: "Top Cruzado Yoga Sin Costuras", price: 399 },
  { sku: "SPO-019", name: "Sudadera Zip Up Media Cremallera", price: 649 },
  { sku: "SPO-020", name: "Chamarra Térmica Running Invierno", price: 899 },
  { sku: "SPO-021", name: "Short Básico Entrenamiento Azul Marino", price: 299 },
  { sku: "SPO-022", name: "Playera Sin Mangas Tank Top Deportivo", price: 299 },
  { sku: "SPO-023", name: "Legging Capri 3/4 Estampado", price: 449 },
  { sku: "SPO-024", name: "Conjunto Deportivo Sudadera + Jogger", price: 999 },
  { sku: "SPO-025", name: "Top Deportivo Espalda Cruzada", price: 399 },
  { sku: "SPO-026", name: "Chamarra Softshell Impermeable", price: 999 },
  { sku: "SPO-027", name: "Legging Tiro Alto Control Abdomen", price: 549 },
  { sku: "SPO-028", name: "Short Falda Pilates Rosa", price: 399 },
  { sku: "SPO-029", name: "Sudadera Crop Deportiva Estampada", price: 449 },
  { sku: "SPO-030", name: "Conjunto Running Playera + Short", price: 699 },
];

const CALZADO_PRODUCTS = [
  { sku: "CAL-001", name: "Tenis Blancos Urbanos", price: 699 },
  { sku: "CAL-002", name: "Tenis Plataforma Chunky", price: 799 },
  { sku: "CAL-003", name: "Zapatilla Stiletto Negro", price: 899 },
  { sku: "CAL-004", name: "Sandalias Tacón Cuadrado Nude", price: 749 },
  { sku: "CAL-005", name: "Botines Punta Fina Café", price: 899 },
  { sku: "CAL-006", name: "Botas Altas Cuero Negro", price: 1199 },
  { sku: "CAL-007", name: "Mocasines Clásicos Camel", price: 649 },
  { sku: "CAL-008", name: "Sandalias Planas Trenzadas", price: 449 },
  { sku: "CAL-009", name: "Zapatos Oxford Vintage", price: 749 },
  { sku: "CAL-010", name: "Huaraches Artesanales Mexicanos", price: 549 },
  { sku: "CAL-011", name: "Sandalias Gladiador Negro", price: 599 },
  { sku: "CAL-012", name: "Tenis Slip-On Casual", price: 599 },
  { sku: "CAL-013", name: "Zapatillas Ballet Flats Rosa", price: 499 },
  { sku: "CAL-014", name: "Botines Chelsea Ante", price: 949 },
  { sku: "CAL-015", name: "Sandalias Con Plataforma Verano", price: 649 },
];

const VESTIDOS_PRODUCTS = [
  { sku: "VES-001", name: "Vestido Midi Floral Verano", price: 799 },
  { sku: "VES-002", name: "Vestido Camisero Denim", price: 749 },
  { sku: "VES-003", name: "Vestido Wrap Envolvente Estampado", price: 849 },
  { sku: "VES-004", name: "Vestido Cami Slip Satín Negro", price: 699 },
  { sku: "VES-005", name: "Vestido Suéter Acanalado Gris", price: 699 },
  { sku: "VES-006", name: "Vestido Largo Fiesta Plisado", price: 999 },
  { sku: "VES-007", name: "Vestido Corto Casual Rayas", price: 549 },
  { sku: "VES-008", name: "Vestido Halter Escote Cruzado", price: 649 },
  { sku: "VES-009", name: "Vestido Tubo Ajustado Negro", price: 649 },
  { sku: "VES-010", name: "Vestido Boho Bordado Blanco", price: 749 },
  { sku: "VES-011", name: "Vestido Cóctel Lentejuelas", price: 1099 },
  { sku: "VES-012", name: "Vestido Playero Kimono", price: 499 },
];

const PANTALONES_PRODUCTS = [
  { sku: "PAN-001", name: "Jeans Mom Fit Azul Claro", price: 799 },
  { sku: "PAN-002", name: "Jeans Skinny Tiro Alto Negro", price: 749 },
  { sku: "PAN-003", name: "Pantalón Palazzo Fluido Terracota", price: 649 },
  { sku: "PAN-004", name: "Pantalón Wide Leg Lino Blanco", price: 699 },
  { sku: "PAN-005", name: "Pantalón Culotte Cuadros", price: 649 },
  { sku: "PAN-006", name: "Jogger Deportivo Fleece Negro", price: 599 },
  { sku: "PAN-007", name: "Pantalón Cargo Satinado Kaki", price: 599 },
  { sku: "PAN-008", name: "Pantalón Recto Vestir Negro", price: 699 },
  { sku: "PAN-009", name: "Pantalón Campana Mezclilla", price: 749 },
  { sku: "PAN-010", name: "Legging Tiro Alto Control Abdomen", price: 549 },
  { sku: "PAN-011", name: "Pantalón Capri Lino Beige", price: 599 },
  { sku: "PAN-012", name: "Pantalón Deportivo Cargo Verde Militar", price: 649 },
];

const ROPA_INTERIOR_PRODUCTS = [
  { sku: "INT-001", name: "Brasier Push Up Encaje Negro", price: 299 },
  { sku: "INT-002", name: "Panty Bikini Algodón Blanco", price: 99 },
  { sku: "INT-003", name: "Conjunto Lencería Encaje Rojo", price: 449 },
  { sku: "INT-004", name: "Brasier Deportivo Sin Varilla", price: 249 },
  { sku: "INT-005", name: "Panty Cachetero Microfibra", price: 129 },
  { sku: "INT-006", name: "Brasier Balconette Satín Nude", price: 279 },
  { sku: "INT-007", name: "Body Moldeador Sin Costuras", price: 399 },
  { sku: "INT-008", name: "Pijama Short Set Algodón", price: 349 },
  { sku: "INT-009", name: "Brasier Triángulo Sin Aro", price: 249 },
  { sku: "INT-010", name: "Panty Tanga Encaje Negro", price: 99 },
  { sku: "INT-011", name: "Fajate Moldeador Cintura Alta", price: 499 },
  { sku: "INT-012", name: "Camisón Satín Botones", price: 379 },
];

const BOLSAS_PRODUCTS = [
  { sku: "BOL-001", name: "Bolsa Tote Cuero Grande", price: 999 },
  { sku: "BOL-002", name: "Bolsa Crossbody Mini", price: 549 },
  { sku: "BOL-003", name: "Cartera Clutch Elegante", price: 699 },
  { sku: "BOL-004", name: "Mochila Urbana Casual", price: 799 },
  { sku: "BOL-005", name: "Bolsa Bandolera Cuero Café", price: 849 },
  { sku: "BOL-006", name: "Bolsa Tejida Artesanal", price: 599 },
  { sku: "BOL-007", name: "Cartera Cadena Dorada", price: 649 },
  { sku: "BOL-008", name: "Bolsa Shopper Lona", price: 449 },
  { sku: "BOL-009", name: "Bolsa Saddle Vintage", price: 899 },
  { sku: "BOL-010", name: "Monedero Piel Pequeño", price: 349 },
  { sku: "BOL-011", name: "Bolsa Hobo Suave", price: 799 },
  { sku: "BOL-012", name: "Cartera Sobre Satinada", price: 549 },
  { sku: "BOL-013", name: "Bolsa Baguette Retro", price: 699 },
  { sku: "BOL-014", name: "Mochila Piel Mini", price: 899 },
  { sku: "BOL-015", name: "Bolsa Playera Transparente", price: 399 },
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

async function createProductWithStock(item, categoryId, token) {
  const product = await authFetch("/api/products", token, {
    method: "POST",
    body: JSON.stringify({
      name: item.name,
      description: item.name,
      basePrice: item.price,
      currency: "MXN",
      sku: item.sku,
      categoryId,
      images: [],
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
  { key: "vestidos", name: "Vestidos", slug: "vestidos", items: VESTIDOS_PRODUCTS },
  { key: "pantalones", name: "Pantalones", slug: "pantalones", items: PANTALONES_PRODUCTS },
  { key: "ropa-interior", name: "Ropa Interior", slug: "ropa-interior", items: ROPA_INTERIOR_PRODUCTS },
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
    await sleep(150); // stay well within the API's rate limits
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
