#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cert_dir="$project_dir/docker/mosquitto/certs"
password="${TB_MQTT_TRUSTSTORE_PASSWORD:-changeit}"

mkdir -p "$cert_dir"
rm -f "$cert_dir/ca.key" "$cert_dir/ca.crt" "$cert_dir/server.key" \
  "$cert_dir/server.csr" "$cert_dir/server.crt" "$cert_dir/server.ext" "$cert_dir/ca.p12"

openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -subj "/CN=thingsboard-mqtt-demo-ca" \
  -keyout "$cert_dir/ca.key" -out "$cert_dir/ca.crt"

openssl req -newkey rsa:2048 -nodes \
  -subj "/CN=localhost" \
  -keyout "$cert_dir/server.key" -out "$cert_dir/server.csr"

cat > "$cert_dir/server.ext" <<'EOF'
subjectAltName=DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth
EOF

openssl x509 -req -days 365 \
  -in "$cert_dir/server.csr" \
  -CA "$cert_dir/ca.crt" -CAkey "$cert_dir/ca.key" -CAcreateserial \
  -out "$cert_dir/server.crt" -extfile "$cert_dir/server.ext"

keytool -importcert -noprompt \
  -alias thingsboard-mqtt-demo-ca \
  -file "$cert_dir/ca.crt" \
  -keystore "$cert_dir/ca.p12" \
  -storetype PKCS12 \
  -storepass "$password"

rm -f "$cert_dir/ca.key" "$cert_dir/server.csr" "$cert_dir/server.ext" "$cert_dir/ca.srl"
chmod 600 "$cert_dir/server.key"
printf 'TLS files generated under %s\n' "$cert_dir"
printf 'Truststore password: %s\n' "$password"
