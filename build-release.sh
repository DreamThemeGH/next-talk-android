#!/bin/bash

# Build script for Nextcloud Talk Enhanced
# Собирает release версию APK с подписью

set -e

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

function print_step() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

function print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

function print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

function print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "🚀 Building Nextcloud Talk Enhanced Release APK"
echo "================================================"

# Проверяем наличие ключа для подписи
KEYSTORE_FILE="talk-enhanced.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    print_warning "Keystore не найден. Создаем новый..."
    keytool -genkey -v -keystore "$KEYSTORE_FILE" \
        -alias talk-enhanced \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Talk Enhanced,OU=Development,O=DreamTheme,L=Unknown,S=Unknown,C=RU"
    print_success "Keystore создан: $KEYSTORE_FILE"
fi

# Очистка предыдущей сборки
print_step "Очистка предыдущих сборок..."
./gradlew clean

# Сборка release APK
print_step "Сборка release APK..."
./gradlew assembleGplayRelease

# Проверяем результат
if [ -f "app/build/outputs/apk/gplay/release/app-gplay-release.apk" ]; then
    APK_SIZE=$(du -h "app/build/outputs/apk/gplay/release/app-gplay-release.apk" | cut -f1)
    print_success "Release APK собран успешно!"
    print_step "Размер: $APK_SIZE"
    print_step "Путь: app/build/outputs/apk/gplay/release/app-gplay-release.apk"
    
    # Копируем в удобное место
    mkdir -p releases
    cp "app/build/outputs/apk/gplay/release/app-gplay-release.apk" "releases/NextcloudTalkEnhanced-$(date +%Y%m%d-%H%M).apk"
    print_success "APK скопирован в папку releases/"
else
    print_error "Сборка не удалась!"
    exit 1
fi

echo ""
print_success "Готово! Можно устанавливать APK на устройство."
