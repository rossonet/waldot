#!/bin/bash
# esporta la chiave privata come binario
gpg --export-secret-keys --armor 9F4C1BDA6A66AE29C038EBEE1267E539E981FA5A > /tmp/secret.asc
# esporta la chiave pubblica
gpg --export --armor 9F4C1BDA6A66AE29C038EBEE1267E539E981FA5A > /tmp/public.asc
echo "PRIVATE:"
echo "$(cat /tmp/secret.asc)"
echo "PUBLIC:"
echo "$(cat /tmp/public.asc)"
