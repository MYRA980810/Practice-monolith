# livecomerce — Project Instructions

## graphify Knowledge Graph (RAG)

A knowledge graph of this codebase lives at `graphify-out/`. **Use it before reading files.**

### When to query the graph

- Any question about which classes/modules are involved in a feature
- Tracing a flow (e.g. "how does payment confirmation reach the store?")
- Finding cross-module dependencies or event-driven connections
- Understanding why a class exists or what it connects to
- Before editing code that touches multiple bounded contexts

### How to query

```bash
graphify query "<your question>"          # broad BFS — what is X connected to?
graphify query "<your question>" --dfs    # DFS — trace a specific path
graphify path "ClassA" "ClassB"           # shortest path between two concepts
graphify explain "ClassName"              # full neighborhood of a single node
```

Examples:
```bash
graphify query "how does order payment confirmation flow through the system"
graphify query "what modules depend on Plan"
graphify path "BillingController" "Store"
graphify explain "StockEventListener"
```

### Graph stats (last run)

- **943 nodes**, **1203 edges**, **134 communities**
- Top god nodes: `Plan` (20 edges), `Order`, `Product`, `UserPrincipal`
- Output files: `graphify-out/graph.html`, `graphify-out/graph.json`, `graphify-out/GRAPH_REPORT.md`

### Keeping the graph current

After adding or modifying files, update incrementally:

```bash
graphify query ""   # or
# run /graphify --update  in Claude Code to re-extract only changed files
```

## Architecture

Spring Boot monolith structured as Spring Modulith with hexagonal architecture per bounded context.

Bounded contexts: `auth`, `catalog`, `order`, `billing`, `store`, `payment`, `logistics`, `live`, `notification`

Each context follows: `api` → `application` (use cases + services) → `domain` → `infrastructure`

Cross-module communication is event-driven via Spring's `ApplicationEventPublisher` with `@ApplicationModuleListener`.
