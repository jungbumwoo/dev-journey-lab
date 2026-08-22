SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

SENTINEL_COMPOSE := compose/replication-sentinel/compose.yml
CLUSTER_COMPOSE := compose/cluster/compose.yml

.PHONY: help doctor sentinel-up sentinel-status sentinel-down sentinel-cli \
	cluster-up cluster-status cluster-down cluster-cli client-image client-run \
	lab down destroy check

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

doctor: ## Check local prerequisites and Docker capacity
	@./scripts/doctor.sh

sentinel-up: ## Start primary, two replicas, and three Sentinels
	@docker compose -p "$${SENTINEL_PROJECT:-redis-sentinel-lab}" -f $(SENTINEL_COMPOSE) up -d
	@./compose/replication-sentinel/scripts/healthcheck.sh

sentinel-status: ## Verify and print the Sentinel topology
	@./compose/replication-sentinel/scripts/healthcheck.sh --verbose

sentinel-down: ## Stop Sentinel topology while preserving volumes
	@docker compose -p "$${SENTINEL_PROJECT:-redis-sentinel-lab}" -f $(SENTINEL_COMPOSE) down

sentinel-cli: ## Open redis-cli against the current Sentinel-discovered primary
	@./scripts/sentinel/cli.sh

cluster-up: ## Start six nodes, create the Cluster, and verify all slots
	@docker compose -p "$${CLUSTER_PROJECT:-redis-cluster-lab}" -f $(CLUSTER_COMPOSE) up -d
	@./compose/cluster/scripts/create-cluster.sh
	@./compose/cluster/scripts/healthcheck.sh

cluster-status: ## Verify and print the Cluster topology
	@./compose/cluster/scripts/healthcheck.sh --verbose

cluster-down: ## Stop Cluster topology while preserving volumes
	@docker compose -p "$${CLUSTER_PROJECT:-redis-cluster-lab}" -f $(CLUSTER_COMPOSE) down

cluster-cli: ## Open cluster-aware redis-cli inside the Docker network
	@docker compose -p "$${CLUSTER_PROJECT:-redis-cluster-lab}" -f $(CLUSTER_COMPOSE) exec toolbox redis-cli -c -h redis-cluster-1

client-image: ## Build the Java/Lettuce lab image
	@docker build -t "$${CLIENT_IMAGE:-redis-operations-client:local}" -f client-lab/Dockerfile .

client-run: ## Run Java scenario: make client-run TOPOLOGY=cluster SCENARIO=cluster-redirect
	@if [[ -z "$(TOPOLOGY)" || -z "$(SCENARIO)" ]]; then echo "TOPOLOGY and SCENARIO are required" >&2; exit 2; fi
	@if [[ "$(TOPOLOGY)" == "cluster" ]]; then \
		docker compose -p "$${CLUSTER_PROJECT:-redis-cluster-lab}" -f $(CLUSTER_COMPOSE) --profile client run --rm client-lab "$(SCENARIO)"; \
	elif [[ "$(TOPOLOGY)" == "sentinel" ]]; then \
		docker compose -p "$${SENTINEL_PROJECT:-redis-sentinel-lab}" -f $(SENTINEL_COMPOSE) --profile client run --rm client-lab "$(SCENARIO)"; \
	else echo "TOPOLOGY must be cluster or sentinel" >&2; exit 2; fi

lab: ## Run a lab: make lab LAB=replication-offset
	@if [[ -z "$(LAB)" ]]; then echo "LAB is required. Run ./scripts/run-lab.sh --list" >&2; exit 2; fi
	@./scripts/run-lab.sh "$(LAB)"

down: ## Stop both topologies and preserve volumes
	@docker compose -p "$${SENTINEL_PROJECT:-redis-sentinel-lab}" -f $(SENTINEL_COMPOSE) down --remove-orphans
	@docker compose -p "$${CLUSTER_PROJECT:-redis-cluster-lab}" -f $(CLUSTER_COMPOSE) down --remove-orphans

destroy: ## Delete only this lab's containers and volumes after confirmation
	@./scripts/destroy.sh

check: ## Static validation and Java tests
	@./scripts/check.sh
