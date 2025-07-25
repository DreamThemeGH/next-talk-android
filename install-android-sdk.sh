#!/bin/bash

# Android SDK Installer for Nextcloud Talk Enhanced Development
# Устанавливает минимально необходимые компоненты Android SDK

set -e

echo "🔧 Установка Android SDK для разработки Nextcloud Talk Enhanced"
echo "================================================================"

# Цвета
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
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

# Определяем архитектуру
ARCH=$(uname -m)
if [[ "$ARCH" == "x86_64" ]]; then
    SDK_ARCH="x86_64"
else
    SDK_ARCH="arm64"
fi

# Директории
ANDROID_HOME="$HOME/Android/Sdk"
CMDLINE_TOOLS_DIR="$ANDROID_HOME/cmdline-tools"
LATEST_DIR="$CMDLINE_TOOLS_DIR/latest"

print_step "Создание директорий..."
mkdir -p "$CMDLINE_TOOLS_DIR"

# Скачиваем command line tools
print_step "Скачивание Android Command Line Tools..."
cd "$CMDLINE_TOOLS_DIR"

if [[ "$ARCH" == "x86_64" ]]; then
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
else
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
fi

print_step "Распаковка command line tools..."
unzip -q cmdline-tools.zip
mv cmdline-tools latest

# Настройка переменных окружения
print_step "Настройка переменных окружения..."
echo "export ANDROID_HOME=\"$ANDROID_HOME\"" >> ~/.bashrc
echo "export ANDROID_SDK_ROOT=\"$ANDROID_HOME\"" >> ~/.bashrc
echo "export PATH=\"\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin\"" >> ~/.bashrc
echo "export PATH=\"\$PATH:\$ANDROID_HOME/platform-tools\"" >> ~/.bashrc

# Применяем переменные для текущей сессии
export ANDROID_HOME="$ANDROID_HOME"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
export PATH="$PATH:$ANDROID_HOME/platform-tools"

# Принимаем лицензии
print_step "Принятие лицензий Android SDK..."
yes | sdkmanager --licenses > /dev/null 2>&1

# Устанавливаем необходимые компоненты
print_step "Установка компонентов Android SDK..."
sdkmanager "platform-tools"
sdkmanager "platforms;android-35"
sdkmanager "platforms;android-34"  
sdkmanager "build-tools;35.0.0"
sdkmanager "build-tools;34.0.0"

# Устанавливаем эмулятор (опционально)
print_warning "Установка эмулятора (может занять время)..."
sdkmanager "emulator"
sdkmanager "system-images;android-35;google_apis;$SDK_ARCH"

print_success "Android SDK установлен!"
print_step "Перезагрузите терминал или выполните: source ~/.bashrc"

echo ""
print_step "Установленные компоненты:"
sdkmanager --list | grep -E "(platform|build-tools)" | grep "Installed"

echo ""
print_success "Готово! Теперь можно собирать Android приложения."
