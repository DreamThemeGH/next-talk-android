#!/bin/bash

# Nextcloud Talk Enhanced - Build Script
# Скрипт для сборки APK с улучшенными уведомлениями

set -e

echo "🚀 Nextcloud Talk Enhanced Build Script"
echo "======================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# Проверяем, что мы в правильной директории
if [ ! -f "gradlew" ]; then
    print_error "gradlew не найден! Убедитесь, что вы находитесь в корне проекта."
    exit 1
fi

# Параметры
BUILD_TYPE=${1:-debug}
FLAVOR=${2:-generic}

print_step "Конфигурация сборки:"
echo "  Build Type: $BUILD_TYPE"
echo "  Flavor: $FLAVOR"
echo ""

# Очистка предыдущей сборки
print_step "Очистка предыдущей сборки..."
./gradlew clean --no-daemon

# Проверка синтаксиса и линтинг
print_step "Проверка синтаксиса с помощью detekt..."
./gradlew detekt --no-daemon || print_warning "Detekt нашел предупреждения, но продолжаем сборку"

# Сборка APK
print_step "Сборка APK..."
case $BUILD_TYPE in
    "debug")
        print_step "Сборка Debug APK..."
        ./gradlew assemble${FLAVOR^}Debug --no-daemon
        APK_PATH="app/build/outputs/apk/$FLAVOR/debug/app-$FLAVOR-debug.apk"
        ;;
    "release")
        print_step "Сборка Release APK..."
        ./gradlew assemble${FLAVOR^}Release --no-daemon
        APK_PATH="app/build/outputs/apk/$FLAVOR/release/app-$FLAVOR-release-unsigned.apk"
        ;;
    *)
        print_error "Неизвестный тип сборки: $BUILD_TYPE. Используйте 'debug' или 'release'"
        exit 1
        ;;
esac

# Проверяем, что APK создался
if [ -f "$APK_PATH" ]; then
    print_success "APK успешно собран!"
    echo "  Путь: $APK_PATH"
    
    # Показываем информацию о APK
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_step "Размер APK: $APK_SIZE"
    
    # Создаем символическую ссылку для удобства
    ln -sf "$APK_PATH" "nextcloud-talk-enhanced-$BUILD_TYPE.apk"
    print_success "Создана ссылка: nextcloud-talk-enhanced-$BUILD_TYPE.apk"
    
else
    print_error "APK не найден по пути: $APK_PATH"
    print_error "Что-то пошло не так во время сборки."
    exit 1
fi

# Если это debug сборка, показываем команду для установки
if [ "$BUILD_TYPE" = "debug" ]; then
    echo ""
    print_step "Для установки на устройство выполните:"
    echo "  adb install -r nextcloud-talk-enhanced-debug.apk"
fi

echo ""
print_success "Сборка завершена успешно! 🎉"
