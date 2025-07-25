#!/bin/bash

# Проверка готовности к сборке Android проекта
# Check Android development environment

set -e

echo "🔍 Проверка Android развернутой среды"
echo "====================================="

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

function print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

function print_error() {
    echo -e "${RED}❌ $1${NC}"
}

function print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

function print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Проверка Java
print_info "Проверка Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    print_success "Java установлена: $JAVA_VERSION"
else
    print_error "Java не найдена! Установите OpenJDK 17 или новее"
    exit 1
fi

# Проверка Android Studio
print_info "Проверка Android Studio..."
if [ -d "/opt/android-studio" ] || [ -d "$HOME/android-studio" ] || command -v studio &> /dev/null; then
    print_success "Android Studio найдена"
else
    print_warning "Android Studio не найдена в стандартных путях"
fi

# Проверка ANDROID_HOME
print_info "Проверка ANDROID_HOME..."
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    print_success "ANDROID_HOME установлена: $ANDROID_HOME"
elif [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
    print_success "ANDROID_HOME найдена автоматически: $ANDROID_HOME"
else
    print_error "ANDROID_HOME не найдена! Настройте SDK"
    print_info "Попробуйте установить через Android Studio: Tools -> SDK Manager"
    exit 1
fi

# Проверка SDK components
print_info "Проверка компонентов Android SDK..."
if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    PLATFORMS=$(ls "$ANDROID_HOME/platforms" 2>/dev/null | grep "android-" | wc -l)
    BUILD_TOOLS=$(ls "$ANDROID_HOME/build-tools" 2>/dev/null | wc -l)
    
    print_success "Платформы: $PLATFORMS"
    print_success "Build Tools: $BUILD_TOOLS"
else
    print_warning "Command line tools не найдены"
fi

# Проверка проекта
print_info "Проверка проекта..."
PROJECT_DIR="/home/freya/DockerProjects/NextcloudAIO/NextFork/next-talk-android"

if [ -f "$PROJECT_DIR/build.gradle" ]; then
    print_success "Главный build.gradle найден"
else
    print_error "Главный build.gradle не найден!"
    exit 1
fi

if [ -f "$PROJECT_DIR/app/build.gradle" ]; then
    print_success "App build.gradle найден"
else
    print_error "App build.gradle не найден!"
    exit 1
fi

if [ -f "$PROJECT_DIR/gradlew" ]; then
    print_success "Gradle wrapper найден"
    chmod +x "$PROJECT_DIR/gradlew"
else
    print_error "Gradle wrapper не найден!"
    exit 1
fi

# Проверка разрешений gradlew
if [ -x "$PROJECT_DIR/gradlew" ]; then
    print_success "Gradle wrapper executable"
else
    print_warning "Настройка прав на gradlew..."
    chmod +x "$PROJECT_DIR/gradlew"
fi

echo ""
print_success "Среда готова для разработки!"
print_info "Следующие шаги:"
echo "  1. Открыть проект в Android Studio: File -> Open -> выбрать $PROJECT_DIR"
echo "  2. Дождаться синхронизации Gradle"
echo "  3. Выбрать flavor 'gplay' для сборки с push уведомлениями"
echo "  4. Собрать APK: Build -> Build Bundle(s) / APK(s) -> Build APK(s)"
echo ""
print_info "Или собрать через командную строку:"
echo "  cd $PROJECT_DIR"
echo "  ./gradlew assembleGplayDebug"
