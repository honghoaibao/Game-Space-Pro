#!/usr/bin/env bash
#
# Standalone tool: generates a release signing keystore for Game Space :
# AT TOOL, and (optionally) uploads the 4 secrets its GitHub Actions
# workflows expect (SIGNING_KEY_BASE64, SIGNING_STORE_PASSWORD,
# SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD) straight to the repo via the
# `gh` CLI. Does NOT need to be run from inside a clone of the project —
# just this one file, anywhere with a JDK (for `keytool`).
#
# Termux (Android): install the two things this needs first —
#   pkg install openjdk-17 gh
# Then run this script exactly the same way as on a desktop.
#
# Usage:
#   ./generate-release-keystore.sh
#   ./generate-release-keystore.sh --repo yourname/GameSpacePro
#   ./generate-release-keystore.sh --alias upload --validity-years 27 \
#       --output-dir ~/storage/downloads/game-space-signing
#
# Safety:
#   - The generated .jks file and its passwords are the ONLY way to publish
#     updates to this app under the same identity once it's distributed.
#     Losing it is permanent — there is no recovery. Back it up somewhere
#     durable (password manager, offline storage) OUTSIDE any git repo.
#   - Passwords are read once via a silent prompt (not echoed to the
#     terminal) and handed to keytool through environment variables
#     (-storepass:env / -keypass:env), never as plain command-line
#     arguments, so they don't show up in `ps` output or shell history.
#   - Whatever directory you point --output-dir at, make sure it's not
#     inside a git repo you might `git add .` carelessly later. The
#     default (./game-space-signing under the current directory) is fine
#     as long as you're not already inside a repo when you run this.

set -euo pipefail

ALIAS="upload"
VALIDITY_YEARS="27"
NON_INTERACTIVE="false"
OUTPUT_DIR="$(pwd)/game-space-signing"
REPO=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --alias) ALIAS="$2"; shift 2 ;;
    --validity-years) VALIDITY_YEARS="$2"; shift 2 ;;
    --non-interactive) NON_INTERACTIVE="true"; shift ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    --repo) REPO="$2"; shift 2 ;;
    -h|--help)
      awk '/^#!/{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
      exit 0
      ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

is_termux="false"
if [[ -n "${PREFIX:-}" && "$PREFIX" == *com.termux* ]]; then
  is_termux="true"
fi

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool not found — it ships with any JDK." >&2
  if [[ "$is_termux" == "true" ]]; then
    echo "On Termux: pkg install openjdk-17" >&2
  else
    echo "Install one (e.g. Temurin 21) and make sure it's on PATH." >&2
  fi
  exit 1
fi

KEYSTORE_PATH="$OUTPUT_DIR/release.jks"
BASE64_PATH="$OUTPUT_DIR/release.jks.base64"

mkdir -p "$OUTPUT_DIR"

if [[ -f "$KEYSTORE_PATH" ]]; then
  echo "A keystore already exists at $KEYSTORE_PATH."
  echo "Delete it first if you really want to generate a new one — doing so"
  echo "invalidates the ability to update any app already published with it."
  exit 1
fi

echo "This will create a release keystore at:"
echo "  $KEYSTORE_PATH"
echo "(alias: $ALIAS, valid ${VALIDITY_YEARS} years)"
echo
echo "You'll be asked for ONE password now, used for both the keystore and"
echo "the key inside it. Write it down somewhere safe — it is not"
echo "recoverable if lost, and losing it means this app can never be"
echo "updated under the same signing identity again."
echo

read -rsp "Choose a password: " STORE_PASSWORD
echo
read -rsp "Confirm password: " STORE_PASSWORD_CONFIRM
echo

if [[ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]]; then
  echo "Passwords didn't match. Nothing was created — run the script again." >&2
  exit 1
fi

if [[ ${#STORE_PASSWORD} -lt 6 ]]; then
  echo "keytool requires at least 6 characters. Nothing was created — run the script again." >&2
  exit 1
fi

export STORE_PASSWORD
KEY_PASSWORD="$STORE_PASSWORD"
export KEY_PASSWORD

# -storepass:env / -keypass:env (rather than letting keytool prompt
# interactively) avoids a real issue found while testing this script:
# keytool, as a separate JVM process, buffers its stdin reads in a way
# that can silently consume more of a redirected/piped stream than the
# prompts it actually shows use — leaving too little for this script's
# own `read` calls afterward in non-TTY contexts. Passing passwords via
# already-set env vars sidesteps the shared-stdin issue entirely, and
# also means the password only needs to be typed once instead of twice.
keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_PATH" \
  -storetype PKCS12 \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$((VALIDITY_YEARS * 365))" \
  -dname "CN=Game Space AT TOOL, OU=, O=, L=, S=, C=VN" \
  -storepass:env STORE_PASSWORD \
  -keypass:env KEY_PASSWORD

base64 -w 0 "$KEYSTORE_PATH" > "$BASE64_PATH"
echo
echo "Base64-encoded keystore written to $BASE64_PATH."

gh_available="false"
if command -v gh >/dev/null 2>&1; then
  gh_available="true"
elif [[ "$is_termux" == "true" ]]; then
  echo
  echo "gh CLI not found. On Termux: pkg install gh"
fi

gh_authenticated="false"
if [[ "$gh_available" == "true" ]] && gh auth status >/dev/null 2>&1; then
  gh_authenticated="true"
elif [[ "$gh_available" == "true" ]]; then
  echo
  echo "gh CLI found but not authenticated. Run 'gh auth login' first —"
  echo "the device-code flow works fine on Termux (it just needs a"
  echo "browser somewhere, even on a different device, to enter the code)."
fi

if [[ "$gh_authenticated" == "true" ]]; then
  GH_REPO_FLAG=()
  if [[ -n "$REPO" ]]; then
    GH_REPO_FLAG=(--repo "$REPO")
  fi

  if [[ "$NON_INTERACTIVE" == "true" ]]; then
    SET_SECRETS="y"
  else
    read -rp "Set the 4 GitHub Actions secrets now${REPO:+ on $REPO}? [y/N] " SET_SECRETS
  fi

  if [[ "${SET_SECRETS,,}" == "y" ]]; then
    gh secret set SIGNING_KEY_BASE64 "${GH_REPO_FLAG[@]}" < "$BASE64_PATH"
    gh secret set SIGNING_STORE_PASSWORD "${GH_REPO_FLAG[@]}" --body "$STORE_PASSWORD"
    gh secret set SIGNING_KEY_ALIAS "${GH_REPO_FLAG[@]}" --body "$ALIAS"
    gh secret set SIGNING_KEY_PASSWORD "${GH_REPO_FLAG[@]}" --body "$KEY_PASSWORD"
    echo "Secrets set. Re-run the Android CI workflow (or push a tag for a release build) to pick them up."
  else
    echo "Skipped. Set the secrets manually — see below."
  fi
fi

cat <<EOF

Manual setup (Settings -> Secrets and variables -> Actions -> New repository secret),
if you didn't use gh above:
  SIGNING_KEY_BASE64      <- contents of $BASE64_PATH
  SIGNING_STORE_PASSWORD  <- the password you just entered
  SIGNING_KEY_ALIAS       <- $ALIAS
  SIGNING_KEY_PASSWORD    <- the same password (store and key password match)

$KEYSTORE_PATH and $BASE64_PATH were NOT added to any git repo by this
script. Back up $KEYSTORE_PATH itself somewhere durable and outside any
repo — the base64 file alone is enough for CI, but keep the original .jks
too, and make sure this whole output directory never ends up in a commit.
EOF
