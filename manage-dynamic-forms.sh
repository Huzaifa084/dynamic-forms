#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/apps/dynamic-forms}"
PID_FILE="${PID_FILE:-$APP_HOME/application.pid}"
LOG_FILE="${LOG_FILE:-$APP_HOME/app.log}"
GRADLE_CMD="${GRADLE_CMD:-$APP_HOME/gradlew}"
JAVA_CMD="${JAVA_CMD:-java}"
JAVA_OPTS="${JAVA_OPTS:-}"
APP_ARGS="${APP_ARGS:-}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_NAME="${DB_NAME:-demo}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-test}"

usage() {
  cat <<'EOF'
Usage: manage-dynamic-forms.sh <command>

Commands:
  run         Build (if needed) and start the application
  stop        Stop the running application
  restart     Stop (if running) and start again (alias: rerun)
  update      git pull --rebase, rebuild, and restart the app
  logs        Follow the application log (Ctrl+C to exit)
  reset-db    Drop and recreate the configured database
  status      Show whether the application process is running
  build       Run the Gradle build only (bootJar)

Environment overrides:
  APP_HOME, JAVA_CMD, JAVA_OPTS, APP_ARGS, DB_HOST, DB_PORT, DB_NAME,
  DB_USER, DB_PASSWORD, PID_FILE, LOG_FILE
EOF
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "Missing required command: $1"
    exit 1
  fi
}

ensure_app_home() {
  if [[ ! -d "$APP_HOME" ]]; then
    log "APP_HOME does not exist: $APP_HOME"
    exit 1
  fi
}

ensure_gradle() {
  if [[ ! -x "$GRADLE_CMD" ]]; then
    log "Gradle wrapper not found or not executable at $GRADLE_CMD"
    exit 1
  fi
}

jar_path() {
  local primary=""
  local fallback=""

  shopt -s nullglob
  for jar in "$APP_HOME"/build/libs/*.jar; do
    [[ -e "$jar" ]] || continue
    if [[ "$jar" == *-plain.jar ]]; then
      if [[ -z "$fallback" || "$jar" -nt "$fallback" ]]; then
        fallback="$jar"
      fi
    else
      if [[ -z "$primary" || "$jar" -nt "$primary" ]]; then
        primary="$jar"
      fi
    fi
  done
  shopt -u nullglob

  if [[ -n "$primary" ]]; then
    printf '%s\n' "$primary"
  elif [[ -n "$fallback" ]]; then
    printf '%s\n' "$fallback"
  else
    return 1
  fi
}

is_running() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid=$(<"$PID_FILE")
    if kill -0 "$pid" >/dev/null 2>&1; then
      return 0
    fi
    rm -f "$PID_FILE"
  fi
  return 1
}

build_app() {
  ensure_app_home
  ensure_gradle
  log "Building application (Gradle bootJar)"
  (cd "$APP_HOME" && "$GRADLE_CMD" bootJar)
}

start_app() {
  ensure_app_home
  require_cmd "$JAVA_CMD"
  if is_running; then
    log "Application already running (PID $(<"$PID_FILE"))"
    return 0
  fi

  local jar
  jar=$(jar_path || true)
  if [[ -z "$jar" ]]; then
    build_app
    jar=$(jar_path)
  fi

  mkdir -p "$(dirname "$LOG_FILE")"
  log "Starting application using $jar"
  (
    cd "$APP_HOME"
    nohup "$JAVA_CMD" $JAVA_OPTS -jar "$jar" $APP_ARGS >>"$LOG_FILE" 2>&1 &
    echo $! >"$PID_FILE"
  )

  sleep 1
  if is_running; then
    log "Application started (PID $(<"$PID_FILE"))"
  else
    log "Application failed to start; check $LOG_FILE"
    exit 1
  fi
}

stop_app() {
  if ! is_running; then
    log "Application is not running"
    return 0
  fi

  local pid
  pid=$(<"$PID_FILE")
  log "Stopping application (PID $pid)"
  kill "$pid" >/dev/null 2>&1 || true

  for _ in {1..30}; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      sleep 1
    else
      break
    fi
  done

  if kill -0 "$pid" >/dev/null 2>&1; then
    log "Force killing PID $pid"
    kill -9 "$pid" >/dev/null 2>&1 || true
  fi

  rm -f "$PID_FILE"
  log "Application stopped"
}

status_app() {
  if is_running; then
    log "Application is running (PID $(<"$PID_FILE"))"
  else
    log "Application is not running"
  fi
}

update_app() {
  ensure_app_home
  require_cmd git
  log "Fetching latest code"
  (cd "$APP_HOME" && git fetch --all)
  log "Rebasing onto tracked branch"
  (cd "$APP_HOME" && git pull --rebase)
  build_app
  stop_app || true
  start_app
}

reset_db() {
  require_cmd psql
  log "Terminating active connections for database $DB_NAME"
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" >/dev/null

  log "Dropping database $DB_NAME"
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -c "DROP DATABASE IF EXISTS \"${DB_NAME}\";"

  log "Creating database $DB_NAME"
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -c "CREATE DATABASE \"${DB_NAME}\";"

  log "Database reset complete"
}

show_logs() {
  if [[ ! -f "$LOG_FILE" ]]; then
    log "Log file not found yet: $LOG_FILE"
  fi
  log "Tailing logs (Ctrl+C to stop)"
  exec tail -f "$LOG_FILE"
}

command="${1:-}"
case "$command" in
  run)
    build_app
    start_app
    ;;
  restart|rerun)
    stop_app || true
    build_app
    start_app
    ;;
  stop)
    stop_app
    ;;
  update)
    update_app
    ;;
  logs)
    show_logs
    ;;
  reset-db)
    reset_db
    ;;
  status)
    status_app
    ;;
  build)
    build_app
    ;;
  ""|-h|--help)
    usage
    ;;
  *)
    log "Unknown command: $command"
    usage
    exit 1
    ;;
esac
