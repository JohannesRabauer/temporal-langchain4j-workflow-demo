# LangChain4j Meets Temporal — Vacation Approval Demo

Baseline project for a live-coding session building a vacation approval system with
[LangChain4j](https://docs.langchain4j.dev/), [Temporal](https://temporal.io/), and
[Quarkus](https://quarkus.io/) + [Qute](https://quarkus.io/guides/qute).

The infrastructure here already works: Docker Compose brings up a Temporal server, its
Postgres persistence store, the Temporal Web UI, and a GPU-accelerated Ollama instance for
LangChain4j. The Quarkus app boots, connects to Temporal, and renders a placeholder page —
but the actual workflow, activities, and AI reasoning are intentionally left empty. That's
what we build live.

## What we'll build live

- A Temporal workflow that models a vacation request from submission to decision
- A LangChain4j-backed activity that has the AI assess/summarize the request
- A human-in-the-loop approval step using a Temporal **Signal**
- A demonstration of crash recovery: restart the app mid-workflow and watch it resume
  exactly where it left off

## Prerequisites

- Docker + Docker Compose
- An NVIDIA GPU with the [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html)
  installed (for GPU-accelerated Ollama). Without a GPU, remove the `deploy.resources.reservations.devices`
  block from the `ollama` service in `docker-compose.yml` to fall back to CPU inference.
- JDK 21 and Maven — only needed if you want to run the app on the host via `quarkus:dev`;
  the wrapper (`./mvnw`) handles the Maven version, and the containerized build stage
  brings its own JDK.

## Running everything in Docker

```bash
docker compose up -d --build
```

This builds the Quarkus app image (compiling it from source inside the build stage — no
local Maven run required) and starts the full stack: Temporal, Postgres, Temporal UI,
Ollama, and the app itself.

One-time step after the first `up`: pull the model Ollama will serve (a multi-GB download —
do this **before** going live, not on stream):

```bash
docker exec ollama ollama pull llama3.1
```

URLs:

- App: <http://localhost:8081>
- Temporal Web UI: <http://localhost:8080>
- Ollama API: <http://localhost:11434>

To demonstrate workflow recovery once the real workflow exists, restart just the app
container and watch Temporal resume it:

```bash
docker compose restart app
```

## Running the app on the host (live-coding inner loop)

For hot reload while writing code, run only the infrastructure in Docker and the app on
the host:

```bash
docker compose up -d postgresql temporal temporal-admin-tools temporal-ui ollama
./mvnw quarkus:dev
```

The app listens on <http://localhost:8081> either way; the Dev UI is at
<http://localhost:8081/q/dev/>.

## Using OpenAI instead of Ollama

The baseline uses local, GPU-accelerated Ollama so the demo needs no API key and incurs no
per-token cost while streaming. To use OpenAI instead, swap the
`io.quarkiverse.langchain4j:quarkus-langchain4j-ollama` dependency in `pom.xml` for
`io.quarkiverse.langchain4j:quarkus-langchain4j-openai`, and set
`quarkus.langchain4j.openai.api-key` (e.g. via the `OPENAI_API_KEY` env var) instead of the
`quarkus.langchain4j.ollama.*` properties.

## Project layout

- `workflow/` — Temporal workflow interfaces and implementations (empty — built live)
- `activity/` — Temporal activities invoked by the workflow (empty — built live)
- `ai/` — LangChain4j AI services (empty — built live)
- `web/` — REST resources and Qute-rendered pages
