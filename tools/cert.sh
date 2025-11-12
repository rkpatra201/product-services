#!/bin/bash

HOST="repo.maven.apache.org"
PORT=443
STOREPASS="changeit"
ALIAS="$HOST-cert"

# Find Java Home
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    echo "🔍 Auto-detected JAVA_HOME: $JAVA_HOME"
fi

TRUSTSTORE="$JAVA_HOME/lib/security/cacerts"

# Optional: take hostname as argument
if [ ! -z "$1" ]; then
  HOST="$1"
  ALIAS="$HOST-cert"
fi

echo "✅ Fetching certificate chain from $HOST:$PORT ..."
echo "🎯 Target keystore: $TRUSTSTORE"

# Step 1: Fetch the FULL certificate chain
echo "📜 Getting certificate chain..."
openssl s_client -connect "$HOST:$PORT" -showcerts </dev/null 2>/dev/null > "$HOST-chain.pem"

if [ $? -ne 0 ]; then
  echo "❌ Failed to fetch certificate from $HOST"
  exit 1
fi

# Step 2: Extract individual certificates from chain
echo "🔗 Extracting certificates from chain..."
csplit -f "$HOST-cert-" "$HOST-chain.pem" '/-----BEGIN CERTIFICATE-----/' '{*}' > /dev/null 2>&1

# Step 3: Import each certificate
cert_count=0
for cert_file in "$HOST-cert-"*; do
    if [ -s "$cert_file" ]; then
        cert_alias="$HOST-cert-$cert_count"
        echo "🔑 Importing certificate $cert_count (alias: $cert_alias)"
        
        keytool -importcert \
          -trustcacerts \
          -alias "$cert_alias" \
          -file "$cert_file" \
          -keystore "$TRUSTSTORE" \
          -storepass "$STOREPASS" \
          -noprompt
          
        if [ $? -eq 0 ]; then
          echo "✅ Certificate $cert_count imported successfully!"
        else
          echo "⚠️  Certificate $cert_count may already exist or failed to import"
        fi
        
        cert_count=$((cert_count + 1))
    fi
done

# Step 4: Verify installation
echo "🔍 Verifying certificate installation..."
keytool -list -keystore "$TRUSTSTORE" -storepass "$STOREPASS" | grep "$HOST"

if [ $? -eq 0 ]; then
    echo "✅ Certificate verification successful!"
else
    echo "❌ Certificate verification failed"
fi

# Cleanup
echo "🧹 Cleaning up temporary files..."
rm -f "$HOST-chain.pem" "$HOST-cert-"*

echo "🎉 Done! Try running Maven now."
